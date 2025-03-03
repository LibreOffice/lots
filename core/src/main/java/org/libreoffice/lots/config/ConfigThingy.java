/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package org.libreoffice.lots.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigThingy represents a node in a tree created by parsing a WollMux configuration file.
 */
public class ConfigThingy implements Iterable<ConfigThingy>
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ConfigThingy.class);

  /**
   * Workaround for https://bugs.java.com/bugdatabase/view_bug.do?bug_id=5091921
   * TODO: Fixed in JDK 7 - probably the workaround can be removed?!
   */
  private static final int INTEGER_MAX = Integer.MAX_VALUE - 1;

  /**
   * Character set in which ConfigThingy files are stored.
   */
  public static final String CHARSET = "UTF-8";

  /**
   * Pattern for Unicode letters and digits.
   */
  private static final Pattern NON_LETTER_OR_DIGITS =
    Pattern.compile("\\P{javaLetterOrDigit}");

  /**
   * Pattern for characters that must be escaped in ConfigThingys.
   */
  private static final Pattern CONFIGTHINGY_SPECIAL = Pattern.compile("[%\n\r\"']");

  /**
   * Indentation for stringRepresentation().
   */
  private static final String INDENT = "  ";

  /**
   * Default value for the minlevel parameter if the get or query methods
   * are used without explicitly specifying minlevel.
   * Search depth 1 means that this is excluded from the search
   * and only descendant nodes are searched for.
   */
  private static final int DEFAULT_MINLEVEL = 1;

  private static final int ST_VALUE_LIST = 0;

  private static final int ST_PAIR_LIST = 1;

  private static final int ST_OTHER = 2;

  /** The child nodes. */
  private List<ConfigThingy> children;

  /** The name of the node. For leaves, this is the (string) value of the node. */
  private String name;

  /** Fallback names (to support old German config items) */
  Map<String, String> oldConfigItems = new HashMap<String, String>() {{
      put("ColumnTransformation", "Spaltenumsetzung");
      put("LibreOfficeSettings", "OOoEinstellungen");
      put("Functions", "Funktionen");
      put("PrintFunctions", "Druckfunktionen");
      put("MailSettings", "EMailEinstellungen");
      put("DataSources", "Datenquellen");
      put("DataSource", "Datenquelle");
      put("PersonalSenderListInit", "PersoenlicheAbsenderlisteInitialisierung");
      put("SearchStrategy", "Suchstrategie");
      put("TextBlocks", "Textbausteine");
      put("Warnings", "Warnungen");
      put("TextFragments", "Textfragmente");
      put("KeyboardShortcuts", "Tastenkuerzel");
      put("DefaultElements", "Standardelemente");
      put("RecipientSelection", "Empfaengerauswahl");
      put("InputFields", "Eingabefelder");
      put("SenderDataColumnTransformation", "AbsenderdatenSpaltenumsetzung");
      put("ExternalApplications", "ExterneAnwendungen");
      put("ContentBasedDirectives", "SachleitendeVerfuegungen");
      put("FunctionDialogs", "Funktionsdialoge");
      put("Search", "Suche");
      put("SearchResult", "Suchergebnis");
      put("Preview", "Vorschau");
      put("BottomArea", "Fussbereich");
      put("Menubar", "Menueleiste");
      put("Toolbars", "Symbolleisten");
      put("LetterheadToolbar", "Briefkopfleiste");
      put("Elemente", "Elements");
      put("Data", "Daten");
      put("DocumentActions", "Dokumentaktionen");
      put("Columns", "Spalten");
      put("DB_COLUMN", "DB_SPALTE");
      put("COPY_NAME", "ABDRUCK_NAME");
      put("OVERRIDE_FRAG_DB_COLUMN", "OVERRIDE_FRAG_DB_SPALTE");
  }};


  /**
   * Parses the data from the file specified by {@code url}.
   *
   * @param name
   *           the name of the root of the created ConfigThingy tree.
   * @throws IOException
   *           if loading data from url (or an included URL) fails.
   * @throws SyntaxErrorException
   *           if a syntactic error is found when parsing the data from url.
   */
  public ConfigThingy(String name, URL url) throws IOException, SyntaxErrorException
  {
    this(name);
    childrenFromUrl(url, new InputStreamReader(url.openStream(), CHARSET));
  }

  /**
   * Parses the string {@code confString} in the context of the null URL
   * (i.e. includes with relative URLs must not occur in confString).
   *
   * @param name
   *          the name of the root of the created ConfigThingy tree.
   * @throws IOException
   *          if loading from an included URL fails.
   * @throws SyntaxErrorException
   *          if a syntactical error is found when parsing the data.
   */
  public ConfigThingy(String name, String confString) throws IOException,
      SyntaxErrorException
  {
    this(name, null, new StringReader(confString));
  }

  /**
   * Parses the data from {@code read} in the context of the URL {@code url}.
   * {@code read} is closed both in case of an error and after a successful read.
   *
   * @param name
   *           the name of the root of the created ConfigThingy tree.
   * @throws IOException
   *           if loading data from {@code url} (or an included URL) fails.
   * @throws SyntaxErrorException
   *           if a syntactic error is found when parsing the data from url.
   */
  public ConfigThingy(String name, URL url, Reader read) throws IOException,
      SyntaxErrorException
  {
    this(name);
    childrenFromUrl(url, read);
  }

  /**
   * Copy Constructor (deep copy).
   */
  public ConfigThingy(ConfigThingy conf)
  {
    this(conf.getName());
    this.addChildCopiesFrom(conf);
  }

  /**
   * Creates a ConfigThingy with name/value {@code name}, without children.
   * Attention. With this method it is possible to create ConfigThingys
   * that do not adhere to the parser's syntax restrictions for keys.
   * If such a ConfigThingy is converted to text using stringRepresentation(),
   * the result will be something which the parser cannot read back in.
   */
  public ConfigThingy(String name)
  {
    this.name = name;
    this.children = new ArrayList<>(1);
  }

  /**
   * Creates an anonymous ConfigThingy with children from {@code children}.
   */
  private ConfigThingy(String name, List<ConfigThingy> children)
  {
    this.name = name;
    this.children = children;
  }

  /**
   * Adds independent copies of all children of {@code conf} to this ConfigThingy.
   * The children are copied along with all descendants.
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
   * Parses the data from {@code read} in the context of {@code url}
   * and appends the corresponding nodes as children to {@code this}.
   *
   * @throws IOException
   *           if loading data from {@code url} (or an included URL) fails.
   * @throws SyntaxErrorException
   *           if a syntactic error is found when parsing the data from {@code url}.
   */
  protected void childrenFromUrl(URL url, Reader read) throws IOException,
      SyntaxErrorException
  {
    try
    {
      Deque<ConfigThingy> stack = new ArrayDeque<>();
      stack.push(this);
      List<StringContentToken> tokens = tokenize(url, read);
      Iterator<StringContentToken> liter = tokens.iterator();
      Token token1;
      Token token2;
      do
      {
        token1 = liter.next();
        ConfigThingy child;
        switch (token1.type())
        {
          case INCLUDE:
            token2 = liter.next();
            if (token2.type() == TokenType.STRING && !token2.contentString().isEmpty())
            {
              try
              {
                URL includeURL = new URL(url, urlEncode(token2.contentString()));
                stack.peek().childrenFromUrl(includeURL,
                  new InputStreamReader(includeURL.openStream(), CHARSET));
              }
              catch (IOException iox)
              {
                throw new IOException(token2.url() + " in line " + token2.line()
                  + " at char " + token2.position()
                  + ": %include failed: ", iox);
              }
            }
            else
            {
              throw new SyntaxErrorException(token2.url()
                + ": URL string (enclosed in quotes) expected in line "
                + token2.line() + " at char " + token2.position());
            }
            break;

          case KEY:
            token2 = liter.next();
            switch (token2.type())
            {
              case OPENPAREN:
                child = new ConfigThingy(token1.contentString());
                stack.peek().addChild(child);
                stack.push(child);
                break;
              case STRING:
                child = new ConfigThingy(token1.contentString());
                ConfigThingy grandchild = new ConfigThingy(token2.contentString());
                child.addChild(grandchild);
                stack.peek().addChild(child);
                break;
              default:
                throw new SyntaxErrorException(token2.url()
                  + ": syntax error in line " + token2.line() + " at char "
                  + token2.position());
            }
            break;

          case STRING:
            child = new ConfigThingy(token1.contentString());
            stack.peek().addChild(child);
            break;

          case CLOSEPAREN:
            // Attention: root must not be popped.
            if (stack.size() <= 1)
              throw new SyntaxErrorException(token1.url()
                + ": Bracket ')' without matching bracket '(' in line "
                + token1.line() + " at char " + token1.position());
            stack.pop();
            break;

          case OPENPAREN:
            child = new ConfigThingy("");
            stack.peek().addChild(child);
            stack.push(child);
            break;

          case END:
            break;

          default:
            throw new SyntaxErrorException(token1.url() + ": syntax error in line"
              + token1.line() + " at char " + token1.position());
        }

      } while (token1.type() != TokenType.END);

      if (stack.size() > 1)
      {
        throw new SyntaxErrorException(token1.url() + ": " + (stack.size() - 1)
          + " closing brackets are missing");
      }
    }
    finally
    {
      try
      {
        read.close();
      }
      catch (Exception x)
      {
        LOGGER.trace("", x);
      }
    }
  }

  /**
   * Chases all characters forbidden in URLs through
   * URLEncoder,encode(ch,{@link #CHARSET}).
   * The space character gets a special treatment (conversion to %20),
   * because URLEncoder.encode() would convert it to "+",
   * which does not lead to the desired result, at least on some web servers.
   */
  public static String urlEncode(String url)
  {
    url = url.replaceAll("\\\\", "/");
    StringBuilder buffy = new StringBuilder();
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
          buffy.append(URLEncoder.encode(Character.toString(ch), CHARSET));
      }

      url = buffy.toString();
    }
    catch (UnsupportedEncodingException x)
    {
      LOGGER.trace("", x);
    }
    return url;
  }

  /**
   * Adds another child to this ConfigThingy.
   * ATTENTION! child is not copied, but inserted as a reference!
   */
  public void addChild(ConfigThingy child)
  {
    children.add(child);
  }

  /**
   * Adds a new child named {@code childName} as the last child and returns a reference to the new child.
   * Attention. With this method it is possible to create ConfigThingys that do not adhere to the
   * parser's syntax restrictions for keys. If such a ConfigThingy is converted to text using
   * {@code stringRepresentation()}, something is created that the parser cannot read back in.
   */
  public ConfigThingy add(String childName)
  {
    ConfigThingy newChild = new ConfigThingy(childName);
    addChild(newChild);
    return newChild;
  }

  /**
   * Returns the number of children.
   */
  public int count()
  {
    return children.size();
  }

  /**
   * Returns an iterator over the children of this ConfigThingy.
   */
  @Override
  public Iterator<ConfigThingy> iterator()
  {
    return children.iterator();
  }

  /**
   * Returns a ConfigThingy named {@code <visible nodes>} whose children are all nodes of the
   * ConfigThingy tree with root {@code root}, have name {@code nodeNameToScanFor} and
   * are visible from node {@code node}.
   * Here, a node is visible from node if it is node itself, a brother/sister node of node, an
   * ancestor of node, or a brother/sister node of an ancestor of node.
   */
  public static ConfigThingy getNodesVisibleAt(ConfigThingy node,
      String nodeNameToScanFor, ConfigThingy root)
  {
    Deque<List<ConfigThingy>> s = new ArrayDeque<>();
    List<ConfigThingy> r = new ArrayList<>();
    getNodesVisibleAt(node, nodeNameToScanFor, s, root, r);
    return new ConfigThingy("<visible nodes>", r);
  }

  private static boolean getNodesVisibleAt(ConfigThingy node,
      String nodeNameToScanFor, Deque<List<ConfigThingy>> s, ConfigThingy root,
      Collection<ConfigThingy> result)
  {
    if (root == node)
    {
      for (List<ConfigThingy> v : s)
        result.addAll(v);
      return true;
    }

    List<ConfigThingy> v = new ArrayList<>();
    for (ConfigThingy child : root)
    {
      if (child.getName().equals(nodeNameToScanFor)) {
        v.add(child);
      }
    }

    s.push(v);

    for (ConfigThingy child : root)
    {
      if (getNodesVisibleAt(node, nodeNameToScanFor, s, child, result)) {
        return true;
      }
    }

    s.pop();
    return false;
  }

  /**
   * Returns the name of this node of the config tree. In the case of a leaf, this corresponds to
   * the (string) value.
   *
   * @return Name of the node, or its value.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Changes the name of this node to {@code newName}.
   * Attention. With this method it is possible to create ConfigThingys that do not adhere
   * to the parser's syntax restrictions for keys.
   * If such a ConfigThingy is converted to text using stringRepresentation(),
   * something is created that the parser cannot read back in.
   */
  public void setName(String newName)
  {
    name = newName;
  }

  /**
   * Returns the first child node.
   *
   * @throws NodeNotFoundException
   *           if {@code this} has no children.
   */
  public ConfigThingy getFirstChild() throws NodeNotFoundException
  {
    if (children.isEmpty())
      throw new NodeNotFoundException("Knoten " + getName() + " hat keine Kinder");
    return children.get(0);
  }

  /**
   * Like getFirstChild(), but if there is no child available,
   * IndexOutOfBoundsException is thrown instead of NodeNotFoundException.
   */
  private ConfigThingy getFirstChildNoThrow()
  {
    return children.get(0);
  }

  /**
   * Returns the last child node.
   *
   * @throws NodeNotFoundException
   *           if {@code this} has no children.
   */
  public ConfigThingy getLastChild() throws NodeNotFoundException
  {
    if (children.isEmpty())
      throw new NodeNotFoundException("Knoten " + getName() + " hat keine Kinder");
    return children.get(children.size() - 1);
  }

  /**
   * Searches the subtree with root {@code this} for nodes with name {@code name}
   * at searchlevel {@code searchlevel} and adds them
   * (or their parents if {@code getParents==true}) to the list {@code found}.
   *
   * @param parent
   *          Der Elternknoten von this.
   * @param name
   *          der Name nach dem zu suchen ist.
   * @param found
   *          the found nodes (or if {@code getParents==true} their parents)
   *          are inserted into this list. Each node appears at most once in this list,
   *          i.e. if getParents==true and a node has several children with name name,
   *          this node is nevertheless inserted only once.
   * @param parentLevel
   *          the search depth for breadth-first search of {@code parent},
   *          i.e. this has search depth {@code parentLevel + 1}
   * @param searchLevel
   *          The search depth at which the nodes are to be searched for.
   *          Nodes at other search depths are not inserted in {@code found}.
   * @param getParents
   *          if {@code true}, the parent nodes are inserted instead of the found nodes.
   *          However, each parent node is always inserted only once,
   *          even if it has several matching children.
   * @return {@code true} if at least one node with search depth searchlevel has been reached,
   *         i.e. if a search with a higher searchlevel could possibly yield results.
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
          if (!found.contains(parent)) {
            found.add(parent);
          }
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
   * Performs a breadth-first search for descendant nodes of {@code this}
   * that have {@code name} as their name.
   *
   * @return If there are corresponding nodes, the lowest search depth is determined
   * on which corresponding nodes can be found and all nodes on this search depth are returned.
   * If that is exactly one, it is returned directly, otherwise a ConfigThingy
   * named {@code <query results>} is returned which has these nodes (and only these) as children.
   *
   * @throws NodeNotFoundException
   *           if no corresponding nodes were found.
   *           If this is not desired, {@link #query(String)} can be used.
   */
  public ConfigThingy get(String name) throws NodeNotFoundException
  {
    return get(name, INTEGER_MAX);
  }

  public <T extends RuntimeException> ConfigThingy get(String name, Class<T> ex, String msg)
  {
  	try
  	{
  		return get(name);
  	}
  	catch (NodeNotFoundException e)
  	{
  		try
			{
				throw ex.getConstructor(String.class, Throwable.class).newInstance(msg, e);
			} catch (InstantiationException | IllegalAccessException
			    | IllegalArgumentException | InvocationTargetException
			    | NoSuchMethodException | SecurityException e1)
			{
				throw new RuntimeException(e1);
			}
  	}
  }

  public String getString(String name)
  {
    try
    {
      return get(name, INTEGER_MAX).toString();
    }
    catch (NodeNotFoundException e)
    {
      return null;
    }
  }

  /**
   * This simplification method to avoid cluttered exception blocks returns the result of
   * {@code get(name).toString()} or {@code defStr} if the element you are looking for does not exist.
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
   * Like {@link #get(String)}, but it returns maximum results of
   * search depth {@code maxlevel} (0 is {@code this}).
   */
  public ConfigThingy get(String name, int maxlevel) throws NodeNotFoundException
  {
    return get(name, maxlevel, DEFAULT_MINLEVEL);
  }

  /**
   * Like {@link #get(String)}, but only results whose search depth is less than/equal
   * to {@code maxlevel} and greater than/equal to {@code minlevel} (0 is {@code this}) are returned.
   */
  public ConfigThingy get(String name, int maxlevel, int minlevel)
      throws NodeNotFoundException
  {
    ConfigThingy res = query(name, false, maxlevel, minlevel);
    if (res.count() == 0)
    {
      if (oldConfigItems.containsKey(name))
        res = get(oldConfigItems.get(name), maxlevel, minlevel);
      if (res.count() == 0)
        throw new NodeNotFoundException("Knoten " + getName() + " hat keinen Nachfahren '" + name + "'");
    }

    if (res.count() == 1) {
      res = res.iterator().next();
    }
    return res;
  }

  /**
   * Like {@link #get(String)}, but it basically sets a ConfigThingy
   * named {@code <query results>} over the results.
   * In case there are no results, a ConfigThingy without children is returned
   * instead of {@code null}.
   */
  public ConfigThingy query(String name)
  {
    return query(name, false, INTEGER_MAX, DEFAULT_MINLEVEL);
  }

  /**
   * Like {@link #get(String, int)}, but it basically sets a ConfigThingy named
   * {@code <query results>} over the results.
   * In case there are no results, a ConfigThingy without children is returned
   * instead of {@code null}.
   */
  public ConfigThingy query(String name, int maxlevel)
  {
    return query(name, false, maxlevel, DEFAULT_MINLEVEL);
  }

  /**
   * Like {@link #get(String, int, int)}, but it basically sets a ConfigThingy
   * named {@code <query results>} over the results.
   * In case there are no results, a ConfigThingy without children is returned
   * instead of null.
   */
  public ConfigThingy query(String name, int maxlevel, int minlevel)
  {
    return query(name, false, maxlevel, minlevel);
  }

  /**
   * Like {@link #get(String)}, but the parent nodes of the found nodes are returned
   * instead of the nodes themselves.
   * Note that each parent node is included in the results only exactly once,
   * even if it has has several matching children,
   *
   * @throws NodeNotFoundException
   *           if no corresponding nodes were found.
   *           If this is not desired, {@link #query(String)} can be used.
   */
  public ConfigThingy getByChild(String name) throws NodeNotFoundException
  {
    return getByChild(name, INTEGER_MAX);
  }

  /**
   * Like {@link #get(String, int)}, but it returns the parent nodes of the
   * found nodes instead of the nodes themselves.
   * Note that each parent node is included in the results only exactly once,
   * even if it has multiple matching children
   *
   * @throws NodeNotFoundException
   *           if no corresponding nodes were found.
   *           If this is not desired, {@link #query(String, int)} can be used.
   */
  public ConfigThingy getByChild(String name, int maxlevel)
      throws NodeNotFoundException
  {
    return getByChild(name, maxlevel, DEFAULT_MINLEVEL);
  }

  /**
   * Like {@link #get(String, int, int)}, but it returns the parent nodes of the
   * found nodes instead of the nodes themselves.
   * Note that each parent node is included in the results only exactly once,
   * even if it has multiple matching children.
   *
   * @throws NodeNotFoundException
   *           if no corresponding nodes were found.
   *           If this is not desired, {@link #query(String, int, int)} can be used.
   */
  public ConfigThingy getByChild(String name, int maxlevel, int minlevel)
      throws NodeNotFoundException
  {
    ConfigThingy res = query(name, true, maxlevel, minlevel);
    if (res.count() == 0)
      throw new NodeNotFoundException("Node " + getName()
        + " has no descendant '" + name + "'");
    if (res.count() == 1) {
      res = res.iterator().next();
    }
    return res;
  }

  /**
   * Like {@link #query(String)}, but the parent nodes of the found nodes are returned
   * instead of the nodes themselves.
   * Note that each parent node is included in the results only exactly once,
   * even if it has multiple matching children.
   */
  public ConfigThingy queryByChild(String name)
  {
    return query(name, true, INTEGER_MAX, DEFAULT_MINLEVEL);
  }

  /**
   * Like {@link #query(String, int)}, but it returns the parent nodes of the
   * found nodes instead of the nodes themselves.
   * Note that each parent node is only included in the results exactly once,
   * even if it has has several matching children.
   */
  public ConfigThingy queryByChild(String name, int maxlevel)
  {
    return query(name, true, maxlevel, DEFAULT_MINLEVEL);
  }

  /**
   * Like {@link #query(String, int, int)}, but it returns the parent nodes
   * of the found nodes instead of the nodes themselves.
   * Note that each parent node is included in the results only exactly once,
   * even if it has multiple matching children.
   */
  public ConfigThingy queryByChild(String name, int maxlevel, int minlevel)
  {
    return query(name, true, maxlevel, minlevel);
  }

  /**
   * Searches recursively for all nodes with a given name.
   *
   * @param name
   * @param maxlevel Maximum depth at which to search.
   * @param getParents If {@code true}, returns the parent node.
   * @return A collection of all nodes with the name {@code name} or its parents.
   */
  public ConfigThingy queryAll(String name, int maxlevel, boolean getParents)
  {
    ArrayList<ConfigThingy> found = new ArrayList<>();

    boolean hasMore;

    int searchlevel = 1;
    do
    {
      hasMore = rollcall(this, name, found, -1, searchlevel++, getParents);
    } while (hasMore && searchlevel < maxlevel + 1);

    return new ConfigThingy("<query results>", found);
  }

  /**
   * If {@code getParents == false} this function behaves like {@link #get(String, int, int)},
   * if {@code getParents == true} like {@link #getByChild(String, int, int)}.
   */
  protected ConfigThingy query(String name, boolean getParents, int maxlevel,
      int minlevel)
  {
    List<ConfigThingy> found = new ArrayList<>();
    boolean haveMore;
    int searchlevel = minlevel;
    do
    {
      if (searchlevel > maxlevel) {
        break;
      }
      haveMore = rollcall(this, name, found, -1, searchlevel, getParents);
      ++searchlevel;
    } while (found.isEmpty() && haveMore);

    if (found.isEmpty() && oldConfigItems.containsKey(name)) {
      ConfigThingy res = query(oldConfigItems.get(name), getParents, maxlevel, minlevel);
      if (res.count() == 0)
        return new ConfigThingy("<query results>");
    }
    if (found.isEmpty()) {
      return new ConfigThingy("<query results>");
    }
    return new ConfigThingy("<query results>", found);
  }

  /**
   * If {@code getParents == false} this function behaves like {@link #get(String, int)},
   * if {@code getParents == true} like {@link #getByChild(String, int)}.
   */
  protected ConfigThingy query(String name, boolean getParents, int maxlevel)
  {
    return query(name, getParents, maxlevel, DEFAULT_MINLEVEL);
  }

  /**
   * If {@code getParents == false} this function behaves like {@link #get(String, int)},
   * if {@code getParents == true} like {@link #getByChild(String, int)}.
   */
  @Override
  public String toString()
  {
    if (children.isEmpty()) {
      return name;
    }

    // Optimierung: Nicht unnötig StringBuilder produzieren
    if (children.size() == 1) {
      return children.get(0).toString();
    }

    StringBuilder buf = new StringBuilder();
    for (ConfigThingy child : children)
      buf.append(child.toString());
    return buf.toString();
  }

  /**
   * Returns a string representation of the complete ConfigThingy tree,
   * suitable to be saved to a file and parsed from there again as ConfigThingy.
   *
   * @param childrenOnly
   *          if true no outermost nesting is created with the name of this.
   * @param stringChar
   *          the character to be used to enclose strings.
   * @param escapeAll
   *          if {@code true}, all characters in strings that are not letters
   *          or digits are escaped with the %u syntax.
   * @throws IllegalArgumentException
   *           if {@code stringChar} is not ' or ".
   */
  public String stringRepresentation(boolean childrenOnly, char stringChar,
      boolean escapeAll)
  {
    if (stringChar != '"' && stringChar != '\'')
      throw new java.lang.IllegalArgumentException(
        "Only \" and ' are allowed as string limiters.");

    StringBuilder buf = new StringBuilder();
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
   * Like {@link #stringRepresentation(boolean, char, boolean)} with {@code escapeAll == false}.
   */
  public String stringRepresentation(boolean childrenOnly, char stringChar)
  {
    return stringRepresentation(childrenOnly, stringChar, false);
  }

  /**
   * Like {@link #stringRepresentation(boolean, char)} with {@code stringRepresentation(false, '"')}.
   */
  public String stringRepresentation()
  {
    return stringRepresentation(false, '"');
  }

  /**
   * Replaces ' with '', \n with %n, % with %%.
   *
   * @param escapeAll
   *          if true all {@link #NON_LETTER_OR_DIGITS} are escaped as Unicode escapes.
   */
  private String escapeString(String str, char stringChar, boolean escapeAll)
  {
    Pattern p;
    if (escapeAll)
      p = NON_LETTER_OR_DIGITS;
    else
      p = CONFIGTHINGY_SPECIAL;

    Matcher m = p.matcher(str);
    ArrayList<Integer> locations = new ArrayList<>();
    while (m.find())
      locations.add(Integer.valueOf(m.start()));
    StringBuilder buffy = new StringBuilder(str);
    while (!locations.isEmpty())
    {
      int idx = locations.remove(locations.size() - 1).intValue();

      String repstr = Character.toString(buffy.charAt(idx));

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
          default:
            break;
        }
      }
      buffy.replace(idx, idx + 1, repstr);
    }
    return buffy.toString();
  }

  /**
   * Appends a textual representation of these ConfigThingys to {@code buf}.
   * Each line is prefixed with {@code childPrefix}.
   *
   * @param escapeAll
   *          if true, all characters in strings that are not letters
   *          or digits are escaped with the %u syntax.
   */
  private void stringRepresentation(StringBuilder buf, String childPrefix,
      char stringChar, boolean escapeAll)
  {

    if (count() == 0) // leaf
    {
      buf.append(stringChar + escapeString(getName(), stringChar, escapeAll)
        + stringChar);
    }
    else if (count() == 1 && getFirstChildNoThrow().count() == 0) // Schlüssel-Wert-Paar
    {
      /*
       * Normally key-value pairs are represented without parentheses,
       * but in the special case that the key is empty (list with only one element)
       * the parentheses must not be omitted.
       */
      buf.append(getName());
      if (getName().length() == 0)
        buf.append('(');
      else
        buf.append(' ');

      getFirstChildNoThrow().stringRepresentation(buf, childPrefix, stringChar,
        escapeAll);
      if (getName().length() == 0) {
        buf.append(')');
      }
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
            if (type == ST_VALUE_LIST) {
              buf.append(',');
            }
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

  private int structureType()
  {
    int count = -1;
    for (ConfigThingy child : children)
    {
      if (count == -1) {
        count = child.count();
      }
      if (count != child.count()) {
        return ST_OTHER;
      }
      if (count > 1) {
        return ST_OTHER;
      }
      if (count == 1 && child.getFirstChildNoThrow().count() > 0) {
        return ST_OTHER;
      }
    }

    return count == 0 ? ST_VALUE_LIST : ST_PAIR_LIST;
  }

  /**
   * This method checks if the passed {@code id} is a valid identifier according to the syntax
   * for WollMux config files.
   *
   * @param id
   *          ID to be checked
   * @throws InvalidIdentifierException
   *          When {@code id} is not a valid identifier
   */
  public static void checkIdentifier(String id) throws InvalidIdentifierException
  {
    if (!id.matches("^[a-zA-Z_][a-zA-Z_0-9]*$"))
      throw new InvalidIdentifierException(id);
  }

  private enum TokenType
  {
    KEY,
    STRING,
    OPENPAREN,
    CLOSEPAREN,
    END,
    INCLUDE,
    LINECOMMENT;
  }

  /**
   * The {@link ConfigThingy#tokenize(URL)} method returns a list of objects
   * that all implement this interface.
   */
  private interface Token
  {
    /**
     * Returns the URL of the document in which this token was found.
     */
    public URL url();

    /**
     * Returns the line in which this token was found.
     */
    public int line();

    /**
     * Returns the position of the first character of this token in its line, counted from 1.
     */
    public int position();

    /**
     * Returns the type of this token, e.g. {@link #KEY}.
     */
    public TokenType type();

    /**
     * Returns the text representation of this token.
     * This is NOT necessarily identical with the string from which this token was parsed.
     * For example, separators like the quotation marks for delimiting strings do not appear
     * in the string returned here.
     * Nor do escape sequences used in the input data stream to represent certain characters
     * such as newline appear in this string.
     */
    public String contentString();
  }

  /**
   * Abstract base class for tokens that store their {@link #contentString()} value in a string variable.
   */
  private abstract static class StringContentToken implements Token
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
   * Token for a string according to the syntax for WollMux config files.
   */
  private static class StringToken extends StringContentToken
  {
    /**
     * Creates a new StringToken
     *
     * @param tokenData
     *          a string for which {@link #atStartOf(String)} returns a value greater than 0.
     */
    public StringToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);

      int len = atStartOf(tokenData);
      if (len < 2) {
        throw new IllegalArgumentException("String token expected!");
      }

      char quote = tokenData.charAt(0);

      /*
       * Evaluate % escapes, as well as replace quotequote with quote
       */
      StringBuilder buffy = new StringBuilder(tokenData.substring(1, len - 1));
      String quoteStr = Character.toString(quote);
      int startidx = 0;
      int idx;
      while (true)
      {
        idx = buffy.indexOf("%", startidx);
        int idx2 = buffy.indexOf(quoteStr, startidx);

        if (idx < 0 && idx2 < 0) {
          break;
        }

        String repstr;
        int replen;

        if (idx < 0 || (idx2 >= 0 && idx2 < idx)) // quotequote
        {
          idx = idx2;
          replen = 2;
          repstr = quoteStr;
        }
        else // % escape
        {
          if (idx + 1 >= buffy.length()) {
            break;
          }

          // default: replace with the same character, i.e. %
          repstr = Character.toString(buffy.charAt(idx));
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
            default:
              break;
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
        return Character.toString(ch);
      }
      catch (NumberFormatException x)
      {
        throw new IllegalArgumentException("Incorrect hex number in %u escape: \"%u"
          + code + "\"");
      }
    }

    @Override
    public TokenType type()
    {
      return TokenType.STRING;
    }

    /**
     * Returns the length of the longest prefix of str which can be interpreted
     * as a token of this class.
     */
    public static int atStartOf(String str)
    {
      if (str.length() < 2) {
        return 0;
      }
      char quote = str.charAt(0);
      if (quote != '"' && quote != '\'') {
        return 0;
      }

      int idx = 1;
      while (true)
      {
        idx = str.indexOf(quote, idx);
        if (idx < 0) {
          return 0;
        }
        ++idx;
        if (idx >= str.length() || str.charAt(idx) != quote) {
          return idx;
        }
        ++idx;
      }
    }
  }

  /**
   * A token for a key according to the syntax for WollMux config files.
   */
  private static class KeyToken extends StringContentToken
  {
    /**
     * Regex for the identification of valid keys.
     */
    private static Pattern p = Pattern.compile("^([a-zA-Z_][a-zA-Z_0-9]*)");

    /**
     * Creates a new KeyToken
     *
     * @param tokenData
     *          a string for which {@link #atStartOf(String)} returns a value greater than 0.
     */
    public KeyToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);
      Matcher m = p.matcher(tokenData);
      if (!m.find()) {
        throw new IllegalArgumentException("Key token expected!");
      }
      content = m.group(1);
    }

    @Override
    public TokenType type()
    {
      return TokenType.KEY;
    }

    /**
     * Returns the length of the longest prefix of str which can be interpreted
     * as a token of this class.
     */
    public static int atStartOf(String str)
    {
      Matcher m = p.matcher(str);
      if (!m.find()) {
        return 0;
      }
      return m.end();
    }
  }

  /**
   * Token for opening round bracket.
   */
  private static class OpenParenToken extends StringContentToken
  {
    public OpenParenToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = "(";
    }

    @Override
    public TokenType type()
    {
      return TokenType.OPENPAREN;
    }

    /**
     * Returns 1 if {@code str} starts with '(', 0 otherwise.
     */
    public static int atStartOf(String str)
    {
      return str.startsWith("(") ? 1 : 0;
    }
  }

  /**
   * Token for closing round bracket.
   */
  private static class CloseParenToken extends StringContentToken
  {
    public CloseParenToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = ")";
    }

    @Override
    public TokenType type()
    {
      return TokenType.CLOSEPAREN;
    }

    /**
     * Returns 1 if {@code str} starts with ')', 0 otherwise.
     */
    public static int atStartOf(String str)
    {
      return str.startsWith(")") ? 1 : 0;
    }
  }

  /**
   * Token for the string "%include".
   */
  private static class IncludeToken extends StringContentToken
  {
    private static final String INC = "%include";

    public IncludeToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = INC;
    }

    @Override
    public TokenType type()
    {
      return TokenType.INCLUDE;
    }

    /**
     * Returns the length of the longest prefix of str which can be interpreted
     * as a token of this class.
     */
    public static int atStartOf(String str)
    {
      return str.startsWith(INC) ? INC.length() : 0;
    }
  }

  /**
   * Token for a comment according to WollMux config file syntax.
   * ATTENTION: Tokens of this class are currently not returned by
   * {@link ConfigThingy#tokenize(URL)}, but discarded.
   */
  private static class LineCommentToken extends StringContentToken
  {

    /**
     * Creates a new LineCommentToken.
     *
     * @param tokenData
     *          a string whose first character is '#'.
     */
    public LineCommentToken(String tokenData, URL url, int line, int position)
    {
      super(url, line, position);
      content = tokenData.substring(1);
    }

    @Override
    public TokenType type()
    {
      return TokenType.LINECOMMENT;
    }

    /**
     * Returns the length of the longest prefix of {@code str}
     * which can be interpreted as a token of this class.
     */
    public static int atStartOf(String str)
    {
      return str.startsWith("#") ? str.length() : 0;
    }
  }

  /**
   * Signals the end of the input data stream.
   */
  private static class EndToken extends StringContentToken
  {
    public EndToken(URL url, int line, int position)
    {
      super(url, line, position);
      content = "";
    }

    @Override
    public TokenType type()
    {
      return TokenType.END;
    }
  }

  /**
   * Splits the data from {@code read} into {@link Tokens}s.
   * As source URL {@code url} is entered in the tokens.
   *
   * @return the list of identified tokens, terminated by at least 7 {@link EndToken}s.
   * @throws IOException
   *           in case something goes wrong when accessing the data from {@code url}.
   * @throws SyntaxErrorException
   *           if a string cannot be identified as a token.
   */
  private static List<StringContentToken> tokenize(URL url, Reader read)
      throws IOException, SyntaxErrorException
  {
    List<StringContentToken> tokens = new ArrayList<>();
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
          if (line.length() == 0) {
            continue;
          }
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
          }
          else
          {
            throw new SyntaxErrorException(url + ": syntax error in line " + lineNo
              + " at char " + (pos + 1) + ", text in error location: \"" + line
              + "\"");
          }
        }
        catch (IllegalArgumentException x)
        {
          throw new SyntaxErrorException(url + ": syntax error in line " + lineNo
            + " at char " + (pos + 1) + ", text in error location: \"" + line
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
   * Returns a textual tree representation of {@code conf}.
   * Each line is prefixed with {@code childPrefix}.
   */
  public static String treeDump(ConfigThingy conf, String childPrefix)
  {
    StringBuilder buf = new StringBuilder();
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
}
