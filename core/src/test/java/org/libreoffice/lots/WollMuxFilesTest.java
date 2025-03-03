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
package org.libreoffice.lots;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.config.ConfigThingy;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.StringBody;
import org.mockserver.verify.VerificationTimes;

public class WollMuxFilesTest
{

  private ClientAndServer mockServer;
  private int port;

  @BeforeEach
  public void startServer()
  {
    mockServer = ClientAndServer.startClientAndServer();
    port = mockServer.getLocalPort();
  }

  @AfterEach
  public void stopServer()
  {
    mockServer.stop();
  }

  @Test
  public void validFileWithServerPara() throws Exception
  {
    File file = File.createTempFile("wollmuxTest", ".conf");
    try (MockServerClient client = new MockServerClient("localhost", port);
	BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
    {
      writer.write(String.format("CONF_SERVER \"http://localhost:%d/\"", port));
      writer.flush();
      client
          .when(HttpRequest.request().withMethod("POST")
              .withBody(StringBody.subString(System.getProperty("user.name"))))
          .respond(HttpResponse.response().withStatusCode(200)
              .withHeader(new Header("Content-Type", "application/json; charset=utf-8"))
              .withBody("DEFAULT_CONTEXT \"test_server\""));

      ConfigThingy test = WollMuxFiles.parseWollMuxConf(file);
      assertEquals("test_server", test.getString("DEFAULT_CONTEXT", null),
          "Got wrong DEFAULT_CONTEXT");
    }
  }

  @Test
  public void validFileWithUserPara() throws Exception
  {
    File file = File.createTempFile("wollmuxTest", ".conf");
    try (MockServerClient client = new MockServerClient("localhost", port);
	BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
    {
      writer.write(String.format("CONF_SERVER \"http://localhost:%d/\"", port));
      writer.newLine();
      writer.write("USERNAME \"test.user\"");
      writer.flush();
      client
          .when(
              HttpRequest.request().withMethod("POST").withBody(StringBody.subString("test.user")))
          .respond(HttpResponse.response().withStatusCode(200)
              .withHeader(new Header("Content-Type", "application/json; charset=utf-8"))
              .withBody("DEFAULT_CONTEXT \"test_server_user\""));

      ConfigThingy test = WollMuxFiles.parseWollMuxConf(file);
      assertEquals("test_server_user", test.getString("DEFAULT_CONTEXT", null),
          "Got wrong DEFAULT_CONTEXT");
    }
  }

  @Test
  public void doNothingWithOutServerPara() throws Exception
  {
    File file = File.createTempFile("wollmuxTest", ".conf");
    try (MockServerClient client = new MockServerClient("localhost", port);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
    {
      writer.write("DEFAULT_CONTEXT \"default_server\"");
      writer.flush();
      client.verify(HttpRequest.request().withMethod("POST"), VerificationTimes.exactly(0));
      ConfigThingy test = WollMuxFiles.parseWollMuxConf(file);
      assertEquals("default_server", test.getString("DEFAULT_CONTEXT", null),
          "Got wrong DEFAULT_CONTEXT");
    }
  }
}
