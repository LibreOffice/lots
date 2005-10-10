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
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
  
  public ConfigThingy(InputStream ins) throws IOException
  {
    List stack = new Vector();
    List tokens = tokenize(ins);
    ListIterator liter = tokens.listIterator();
    Token token1, token2;
    do{
      token1 = (Token)liter.next();
      token2 = (Token)liter.next(); liter.previous();
      switch (token1.type())
      {
        case Token.KEY:
            switch (token2.type())
            {
              case Token.OPENPAREN: 
                  break;
              case Token.STRING:
                  break;
              default:
                  throw new SyntaxErrorException("Syntaxfehler in Zeile "+token2.line()+" bei Zeichen "+token2.position());
            }
            break;
      }
      
    } while(token1.type() != Token.END);
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
  
  public int count()
  {
    return children.size();
  }
    
  public Iterator iterator()
  {
    return children.iterator();
  }

  private boolean rollcall(ConfigThingy parent, String name, List found, int parentLevel, int searchLevel)
  {
    int level = parentLevel + 1;
    if (searchLevel == level)
    {
      if (name.equals(this.name))
      {
        if (level > 1 && parent.count() > 1)
          found.add(parent);
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
        haveMore = haveMore || child.rollcall(this, name, found, level, searchLevel);
      }
      return haveMore;
    }
    return true;
  }
  
  public ConfigThingy get(String name)
  {
    List found = new Vector();
    boolean haveMore;
    int searchlevel = 1;
    do{
      haveMore = rollcall(this, name, found, -1, searchlevel);
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
    
    public StringContentToken(int line, int position)
    {
      myLine = line;
      myPosition = position;
    }
    
    public int line() {return myLine;}
    public int position() {return myPosition;}
    public String contentString() {return content;}
  }
  
  private static class StringToken extends StringContentToken
  {
    private static Pattern p = Pattern.compile("^\"((([^\"])|(\"\"))*)\"");
    
    public StringToken(String tokenData, int line, int position)
    {
      super(line, position);
      Matcher m = p.matcher(tokenData);
      if (!m.find()) throw new SyntaxErrorException("String token expected!");
      //extract string inside "..." and replace "" with "
      content = m.group(1).replaceAll("\"\"","\"");
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
    
    public KeyToken(String tokenData, int line, int position)
    {
      super(line, position);
      Matcher m = p.matcher(tokenData);
      if (!m.find()) throw new SyntaxErrorException("Key token expected!");
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
    public OpenParenToken(int line, int position)
    {
      super(line, position);
      content = "(";
    }
    public int type() {return Token.OPENPAREN;}
    public static int atStartOf(String str)
    {
      return str.charAt(0) == '(' ? 1 : 0;
    }
  }
  
  private static class CloseParenToken extends StringContentToken
  {
    public CloseParenToken(int line, int position)
    {
      super(line, position);
      content = ")";
    }
    public int type() {return Token.CLOSEPAREN;}
    public static int atStartOf(String str)
    {
      return str.charAt(0) == ')' ? 1 : 0;
    }
    
  }
 
  private static class EndToken extends StringContentToken
  {
    public EndToken(int line, int position)
    {
      super(line, position);
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
  private static List tokenize(InputStream ins) throws IOException, SyntaxErrorException
  {
    List tokens = new Vector();
    BufferedReader in = new BufferedReader(new InputStreamReader(ins));
    String line;
    
    int lineNo = 0;
    Pattern whitespace = Pattern.compile("^\\p{Space}|,|;");
    while(null != (line = in.readLine()))
    {
      ++lineNo;
      int pos = 0;
      
      while (line.length() > 0)
      {
        Matcher wsm = whitespace.matcher(line);
        if (wsm.find())
        {
          pos += wsm.start();
          line = line.substring(wsm.start());
        }
        
        int tokenLength;
        if (0 != (tokenLength = KeyToken.atStartOf(line)))
        {
          tokens.add(new KeyToken(line,lineNo,pos));
        }
        else
          if (0 != (tokenLength = StringToken.atStartOf(line)))
          {
            tokens.add(new StringToken(line,lineNo,pos));
          }
          else
            if (0 != (tokenLength = OpenParenToken.atStartOf(line)))
            {
              tokens.add(new OpenParenToken(lineNo,pos));
            }
            else
              if (0 != (tokenLength = CloseParenToken.atStartOf(line)))
              {
                tokens.add(new CloseParenToken(lineNo,pos));
              }
              else
              {
                throw new SyntaxErrorException("Syntaxfehler in Zeile "+lineNo+" bei Zeichen "+(pos+1));
              }
        
        pos += tokenLength;
        line = line.substring(tokenLength);
      }
    }
    
      // add a couple EndTokens so that users don't have to worry about
      // checking if there's enough input remaining
    ++lineNo;
    for (int i = 0; i < 7; ++i) tokens.add(new EndToken(lineNo,0));
    
    return tokens;
  }
  
  public static void main(String[] args)
  {
  }
}
