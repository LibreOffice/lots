package de.muenchen.allg.itd51.wollmux.func;

import java.io.File;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;

import java.util.regex.Matcher;
import java.net.MalformedURLException;
import java.net.URL;

public class Dateinamensanpassungen
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(Dateinamensanpassungen.class);

  private static final Pattern PROP = Pattern.compile("\\$\\{([^\\}]+)\\}");

  private Dateinamensanpassungen()
  {
  }

  /**
   * Dieser Funktion kann eine durch Pipe ('|') getrennte Liste mit
   * Pfaden/Dateinamen �bergeben werden, wovon der erste Eintrag dieser Liste
   * zur�ckgegeben wird, dessen Pfad-Anteil tats�chlich verf�gbar ist.
   * Innerhalb eines Pfades/Dateinamens kann vor der Verf�gbarkeitspr�fung mit
   * ${<name>} der Wert einer Java-Systemproperty in den Dateinamen eingef�gt
   * werden.
   */
  public static String verfuegbarenPfadVerwenden(String fileName)
  {
    String[] paths = fileName.split("\\s*\\|\\s*");
    String first = null;
    for (String p : paths)
    {
      // alle ${<prop>} durch evaluierten Inhalt ersetzen
      Matcher m = PROP.matcher(p);
      StringBuffer buf = new StringBuffer();
      while (m.find())
      {
        String propVal = System.getProperty(m.group(1).trim());
        if (propVal == null)
          propVal = "";
        m.appendReplacement(buf, propVal);
      }
      m.appendTail(buf);

      if (first == null)
        first = buf.toString();

      File f = new File(buf.toString());
      File parent = f.getParentFile();
      if (parent != null && parent.isDirectory())
        return f.toString();
    }
    if (first == null)
      first = paths[0];
    return new File(first).getName();
  }

  /**
   * Arbeitet wie
   * {@link Dateinamensanpassungen#verfuegbarenPfadVerwenden(String)} und
   * nimmt zus�tzlich die folgenden LHM-spezifischen Dateinamensanpassungen
   * vor:
   * 
   * a. Substituiert werden � in ss � in ae � in oe � in ue, � in Ae, � in �e,
   * � in Oe
   * 
   * b. Alle Sonderzeichen, Satzzeichen etc. sollen durch _ ersetzt werden,
   * au�er dem Punkt vor der Dateiendung (.odt)
   * 
   * c. Damit sind im Dateinamen nur noch die Zahlen von 0-9, die Buchstaben
   * von a-z und A-Z und der Unterstrich _ vorhanden
   * 
   * d. Die L�nge des Dateinamens wird auf maximal 240 Zeichen (inkl. Pfad)
   * begrenzt; ist der ermittelte Dateiname l�nger, so wird er nach 240
   * Zeichen abgeschnitten (genau genommen wird nach 236 Zeichen abgeschnitten
   * und dann wird die Endung .odt angeh�ngt).
   * 
   * Arbeitsverzeichnispfad in LibreOffice wird an Dateiname angehängt, falls spezifizierte Dateiname nicht absolut ist.
   */
  public static String lhmDateinamensanpassung(String fileName)
  {
    File f = new File(fileName);
    if (!f.isAbsolute())
    {
      try
      {
//      holt das Arbeitsverzeichnispfad aus LO  
        Object ps = UNO.createUNOService("com.sun.star.util.PathSettings");
        URL dir = new URL(AnyConverter.toString(UNO.getProperty(ps, "Work")));
        f = new File(dir.getPath(), fileName);
      } catch (com.sun.star.lang.IllegalArgumentException
          | MalformedURLException e)
      {
        LOGGER.error("", e);
      } catch (UnoHelperException e)
      {
        LOGGER.error(".", e);
      }
    }
    String pfad = verfuegbarenPfadVerwenden(f.getAbsolutePath());
    File file = new File(pfad);
    int parentLength = 0;
    if (file.getParent() != null)
      parentLength = file.getParent().length() + 1;

    String name = file.getName();
    String suffix = "";
    int idx = name.lastIndexOf('.');
    if (idx >= 0)
    {
      suffix = name.substring(idx);
      if (suffix.matches("\\.\\w{3,4}"))
        name = name.substring(0, idx);
      else
        suffix = "";
    }

    name = name.replaceAll("�", "ss");
    name = name.replaceAll("�", "ae");
    name = name.replaceAll("�", "oe");
    name = name.replaceAll("�", "ue");
    name = name.replaceAll("�", "Ae");
    name = name.replaceAll("�", "Oe");
    name = name.replaceAll("�", "Ue");
    name = name.replaceAll("[^a-zA-Z_0-9]", "_");

    int maxlength = 240 - suffix.length() - parentLength;
    if (name.length() > maxlength)
      name = name.substring(0, maxlength);

    name = name + suffix;

    file = new File(file.getParentFile(), name);
    return file.toString();
  }
}
