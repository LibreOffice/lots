package de.muenchen.allg.itd51.wollmux.core.parser;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

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
  @Test
  public void performance() throws IOException, XMLGeneratorException
  {
    final XMLGenerator generator = new XMLGenerator(getClass().getResource("performance.conf"));
    final long start = System.currentTimeMillis();
    generator.generateXML();
    final long time = System.currentTimeMillis() - start;
    assertTrue("Performance is bad: " + time + " millis", time < 1000);
  }

}
