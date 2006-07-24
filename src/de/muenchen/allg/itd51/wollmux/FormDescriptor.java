/*
 * Dateiname: FormDescriptor.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert die Formularbeschreibung eines Formulars in Form
 *            von ein bis mehreren WM(CMD'Form')-Kommandos mit den zugehörigen Notizen.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 24.07.2006 | LUT | Erstellung als FormDescriptor
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.sun.star.container.XEnumeration;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;

/**
 * Diese Klasse repräsentiert eine Formularbeschreibung eines Formulardokuments
 * in Form eines oder mehrerer WM(CMD'Form')-Kommandos mit den zugehörigen
 * Notizfeldern, die die Beschreibungstexte in ConfigThingy-Syntax enthalten.
 * Die Klasse startet zunächst als ein leerer Container für
 * DocumentCommand.Form-Objekte, in den über die add()-Methode einzelne
 * DocumentCommand.Form-Objekte hinzugefügt werden können. Logisch betrachtet
 * werden alle Beschreibungstexte zu einer großen ConfigThingy-Struktur
 * zusammengefügt und über die Methode toConfigThingy() bereitgestellt.
 * 
 * Die Klasse bietet darüber hinaus Methoden zum Abspeichern und Auslesen der
 * original-Feldwerte im Notizfeld des ersten DocumentCommand.Form-Objekts an.
 * 
 * Im Zusammenhang mit der EntwicklerGUI können auch alle Operationen der
 * EntwicklerGUI an der Formularbeschreibung (Hinzufügen/Löschen/Verschieben von
 * Eingabeelementen) über diese Klasse abstrahiert werden.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class FormDescriptor
{
  /**
   * Enthält alle WM(CMD'Form')-Kommandos dieser Formularbeschreibung.
   */
  private Vector formCmds;

  /**
   * Enthält ein Mapping formCmd --> Notizfeld (annotation)
   */
  private HashMap annotationFields;

  /**
   * Enthält ein Mapping formCmd --> geparste ConfigThingy-Objekte
   */
  private HashMap configs;

  /**
   * Erzeugt einen neuen leeren FormDescriptor, dem über add()
   * WM(CMD'Form')-Kommandos mit Formularbeschreibungsnotizen hinzugefügt werden
   * können.
   */
  public FormDescriptor()
  {
    formCmds = new Vector();
    annotationFields = new HashMap();
    configs = new HashMap();
  }

  /**
   * Die Methode fügt die Formularbeschreibung, die unterhalb der Notiz des
   * WM(CMD'Form')-Kommandos gefunden wird zur Gesamtformularbeschreibung hinzu.
   * 
   * @param formCmd
   *          Das formCmd, das die Notzi mit der hinzuzufügenden
   *          Formularbeschreibung enthält.
   * @throws ConfigurationErrorException
   *           Die Notiz der Formularbeschreibung ist nicht vorhanden, die
   *           Formularbeschreibung ist nicht vollständig oder kann nicht
   *           geparst werden.
   */
  public void add(DocumentCommand.Form formCmd)
      throws ConfigurationErrorException
  {
    XTextRange range = formCmd.getTextRange();
    if (range != null)
    {
      Object cursor = range.getText().createTextCursorByRange(range);

      Object annotationField = findAnnotationFieldRecursive(cursor);
      if (annotationField == null)
        throw new ConfigurationErrorException(
            "Die Notiz mit der Formularbeschreibung fehlt.");

      Object content = UNO.getProperty(annotationField, "Content");
      if (content != null)
      {
        ConfigThingy conf;
        try
        {
          conf = new ConfigThingy("", null,
              new StringReader(content.toString()));
        }
        catch (java.lang.Exception e)
        {
          throw new ConfigurationErrorException(
              "Die Formularbeschreibung innerhalb der Notiz ist fehlerhaft:\n"
                  + e.getMessage());
        }

        ConfigThingy formulars = conf.query("Formular");
        if (formulars.count() == 0)
          throw new ConfigurationErrorException(
              "Die Formularbeschreibung innerhalb der Notiz enthält keinen Abschnitt \"Formular\".");

        formCmds.add(formCmd);
        annotationFields.put(formCmd, annotationField);
        configs.put(formCmd, conf);
      }
    }
  }

  /**
   * Liefert eine ConfigThingy-Repräsentation, die unterhalb des Wurzelknotens
   * "WM" der Reihe nach die Vereinigung der "Formular"-Abschnitte aller
   * Formularbeschreibungen der enthaltenen WM(CMD'Form')-Kommandos enthält.
   * 
   * @return ConfigThingy-Repräsentation mit dem Wurzelknoten "WM", die alle
   *         "Formular"-Abschnitte der Formularbeschreibungen enthält.
   */
  public ConfigThingy toConfigThingy()
  {
    ConfigThingy formDescriptors = new ConfigThingy("WM");

    Iterator cmds = formCmds.iterator();
    while (cmds.hasNext())
    {
      ConfigThingy conf = (ConfigThingy) configs.get(cmds.next());
      if (conf != null) formDescriptors.addChild(conf);
    }
    return formDescriptors;
  }

  // Helper-Methoden:

  /**
   * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
   * Interface rekursiv nach TextField.Annotation-Objekten und liefert das erste
   * gefundene TextField.Annotation-Objekt zurück, oder null, falls kein
   * entsprechendes Element gefunden wurde.
   * 
   * @param element
   *          Das erste gefundene InputField.
   */
  private XTextField findAnnotationFieldRecursive(Object element)
  {
    // zuerst die Kinder durchsuchen (falls vorhanden):
    if (UNO.XEnumerationAccess(element) != null)
    {
      XEnumeration xEnum = UNO.XEnumerationAccess(element).createEnumeration();

      while (xEnum.hasMoreElements())
      {
        try
        {
          Object child = xEnum.nextElement();
          XTextField found = findAnnotationFieldRecursive(child);
          // das erste gefundene Element zurückliefern.
          if (found != null) return found;
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    // jetzt noch schauen, ob es sich bei dem Element um eine Annotation
    // handelt:
    if (UNO.XTextField(element) != null)
    {
      Object textField = UNO.getProperty(element, "TextField");
      if (UNO.supportsService(
          textField,
          "com.sun.star.text.TextField.Annotation"))
      {
        return UNO.XTextField(textField);
      }
    }

    return null;
  }

}
