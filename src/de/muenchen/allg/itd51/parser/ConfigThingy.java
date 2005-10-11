/*
* Dateiname: ConfigThingy.java
* Projekt  : WollMux
* Funktion : Parsen und Repräsentation von WollMux-Konfigurationsdateien
* 
* Copyright: Landeshauptstadt München
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
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO Doku NICHT vergessen, zu erwähnen, dass ";" und "," Whitespace sind.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ConfigThingy
{
  private List children;
  private String name;
  
  public ConfigThingy(String name, URL url) throws IOException
  {
    this(name);
    childrenFromUrl(url);
  }
  
  protected void childrenFromUrl(URL url) throws IOException
  {
    Stack stack = new Stack();
    stack.push(this);
    List tokens = tokenize(url);
    Iterator liter = tokens.iterator();
    Token token1, token2;
    do{
      token1 = (Token)liter.next();
      ConfigThingy child;
      switch (token1.type())
      {
        case Token.INCLUDE:
            token2 = (Token)liter.next();
            switch (token2.type())
            {
              case Token.STRING:
                   try{
                     ((ConfigThingy)stack.peek()).childrenFromUrl(new URL(url,token2.contentString()));
                   } catch(IOException iox)
                   {
                     throw new IOException(token2.url()+" in Zeile "+token2.line()+" bei Zeichen "+token2.position()+": %include fehlgeschlagen: "+iox.toString());
                   }
                  break;
              default:
                  throw new SyntaxErrorException(token2.url()+": URL-String (umschlossen von Gänsefüßchen) erwartet in Zeile "+token2.line()+" bei Zeichen "+token2.position());
            }
            break;
            
        case Token.KEY:
            token2 = (Token)liter.next();
            switch (token2.type())
            {
              case Token.OPENPAREN:
                  child = new ConfigThingy(token1.contentString());
                  ((ConfigThingy)stack.peek()).addChild(child);
                  stack.push(child);
                  break;
              case Token.STRING:
                  child = new ConfigThingy(token1.contentString());
                  ConfigThingy grandchild = new ConfigThingy(token2.contentString());
                  child.addChild(grandchild);
                  ((ConfigThingy)stack.peek()).addChild(child);
                  break;
              default:
                  throw new SyntaxErrorException(token2.url()+": Syntaxfehler in Zeile "+token2.line()+" bei Zeichen "+token2.position());
            }
            break;
        
        case Token.STRING:
            child = new ConfigThingy(token1.contentString());
            ((ConfigThingy)stack.peek()).addChild(child);
            break;
        
        case Token.CLOSEPAREN:
            //Achtung: Wurzel darf nicht gepoppt werden.
          if (stack.size() <= 1) throw new SyntaxErrorException(token1.url()+": Klammer ')' ohne passende Klammer '(' in Zeile "+token1.line()+" bei Zeichen "+token1.position()); 
          stack.pop();  
          break;
          
        case Token.OPENPAREN:
          child = new ConfigThingy("");
          ((ConfigThingy)stack.peek()).addChild(child);
          stack.push(child);
          break;

        case Token.END:
          break;

        default:
          throw new SyntaxErrorException(token1.url()+": Syntaxfehler in Zeile "+token1.line()+" bei Zeichen "+token1.position());
      }
      
    } while(token1.type() != Token.END);
    
    if (stack.size() > 1)
      throw new SyntaxErrorException(token1.url()+": "+(stack.size() - 1) + " schließende Klammern fehlen");
  }
  
  private ConfigThingy(String name)
  {
    this.name = name;
    this.children = new Vector();
  }
  
  private ConfigThingy(List children)
  {
    this.name="";
    this.children = children;
  }

  public void addChild(ConfigThingy child)
  {
    children.add(child);
  }
  
  public int count()
  {
    return children.size();
  }
    
  public Iterator iterator()
  {
    return children.iterator();
  }
  
  public String getName()
  {
    return name;
  }

  private boolean rollcall(ConfigThingy parent, String name, List found, int parentLevel, int searchLevel, boolean getParents)
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
    else //if searchLevel < level
    {
      Iterator iter = children.iterator();
      boolean haveMore = false;
      while (iter.hasNext())
      {
        ConfigThingy child = (ConfigThingy)iter.next();
        boolean result = child.rollcall(this, name, found, level, searchLevel, getParents); 
        haveMore = haveMore || result;
      }
      return haveMore;
    }
    return true;
  }
  
  
  public ConfigThingy get(String name)
  {
    return get(name, false);
  }
  
  public ConfigThingy getByChild(String name)
  {
    return get(name, true);
  }
  
  protected ConfigThingy get(String name, boolean getParents)
  {
    List found = new Vector();
    boolean haveMore;
    int searchlevel = 1;
    do{
      haveMore = rollcall(this, name, found, -1, searchlevel, getParents);
      ++searchlevel;
    }while(found.isEmpty() && haveMore );
    
    switch (found.size())
    {
      case 0: return null;
      case 1: return (ConfigThingy)found.get(0);
      default: return new ConfigThingy(found); 
    }
  }
  
  public String toString()
  {
    if (children.isEmpty()) return name;
    StringBuffer buf = new StringBuffer();
    Iterator iter = children.iterator();
    while (iter.hasNext()) buf.append(iter.next().toString());
    return buf.toString();
  }
  
  private interface Token
  {
    public static final int KEY = 0;
    public static final int STRING = 1;
    public static final int OPENPAREN = 2;
    public static final int CLOSEPAREN = 3;
    public static final int END = 4;
    public static final int INCLUDE = 5;
    public static final int LINECOMMENT = 6;
    public URL url();
    public int line();
    public int position();
    public int type();
    public String contentString();
  }
  
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
    
    public URL url() {return myURL;}
    public int line() {return myLine;}
    public int position() {return myPosition;}
    public String contentString() {return content;}
  }
  
  private static class StringToken extends StringContentToken
  {
    private static Pattern p = Pattern.compile("^\"((([^\"])|(\"\"))*)\"");
    
    public StringToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);
      Matcher m = p.matcher(tokenData);
      if (!m.find()) throw new IllegalArgumentException("String token expected!");
      //extract string inside "..." and replace "" with ", %n with "\n" and 
      //%% with %
      content = m.group(1).replaceAll("\"\"","\"").replaceAll("%n","\n").replaceAll("%%","%");
    }
    public int type() {return Token.STRING;}
    public static int atStartOf(String str)
    {
      Matcher m = p.matcher(str);
      if (!m.find()) return 0;
      return m.end();
    }
  }
  
  private static class KeyToken extends StringContentToken
  {
    private static Pattern p = Pattern.compile("^([a-zA-Z_][a-zA-Z_0-9]*)");
    
    public KeyToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);
      Matcher m = p.matcher(tokenData);
      if (!m.find()) throw new IllegalArgumentException("Key token expected!");
      content = m.group(1);
    }
    public int type() {return Token.KEY;}
    public static int atStartOf(String str)
    {
      Matcher m = p.matcher(str);
      if (!m.find()) return 0;
      return m.end();
    }
  }

  private static class OpenParenToken extends StringContentToken
  {
    public OpenParenToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = "(";
    }
    public int type() {return Token.OPENPAREN;}
    public static int atStartOf(String str)
    {
      return str.startsWith("(") ? 1 : 0;
    }
  }
  
  private static class CloseParenToken extends StringContentToken
  {
    public CloseParenToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = ")";
    }
    public int type() {return Token.CLOSEPAREN;}
    public static int atStartOf(String str)
    {
      return str.startsWith(")") ? 1 : 0;
    }
    
  }
 
  private static class IncludeToken extends StringContentToken
  {
    private static final String inc = "%include";
    public IncludeToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = inc;
    }
    public int type() {return Token.INCLUDE;}
    public static int atStartOf(String str)
    {
      return str.startsWith(inc) ? inc.length() : 0;
    }
  }
  
  private static class LineCommentToken extends StringContentToken
  {
  
    public LineCommentToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);
      content = tokenData.substring(1);
    }
    public int type() {return Token.LINECOMMENT;}
    public static int atStartOf(String str)
    {
      return str.startsWith("#") ? str.length() : 0;
    }
  }
  
  private static class EndToken extends StringContentToken
  {
    public EndToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = "";
    }
    public int type() {return Token.END;}
    public static int atStartOf(String str) {return 0;}
  }
  
  
  /**
   * 
   * @param ins
   * @return the list of Tokens, terminated by 7 end tokens.
   * @throws IOException
   * @throws SyntaxErrorException
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static List tokenize(URL url) throws IOException, SyntaxErrorException
  {
    InputStream ins = url.openStream();
    List tokens = new Vector();
    BufferedReader in = new BufferedReader(new InputStreamReader(ins));
    String line;
    
    int lineNo = 0;
    Pattern whitespace = Pattern.compile("^(\\p{Space}|,|;)+");
    while(null != (line = in.readLine()))
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
        if (0 != (tokenLength = KeyToken.atStartOf(line)))
        {
          tokens.add(new KeyToken(line,url,lineNo,pos+1));
        }
        else
          if (0 != (tokenLength = StringToken.atStartOf(line)))
          {
            tokens.add(new StringToken(line,url, lineNo,pos+1));
          }
          else
            if (0 != (tokenLength = OpenParenToken.atStartOf(line)))
            {
              tokens.add(new OpenParenToken(url,lineNo,pos+1));
            }
            else
              if (0 != (tokenLength = CloseParenToken.atStartOf(line)))
              {
                tokens.add(new CloseParenToken(url,lineNo,pos+1));
              }
              else
                if (0 != (tokenLength = IncludeToken.atStartOf(line)))
                {
                  tokens.add(new IncludeToken(url,lineNo,pos+1));
                }
                else
                  if (0 != (tokenLength = LineCommentToken.atStartOf(line)))
                  {
                    //LineCommentTokens werden nicht in tokens eingefügt, weil
                    //der Parser im Fall von 2er Paaren wie KEY STRING nicht in
                    //der Lage ist über Kommentare hinwegzulesen. Anstatt ihm das
                    //Einzubauen ist es einfacher, Kommentare einfach wegzuschmeissen.
                    //tokens.add(new LineCommentToken(line,url,lineNo,pos+1));
                  }
              else
              {
                throw new SyntaxErrorException(url+": Syntaxfehler in Zeile "+lineNo+" bei Zeichen "+(pos+1));
              }
        
        pos += tokenLength;
        line = line.substring(tokenLength);
      }
    }
    
      // add a couple EndTokens so that users don't have to worry about
      // checking if there's enough input remaining
    ++lineNo;
    for (int i = 0; i < 7; ++i) tokens.add(new EndToken(url,lineNo,0));
    
    return tokens;
  }
  
  public static String treeDump(ConfigThingy conf, String childPrefix)
  {
    StringBuffer buf = new StringBuffer();
    buf.append("\""+conf.name+"\"\n");
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy child = (ConfigThingy)iter.next();
      buf.append(childPrefix+"|\n"+childPrefix+"+--");
      char ch = iter.hasNext()?'|':' ';
      buf.append(treeDump(child, childPrefix+ch+"  "));
    }
    return buf.toString();
  }
  
  
  /**
   * Testet die Funktionsweise von ConfigThingy
   * @param args url [ get1 get2 ... ], dabei ist url die URL einer zu lesenden 
   *              Config-Datei. Das Programm gibt den sich daraus ergebenden
   *              ConfigThingy-Baum aus. Optional können noch weitere Argumente
   *              übergeben werden, die einen get-Pfad angeben 
   *              (configthingy.get(get1).get(get2)...) dessen Ergebnis 
   *              ausgegeben wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void main(String[] args) throws IOException
  {
    if (args.length < 1)
    {
      System.out.println("USAGE: <url> [ <get1> <get2> ... ]");
      System.exit(0);
    }
    
    File cwd = new File(System.getProperty("user.dir"));

    args[0] = args[0].replaceAll("\\\\","/");
    ConfigThingy conf = new ConfigThingy(args[0], new URL(cwd.toURL(),args[0]));
    
    System.out.println("Dies ist der Ergebnisbaum: \n\n"+treeDump(conf, ""));
    
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
          byChild = conf.getByChild(args[i]);
          getbychildstr = getstr + ".getByChild(\""+args[i]+"\")"; 
        }
        getstr = getstr + ".get(\""+args[i]+"\")";
        conf = conf.get(args[i]);
        if (conf == null) break;
      }
      
      if (conf == null)
      {
        System.out.println(getstr+" == null!");
      }
      else
      {
        System.out.println("Dies ist der Teilbaum für "+getstr+": \n\n");
        System.out.println(treeDump(conf, ""));
        System.out.println("\n\nDies ist der toString() Wert des Teilbaums:\n"+"\""+conf.toString()+"\"");
      }
      
      System.out.print("\n\n");
      
      if (byChild == null)
      {
        System.out.println(getbychildstr+" == null!");
      }
      else
      {
        System.out.println("Dies ist der Teilbaum für "+getbychildstr+": \n\n");
        System.out.println(treeDump(byChild, ""));
        System.out.println("\n\nDies ist der toString() Wert des Teilbaums:\n"+"\""+byChild.toString()+"\"");
      }
    }
  }
}
