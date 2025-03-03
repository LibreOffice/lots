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
package org.libreoffice.lots.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.db.ColumnNotFoundException;
import org.libreoffice.lots.db.Dataset;
import org.libreoffice.lots.db.Datasource;
import org.libreoffice.lots.db.LDAPDatasource;
import org.libreoffice.lots.db.QueryPart;
import org.libreoffice.lots.db.QueryResults;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;

public class LDAPDatasourceTest
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
  public void testLDAPDatasourceAbsolute() throws Exception
  {
    String url = "ldap://localhost:" + server.getListenPort();
    Datasource ds = new LDAPDatasource(null,
        new ConfigThingy("", "NAME \"ldap\" URL \"" + url + "\" BASE_DN \"" + BASE_DN
            + "\" OBJECT_CLASS \"person\" Columns ((DB_COLUMN \"column\" PATH \"0:sn\") (DB_COLUMN \"column2\" PATH \"-1:ou\")) Schluessel (\"column\")"),
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
  public void testLDAPDatasourceAbsoluteRelative() throws Exception
  {
    String url = "ldap://localhost:" + server.getListenPort();
    Datasource ds = new LDAPDatasource(null, new ConfigThingy("", "NAME \"ldap\" URL \"" + url + "\" BASE_DN \""
        + BASE_DN
        + "\" OBJECT_CLASS \"person\" Columns ((DB_COLUMN \"column\" PATH \"0:sn\") (DB_COLUMN \"column2\" PATH \"-1:ou\")) Schluessel (\"column\" \"column2\")"),
        null);
    assertEquals("ldap", ds.getName());
    assertEquals(List.of("column", "column2"), ds.getSchema());

    QueryResults results = ds.find(List.of(new QueryPart("column", "Ldap")));
    assertEquals(1, results.size());
    results = ds.getDatasetsByKey(List.of("(&(sn=Ldap))==%§%==column2=Users&:=&:%"));
    assertEquals(1, results.size());
  }

  @Test
  public void testLDAPDatasourceRelative() throws Exception
  {
    String url = "ldap://localhost:" + server.getListenPort();
    Datasource ds = new LDAPDatasource(null, new ConfigThingy("", "NAME \"ldap\" URL \"" + url + "\" BASE_DN \""
        + BASE_DN
        + "\" OBJECT_CLASS \"person\" Columns ((DB_COLUMN \"column\" PATH \"0:sn\") (DB_COLUMN \"column2\" PATH \"-1:ou\")) Schluessel (\"column2\")"),
        null);
    assertEquals("ldap", ds.getName());
    assertEquals(List.of("column", "column2"), ds.getSchema());

    QueryResults results = ds.find(List.of(new QueryPart("column2", "Users")));
    assertEquals(1, results.size());
    results = ds.getDatasetsByKey(List.of("==%§%==column2=Users&:=&:%"));
    assertEquals(1, results.size());
  }

}
