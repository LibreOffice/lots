package de.muenchen.allg.itd51.wollmux.core.parser;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import de.muenchen.allg.itd51.wollmux.core.parser.generator.xml.XMLGenerator;
import de.muenchen.allg.itd51.wollmux.core.parser.generator.xml.XMLGeneratorException;

/**
 * A Test class to verify that the scanner performance is ok.
 *
 * @author Daniel Sikeler
 */
public class TestPerformance
{

  /**
   * Generate a XML-document out of a large configuration.
   *
   * @throws IOException
   *           Couldn't read the configuration.
   * @throws XMLGeneratorException
   *           Problems with generator.
   */
  @org.junit.jupiter.api.Test
  public void performance() throws IOException, XMLGeneratorException
  {
    final XMLGenerator generator = new XMLGenerator(getClass().getResource("performance.conf"));
    final long start = System.currentTimeMillis();
    generator.generateXML();
    final long time = System.currentTimeMillis() - start;
    assertTrue(time < 1000, "Performance is bad: " + time + " millis");
  }

}
