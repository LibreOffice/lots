/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.core.parser;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.generator.xml.XMLGenerator;
import de.muenchen.allg.itd51.wollmux.core.parser.generator.xml.XMLGeneratorException;

/**
 * A Test class to verify that the scanner performance is ok.
 *
 * @author Daniel Sikeler
 */
@Disabled
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
    assertTrue(time < 1000, "Performance is bad: " + time + " millis");
  }

}
