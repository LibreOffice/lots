package de.muenchen.allg.itd51.wollmux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.File;
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

  @BeforeEach
  public void startServer()
  {
    mockServer = ClientAndServer.startClientAndServer(7549);
  }

  @AfterEach
  public void stopServer()
  {
    mockServer.stop();
  }

  @Test
  public void validFileWithServerPara() throws Exception
  {
    try (MockServerClient client = new MockServerClient("localhost", 7549))
    {
      client
          .when(HttpRequest.request().withMethod("POST")
              .withBody(StringBody.subString(System.getProperty("user.name"))))
          .respond(HttpResponse.response().withStatusCode(200)
              .withHeader(new Header("Content-Type", "application/json; charset=utf-8"))
              .withBody("DEFAULT_CONTEXT \"test_server\""));

      ConfigThingy test = WollMuxFiles
          .parseWollMuxConf(new File(getClass().getResource("wollmuxConfTest.conf").toURI()));
      assertEquals("test_server", test.getString("DEFAULT_CONTEXT", null),
          "Got wrong DEFAULT_CONTEXT");
    }
  }

  @Test
  public void validFileWithUserPara() throws Exception
  {
    try (MockServerClient client = new MockServerClient("localhost", 7549))
    {
      client
          .when(
              HttpRequest.request().withMethod("POST").withBody(StringBody.subString("test.user")))
          .respond(HttpResponse.response().withStatusCode(200)
              .withHeader(new Header("Content-Type", "application/json; charset=utf-8"))
              .withBody("DEFAULT_CONTEXT \"test_server_user\""));

      ConfigThingy test = WollMuxFiles
          .parseWollMuxConf(new File(getClass().getResource("wollmuxConfTest2.conf").toURI()));
      assertEquals("test_server_user", test.getString("DEFAULT_CONTEXT", null),
          "Got wrong DEFAULT_CONTEXT");
    }
  }

  @Test
  public void doNothingWithOutServerPara() throws Exception
  {
    try (MockServerClient client = new MockServerClient("localhost", 7549))
    {
      client.verify(HttpRequest.request().withMethod("POST"), VerificationTimes.exactly(0));
      ConfigThingy test = WollMuxFiles
          .parseWollMuxConf(new File(getClass().getResource("wollmuxConfTest3.conf").toURI()));
      assertEquals("default_server", test.getString("DEFAULT_CONTEXT", null),
          "Got wrong DEFAULT_CONTEXT");
    }
  }
}
