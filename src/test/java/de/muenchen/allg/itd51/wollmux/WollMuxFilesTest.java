package de.muenchen.allg.itd51.wollmux;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.StringBody;
import org.mockserver.verify.VerificationTimes;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

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
