/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.db;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.config.NodeNotFoundException;
import org.libreoffice.lots.util.L;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data source for accessing an LDAP directory.
 *
 * @author Max Meier (D-III-ITD 5.1)
 */
public class LDAPDatasource extends Datasource
{

  private static final Logger LOGGER = LoggerFactory.getLogger(LDAPDatasource.class);

  private List<String> schema;

  private String datasourceName;

  private String url;

  private String baseDN;

  private String objectClass;

  /** Properties for the connection to the LDAP server. */
  private Properties properties = new Properties();

  /** Separator for generating keys from multiple key values. */
  private static final String SEPARATOR = "&:=&:%";

  /**
   * Splits the first part of the key, corresponding to paths with Level 0, from the rest of the key.
   */
  private static final String KEY_SEPARATOR_0_NON_0_RE = "==%§%==";

  /** Map of query strings to LDAP attribute names */
  private Map<String, ColumnDefinition> columnDefinitions = new HashMap<>();

  /** Key-Attribute (LDAP) (Strings). */
  private List<Object> keyAttributes = new ArrayList<>();

  /**
   * What types of paths are present as key columns? (ABSOLUTE_ONLY, RELATIVE_ONLY,
   * ABSOLUTE_AND_RELATIVE).
   */
  private int keyStatus; // 0:= only absolute attributes, 1:= absolute and

  // relative Attribute, 2:= only relative Attribute
  /** Only attribute paths of the form 0:*. */
  private static final int ABSOLUTE_ONLY = 0;

  /** Only attribute paths of the form num:*, where num is not 0. */
  private static final int RELATIVE_ONLY = 2;

  /** Both attribute paths of the form 0:* and the form num:* with num not equal to 0. */
  private static final int ABSOLUTE_AND_RELATIVE = 1;

  /** Regex for allowed identifiers */
  private static final Pattern SPALTENNAME = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");

  /**
   * Regex for checking the syntax of BASE_DN. TODO: Likely too restrictive!
   */
  private static final Pattern BASEDN_RE = Pattern
      .compile("^[a-zA-Z]+=[a-zA-ZäÄöÖüÜß \\\\()-]+(,[a-zA-Z]+=[a-zA-ZäÄöÖüÜß \\\\()-]+)*$");

  /**
   * Regex for checking the syntax of LDAP attribute identifiers. TODO: Likely too restrictive!
   */
  private static final Pattern ATTRIBUTE_RE = Pattern.compile("^[a-zA-Z]+$");

  /**
   * Regex to check if a key is legal for the LDAP data source.
   */
  private static final Pattern KEY_RE = Pattern.compile("^(\\(&(\\([^()=]+[^()]*\\))+\\))?"
      + KEY_SEPARATOR_0_NON_0_RE + "([a-zA-Z_][a-zA-Z0-9_]*=.*" + SEPARATOR + ")?$");

  /**
   * temporary cache for relative attributes (is created with every new search)
   */
  private Map<CacheKey, Attributes> attributeCache = new HashMap<>();

  /**
   * Creates a new LDAP Datasource.
   *
   * @param nameToDatasource
   *          Contains all data sources that were fully instantiated up to the
   *          point of defining this LDAPDatasource (not currently used).
   * @param sourceDesc
   *          The 'data source' node containing the description of this LDAPDatasource..
   * @param context
   *          The context relative to which URLs should be resolved (not currently used)..
   * @throws ConfigurationErrorException
   *           If there is an error in the definition in sourceDesc.
   */
  @SuppressWarnings("squid:S2068")
  public LDAPDatasource(Map<String, Datasource> nameToDatasource, ConfigThingy sourceDesc,
      URL context)
  {
    setTimeout(Datasource.getDatasourceTimeout());

    datasourceName = parseConfig(sourceDesc, "NAME", () -> L.m("NAME of data source is missing"));
    url = parseConfig(sourceDesc, "URL", () -> errorMessage() + L.m("URL of LDAP-Server is missing."));

    try
    {
      new URI(url);
    } catch (URISyntaxException e)
    {
      throw new ConfigurationErrorException(L.m("Error in LDAP-URL " + url, e));
    }

    baseDN = parseConfig(sourceDesc, "BASE_DN",
        () -> errorMessage() + L.m("BASE_DN of LDAP-Server is missing."));
    if (!BASEDN_RE.matcher(baseDN).matches())
    {
      throw new ConfigurationErrorException(L.m("BASE_DN-value is invalid: " + baseDN));
    }

    objectClass = parseConfig(sourceDesc, "OBJECT_CLASS",
        () -> errorMessage() + L.m("No OBJECT_CLASS defined."));
    if (!ATTRIBUTE_RE.matcher(objectClass).matches())
    {
      throw new ConfigurationErrorException(
          L.m("OBJECT_CLASS contains of forbidden characters: " + objectClass));
    }

    String user = "";
    String password = "";
    try
    {
      user = sourceDesc.get("USER").toString();
      password = sourceDesc.get("PASSWORD").toString();
    } catch (NodeNotFoundException e)
    {
      LOGGER.debug("Username oder Passwort für den LDAP-Server fehlt.", e);
    }

    // set properties
    properties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    properties.put(Context.PROVIDER_URL, url);

    if (!user.isEmpty() && !password.isEmpty())
    {
      properties.put(Context.SECURITY_PRINCIPAL, user);
      properties.put(Context.SECURITY_CREDENTIALS, password);
    }

    ConfigThingy spalten = sourceDesc.query("Columns");

    if (spalten.count() == 0)
      throw new ConfigurationErrorException(errorMessage() + L.m("Section 'Columns' is missing."));

    schema = new ArrayList<>();

    // Iterate over all column relationships
    for (ConfigThingy spaltenDesc : spalten)
    {
      // Iterate over a column relationship
      for (ConfigThingy spalteDesc : spaltenDesc)
      {
        String spalte = parseConfig(spalteDesc, "DB_COLUMN",
            () -> errorMessage() + L.m("Specification of DB_COLUMN is missing"));
        if (!SPALTENNAME.matcher(spalte).matches())
        {
          throw new ConfigurationErrorException(errorMessage()
              + L.m("Column \"{0}\" does not match the syntax of an identifier", spalte));
        }

        String path = parseConfig(spalteDesc, "PATH",
            () -> L.m("Path specification is missing for column {0}", spalte));
        int relativePath;
        String attributeName;
        String columnObjectClass = null;
        String lineSeparator = null;

        // get relativePath and attributeName
        String[] splitted = path.split(":");

        if (splitted.length != 2)
        {
          throw new ConfigurationErrorException(
              errorMessage() + L.m("Syntax error in path specification of {0}", spalte));
        }

        try
        {
          relativePath = Integer.parseInt(splitted[0]);
        } catch (NumberFormatException e)
        {
          throw new ConfigurationErrorException(
              errorMessage() + L.m("Syntaxerror in specification of the relative path of {0}", spalte));
        }

        attributeName = splitted[1];
        if (!ATTRIBUTE_RE.matcher(attributeName).matches())
          throw new ConfigurationErrorException(
              L.m("Illegal attribute identifier: \"{0}\"", attributeName));

        columnObjectClass = spalteDesc.getString("OBJECT_CLASS");
        lineSeparator = spalteDesc.getString("LINE_SEPARATOR");

        ColumnDefinition columnAttr = new ColumnDefinition(spalte, relativePath, attributeName);
        columnAttr.columnObjectClass = columnObjectClass;
        columnAttr.lineSeparator = lineSeparator;
        columnDefinitions.put(spalte, columnAttr);
        schema.add(spalte);
      }
    }

    // Key-Attribute
    ConfigThingy keys = sourceDesc.query("Schluessel");

    if (keys.count() == 0)
      throw new ConfigurationErrorException(errorMessage() + L.m("Schluessel section is missing."));

    ConfigThingy keySpalten;

    try
    {
      // The last defined key is used.
      keySpalten = keys.getLastChild();
    } catch (NodeNotFoundException e)
    {
      throw new ConfigurationErrorException(
          L.m("Impossible. Count() was checked before."), e);
    }

    Iterator<ConfigThingy> keyIterator = keySpalten.iterator();

    if (!keyIterator.hasNext())
    {
      throw new ConfigurationErrorException(
          errorMessage() + L.m("No Key column specified."));
    }

    boolean onlyRelative = true; // True if there are no attribute paths of the form 0:*
    boolean onlyAbsolute = true; // True if there are only attribute paths of the form 0:*

    // Save the key attributes.
    while (keyIterator.hasNext())
    {
      String currentName = keyIterator.next().toString();

      ColumnDefinition currentKeyLDAPAttribute = columnDefinitions.get(currentName);

      // Is key attribute present?
      if (currentKeyLDAPAttribute == null)
        throw new ConfigurationErrorException(L.m(
            "Column \"{0}\" was not defined in the schema and therefore it cannot be used as a key column.",
            currentName));

      if (currentKeyLDAPAttribute.relativePath != 0)
      {
        onlyAbsolute = false;
      }
      if (currentKeyLDAPAttribute.relativePath == 0)
      {
        onlyRelative = false;
      }

      keyAttributes.add(currentName);
    }

    if (onlyAbsolute)
    {
      keyStatus = ABSOLUTE_ONLY;
    } else if (onlyRelative)
    {
      keyStatus = RELATIVE_ONLY;
    } else
    {
      keyStatus = ABSOLUTE_AND_RELATIVE;
    }
  }

  /** Set the timeout properties. */
  private void setTimeout(long timeout)
  {
    properties.setProperty("com.sun.jndi.ldap.connect.timeout", Long.toString(timeout));
    properties.setProperty("com.sun.jndi.dns.timeout.initial", Long.toString(timeout));
    properties.setProperty("com.sun.jndi.dns.timeout.retries", "1");
  }

  /**
   * Represents a column definition
   *
   * @author Max Meier (D-III-ITD 5.1)
   */
  private static class ColumnDefinition
  {

    /**
     * Relative path:
     * 0 := Attribute located in the same node
     * Negative := relative path specification 'upward' from the current node
     * Positive := relative path specification from the root
     */
    int relativePath;

    /**
     * Name of the column.
     */
    String columnName;

    /** LDAP attribute name */
    String attributeName = null;

    /** Exclusive objectClass */
    String columnObjectClass = null;

    /** line separator */
    String lineSeparator;

    ColumnDefinition(String columnName, int relativePath, String attributeName)
    {
      this.columnName = columnName;
      this.relativePath = relativePath;
      this.attributeName = attributeName;
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Datasource#getSchema()
   */
  @Override
  public List<String> getSchema()
  {
    return schema;
  }

  /**
   *
   * @param key
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private List<QueryPart> keyToFindQuery(String key)
  {
    String[] ks = key.split(KEY_SEPARATOR_0_NON_0_RE, 2);
    ks = ks[1].split(SEPARATOR);

    List<QueryPart> query = new ArrayList<>(ks.length);

    for (int i = 0; i < ks.length; ++i)
    {
      String[] q = ks[i].split("=", 2);
      query.add(new QueryPart(q[0], q[1]));
    }

    return query;
  }

  @Override
  public QueryResults getContents()
  {
    return new QueryResultsList(new Vector<Dataset>(0));
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Datasource#getDatasetsByKey(java.util.Collection , long)
   */
  @Override
  public QueryResults getDatasetsByKey(Collection<String> keys)
  {
    if (keys.isEmpty())
    {
      return new QueryResultsList(new ArrayList<Dataset>(0));
    }

    List<Dataset> results = new ArrayList<>(keys.size());

    try
    {
      attributeCache.clear();

      if (keyStatus == ABSOLUTE_ONLY || keyStatus == ABSOLUTE_AND_RELATIVE)
      { // Absolute attributes present.
        results.addAll(handleAbsoluteKeys(keys));
      } else
      { // Only relative attributes
        for (String currentKey : keys)
        {
          List<QueryPart> query = keyToFindQuery(currentKey);

          QueryResults res = find(query);
          for (Dataset ds : res)
            results.add(ds);
        }
      }

      return new QueryResultsList(results);
    } finally
    {
      attributeCache.clear();
    }
  }

  private List<Dataset> handleAbsoluteKeys(Collection<String> keys)
  {
    List<Dataset> results = new ArrayList<>();
    // build searchFilter
    StringBuilder searchFilter = new StringBuilder();

    for (String currentKey : keys)
    {
      if (!KEY_RE.matcher(currentKey).matches())
      {
        continue;
      }
      String[] ks = currentKey.split(KEY_SEPARATOR_0_NON_0_RE, 2);
      searchFilter.append(ks[0]);
    }
    if (searchFilter.length() == 0)
    {
      return results;
    }
    searchFilter.insert(0, "(|");
    searchFilter.append(")");

    // search LDAP
    NamingEnumeration<SearchResult> currentResults = searchLDAP("", searchFilter.toString(),
        SearchControls.SUBTREE_SCOPE, true);

    while (currentResults != null && currentResults.hasMoreElements())
    {
      try
      {
        SearchResult currentResult = currentResults.next();
        Dataset dataset = getDataset(currentResult);
        if (keyStatus == ABSOLUTE_ONLY || keys.contains(dataset.getKey()))
        {
          results.add(dataset);
        }
      } catch (NamingException e)
      {
        LOGGER.error("Error in LDAP-Directory.", e);
      }
    }
    return results;
  }

  /**
   * Saves a list of (Ldap)Names that belong to a search query for attributes with a path
   * level not equal to 0 (all with the same level).
   *
   * @author Max Meier (D-III-ITD 5.1)
   */
  private static class RelativePaths
  {

    private final int relative;

    private final List<Name> paths; // of (Ldap)Names

    RelativePaths(int relative, List<Name> paths)
    {
      this.relative = relative;
      this.paths = paths;
    }

  }

  /**
   * An (Ldap)Name and the path level for the search. Typically, the path level is not equal to 0,
   * but a level of 0 is also possible (can arise when intersecting positive and negative candidates).
   *
   * @author Max Meier (D-III-ITD 5.1)
   */
  private static class RelativePath
  {

    private final int relative;

    private final Name name;

    RelativePath(int relative, Name name)
    {
      this.relative = relative;
      this.name = name;
    }

  }

  /**
   * Returns, for the path level 'pathLength,' all nodes that match the LDAP search filter
   *
   * @author Max Meier (D-III-ITD 5.1)
   */
  private RelativePaths getPaths(String filter, int pathLength)
  {

    List<Name> paths = null;
    DirContext ctx = null;

    try
    {
      ctx = new InitialLdapContext(properties, null);
      NameParser np = ctx.getNameParser("");
      int rootSize = np.parse(baseDN).size();
      SearchControls sc = new SearchControls();
      sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

      sc.setTimeLimit((int) Datasource.getDatasourceTimeout());

      LOGGER.trace("ctx.search({}, {}, sc) mit Zeitlimit {}", baseDN, filter, sc.getTimeLimit());
      NamingEnumeration<SearchResult> enumer = ctx.search(baseDN, filter, sc);
      LOGGER.trace("ctx.search() abgeschlossen");

      paths = new Vector<>();

      while (enumer != null && enumer.hasMoreElements())
      {
        SearchResult result = enumer.nextElement();
        String path = preparePath(result.getNameInNamespace());
        Name pathName = np.parse(path);
        /*
         * CAUTION: here, you CANNOT test (pathLength < 0 && (pathName.size()+rootLength > abs(pathLength)))
         * because negative conditions apply to descendants.
         * So, you would need to evaluate the depth of the deepest descendant, which we do not know.
         */
        if (pathName.size() + rootSize == pathLength || pathLength < 0)
          paths.add(pathName);
      }

      ctx.close();

    } catch (NamingException e)
    {
      LOGGER.error("Internal error in LDAP.", e);
    } finally
    {
      if (ctx != null)
      {
        try
        {
          ctx.close();
        } catch (NamingException e)
        {
          LOGGER.error("", e);
        }
      }
    }

    return new RelativePaths(pathLength, paths);

  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Datasource#find(java.util.List, long)
   */
  @Override
  public QueryResults find(List<QueryPart> query)
  {
    StringBuilder searchFilter = new StringBuilder();
    List<RelativePaths> positiveSubtreePathLists = new ArrayList<>();

    List<List<RelativePath>> negativeSubtreePathLists = new ArrayList<>();

    Map<Integer, String> mapNon0PathLevelToSearchFilter = new HashMap<>();

    boolean first = true;
    for (QueryPart currentQuery : query)
    {
      ColumnDefinition colDef = columnDefinitions.get(currentQuery.getColumnName());

      if (colDef == null)
      {
        return new QueryResultsList(new Vector<Dataset>(0));
      }

      String attributeName = colDef.attributeName;
      int relativePath = colDef.relativePath;
      String attributeValue = currentQuery.getSearchString();

      if (attributeValue.isEmpty())
      {
        continue;
      }

      String columnObjectClass = colDef.columnObjectClass;
      String currentSearchFilter = "(" + ldapEscape(attributeName) + "="
          + ldapEscape(attributeValue) + ")";
      if (columnObjectClass != null)
      {
        currentSearchFilter = "(&" + currentSearchFilter + "(objectClass="
            + ldapEscape(columnObjectClass) + "))";
      }

      if (relativePath == 0)
      { // edit filter
        if (first)
        {
          searchFilter.append(currentSearchFilter);
          first = false;
        } else
        {
          searchFilter.insert(0, "(&" + currentSearchFilter).append(")");
        }
      } else
      { // edit searchFilters for subtree searches:

        Integer key = Integer.valueOf(relativePath);

        String non0LevelSearchFilter = mapNon0PathLevelToSearchFilter.get(key);

        if (non0LevelSearchFilter == null)
        {
          non0LevelSearchFilter = currentSearchFilter;
        } else
        {
          non0LevelSearchFilter = "(&" + currentSearchFilter + non0LevelSearchFilter + ")";
        }

        mapNon0PathLevelToSearchFilter.put(key, non0LevelSearchFilter);

      }

    }

    // TODO: Possible optimization: Do not iterate through attributeKeys in a 
    // random order
    for (Map.Entry<Integer, String> ent : mapNon0PathLevelToSearchFilter.entrySet())
    {
      Integer currentKey = ent.getKey();

      int relativePath = currentKey.intValue();

      String pathFilter = ent.getValue();

      RelativePaths paths = getPaths(pathFilter, relativePath);

      if (relativePath > 0)
      {
        positiveSubtreePathLists.add(paths);
      } else
      {
        /*
         * Convert RelativePaths into a list of RelativePath objects because later in the merge step,
         * there should be a list that can contain paths of different levels mixed together.
         */
        List<RelativePath> negativeSubtreePaths = new ArrayList<>();
        for (Name currentName : paths.paths)
        {
          RelativePath newNegativePath = new RelativePath(paths.relative, currentName);
          negativeSubtreePaths.add(newNegativePath);
        }
        negativeSubtreePathLists.add(negativeSubtreePaths);
      }
    }

    /*
     * Intersect all specified positively relative paths
     *
     * This algorithm compares two lists, each representing all relevant paths of a search attribute.
     * First, the lists are sorted by the length of the contained paths.
     * Then, it checks whether for each element in the longer path list,
     * there exists an element in the shorter path list that is a prefix of the first element.
     * If yes, the element is added to the mergedPositiveSubtreePathLists and further considered.
     * If not, the intersection property is not satisfied, and the path is discarded.
     */
    List<Name> mergedPositiveSubtreePathLists = null;

    int mergedCurrentSize = 0; // TODO: The name is nonsense. The variable represents
    // Level is okay, 'size' refers to the length
    // The (Ldap)Names

    if (!positiveSubtreePathLists.isEmpty())
    /*
     * TODO: Move 'if' to the outside (possibly switch to an iterator as mentioned in the TODO below)
     * so that mergedPositiveSubtreePathLists doesn't have to be initialized with null,and it can be proven to be initialized
     */
    {
      RelativePaths currentSubtreePaths = positiveSubtreePathLists.get(0);
      /*
       * TODO: Here, a list of random levels is selected (according to the sorting of attributeMap.keySet()).
       * Where is the sorting mentioned above?
       */
      mergedPositiveSubtreePathLists = currentSubtreePaths.paths;
      mergedCurrentSize = currentSubtreePaths.relative;
    }
    for (int n = 1; n < positiveSubtreePathLists.size(); n++) // TOD0 Iterator
    // Use.
    {

      RelativePaths currentSubtreePaths = positiveSubtreePathLists.get(n);

      List<Name> shorterLdapNames;
      List<Name> longerLdapNames;

      if (currentSubtreePaths.relative < mergedCurrentSize)
      {
        shorterLdapNames = currentSubtreePaths.paths;
        longerLdapNames = mergedPositiveSubtreePathLists;
      } else
      {
        shorterLdapNames = mergedPositiveSubtreePathLists;
        longerLdapNames = currentSubtreePaths.paths;
        mergedCurrentSize = currentSubtreePaths.relative;
      }

      mergedPositiveSubtreePathLists = new Vector<>();

      for (int m = 0; m < longerLdapNames.size(); m++)
      {
        Name longerName = longerLdapNames.get(m);

        for (int p = 0; p < shorterLdapNames.size(); p++)
        {
          Name shorterName = shorterLdapNames.get(p);
          if (longerName.startsWith(shorterName))
          {
            mergedPositiveSubtreePathLists.add(longerName);
            break;
          }
        }

      }

    }

    /*
     * bilde die Schnittmenge aller angegebenen negativen relativen Pfade
     *
     * Vergleiche jeweils zwei Listen, die je ein Suchattribut repräsentieren.
     */
    List<RelativePath> mergedNegativeList = null;
    if (!negativeSubtreePathLists.isEmpty()) // TODO: Move 'if' upwards to avoid initializing 
    // mergedNegativeList with null.
    {
      mergedNegativeList = negativeSubtreePathLists.get(0);
    }

    for (int n = 1; n < negativeSubtreePathLists.size(); n++)
    {
      List<RelativePath> newMergedNegativeList = new ArrayList<>();

      /*
       * All objects in currentList have the same level.
       */
      List<RelativePath> currentList = negativeSubtreePathLists.get(n);

      for (int m = 0; m < mergedNegativeList.size(); m++)
      {
        RelativePath currentPath = mergedNegativeList.get(m);

        /*
         * Search in currentList for a path related to currentPath that makes a statement about a
         * subset or a superset of the descendants of currentPath, which are potential results.
         * For example
         */
        // A1:-2 Here, A1 is a node that corresponds to a search condition with level -2
        // matches, meaning.
        // / | potential results come from its grandchildren.
        // D B:-1 BAt B, children are potential results. The grandchildren of A1
        // are a
        // | | \ superset of the children of B.
        // | | \
        // E C1 C2
        /*
         * Gibt es so einen Match nicht, dann fliegt currentPath raus (indem es nicht nach
         * newMergedNegativeList übertragen wird). Gibt es so einen Match, so wird der längere Pfad
         * von beiden in die newMergedNegativeList übernommen (im Beispiel B).
         */
        for (int p = 0; p < currentList.size(); p++)
        {
          RelativePath otherPath = currentList.get(p);

          RelativePath shorter;
          RelativePath longer;

          if (currentPath.name.size() < otherPath.name.size())
          {
            shorter = currentPath;
            longer = otherPath;
          } else
          {
            shorter = otherPath;
            longer = currentPath;
          }

          if (currentPath.name.size() - currentPath.relative == otherPath.name.size()
              - otherPath.relative && longer.name.startsWith(shorter.name))
          {
            newMergedNegativeList.add(longer);
            // *
            // * Achtung: Kein break hier! Beispiel
            // *
            // * A A ist currentPath mit Level -2
            // * / \ B1 und B2 sind in der currentList mit Level -1
            // * / \ Sowohl B1 als auch B2 müssen in die
            // * B1 B2 newMergedNegativeList kommen!
            // * | |
            // * C1 C2
            // *
          }

        }
      }

      mergedNegativeList = newMergedNegativeList;

    }

    /*
     * Intersection of the positively and negatively relative lists
     */
    List<RelativePath> mergedNegativeSubtreePaths;
    if (mergedPositiveSubtreePathLists != null && mergedNegativeList != null)
    {

      mergedNegativeSubtreePaths = new Vector<>();

      for (int n = 0; n < mergedNegativeList.size(); n++)
      {
        RelativePath currentPath = mergedNegativeList.get(n);

        for (int m = 0; m < mergedPositiveSubtreePathLists.size(); m++)
        {
          Name currentName = mergedPositiveSubtreePathLists.get(m);

          if (currentPath.name.size() < currentName.size())
          {

            if (currentName.startsWith(currentPath.name)
                && currentPath.name.size() - currentPath.relative >= currentName.size())
            {
              /*
               * We create a new RelativePath with the name of the positive (currentName),
               * which is deeper in the tree, and a (negative) level that selects the same descendant
               * level as the level of the negative (currentPath).
               * Note: It's possible that the newly formed level is 0.
               */
              RelativePath newPath = new RelativePath(
                  currentName.size() - currentPath.name.size() + currentPath.relative, currentName);
              mergedNegativeSubtreePaths.add(newPath);
              // No break because the same negative currentPath can match multiple
              // Intersections are possible.
            }

          } else
          {

            if (currentPath.name.startsWith(currentName))
            {
              mergedNegativeSubtreePaths.add(currentPath);
            }

          }

        }
      }
    } else
    {
      mergedNegativeSubtreePaths = mergedNegativeList;
    }

    // TODO: The lists should never be null (see previous TODOs)
    // Accordingly, isEmpty() should be tested here
    if (searchFilter.length() == 0 && mergedPositiveSubtreePathLists == null
        && mergedNegativeSubtreePaths == null)
    {
      return new QueryResultsList(new Vector<Dataset>(0));
    }

    List<SearchResult> currentResultList = new ArrayList<>();

    /*
     * TODO: Better to switch to using havePositiveConstraints and 
     * haveNegativeConstraints Booleans instead of checking the size.
     * Checking the size could lead to the misconception that mergedNegativeSubtreePaths
     * should be tested here, which is not the case. If the merged list is empty but there
     * are positive constraints, an empty result list must be returned.
     */
    if (negativeSubtreePathLists.isEmpty())
    {

      List<String> positiveSubtreeStrings = new ArrayList<>();

      // create Strings from Names
      if (positiveSubtreePathLists.isEmpty())
      {
        positiveSubtreeStrings.add("");
      } else
      {

        for (int n = 0; n < mergedPositiveSubtreePathLists.size(); n++)
        {
          Name currentName = mergedPositiveSubtreePathLists.get(n);
          positiveSubtreeStrings.add(currentName.toString());
        }

      }

      // General Search

      for (String subTree : positiveSubtreeStrings)
      {
        String comma = ",";
        if (subTree.isEmpty())
        {
          comma = "";
        }
        NamingEnumeration<SearchResult> currentResults = searchLDAP(subTree + comma,
            searchFilter.toString(), SearchControls.SUBTREE_SCOPE, true);

        while (currentResults != null && currentResults.hasMoreElements())
        {
          SearchResult sr = currentResults.nextElement();
          String name = preparePath(sr.getNameInNamespace());
          sr.setName(name + (name.length() > 0 ? comma : "") + subTree);
          currentResultList.add(sr);
        }

      }
    } else
    { // Breadth-first search starting from the nodes of mergedNegativeSubtreePaths
      for (RelativePath currentRelativePath : mergedNegativeSubtreePaths)
      {
        int depth = -currentRelativePath.relative;
        // CAUTION: depth can be 0. See comment when forming the intersection
        // negative and positive paths.

        Name currentName = currentRelativePath.name;
        String currentPath = currentName.toString();
        List<SearchResult> currentSearch = searchLDAPLevel(currentPath, searchFilter.toString(),
            depth);

        currentResultList.addAll(currentSearch);
      }

    }

    List<Dataset> results = new ArrayList<>();

    // generate Datasets from SearchResults

    attributeCache.clear();

    try
    {
      for (SearchResult currentResult : currentResultList)
      {
        results.add(getDataset(currentResult));
      }
    } finally
    {
      attributeCache.clear();
    }

    return new QueryResultsList(results);
  }

  /**
   * Escaping according to RFC 2254 Section 4. Asterisks are not escaped
   * because they should retain their normal asterisk meaning
   */
  private String ldapEscape(String value)
  {
    return value.replaceAll("\\\\", "\\\\5c").replaceAll("\\(", "\\\\28")
        .replaceAll("\\)", "\\\\29").replaceAll("\\00", "\\\\00");
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.db.Datasource#getName()
   */
  @Override
  public String getName()
  {
    return datasourceName;
  }

  /**
   * Generates a key from an ordered vector of key values.
   *
   * @param values
   * @return
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String generateKey(Map<String, String> data)
  {
    List<ColumnDefinition> keyColumns = new ArrayList<>();
    Iterator<Object> iter = keyAttributes.iterator();
    while (iter.hasNext())
    {
      String keyAttr = (String) iter.next();
      ColumnDefinition colDef = columnDefinitions.get(keyAttr);
      keyColumns.add(colDef);
    }

    // Sort columns alphabetically and by path level to create a
    // well-defined key that is independent of the order, resulting in a consistent key
    // of the map.
    Collections.sort(keyColumns, (o1, o2) -> {
      ColumnDefinition colDef1 = o1;
      ColumnDefinition colDef2 = o2;
      int comp1 = colDef1.attributeName.compareTo(colDef2.attributeName);
      int comp2 = colDef1.relativePath - colDef2.relativePath;
      int comp = comp1;
      if (comp == 0)
      {
        comp = comp2;
      }
      return comp;
    });

    StringBuilder key = new StringBuilder();
    for (ColumnDefinition colDef : keyColumns)
    {
      if (colDef.relativePath == 0)
      {
        key.append('(');
        key.append(ldapEscape(colDef.attributeName));
        key.append('=');
        String value = data.get(colDef.columnName);
        if (value == null)
        {
          value = "*";
        }
        key.append(ldapEscape(value));
        key.append(')');
      }
    }

    if (key.length() > 0)
    {
      key.insert(0, "(&");
      key.append(')');
    }

    key.append(KEY_SEPARATOR_0_NON_0_RE);

    for (ColumnDefinition colDef : keyColumns)
    {
      if (colDef.relativePath != 0)
      {
        key.append(colDef.columnName);
        key.append('=');
        String value = data.get(colDef.columnName);
        if (value == null)
        {
          value = "";
        }
        key.append(value.replaceAll("\\*", "").replaceAll(SEPARATOR, ""));
        key.append(SEPARATOR);
      }
    }

    return key.toString();
  }

  private static class CacheKey
  {
    private static final String CACHE_KEY_SEPARATOR = "/{%§";

    private String hash;

    public CacheKey(Name attributePath, String[] searchAttributes)
    {
      StringBuilder buf = new StringBuilder();
      buf.append(searchAttributes.length);
      buf.append(CACHE_KEY_SEPARATOR);
      buf.append(attributePath.toString());
      for (int n = 0; n < searchAttributes.length; ++n)
      {
        buf.append(searchAttributes[n]);
      }
      hash = buf.toString();
    }

    @Override
    public int hashCode()
    {
      return hash.hashCode();
    }

    @Override
    public boolean equals(Object other)
    {
      if (other == null || other.getClass() != getClass())
      {
        return false;
      }
      CacheKey otherKey = (CacheKey) other;
      return hash.equals(otherKey.hash);
    }
  }

  /**
   * Completes SearchResults with data from the directory and returns a dataset
   *
   * @param searchResult
   * @param endTime
   * @return Dataset
   * @author Max Meier (D-III-ITD 5.1)
   *
   */
  private Dataset getDataset(SearchResult searchResult)
  {
    Attributes attributes = searchResult.getAttributes();

    Map<String, String> relation = new HashMap<>();

    Name pathName = null;
    Name rootName = null;
    DirContext ctx = null;

    try
    {
      String tempPath = searchResult.getNameInNamespace();
      tempPath = preparePath(tempPath);

      ctx = new InitialLdapContext(properties, null);
      NameParser nameParser = ctx.getNameParser("");
      pathName = nameParser.parse(tempPath);
      rootName = nameParser.parse(baseDN); // TOD0: This is a constant, only
      // calculated only once (except that this
      // only with a functioning
      // network connection is possible). Testing with the network cable unplugged

    } catch (NamingException e)
    {
      LOGGER.error("Fehler beim Zugriff auf das LDAP-Verzeichnis.", e);
    }

    for (Map.Entry<String, ColumnDefinition> columnDefEntry : columnDefinitions.entrySet())
    {
      ColumnDefinition currentAttribute = columnDefEntry.getValue();

      int relativePath = currentAttribute.relativePath;
      String attributeName = currentAttribute.attributeName;

      String value = null;

      if (relativePath == 0)
      { // value can be found in the attributes

        try
        {
          if (attributes.get(attributeName) != null)
            value = (String) attributes.get(attributeName).get();
        } catch (NamingException | NullPointerException e)
        {
          LOGGER.trace("", e);
          // do nothing (Attribute value is not present and remains 'null'.)
        }

      } else
      { // value is stored somewhere else in the directory

        Name attributePath = (Name) rootName.clone();

        try
        {

          if (relativePath < 0)
          { // Path relative to the current element

            attributePath.addAll(pathName.getPrefix(pathName.size() + relativePath));

          } else
          { // relativePath > 0, path relative to the root

            attributePath.addAll(pathName.getPrefix(relativePath - rootName.size()));
          }

          String[] searchAttributes = { attributeName };

          Attributes foundAttributes;

          CacheKey key = new CacheKey(attributePath, searchAttributes);
          foundAttributes = attributeCache.get(key);

          if (foundAttributes == null)
          {
            foundAttributes = ctx.getAttributes(attributePath, searchAttributes);
            attributeCache.put(key, foundAttributes);
          }

          Attribute foundAttribute = foundAttributes.get(attributeName);

          if (foundAttribute != null)
          {
            value = (String) foundAttribute.get();
          }

        } catch (NamingException | NullPointerException | IndexOutOfBoundsException e)
        {
          // do nothing (Attribute value is not present and remains 'null')
          LOGGER.trace("", e);
        }
      }

      if (value != null)
      {
        String lineSeparator = currentAttribute.lineSeparator;
        if (lineSeparator != null)
        {
          value = value.replaceAll(lineSeparator, "\n");
        }
        relation.put(columnDefEntry.getKey(), value);
      }
    }

      String key = generateKey(relation);

      return new LDAPDataset(key, relation);
  }

  /**
   * Searches in the subtree path + BASE_DN for nodes that match the search criteria filter.
   *
   * @param path
   *          The path of the starting node. Concatenated with BASE_DN
   * @param filter
   *          The search filter.
   * @param searchScope
   *          SearchControls.SUBTREE_SCOPE, SearchControls.OBJECT_SCOPE or
   *          SearchControls.ONELEVEL_SCOPE, to specify where to search.
   * @param onlyObjectClass
   *          If true, only nodes matching the objectClass {@link #objectClass} will be returned.
   * @return The search results
   * @author Max Meier (D-III-ITD 5.1)
   * @throws NamingException
   *
   */
  private NamingEnumeration<SearchResult> searchLDAP(String path, String filter, int searchScope,
      boolean onlyObjectClass)
  {
    LOGGER.debug("searchLDAP({}, {}, {}, {})", path, filter, searchScope, onlyObjectClass);

    SearchControls searchControls = new SearchControls();

    searchControls.setSearchScope(searchScope);

    searchControls.setTimeLimit((int) Datasource.getDatasourceTimeout());

    if (onlyObjectClass)
    {
      filter = "(&(objectClass=" + objectClass + ")" + filter + ")";
    } else
    {
      filter = "(&(objectClass=" + "*" + ")" + filter + ")"; // TODO: The objectClass=* is unnecessary
    }

    Optional<NamingEnumeration<SearchResult>> result = Optional.empty();
    DirContext ctx = null;

    try
    {
      ctx = new InitialLdapContext(properties, null);
      NameParser nameParser = ctx.getNameParser("");
      Name name = nameParser.parse(path + baseDN);

      LOGGER.trace("ctx.search({}, {}, searchControls) mit Zeitlimit {}", name, filter,
          searchControls.getTimeLimit());
      result = Optional.of(ctx.search(name, filter, searchControls));
      LOGGER.trace("ctx.search() abgeschlossen");
    } catch (NamingException e)
    {
      LOGGER.error("", e);
    } finally
    {
      if (ctx != null)
      {
        try
        {
          ctx.close();
        } catch (NamingException e)
        {
          LOGGER.error("", e);
        }
      }
    }

    result.ifPresent(r -> LOGGER
        .debug(r.hasMoreElements() ? "Ergebnisse gefunden" : "keine Ergebnisse gefunden"));

    return result.orElse(null);
  }

  /**
   * Searches the descendants of the node designated by path + BASE_DN with a distance of 'level'
   * from this node for nodes that match the search criteria filter.
   * Only objects with objectClass = {@link #objectClass} are returned
   *
   * @return A list of {@link SearchResult}s
   * @author Max Meier (D-III-ITD 5.1)
   *
   */
  private List<SearchResult> searchLDAPLevel(String path, String filter, int level)
  {
    List<String> seeds = new ArrayList<>();
    seeds.add(path);

    String comma;

    for (int n = 0; n < (level - 1); n++)
    {
      List<String> nextSeeds = new ArrayList<>();

      for (String searchPath : seeds)
      {
        comma = ",";
        if (searchPath.isEmpty())
        {
          comma = "";
        }

        NamingEnumeration<SearchResult> enumer = searchLDAP(searchPath + comma, "",
            SearchControls.ONELEVEL_SCOPE, false);

        while (enumer != null && enumer.hasMoreElements())
        {
          SearchResult currentResult = enumer.nextElement();
          String subPath = preparePath(currentResult.getNameInNamespace());
          comma = ",";
          if (subPath.isEmpty())
          {
            comma = "";
          }
          String currentPath = subPath + comma + searchPath;
          nextSeeds.add(currentPath);
        }
      }

      seeds = nextSeeds;

    }

    List<SearchResult> result = new ArrayList<>();

    for (String currentPath : seeds)
    {
      comma = ",";
      if (currentPath.isEmpty())
      {
        comma = "";
      }

      NamingEnumeration<SearchResult> enumer = searchLDAP(currentPath + comma, filter,
          level == 0 ? SearchControls.OBJECT_SCOPE : SearchControls.ONELEVEL_SCOPE, true);

      while (enumer != null && enumer.hasMoreElements())
      {
        SearchResult sr = enumer.nextElement();
        String name = preparePath(sr.getNameInNamespace());
        String actualPath = name + (name.length() > 0 ? comma : "") + currentPath;
        sr.setName(actualPath);
        result.add(sr);
      }

    }

    return result;
  }

  /**
   * Remove enclosing double quotes from path if present.
   * [The following explanation may not be accurate anymore since the switch from getName()
   * to getNameInNamespace() was made. It may be possible to eliminate the entire double quote removal process.]
   * This needs to be done because the character '/' is allowed in LDAP path components,
   * but is used as a component separator in JNDI. Therefore, paths containing '/' are enclosed in double quotes
   * by .getName() and cannot be parsed anymore.
     After removing the double quotes, it is checked whether path ends with {@link #baseDN} 
     (usually the case), and if so, this suffix is removed.
    [The following may also be outdated with the switch to getNameInNamespace(): 
    TODO I'm not sure if there's more to be done here. 
    What about contained double quotes? Can that happen? How are they escaped?]
   *
   * @author Max Meier, Matthias Benkmann (D-III-ITD 5.1)
   *
   */
  private String preparePath(String path)
  {

    int tempEnd = path.length() - 1;
    if (tempEnd > 0 && path.charAt(0) == '"' && path.charAt(tempEnd) == '"')
    {
      path = path.substring(1, tempEnd);
    }
    if (path.endsWith(baseDN))
    {
      int end = path.length() - baseDN.length();
      if (end > 0)
      { // for the comma present if path != baseDN
        --end;
      }
      path = path.substring(0, end);
    }

    return path;

  }

  private String errorMessage()
  {
    return L.m("Error in data source definition of \"{0}\": ", datasourceName);
  }

  private class LDAPDataset implements Dataset
  {

    private String key;

    private Map<String, String> relation;

    LDAPDataset(String key, Map<String, String> relation)
    {
      this.key = key;
      this.relation = relation;
    }

    @Override
    public String get(java.lang.String columnName) throws ColumnNotFoundException
    {
      if (!schema.contains(columnName))
      {
        throw new ColumnNotFoundException(L.m("Column \"{0}\" is not defined in schema", columnName));
      }

      return relation.get(columnName);
    }

    @Override
    public String getKey()
    {
      return key;
    }

  }
}
