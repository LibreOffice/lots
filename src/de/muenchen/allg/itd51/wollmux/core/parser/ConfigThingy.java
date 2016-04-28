/*
 * Dateiname: ConfigThingy.java
 * Projekt  : WollMux
 * Funktion : Parsen und Repräsentation von WollMux-Konfigurationsdateien
 * 
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 05.10.2005 | BNK | Erstellung
 * 10.10.2005 | BNK | Parser fertiggestellt
 *                  | InputStream -> URL, Unterstützung von includes
 *                  | alles ab # ist jetzt Kommentar
 *                  | Testprogramm hinzugefügt
 * 11.10.2005 | BNK | Einbetten von Zeilenumbrüchen mittels %n
 *                  | Ausführlich kommentiert
 * 12.10.2005 | BNK | get() benennt jetzt Ergebnisknoten "<query results>"
 * 13.10.2005 | BNK | Der InputStream von dem die Daten gelesen werden ist
 *                  | jetzt unabhängig von der Kontext-URL.
 *                  | '..' Strings sind jetzt auch erlaubt
 * 13.10.2005 | BNK | Von InputStream auf Reader umgestellt.                  
 * 13.10.2005 | BNK | +query(), +queryByChild()
 * 13.10.2005 | BNK | +getNodesVisibleAt()
 * 13.10.2005 | BNK | public-Version von getNodesVisibleAt() mit etwas passenderer Signatur
 * 13.10.2005 | BNK | getNodesVisibleAt() -> static
 * 14.10.2005 | BNK | get() und getByChild() auf Werfen von NodeNotFoundException umgestellt.
 * 14.10.2005 | BNK | besserer Exception-Text
 * 14.10.2005 | BNK | +getFirstChild() und getLastChild() 
 * 02.11.2005 | BNK | Streams nach Lesen schliessen.
 * 02.11.2005 | BNK | UTF8 beim Lesen annehmen
 *                  | +stringRepresentation()
 *                  | +add(childName)
 * 03.11.2005 | BNK | +stringRepresentation(chilrenOnly, stringChar)
 *                  | Default stringChar ist jetzt wieder "
 *                  | +Warnung, dass mittels add(String) ConfigThingys produziert
 *                  | werden können, die den Syntaxregeln widersprechen.
 * 03.11.2005 | BNK | BOM (\uFEFF) auf Whitespace-Liste gesetzt 
 * 07.11.2005 | BNK | +setName()
 * 06.02.2006 | BNK | addChild() public
 * 15.02.2005 | BNK | leere %include URLs abgefangen
 * 22.03.2006 | BNK | urlEncodierung von nicht-ASCII-Zeichen in %include URLs. (R1360)
 * 24.03.2006 | BNK | urlEncodierung nur noch von in URLs nicht erlaubten Zeichen (R1377)
 * 22.05.2006 | BNK | get und Konsorten erlauben jetzt ein maxlevel Argument
 * 13.11.2006 | BNK | %uXXXX Syntax wird verstanden und stringRepresentation(..,..,..) erzeugt sie.
 * 14.11.2006 | BNK | %-Escapes am Anfang eines Strings werden korrekt geparst.
 * 15.06.2007 | BNK | urlEncode() Sonderbehandlung für Leerzeichen ("%20" statt "+")
 *                  | urlEncode() public gemacht. Nicht wirklich schöne Lösung, aber mei.
 * 02.08.2007 | BNK | +ConfigThingy(String, String)       
 * 07.08.2007 | BNK | Bei Syntaxfehlern im Exceptiontext den beanstandeten Text angeben.
 * 10.08.2007 | BNK | Fehler bei der stringRepresentation() von Listen mit nur einem Element behoben.
 * 25.02.2008 | BNK | ConfigThingy generisiert
 * 12.06.2009 | BED | get, query und queryByChild um jeweils eine Version mit minlevel Argument erweitert
 * 19.08.2009 | BNK | [R52737]FIXED: Lange Strings führen zu StackOverflowError in ConfigThingy
 * 11.01.2010 | BED | [R67584] +trimConfigThingy() zur Speicheroptimierung
 * 23.04.2010 | BED | Non-breaking spaces werden auch als Whitespace erkannt
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ein ConfigThingy repräsentiert einen Knoten eines Baumes, der durch das Parsen
 * einer WollMux-Konfigurationsdatei entsteht.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ConfigThingy implements Iterable<ConfigThingy>
{
  /**
   * Wegen JIT bug http://bugs.sun.com/view_bug.do?bug_id=6196102, den die
   * hirnverbrannten Idioten von Sun seit 2004 nicht behoben haben, weil es Ihnen
   * egal ist, ob irgendwo geschäftskritische Anwendungen abschmieren und Entwickler
   * viel Zeit investieren um Heisenbugs zu jagen, und überhaupt, wer arbeitet denn
   * schon mit so großen Zahlen. Wenn viele amerikanische Teenager nicht bis 3 zählen
   * können, kann man doch nicht erwarten, dass Java zuverlässig weiß dass die Zahl 2
   * kleiner ist als 2147483647 und nicht größer. Computer sind ja auch nur Menschen
   * und können bei so großen Zahlen schon mal einen Fehler machen.
   */
  private static final int INTEGER_MAX = Integer.MAX_VALUE - 1;

  /**
   * Der Name des Zeichensatzes, in dem ConfigThingy-Dateien gespeichert werden.
   */
  public static final String CHARSET = "UTF-8";

  /**
   * Pattern für Unicode Buchstaben und Ziffern.
   */
  private static final Pattern NON_LETTER_OR_DIGITS =
    Pattern.compile("\\P{javaLetterOrDigit}");

  /**
   * Pattern für Zeichen, die in ConfigThingys escapet werden müssen.
   */
  private static final Pattern CONFIGTHINGY_SPECIAL = Pattern.compile("[%\n\r\"']");

  /**
   * Einrückung für stringRepresentation().
   */
  private static final String INDENT = "  ";

  /**
   * Default-Wert für den minlevel-Parameter, falls die get- bzw. query-Methoden ohne
   * explizite Angabe von minlevel verwendet werden. Suchtiefe 1 bedeutet, dass this
   * von der Suche ausgenommen ist und nur nach Nachfahrenknoten gesucht wird.
   */
  private static final int DEFAULT_MINLEVEL = 1;

  /** Die Kindknoten. */
  private List<ConfigThingy> children;

  /** Der Name des Knotens. Bei Blättern ist dies der (String-)Wert des Knotens. */
  private String name;

  /**
   * Parst die Daten aus der Datei die durch url bestimmt wird.
   * 
   * @param name
   *          der Name der Wurzel des erzeugten ConfigThingy-Baumes.
   * @throws IOException
   *           falls das Laden von Daten von url (oder einer includeten URL)
   *           fehlschlägt.
   * @throws SyntaxErrorException
   * @throws SyntaxErrorException
   *           falls beim Parsen der Daten von url ein syntaktischer Fehler gefunden
   *           wird.
   */
  public ConfigThingy(String name, URL url) throws IOException, SyntaxErrorException
  {
    this(name);
    childrenFromUrl(url, new InputStreamReader(url.openStream(), CHARSET));
  }

  /**
   * Parst den String confString im Kontext der null URL (d,h, includes mit relativen
   * URLs dürfen in confString nicht vorkommen).
   * 
   * @param name
   *          der Name der Wurzel des erzeugten ConfigThingy-Baumes.
   * @throws IOException
   *           falls das Laden vonr einer includeten URL fehlschlägt.
   * @throws SyntaxErrorException
   *           falls beim Parsen der Daten ein syntaktischer Fehler gefunden wird.
   */
  public ConfigThingy(String name, String confString) throws IOException,
      SyntaxErrorException
  {
    this(name, null, new StringReader(confString));
  }

  /**
   * Parst die Daten aus read im Kontext der URL url. read wird sowohl im Fehlerfalle
   * als auch nach dem erfolgreichen Einlesen geschlossen.
   * 
   * @param name
   *          der Name der Wurzel des erzeugten ConfigThingy-Baumes.
   * @throws IOException
   *           falls das Laden von Daten von url (oder einer includeten URL)
   *           fehlschlägt.
   * @throws SyntaxErrorException
   *           falls beim Parsen der Daten von url ein syntaktischer Fehler gefunden
   *           wird.
   */
  public ConfigThingy(String name, URL url, Reader read) throws IOException,
      SyntaxErrorException
  {
    this(name);
    childrenFromUrl(url, read);
  }

  /**
   * Copy Constructor (deep copy).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public ConfigThingy(ConfigThingy conf)
  {
    this(conf.getName());
    this.addChildCopiesFrom(conf);
  }

  /**
   * Erzeugt ein ConfigThingy mit Name/Wert name, ohne Kinder. Achtung! Mit dieser
   * Methode ist es möglich, ConfigThingys zu erzeugen, die sich nicht an die
   * Syntaxbeschränkungen des Parsers für Schlüssel halten. Wird so ein ConfigThingy
   * mittels stringRepresentation() in Text konvertiert, entsteht etwas, das der
   * Parser nicht wieder einlesen kann.
   */
  public ConfigThingy(String name)
  {
    this.name = name;
    this.children = new Vector<ConfigThingy>(1);
  }

  /**
   * Erzeugt ein anonymes ConfigThingy mit Kindern aus children.
   */
  private ConfigThingy(String name, List<ConfigThingy> children)
  {
    this.name = name;
    this.children = children;
  }

  /**
   * Fügt diesem ConfigThingy unabhängige Kopien aller Kinder von conf hinzu. Die
   * Kinder werden samt aller Nachfahren kopiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void addChildCopiesFrom(ConfigThingy conf)
  {
    for (ConfigThingy childToCopy : conf)
    {
      ConfigThingy childCopy = new ConfigThingy(childToCopy.getName());
      childCopy.addChildCopiesFrom(childToCopy);
      this.addChild(childCopy);
    }
  }

  /**
   * Parst die Daten aus read im Kontext von url und hängt die entsprechenden Knoten
   * als Kinder an this an.
   * 
   * @throws IOException
   *           falls das Laden von Daten von url (oder einer includeten
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @throws SyntaxErrorException
   */
  protected void childrenFromUrl(URL url, Reader read) throws IOException,
      SyntaxErrorException
  {
    try
    {
      Stack<ConfigThingy> stack = new Stack<ConfigThingy>();
      stack.push(this);
      List<StringContentToken> tokens = tokenize(url, read);
      Iterator<StringContentToken> liter = tokens.iterator();
      Token token1, token2;
      do
      {
        token1 = liter.next();
        ConfigThingy child;
        switch (token1.type())
        {
          case Token.INCLUDE:
            token2 = liter.next();
            if (token2.type() == Token.STRING && !token2.contentString().equals(""))
            {
              try
              {
                URL includeURL = new URL(url, urlEncode(token2.contentString()));
                stack.peek().childrenFromUrl(includeURL,
                  new InputStreamReader(includeURL.openStream(), CHARSET));
              }
              catch (IOException iox)
              {
                throw new IOException(token2.url() + " in Zeile " + token2.line()
                  + " bei Zeichen " + token2.position()
                  + ": %include fehlgeschlagen: " + iox.toString());
              }
            }
            else
            {
              throw new SyntaxErrorException(token2.url()
                + ": URL-String (umschlossen von Gänsefüßchen) erwartet in Zeile "
                + token2.line() + " bei Zeichen " + token2.position());
            }
            break;

          case Token.KEY:
            token2 = liter.next();
            switch (token2.type())
            {
              case Token.OPENPAREN:
                child = new ConfigThingy(token1.contentString());
                stack.peek().addChild(child);
                stack.push(child);
                break;
              case Token.STRING:
                child = new ConfigThingy(token1.contentString());
                ConfigThingy grandchild = new ConfigThingy(token2.contentString());
                child.addChild(grandchild);
                stack.peek().addChild(child);
                break;
              default:
                throw new SyntaxErrorException(token2.url()
                  + ": Syntaxfehler in Zeile " + token2.line() + " bei Zeichen "
                  + token2.position());
            }
            break;

          case Token.STRING:
            child = new ConfigThingy(token1.contentString());
            stack.peek().addChild(child);
            break;

          case Token.CLOSEPAREN:
            // Achtung: Wurzel darf nicht gepoppt werden.
            if (stack.size() <= 1)
              throw new SyntaxErrorException(token1.url()
                + ": Klammer ')' ohne passende Klammer '(' in Zeile "
                + token1.line() + " bei Zeichen " + token1.position());
            stack.pop();
            break;

          case Token.OPENPAREN:
            child = new ConfigThingy("");
            stack.peek().addChild(child);
            stack.push(child);
            break;

          case Token.END:
            break;

          default:
            throw new SyntaxErrorException(token1.url() + ": Syntaxfehler in Zeile "
              + token1.line() + " bei Zeichen " + token1.position());
        }

      } while (token1.type() != Token.END);

      if (stack.size() > 1)
      {
        throw new SyntaxErrorException(token1.url() + ": " + (stack.size() - 1)
          + " schließende Klammern fehlen");
      }
    }
    finally
    {
      try
      {
        read.close();
      }
      catch (Exception x)
      {}
      ;
    }
  }

  /**
   * Jagt alle in URLs verbotenen Zeichen durch URLEncoder,encode(ch,{@link #CHARSET}
   * ). Das Leerzeichen bekommt eine Sonderbehandlung (Umsetzung nach %20), weil
   * URLEncoder.encode() es nach "+" umsetzen würde, was zumindest bei unseren
   * Webservern nicht zum gewünschten Ergebnis führt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public static String urlEncode(String url)
  {
    url = url.replaceAll("\\\\", "/");
    StringBuffer buffy = new StringBuffer();
    try
    {
      for (int i = 0; i < url.length(); ++i)
      {
        char ch = url.charAt(i);
        if (ch == ' ')
          buffy.append("%20");
        else if ((('a' <= ch) && (ch <= 'z')) || (('A' <= ch) && (ch <= 'Z'))
          || (('0' <= ch) && (ch <= '9')) || ch == ';' || ch == '/' || ch == '?'
          || ch == ':' || ch == '@' || ch == '&' || ch == '=' || ch == '+'
          || ch == '$' || ch == ',' || ch == '-' || ch == '_' || ch == '.'
          || ch == '!' || ch == '~' || ch == '*' || ch == '\'' || ch == '('
          || ch == ')' || ch == '%' || ch == '#')
        {
          buffy.append(ch);
        }
        else
          buffy.append(URLEncoder.encode("" + ch, CHARSET));
      }

      url = buffy.toString();
    }
    catch (UnsupportedEncodingException x)
    {}
    return url;
  }

  /**
   * Fügt dem ConfigThingy ein weiteres Kind hinzu. ACHTUNG! child wird nicht
   * kopiert, sondern als Refernz eingefügt!
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addChild(ConfigThingy child)
  {
    children.add(child);
  }

  /**
   * Fügt ein neues Kind namens childName als letztes Kind an und liefert eine
   * Referenz auf das neue Kind. Achtung! Mit dieser Methode ist es möglich,
   * ConfigThingys zu erzeugen, die sich nicht an die Syntaxbeschränkungen des
   * Parsers für Schlüssel halten. Wird so ein ConfigThingy mittels
   * stringRepresentation() in Text konvertiert, entsteht etwas, das der Parser nicht
   * wieder einlesen kann.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy add(String childName)
  {
    ConfigThingy newChild = new ConfigThingy(childName);
    addChild(newChild);
    return newChild;
  }

  /**
   * Liefert die Anzahl der Kinder zurück.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public int count()
  {
    return children.size();
  }

  /**
   * Liefert einen Iterator über die Kinder dieses ConfigThingys.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  @Override
  public Iterator<ConfigThingy> iterator()
  {
    return children.iterator();
  }

  /**
   * Liefert ein ConfigThingy names "<visible nodes>", dessen Kinder alle Knoten des
   * ConfigThingy-Baumes mit Wurzel root sind, die Name nodeNameToScanFor haben und
   * vom Knoten node aus sichtbar sind. Dabei ist ein Knoten sichtbar von node, wenn
   * er node selbst, ein Bruder- bzw. Schwesterknoten von node, ein Vorfahre von node
   * oder ein Bruder-/Schwesterknoten eines Vorfahren von node ist.
   */
  public static ConfigThingy getNodesVisibleAt(ConfigThingy node,
      String nodeNameToScanFor, ConfigThingy root)
  {
    Stack<Vector<ConfigThingy>> s = new Stack<Vector<ConfigThingy>>();
    Vector<ConfigThingy> r = new Vector<ConfigThingy>();
    getNodesVisibleAt(node, nodeNameToScanFor, s, root, r);
    return new ConfigThingy("<visible nodes>", r);
  }

  private static boolean getNodesVisibleAt(ConfigThingy node,
      String nodeNameToScanFor, Stack<Vector<ConfigThingy>> s, ConfigThingy root,
      Collection<ConfigThingy> result)
  {
    if (root == node)
    {
      for (Vector<ConfigThingy> v : s)
        result.addAll(v);
      return true;
    }

    Vector<ConfigThingy> v = new Vector<ConfigThingy>();
    for (ConfigThingy child : root)
    {
      if (child.getName().equals(nodeNameToScanFor)) v.add(child);
    }

    s.push(v);

    for (ConfigThingy child : root)
    {
      if (getNodesVisibleAt(node, nodeNameToScanFor, s, child, result)) return true;
    }

    s.pop();
    return false;
  }

  /**
   * Liefert den Namen dieses Knotens des Config-Baumes zurück. Im Falle eines
   * Blattes entspricht dies dem (String-)Wert.
   * 
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getName()
  {
    return name;
  }

  /**
   * Ändert den Namen dieses Knotens auf newName. Achtung! Mit dieser Methode ist es
   * möglich, ConfigThingys zu erzeugen, die sich nicht an die Syntaxbeschränkungen
   * des Parsers für Schlüssel halten. Wird so ein ConfigThingy mittels
   * stringRepresentation() in Text konvertiert, entsteht etwas, das der Parser nicht
   * wieder einlesen kann.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setName(String newName)
  {
    name = newName;
  }

  /**
   * Liefert den ersten Kind-Knoten.
   * 
   * @throws NodeNotFoundException
   *           falls this keine Kinder hat.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy getFirstChild() throws NodeNotFoundException
  {
    if (children.isEmpty())
      throw new NodeNotFoundException("Knoten " + getName() + " hat keine Kinder");
    return children.get(0);
  }

  /**
   * Wie getFirstChild(), aber falls kein Kind vorhanden wird
   * IndexOutOfBoundsException geworfen anstatt NodeNotFoundException.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private ConfigThingy getFirstChildNoThrow()
  {
    return children.get(0);
  }

  /**
   * Liefert den letzten Kind-Knoten.
   * 
   * @throws NodeNotFoundException
   *           falls this keine Kinder hat.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy getLastChild() throws NodeNotFoundException
  {
    if (children.isEmpty())
      throw new NodeNotFoundException("Knoten " + getName() + " hat keine Kinder");
    return children.get(children.size() - 1);
  }

  /**
   * Durchsucht den Teilbaum mit Wurzel this nach Knoten mit Name name auf Suchtiefe
   * searchlevel und fügt sie (oder falls getParents==true ihre Eltern) in die Liste
   * found ein.
   * 
   * @param parent
   *          Der Elternknoten von this.
   * @param name
   *          der Name nach dem zu suchen ist.
   * @param found
   *          in diese Liste werden die gefundenen Knoten (oder falls
   *          getParents==true ihre Eltern) eingefügt. Jeder Knoten taucht maximal
   *          einmal in dieser Liste auf, d.h. falls getParents==true und ein Knoten
   *          mehrere Kinder mit Name name hat, wird dieser Knoten trotzdem nur
   *          einmal eingefügt.
   * @param parentLevel
   *          die Suchtiefe bei Breitensuche von parent, d.h. this hat Suchtiefe
   *          parentLevel + 1
   * @param searchLevel
   *          die Suchtiefe bei Breitensuche auf der die Knoten gesucht werden
   *          sollen. Knoten auf anderen Suchtiefen werden nicht in found eingefügt.
   * @param getParents
   *          falls true werden die Elternknoten statt der gefundenen Knoten
   *          eingefügt. Jeder Elternknoten wird allerdings grundsätzlich nur einmal
   *          eingefügt, auch wenn er mehrere passende Kinder hat.
   * @return true falls noch mindestens ein Knoten mit Suchtiefe searchlevel erreicht
   *         wurde, d.h. falls eine Suche mit höherem searchlevel prinzipiell
   *         Ergebnisse bringen könnte.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private boolean rollcall(ConfigThingy parent, String name,
      List<ConfigThingy> found, int parentLevel, int searchLevel, boolean getParents)
  {
    int level = parentLevel + 1;
    if (searchLevel == level)
    {
      if (name.equals(this.name))
      {
        if (getParents)
        {
          if (!found.contains(parent)) found.add(parent);
        }
        else
          found.add(this);
      }
    }
    else
    // if searchLevel < level
    {
      boolean haveMore = false;
      for (ConfigThingy child : children)
      {
        boolean result =
          child.rollcall(this, name, found, level, searchLevel, getParents);
        haveMore = haveMore || result;
      }
      return haveMore;
    }
    return true;
  }

  /**
   * Führt eine Breitensuche nach Nachfahrenknoten von this durch, die name als Name
   * haben.
   * 
   * @return Falls es entsprechende Knoten gibt, wird die niedrigste Suchtiefe
   *         bestimmt auf der entsprechende Knoten zu finden sind und es werden alle
   *         Knoten auf dieser Suchtiefe zurückgeliefert. Falls dies genau einer ist,
   *         wird er direkt zurückgeliefert, ansonsten wird ein ConfigThingy mit
   *         Namen "<query results>" geliefert, das diese Knoten (und nur diese) als
   *         Kinder hat.
   * @throws NodeNotFoundException
   *           falls keine entsprechenden Knoten gefunden wurden. Falls das nicht
   *           gewünscht ist, kann {@link #query(String)} benutzt werden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy get(String name) throws NodeNotFoundException
  {
    return get(name, INTEGER_MAX);
  }

  /**
   * Diese Vereinfachungs-Methode zur Vermeidung unübersichtlicher Exception-Blöcke
   * liefert das Ergebnis von get(name).toString() zurück oder defStr, falls es das
   * gesuchte Element nicht gibt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public String getString(String name, String defStr)
  {
    try
    {
      return get(name, INTEGER_MAX).toString();
    }
    catch (NodeNotFoundException e)
    {
      return defStr;
    }
  }

  /**
   * Wie {@link #get(String)}, es werden aber maximal Ergebnisse von Suchtiefe
   * maxlevel (0 ist this) zurückgeliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy get(String name, int maxlevel) throws NodeNotFoundException
  {
    return get(name, maxlevel, DEFAULT_MINLEVEL);
  }

  /**
   * Wie {@link #get(String)}, es werden aber nur Ergebnisse, deren Suchtiefe
   * kleiner/gleich maxlevel und größer/gleich minlevel ist (0 ist this),
   * zurückgeliefert.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public ConfigThingy get(String name, int maxlevel, int minlevel)
      throws NodeNotFoundException
  {
    ConfigThingy res = query(name, false, maxlevel, minlevel);
    if (res.count() == 0)
      throw new NodeNotFoundException("Knoten " + getName()
        + " hat keinen Nachfahren '" + name + "'");
    if (res.count() == 1) res = res.iterator().next();
    return res;
  }

  /**
   * Wie {@link #get(String)}, aber es wird grundsätzlich ein ConfigThingy mit Namen
   * "&lt;query results>" über die Resultate gesetzt. Im Falle, dass es keine
   * Resultate gibt, wird nicht null sondern ein ConfigThingy ohne Kinder geliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy query(String name)
  {
    return query(name, false, INTEGER_MAX, DEFAULT_MINLEVEL);
  }

  /**
   * Wie {@link #get(String, int)}, aber es wird grundsätzlich ein ConfigThingy mit
   * Namen "<query results>" über die Resultate gesetzt. Im Falle, dass es keine
   * Resultate gibt, wird nicht null sondern ein ConfigThingy ohne Kinder geliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy query(String name, int maxlevel)
  {
    return query(name, false, maxlevel, DEFAULT_MINLEVEL);
  }

  /**
   * Wie {@link #get(String, int, int)}, aber es wird grundsätzlich ein ConfigThingy
   * mit Namen "<query results>" über die Resultate gesetzt. Im Falle, dass es keine
   * Resultate gibt, wird nicht null sondern ein ConfigThingy ohne Kinder geliefert.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public ConfigThingy query(String name, int maxlevel, int minlevel)
  {
    return query(name, false, maxlevel, minlevel);
  }

  /**
   * Wie {@link #get(String)}, aber es werden die Elternknoten der gefundenen Knoten
   * zurückgeliefert anstatt der Knoten selbst. Es ist zu beachten, dass jeder
   * Elternknoten nur genau einmal in den Ergebnissen enthalten ist, auch wenn er
   * mehrere passende Kinder hat,
   * 
   * @throws NodeNotFoundException
   *           falls keine entsprechenden Knoten gefunden wurden. Falls das nicht
   *           gewünscht ist, kann {@link #query(String)} benutzt werden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy getByChild(String name) throws NodeNotFoundException
  {
    return getByChild(name, INTEGER_MAX);
  }

  /**
   * Wie {@link #get(String, int)}, aber es werden die Elternknoten der gefundenen
   * Knoten zurückgeliefert anstatt der Knoten selbst. Es ist zu beachten, dass jeder
   * Elternknoten nur genau einmal in den Ergebnissen enthalten ist, auch wenn er
   * mehrere passende Kinder hat,
   * 
   * @throws NodeNotFoundException
   *           falls keine entsprechenden Knoten gefunden wurden. Falls das nicht
   *           gewünscht ist, kann {@link #query(String, int)} benutzt werden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy getByChild(String name, int maxlevel)
      throws NodeNotFoundException
  {
    return getByChild(name, maxlevel, DEFAULT_MINLEVEL);
  }

  /**
   * Wie {@link #get(String, int, int)}, aber es werden die Elternknoten der
   * gefundenen Knoten zurückgeliefert anstatt der Knoten selbst. Es ist zu beachten,
   * dass jeder Elternknoten nur genau einmal in den Ergebnissen enthalten ist, auch
   * wenn er mehrere passende Kinder hat,
   * 
   * @throws NodeNotFoundException
   *           falls keine entsprechenden Knoten gefunden wurden. Falls das nicht
   *           gewünscht ist, kann {@link #query(String, int, int)} benutzt werden.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public ConfigThingy getByChild(String name, int maxlevel, int minlevel)
      throws NodeNotFoundException
  {
    ConfigThingy res = query(name, true, maxlevel, minlevel);
    if (res.count() == 0)
      throw new NodeNotFoundException("Knoten " + getName()
        + " hat keinen Nachfahren '" + name + "'");
    if (res.count() == 1) res = res.iterator().next();
    return res;
  }

  /**
   * Wie {@link #query(String)}, aber es werden die Elternknoten der gefundenen
   * Knoten zurückgeliefert anstatt der Knoten selbst. Es ist zu beachten, dass jeder
   * Elternknoten nur genau einmal in den Ergebnissen enthalten ist, auch wenn er
   * mehrere passende Kinder hat,
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy queryByChild(String name)
  {
    return query(name, true, INTEGER_MAX, DEFAULT_MINLEVEL);
  }

  /**
   * Wie {@link #query(String, int)}, aber es werden die Elternknoten der gefundenen
   * Knoten zurückgeliefert anstatt der Knoten selbst. Es ist zu beachten, dass jeder
   * Elternknoten nur genau einmal in den Ergebnissen enthalten ist, auch wenn er
   * mehrere passende Kinder hat,
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ConfigThingy queryByChild(String name, int maxlevel)
  {
    return query(name, true, maxlevel, DEFAULT_MINLEVEL);
  }

  /**
   * Wie {@link #query(String, int, int)}, aber es werden die Elternknoten der
   * gefundenen Knoten zurückgeliefert anstatt der Knoten selbst. Es ist zu beachten,
   * dass jeder Elternknoten nur genau einmal in den Ergebnissen enthalten ist, auch
   * wenn er mehrere passende Kinder hat,
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public ConfigThingy queryByChild(String name, int maxlevel, int minlevel)
  {
    return query(name, true, maxlevel, minlevel);
  }
  
  /**
   * Sucht rekursiv nach allen Knoten mit einem bestimmten Namen.
   * 
   * @param name
   * @param maxlevel Maximale Tiefe in der gesucht werden soll.
   * @param getParents Wenn ture, liefert den Elternknoten zurück. 
   * @return
   */
  public ConfigThingy queryAll(String name, int maxlevel, boolean getParents)
  {
    ArrayList<ConfigThingy> found = new ArrayList<ConfigThingy>();
    
    boolean hasMore = false;
    
    int searchlevel = 1;
    do
    {
      hasMore = rollcall(this, name, found, -1, searchlevel++, getParents);
    } while (hasMore && searchlevel < maxlevel + 1);
    
    return new ConfigThingy("<query results>", found);
  }

  /**
   * Falls getParents == false verhält sich diese Funktion wie
   * {@link #get(String, int, int)}, falls getParents == true wie
   * {@link #getByChild(String, int, int)}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  protected ConfigThingy query(String name, boolean getParents, int maxlevel,
      int minlevel)
  {
    Vector<ConfigThingy> found = new Vector<ConfigThingy>();
    boolean haveMore;
    int searchlevel = minlevel;
    do
    {
      if (searchlevel > maxlevel) break;
      haveMore = rollcall(this, name, found, -1, searchlevel, getParents);
      ++searchlevel;
    } while (found.isEmpty() && haveMore);

    if (found.size() == 0) return new ConfigThingy("<query results>");
    return new ConfigThingy("<query results>", found);
  }

  /**
   * Falls getParents == false verhält sich diese Funktion wie
   * {@link #get(String, int)}, falls getParents == true wie
   * {@link #getByChild(String, int)}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  protected ConfigThingy query(String name, boolean getParents, int maxlevel)
  {
    return query(name, getParents, maxlevel, DEFAULT_MINLEVEL);
  }

  /**
   * Falls der Knoten this ein Blatt ist wird der Name des Knotens geliefert,
   * ansonsten die Konkatenation aller Blätter des unter this liegenden Teilbaums.
   */
  @Override
  public String toString()
  {
    if (children.isEmpty()) return name;

    // Optimierung: Nicht unnötig StringBuilder produzieren
    if (children.size() == 1) return children.get(0).toString();

    StringBuilder buf = new StringBuilder();
    for (ConfigThingy child : children)
      buf.append(child.toString());
    return buf.toString();
  }

  /**
   * Gibt eine String-Darstellung des kompletten ConfigThingy-Baumes zurück, die
   * geeignet ist, in eine Datei gespeichert und von dort wieder als ConfigThingy
   * geparst zu werden.
   * 
   * @param childrenOnly
   *          wenn true wird keine äusserste Verschachtelung mit dem Namen von this
   *          erzeugt.
   * @param stringChar
   *          das Zeichen, das zum Einschliessen von Strings verwendet werden soll.
   * @param escapeAll
   *          falls true werden in Strings alle Zeichen, die nicht Buchstabe oder
   *          Ziffer sind mit der %u Syntax escapet.
   * @throws IllegalArgumentException
   *           falls stringChar nicht ' oder " ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String stringRepresentation(boolean childrenOnly, char stringChar,
      boolean escapeAll) throws java.lang.IllegalArgumentException
  {
    if (stringChar != '"' && stringChar != '\'')
      throw new java.lang.IllegalArgumentException(
        "Als Stringbegrenzer sind nur \" und ' erlaubt.");

    StringBuffer buf = new StringBuffer();
    if (!childrenOnly)
      stringRepresentation(buf, "", stringChar, escapeAll);
    else
    {
      for (ConfigThingy child : children)
      {
        child.stringRepresentation(buf, "", stringChar, escapeAll);
        buf.append('\n');
      }
    }
    return buf.toString();
  }

  /**
   * Wie {@link #stringRepresentation(boolean, char, boolean)} mit escapeAll ==
   * false.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String stringRepresentation(boolean childrenOnly, char stringChar)
      throws java.lang.IllegalArgumentException
  {
    return stringRepresentation(childrenOnly, stringChar, false);
  }

  /**
   * Wie {@link #stringRepresentation(boolean, char) stringRepresentation(false,
   * '"')}.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String stringRepresentation()
  {
    return stringRepresentation(false, '"');
  }

  /**
   * Ersetzt ' durch '', \n durch %n, % durch %%
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @param escapeAll
   *          falls true werden alle {@link #NON_LETTER_OR_DIGITS} als
   *          Unicode-Escapes escapet.
   */
  private String escapeString(String str, char stringChar, boolean escapeAll)
  {
    Pattern p;
    if (escapeAll)
      p = NON_LETTER_OR_DIGITS;
    else
      p = CONFIGTHINGY_SPECIAL;

    Matcher m = p.matcher(str);
    ArrayList<Integer> locations = new ArrayList<Integer>();
    while (m.find())
      locations.add(Integer.valueOf(m.start()));
    StringBuilder buffy = new StringBuilder(str);
    while (!locations.isEmpty())
    {
      int idx = locations.remove(locations.size() - 1).intValue();

      String repstr = "" + buffy.charAt(idx);

      if (escapeAll)
      {
        repstr = Integer.toHexString(buffy.charAt(idx));
        while (repstr.length() < 4)
          repstr = "0" + repstr;
        repstr = "%u" + repstr;
      }
      else
      {
        switch (buffy.charAt(idx))
        {
          case '\'':
            repstr = stringChar == '\'' ? "''" : "'";
            break;
          case '"':
            repstr = stringChar == '"' ? "\"\"" : "\"";
            break;
          case '\n':
            repstr = "%n";
            break;
          case '\r':
            repstr = "%u000a";
            break;
          case '%':
            repstr = "%%";
            break;
        }
      }
      buffy.replace(idx, idx + 1, repstr);
    }
    return buffy.toString();
  }

  /**
   * Hängt eine textuelle Darstellung diese ConfigThingys an buf an. Jeder Zeile wird
   * childPrefix vorangestellt.
   * 
   * @param escapeAll
   *          falls true werden in Strings alle Zeichen, die nicht Buchstabe oder
   *          Ziffer sind mit der %u Syntax escapet.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void stringRepresentation(StringBuffer buf, String childPrefix,
      char stringChar, boolean escapeAll)
  {

    if (count() == 0) // Blatt
    {
      buf.append(stringChar + escapeString(getName(), stringChar, escapeAll)
        + stringChar);
    }
    else if (count() == 1 && getFirstChildNoThrow().count() == 0) // Schlüssel-Wert-Paar
    {
      /*
       * Normalerweise werden Schlüssel-Wert-Paare ohne Klammern repräsentiert, aber
       * im Spezialfall, dass der Schlüssel leer ist (Liste mit nur einem Element)
       * dürfen die Klammern nicht weggelassen werden.
       */
      buf.append(getName());
      if (getName().length() == 0)
        buf.append('(');
      else
        buf.append(' ');

      getFirstChildNoThrow().stringRepresentation(buf, childPrefix, stringChar,
        escapeAll);
      if (getName().length() == 0) buf.append(')');
    }
    else
    {
      int type = structureType();
      if (type == ST_VALUE_LIST || type == ST_PAIR_LIST) // nur Kinder, keine
      // Enkelkinder
      {
        buf.append(childPrefix);
        buf.append(getName());
        buf.append('(');
        Iterator<ConfigThingy> iter = iterator();
        while (iter.hasNext())
        {
          ConfigThingy child = iter.next();
          child.stringRepresentation(buf, childPrefix, stringChar, escapeAll);
          if (iter.hasNext())
          {
            if (type == ST_VALUE_LIST) buf.append(',');
            buf.append(' ');
          }
        }
        buf.append(')');
        buf.append("\n");
      }
      else
      {
        buf.append('\n');
        buf.append(childPrefix);
        buf.append(getName());
        buf.append("(\n");
        for (ConfigThingy child : children)
        {
          if (child.count() == 0
            || (child.count() == 1 && child.getFirstChildNoThrow().count() == 0))
            buf.append(childPrefix + INDENT);

          child.stringRepresentation(buf, childPrefix + INDENT, stringChar,
            escapeAll);

          if (child.count() == 0
            || (child.count() == 1 && child.getFirstChildNoThrow().count() == 0))
            buf.append('\n');
        }
        buf.append(childPrefix);
        buf.append(")\n");
      }
    }
  }

  private static final int ST_VALUE_LIST = 0;

  private static final int ST_PAIR_LIST = 1;

  private static final int ST_OTHER = 2;

  private int structureType()
  {
    int count = -1;
    for (ConfigThingy child : children)
    {
      if (count == -1) count = child.count();
      if (count != child.count()) return ST_OTHER;
      if (count > 1) return ST_OTHER;
      if (count == 1 && child.getFirstChildNoThrow().count() > 0) return ST_OTHER;
    }

    return count == 0 ? ST_VALUE_LIST : ST_PAIR_LIST;
  }

  /**
   * Diese Methode tut nichts ausser zu prüfen, ob es sich bei dem übergebenen String
   * id um einen gültigen Bezeichner gemäß der Syntax für WollMux-Config-Dateien
   * handelt und im negativen Fall eine InvalidIdentifierException zu werfen.
   * 
   * @param id
   *          zu prüfende ID
   * @author Christoph Lutz (D-III-ITD-5.1)
   * @throws InvalidIdentifierException
   */
  public static void checkIdentifier(String id) throws InvalidIdentifierException
  {
    if (!id.matches("^[a-zA-Z_][a-zA-Z_0-9]*$"))
      throw new InvalidIdentifierException(id);
  }

  /**
   * Die Methode {@link ConfigThingy#tokenize(URL)} liefert eine Liste von Objekten,
   * die alle dieses Interface implementieren.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private interface Token
  {
    public static final int KEY = 0;

    public static final int STRING = 1;

    public static final int OPENPAREN = 2;

    public static final int CLOSEPAREN = 3;

    public static final int END = 4;

    public static final int INCLUDE = 5;

    public static final int LINECOMMENT = 6;

    /**
     * Liefert die URL des Dokuments, in dem dieses Token gefunden wurde.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public URL url();

    /**
     * Liefert die Zeile in der dieses Token gefunden wurde.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public int line();

    /**
     * Liefert die Position des ersten Zeichens dieses Tokens in seiner Zeile,
     * gezählt ab 1.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public int position();

    /**
     * Liefert die Art dieses Tokens, z.B. {@link #KEY}.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public int type();

    /**
     * Liefert die Textrepräsentation dieses Tokens. Diese ist NICHT zwangsweise
     * identisch mit der Zeichenfolge aus der dieses Token geparst wurde. Zum
     * Beispiel tauchen Trennzeichen wie die Gänsefüßchen zur Abgrenzung von Strings
     * in dem hier zurückgelieferten String nicht auf. Ebensowenig sind in diesem
     * String Escape-Sequenzen zu finden, die im Eingabedatenstrom verwendet werden,
     * um bestimmte Zeichen wie z.B. Newline darzustellen.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public String contentString();
  }

  /**
   * Abstrakte Basis-Klasse für Tokens, die ihren {@link #contentString()} Wert in
   * einer String-Variable speichern.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static abstract class StringContentToken implements Token
  {
    protected String content;

    protected int myLine;

    protected int myPosition;

    protected URL myURL;

    public StringContentToken(URL url, int line, int position)
    {
      myURL = url;
      myLine = line;
      myPosition = position;
    }

    @Override
    public URL url()
    {
      return myURL;
    }

    @Override
    public int line()
    {
      return myLine;
    }

    @Override
    public int position()
    {
      return myPosition;
    }

    @Override
    public String contentString()
    {
      return content;
    }
  }

  /**
   * Token für einen String gemäß Syntax für WollMux-Config-Dateien.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class StringToken extends StringContentToken
  {
    /**
     * Erzeugt ein neues StringToken
     * 
     * @param tokenData
     *          ein String, für den {@link #atStartOf(String)} einen Wert größer 0
     *          zurückliefert.
     */
    public StringToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);

      int len = atStartOf(tokenData);
      if (len < 2) throw new IllegalArgumentException("String token expected!");

      char quote = tokenData.charAt(0);

      /*
       * %-Escapes auswerten, sowie quotequote durch quote ersetzen
       */
      StringBuilder buffy = new StringBuilder(tokenData.substring(1, len - 1));
      String quoteStr = "" + quote;
      int startidx = 0;
      int idx;
      while (true)
      {
        idx = buffy.indexOf("%", startidx);
        int idx2 = buffy.indexOf(quoteStr, startidx);

        if (idx < 0 && idx2 < 0) break;

        String repstr;
        int replen;

        if (idx < 0 || (idx2 >= 0 && idx2 < idx)) // quotequote
        {
          idx = idx2;
          replen = 2;
          repstr = quoteStr;
        }
        else
        // if (idx >= 0 && (idx2 < 0 || idx2 >= idx)) // %-Escape
        {
          if (idx + 1 >= buffy.length()) break;

          // default: durch selbes Zeichen, also % ersetzen
          repstr = "" + buffy.charAt(idx);
          replen = 1;

          switch (buffy.charAt(idx + 1))
          {
            case 'n':
              repstr = "\n";
              replen = 2;
              break;
            case '%':
              repstr = "%";
              replen = 2;
              break;
            case 'u':
              repstr = parseUnicode(buffy, idx + 2);
              replen = 6;
              break;
            /*
             * darf nicht gemacht werden, weil zum Beispiel in URLs %-escapes
             * vorkommen:
             * 
             * default: throw new IllegalArgumentException("Unknown escape in string
             * token '%" + buffy.charAt(idx)+"'!");
             */
          }
        }
        buffy.replace(idx, idx + replen, repstr);
        startidx = idx + repstr.length();
      }

      content = buffy.toString();
    }

    private String parseUnicode(StringBuilder str, int idx)
    {
      if (idx + 4 > str.length())
        throw new IllegalArgumentException("Incomplete %u escape!");
      String code = str.substring(idx, idx + 4);
      try
      {
        char ch = (char) Integer.parseInt(code, 16);
        return "" + ch;
      }
      catch (NumberFormatException x)
      {
        throw new IllegalArgumentException("Incorrect hex number in %u escape: \"%u"
          + code + "\"");
      }
    }

    @Override
    public int type()
    {
      return Token.STRING;
    }

    /**
     * Liefert die Länge des längsten Prefixes von str, das sich als Token dieser
     * Klasse interpretieren lässt.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      if (str.length() < 2) return 0;
      char quote = str.charAt(0);
      if (quote != '"' && quote != '\'') return 0;

      int idx = 1;
      while (true)
      {
        idx = str.indexOf(quote, idx);
        if (idx < 0) return 0;
        ++idx;
        if (idx >= str.length() || str.charAt(idx) != quote) return idx;
        ++idx;
      }
    }
  }

  /**
   * Ein Token für einen Schlüssel gemäß Syntax für WollMux-Config-Dateien.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class KeyToken extends StringContentToken
  {
    /**
     * Regex zur Identifikation von legalen Schlüsseln.
     */
    private static Pattern p = Pattern.compile("^([a-zA-Z_][a-zA-Z_0-9]*)");

    /**
     * Erzeugt ein neues KeyToken
     * 
     * @param tokenData
     *          ein String, für den {@link #atStartOf(String)} einen Wert größer 0
     *          zurückliefert.
     */
    public KeyToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);
      Matcher m = p.matcher(tokenData);
      if (!m.find()) throw new IllegalArgumentException("Key token expected!");
      content = m.group(1);
    }

    @Override
    public int type()
    {
      return Token.KEY;
    }

    /**
     * Liefert die Länge des längsten Prefixes von str, das sich als Token dieser
     * Klasse interpretieren lässt.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      Matcher m = p.matcher(str);
      if (!m.find()) return 0;
      return m.end();
    }
  }

  /**
   * Token für öffnende runde Klammer.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class OpenParenToken extends StringContentToken
  {
    public OpenParenToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = "(";
    }

    @Override
    public int type()
    {
      return Token.OPENPAREN;
    }

    /**
     * Liefert 1, falls str mit '(' beginnt, ansonsten 0.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      return str.startsWith("(") ? 1 : 0;
    }
  }

  /**
   * Token für schließende runde Klammer.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class CloseParenToken extends StringContentToken
  {
    public CloseParenToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = ")";
    }

    @Override
    public int type()
    {
      return Token.CLOSEPAREN;
    }

    /**
     * Liefert 1, falls str mit ')' beginnt, ansonsten 0.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      return str.startsWith(")") ? 1 : 0;
    }
  }

  /**
   * Token für den String "%include".
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class IncludeToken extends StringContentToken
  {
    private static final String inc = "%include";

    public IncludeToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = inc;
    }

    @Override
    public int type()
    {
      return Token.INCLUDE;
    }

    /**
     * Liefert die Länge des längsten Prefixes von str, das sich als Token dieser
     * Klasse interpretieren lässt.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      return str.startsWith(inc) ? inc.length() : 0;
    }
  }

  /**
   * Token für einen Kommentar gemäß Syntax von WollMux-Config-Dateien. ACHTUNG:
   * Tokens dieser Klasse werden derzeit von {@link ConfigThingy#tokenize(URL)} nicht
   * zurückgeliefert, sondern verworfen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class LineCommentToken extends StringContentToken
  {

    /**
     * Erzeugt einen neuen LineCommenToken.
     * 
     * @param tokenData
     *          ein String, dessen erstes Zeichen '#' ist.
     */
    public LineCommentToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);
      content = tokenData.substring(1);
    }

    @Override
    public int type()
    {
      return Token.LINECOMMENT;
    }

    /**
     * Liefert die Länge des längsten Prefixes von str, das sich als Token dieser
     * Klasse interpretieren lässt.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public static int atStartOf(String str)
    {
      return str.startsWith("#") ? str.length() : 0;
    }
  }

  /**
   * Signalisiert das Ende des Eingabedatenstroms.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class EndToken extends StringContentToken
  {
    public EndToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = "";
    }

    @Override
    public int type()
    {
      return Token.END;
    }
  }

  /**
   * Zerlegt die Daten aus read in {@link Token}s. Als Quell-URL wird in den Tokens
   * url eingetragen.
   * 
   * @return die Liste der identifizierten Tokens, abgeschlossen durch mindestens 7
   *         {@link EndToken}s.
   * @throws IOException
   *           falls beim Zugriff auf die Daten von url etwas schief geht.
   * @throws SyntaxErrorException
   *           falls eine Zeichenfolge nicht als Token identifiziert werden kann.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static List<StringContentToken> tokenize(URL url, Reader read)
      throws IOException, SyntaxErrorException
  {
    List<StringContentToken> tokens = new Vector<StringContentToken>();
    BufferedReader in = new BufferedReader(read);
    String line;

    int lineNo = 0;
    Pattern whitespace = Pattern.compile("^(\\p{Space}|\\u00A0|,|;|\\uFEFF)+");
    while (null != (line = in.readLine()))
    {
      ++lineNo;
      int pos = 0;

      while (line.length() > 0)
      {
        Matcher wsm = whitespace.matcher(line);
        if (wsm.find())
        {
          pos += wsm.end();
          line = line.substring(wsm.end());
          if (line.length() == 0) continue;
        }

        int tokenLength;
        try
        {
          if (0 != (tokenLength = KeyToken.atStartOf(line)))
          {
            tokens.add(new KeyToken(line, url, lineNo, pos + 1));
          }
          else if (0 != (tokenLength = StringToken.atStartOf(line)))
          {
            tokens.add(new StringToken(line, url, lineNo, pos + 1));
          }
          else if (0 != (tokenLength = OpenParenToken.atStartOf(line)))
          {
            tokens.add(new OpenParenToken(url, lineNo, pos + 1));
          }
          else if (0 != (tokenLength = CloseParenToken.atStartOf(line)))
          {
            tokens.add(new CloseParenToken(url, lineNo, pos + 1));
          }
          else if (0 != (tokenLength = IncludeToken.atStartOf(line)))
          {
            tokens.add(new IncludeToken(url, lineNo, pos + 1));
          }
          else if (0 != (tokenLength = LineCommentToken.atStartOf(line)))
          {
            // LineCommentTokens werden nicht in tokens eingefügt, weil
            // der Parser im Fall von 2er Paaren wie KEY STRING nicht in
            // der Lage ist über Kommentare hinwegzulesen. Anstatt ihm das
            // Einzubauen ist es einfacher, Kommentare einfach wegzuschmeissen.
            // tokens.add(new LineCommentToken(line,url,lineNo,pos+1));
          }
          else
          {
            throw new SyntaxErrorException(url + ": Syntaxfehler in Zeile " + lineNo
              + " bei Zeichen " + (pos + 1) + ", Text an Fehlerstelle: \"" + line
              + "\"");
          }
        }
        catch (IllegalArgumentException x)
        {
          throw new SyntaxErrorException(url + ": Syntaxfehler in Zeile " + lineNo
            + " bei Zeichen " + (pos + 1) + ", Text an Fehlerstelle: \"" + line
            + "\"", x);
        }

        pos += tokenLength;
        line = line.substring(tokenLength);
      }
    }

    // add a couple EndTokens so that users don't have to worry about
    // checking if there's enough input remaining
    ++lineNo;
    for (int i = 0; i < 7; ++i)
      tokens.add(new EndToken(url, lineNo, 0));

    return tokens;
  }

  /**
   * Liefert eine textuelle Baumdarstellung von conf. Jeder Zeile wird childPrefix
   * vorangestellt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static String treeDump(ConfigThingy conf, String childPrefix)
  {
    StringBuffer buf = new StringBuffer();
    buf.append("\"" + conf.name + "\"\n");
    Iterator<ConfigThingy> iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy child = iter.next();
      buf.append(childPrefix + "|\n" + childPrefix + "+--");
      char ch = iter.hasNext() ? '|' : ' ';
      buf.append(treeDump(child, childPrefix + ch + "  "));
    }
    return buf.toString();
  }

  /**
   * Testet die Funktionsweise von ConfigThingy
   * 
   * @param args
   *          url [ get1 get2 ... ], dabei ist url die URL einer zu lesenden
   *          Config-Datei. Das Programm gibt den sich daraus ergebenden
   *          ConfigThingy-Baum aus. Optional können noch weitere Argumente übergeben
   *          werden, die einen get-Pfad angeben
   *          (configthingy.get(get1).get(get2)...) dessen Ergebnis ausgegeben wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @throws SyntaxErrorException
   */
  public static void main(String[] args) throws IOException, SyntaxErrorException
  {
    if (args.length < 1)
    {
      System.out.println("USAGE: <url> [ <get1> <get2> ... ]");
      System.exit(0);
    }

    File cwd = new File(System.getProperty("user.dir"));

    args[0] = args[0].replaceAll("\\\\", "/");
    ConfigThingy conf =
      new ConfigThingy(args[0], new URL(cwd.toURI().toURL(), args[0]));

    System.out.println("Dies ist die stringRepresentation(): \n\n"
      + conf.stringRepresentation());
    System.out.println("Dies ist die stringRepresentation() unicodifiziert: \n\n"
      + conf.stringRepresentation(false, '"', true));

    System.out.println("Dies ist der Ergebnisbaum: \n\n" + treeDump(conf, ""));

    if (args.length > 1)
    {
      System.out.println("\n\n");

      ConfigThingy byChild = null;

      String getstr = "";
      String getbychildstr = "";
      for (int i = 1; i < args.length; ++i)
      {
        if (i + 1 == args.length)
        {
          try
          {
            byChild = conf.getByChild(args[i]);
          }
          catch (NodeNotFoundException x)
          {}
          getbychildstr = getstr + ".getByChild(\"" + args[i] + "\")";
        }
        getstr = getstr + ".get(\"" + args[i] + "\")";
        try
        {
          conf = conf.get(args[i]);
        }
        catch (NodeNotFoundException x)
        {
          conf = null;
        }
        if (conf == null) break;
      }

      if (conf == null)
      {
        System.out.println(getstr + " == null!");
      }
      else
      {
        System.out.println("Dies ist der Teilbaum für " + getstr + ": \n\n");
        System.out.println(treeDump(conf, ""));
        System.out.println("\n\nDies ist der toString() Wert des Teilbaums:\n"
          + "\"" + conf.toString() + "\"");
      }

      System.out.print("\n\n");

      if (byChild == null)
      {
        System.out.println(getbychildstr + " == null!");
      }
      else
      {
        System.out.println("Dies ist der Teilbaum für " + getbychildstr + ": \n\n");
        System.out.println(treeDump(byChild, ""));
        System.out.println("\n\nDies ist der toString() Wert des Teilbaums:\n"
          + "\"" + byChild.toString() + "\"");
      }
    }
  }
}
