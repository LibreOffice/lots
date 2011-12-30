/*
 * Dateiname: WollMuxStringReplacer.java
 * Projekt  : n/a
 * Funktion : Tool zum Ersetzen der Vorkommen von Strings innerhalb des WollMux
 *            Source Codes.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330.html
 *
 * @author Daniel Benkmann
 * 
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dieses Tool ersetzt alle Vorkommen des übergebenen Strings in den Ausgabe-Strings
 * des WollMux Source Codes durch einen anderen String. Dabei wird auf dieselbe Logik
 * zurückgegriffen, die auch für den LocalizationUpdater des WollMux verwendet wird.
 * Das heißt, es werden beim Ersetzen nur Strings berücksichtigt, die im Quelltext
 * innerhalb eines L.m-Ausdrucks stehen.
 * 
 * Der Einfachheit halber findet der WollMuxStringReplacer auch L.m-Ausdrücke in
 * auskommentierten Code-Stellen. Eine Erkennung und Ausfilterung von Kommentaren
 * wäre zu aufwändig und würde den Code unnötig verkomplizieren.
 * 
 * @author Daniel Benkmann (Code basiert zum Teil auf LocalizationUpdater von
 *         Christoph Lutz)
 */
public class WollMuxStringReplacer
{
  /**
   * Das Wurzelverzeichnis der Source-Dateien, die bearbeitet werden sollen. Der Wert
   * ist derzeit hardcodiert und geht davon aus, dass der WollMuxStringReplacer aus
   * dem /misc-Ordner des WollMux-Projektverzeichnisses aufgerufen wird.
   */
  private static File sourcesDir = new File("../src/");

  /**
   * Pattern zum Beschreiben eines Strings im WollMux-Quelltext. In Gruppe 1 wird der
   * Stringinhalt zurückgeliefert (ohne die Anführungszeichen, aber nach wie vor in
   * Java-Syntax escaped). Ein einzelner String in Java kann sich niemals über
   * mehrere Codezeilen erstrecken (das wäre syntaktisch falsch). In auskommentierten
   * Codezeilen kann jedoch ein String ungünstig über mehrere Codezeilen umgebrochen
   * werden. Der Ausschluss der Zeichen \n\r verhindert daher die Erkennung von
   * ungültigen Strings.
   */
  private static Pattern STRING = Pattern.compile("\"((?:\\\\\"|[^\"\r\n])*)\"");

  /**
   * Pattern, mit dem nach L.m(STRING[, args])-Ausdrücken gesucht wird. In Gruppe 1
   * wird der um Leerzeichen getrimmte {@link WollMuxStringReplacer#STRING}-Ausdruck
   * zurückgeliefert. STRING-Ausdrücke können dabei auch mit "+" verkettete Strings
   * sein.
   */
  private static Pattern L_M = Pattern.compile("L.m\\(\\s*(" + STRING
    + "(?:\\s*\\+\\s*" + STRING + ")*)\\s*[\\),]");

  /**
   * Die main-Methode, die das Ersetzen von Strings im Quelltext startet. Dabei wird
   * als erstes Argument der String übergeben, der ersetzt werden soll (z.B.
   * "WollMux") und als zweites Argument der String, durch den der erste String
   * ersetzt werden soll (z.B. "SuperOfficeTool").
   * 
   * Es wird davon ausgegangen, dass die Methode aus dem /misc-Unterverzeichnis des
   * WollMux-Projektverzeichnisses gestartet wird (siehe auch
   * {@link WollMuxStringReplacer#sourcesDir}).
   * 
   * ACHTUNG: Es wird NICHT automatisch ein Backup der veränderten Quelltext-Dateien
   * angelegt, sondern sie werden direkt überschrieben! Bitte selbst sicherstellen,
   * dass vorher ein Backup des src-Verzeichnisses angelegt wurde bzw. alle etwaigen
   * Code-Änderungen vorher ins Repository eingecheckt wurden, so dass die Dateien im
   * Zweifelsfall wiederhergestellt werden können.
   * 
   * @param args
   *          als einziges Argument wird der neue Name für den WollMux übergeben
   * 
   * @author Daniel Benkmann
   */
  public static void main(String[] args)
  {
    if (args.length != 2)
    {
      System.out.println("Diesem Programm muessen genau zwei String-Argumente uebergeben werden!");
      System.out.println("1. Argument: zu ersetzender String; 2. Argument: neuer String");
      System.out.println("Beispielaufruf: java WollMuxStringReplacer \"WollMux\" \"SuperOfficeTool\"");
      System.out.println("(Anfuehrungszeichen optional, sofern die Strings keine Whitespaces enthalten.)");
      System.out.println("Programmausfuehrung wird jetzt abgebrochen.");
      System.exit(-1);
    }
    String toReplace = args[0];
    String replacement = args[1];
    replaceStringInSources(toReplace, replacement);
  }

  /**
   * Macht die eigentliche Arbeit.
   * 
   * @author Daniel Benkmann
   */
  private static void replaceStringInSources(String toReplace, String replacement)
  {
    System.out.println("Ersetze \"" + toReplace + "\" durch \"" + replacement + "\"");

    String literalReplacement = Matcher.quoteReplacement(replacement);
    Pattern toReplacePattern = Pattern.compile(Pattern.quote(toReplace));

    // Alle .java Files aus sourcesDir iterieren und L.m()s rausziehen
    List<File> sources = new ArrayList<File>();
    addJavaFilesRecursive(sources, sourcesDir);
    for (Iterator<File> iter = sources.iterator(); iter.hasNext();)
    {
      File file = iter.next();
      String sourceCode = readFile(file, "UTF-8");
      StringBuffer replacementSourceCode = new StringBuffer();
      Matcher m = L_M.matcher(sourceCode);
      while (m.find())
      {
        String lmComplete = m.group(0);
        String lmFirstArg = m.group(1);
        StringBuffer concatinatedStrings = new StringBuffer();
        Matcher strings = STRING.matcher(lmFirstArg);
        while (strings.find())
        {
          concatinatedStrings.append(strings.group(1));
        }
        StringBuffer sb = new StringBuffer();
        Matcher names = toReplacePattern.matcher(concatinatedStrings);
        while (names.find())
        {
          names.appendReplacement(sb, literalReplacement);
        }
        names.appendTail(sb);
        String lmNameReplaced =
          lmComplete.replaceAll(Pattern.quote(lmFirstArg),
            Matcher.quoteReplacement("\"" + sb.toString() + "\""));
        m.appendReplacement(replacementSourceCode,
          Matcher.quoteReplacement(lmNameReplaced));
      }
      m.appendTail(replacementSourceCode);

      try
      {
        BufferedWriter out =
          new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
            "UTF-8"));
        out.write(replacementSourceCode.toString());
        out.close();
      }
      catch (IOException e)
      {
        System.err.println("Error while writing file " + file.getName() + ":");
        e.printStackTrace();
      }
    }

  }

  /**
   * Liefert den kompletten Inhalt der mit encoding encodierten Datei file als String
   * zurück.
   * 
   * @param file
   * @throws FileNotFoundException
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static String readFile(File file, String encoding)
  {
    StringBuffer str = new StringBuffer();
    try
    {
      InputStreamReader r =
        new InputStreamReader(new FileInputStream(file), encoding);
      char[] buff = new char[1024];
      int count;
      while ((count = r.read(buff)) > 0)
      {
        str.append(new String(buff, 0, count));
      }
      r.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return str.toString();
  }

  /**
   * Fügt alle Dateien, die mit .java enden aus diesem Verzeichnis fileOrDir und aus
   * allen Unterverzeichnissen zur Liste l hinzu.
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static void addJavaFilesRecursive(List<File> l, File fileOrDir)
  {
    if (fileOrDir.isFile() && fileOrDir.getName().endsWith(".java"))
    {
      l.add(fileOrDir);
      return;
    }

    if (fileOrDir.isDirectory())
    {
      File[] files = fileOrDir.listFiles();
      for (int i = 0; i < files.length; i++)
      {
        addJavaFilesRecursive(l, files[i]);
      }
    }
  }
}
