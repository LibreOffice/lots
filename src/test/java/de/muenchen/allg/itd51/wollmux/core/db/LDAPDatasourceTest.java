package de.muenchen.allg.itd51.wollmux.core.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

class LDAPDatasourceTest
{
  public static InMemoryDirectoryServer server;
  public static final String BASE_DN = "dc=myorg,dc=com";

  @BeforeAll
  public static void setup() throws Exception
  {
    // TODO: use ApacheDS or embedded-ldap-junit as soon as they support JUnit5
    InMemoryListenerConfig listener = InMemoryListenerConfig.createLDAPConfig("test");
    InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(BASE_DN);
    config.setListenerConfigs(listener);
    server = new InMemoryDirectoryServer(config);
    server.applyChangesFromLDIF(Paths.get(LDAPDatasourceTest.class.getResource("users.ldif").toURI()).toFile());
    server.startListening();
  }

  @AfterAll
  public static void tearDown()
  {
    server.shutDown(true);
  }

  @Test
  void testLDAPDatasourceAbsolute() throws Exception
  {
    String url = "ldap://localhost:" + server.getListenPort();
    Datasource ds = new LDAPDatasource(null,
        new ConfigThingy("", "NAME \"ldap\" URL \"" + url + "\" BASE_DN \"" + BASE_DN
            + "\" OBJECT_CLASS \"person\" Spalten ((DB_SPALTE \"column\" PATH \"0:sn\") (DB_SPALTE \"column2\" PATH \"-1:ou\")) Schluessel (\"column\")"),
        null);
    assertEquals("ldap", ds.getName());
    assertEquals(List.of("column", "column2"), ds.getSchema());

    QueryResults results = ds.getContents();
    assertEquals(0, results.size());
    results = ds.getDatasetsByKey(List.of("(&(sn=LDAP))==%§%=="));
    assertEquals(1, results.size());
    results = ds.find(List.of(new QueryPart("column", "Ldap")));
    assertEquals(1, results.size());
    Dataset data = results.iterator().next();
    assertEquals("(&(sn=Ldap))==%§%==", data.getKey());
    assertEquals("Ldap", data.get("column"));
    assertThrows(ColumnNotFoundException.class, () -> data.get("unknown"));
  }

  @Test
  void testLDAPDatasourceAbsoluteRelative() throws Exception
  {
    String url = "ldap://localhost:" + server.getListenPort();
    Datasource ds = new LDAPDatasource(null, new ConfigThingy("", "NAME \"ldap\" URL \"" + url + "\" BASE_DN \""
        + BASE_DN
        + "\" OBJECT_CLASS \"person\" Spalten ((DB_SPALTE \"column\" PATH \"0:sn\") (DB_SPALTE \"column2\" PATH \"-1:ou\")) Schluessel (\"column\" \"column2\")"),
        null);
    assertEquals("ldap", ds.getName());
    assertEquals(List.of("column", "column2"), ds.getSchema());

    QueryResults results = ds.find(List.of(new QueryPart("column", "Ldap")));
    assertEquals(1, results.size());
    results = ds.getDatasetsByKey(List.of("(&(sn=Ldap))==%§%==column2=Users&:=&:%"));
    assertEquals(1, results.size());
  }

  @Test
  void testLDAPDatasourceRelative() throws Exception
  {
    String url = "ldap://localhost:" + server.getListenPort();
    Datasource ds = new LDAPDatasource(null, new ConfigThingy("", "NAME \"ldap\" URL \"" + url + "\" BASE_DN \""
        + BASE_DN
        + "\" OBJECT_CLASS \"person\" Spalten ((DB_SPALTE \"column\" PATH \"0:sn\") (DB_SPALTE \"column2\" PATH \"-1:ou\")) Schluessel (\"column2\")"),
        null);
    assertEquals("ldap", ds.getName());
    assertEquals(List.of("column", "column2"), ds.getSchema());

    QueryResults results = ds.find(List.of(new QueryPart("column2", "Users")));
    assertEquals(1, results.size());
    results = ds.getDatasetsByKey(List.of("==%§%==column2=Users&:=&:%"));
    assertEquals(1, results.size());
  }

}
