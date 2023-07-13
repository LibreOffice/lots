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
package org.libreoffice.lots.config.generator.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.generator.xml.ConfGenerator;
import org.libreoffice.lots.config.generator.xml.XMLGenerator;
import org.libreoffice.lots.config.generator.xml.XMLGeneratorException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * A Test class to verify that the scanner performance is ok.
 *
 */
public class TestXMLGenerator
{

  /**
   * Scan a configuration and create a new configuration out of the resulting XML-document.
   *
   * @throws XMLGeneratorException
   *           Problems with generator.
   * @throws SAXException
   *           Invalid generatored XML-document.
   * @throws IOException
   *           Couldn't read or write the configuration.
   */
  @Test
  public void generateWithoutInclude() throws XMLGeneratorException, SAXException, IOException
  {
    final File in = new File(getClass().getResource("scannerTest.conf").getFile());
    final File out = new File(in.getParentFile(), "tmp.conf");
    Files.copy(in.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
    final Document doc = new XMLGenerator(new FileInputStream(out)).generateXML();
    final File schemaFile = new File("src/main/resources/configuration.xsd");
    final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    final Schema schema = schemaFactory.newSchema(schemaFile);
    final Validator validator = schema.newValidator();
    final Source source = new DOMSource(doc);
    validator.validate(source);
    final XPath xpath = XPathFactory.newInstance().newXPath();
    try
    {
      String name = (String) xpath.evaluate("config/file/key[@id='NAME']/value", doc, XPathConstants.STRING);
      assertEquals("WollMux%\n", name, "name with \\n");
    } catch (XPathExpressionException e)
    {
      assertFalse(true, "No key 'NAME'");
    }
    ConfGenerator generator = new ConfGenerator(doc);
    generator.generateConf(new FileOutputStream(out), 0);
    // Whitespace was replaced
    boolean windowsOS = System.getProperty("os.name").toLowerCase().contains("windows");
    if (windowsOS)
    {
      String config_win = "A 'X\"\"Y'\r\nB 'X\"Y'\r\nC \"X''Y\"\r\nD \"X'Y\"\r\nGUI (\r\n  Dialoge (\r\n    Dialog1 (\r\n      (TYPE \"textbox\" LABEL \"Name\")\r\n    )\r\n  )\r\n)\r\nAnredevarianten (\"Herr\", \"Frau\", \"Pinguin\")\r\n(\"Dies\", \"ist\", \"eine\", \"unbenannte\", \"Liste\")\r\nNAME \"WollMux%%%n\" # FARBSCHEMA \"Ekelig\"\r\n\r\n";
      assertEquals(in.length(), out.length() + 8, "Different content length");
      assertEquals(config_win, generator.generateConf("UTF-8"), "wrong string");
    } else
    {
      final String config = "A 'X\"\"Y'\nB 'X\"Y'\nC \"X''Y\"\nD \"X'Y\"\nGUI (\n  Dialoge (\n    Dialog1 (\n      (TYPE \"textbox\" LABEL \"Name\")\n    )\n  )\n)\nAnredevarianten (\"Herr\", \"Frau\", \"Pinguin\")\n(\"Dies\", \"ist\", \"eine\", \"unbenannte\", \"Liste\")\nNAME \"WollMux%%%n\" # FARBSCHEMA \"Ekelig\"\n\n";
      assertEquals(in.length(), out.length() + 9, "Different content length");
      assertEquals(config, generator.generateConf("UTF-8"), "wrong string");
    }
    out.delete();
  }

  /**
   * Generate a configuration out of a configuration. Scan it and than write it again.
   *
   * @throws XMLGeneratorException
   *           Generator problems.
   * @throws SAXException
   *           Malformed XML-document generated.
   * @throws IOException
   *           Couldn't read or write.
   * @throws URISyntaxException
   */
  @Test
  public void generateWithInclude() throws XMLGeneratorException, SAXException, IOException, URISyntaxException
  {
    final File in = new File(getClass().getResource("scannerTest.conf").toURI());
    final File out = new File(in.getParentFile(), "tmp.conf");
    final File in2 = new File(getClass().getResource("scannerTest2.conf").toURI());
    final File out2 = new File(in2.getParentFile(), "tmp2.conf");
    Files.copy(in.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
    Files.copy(in2.toPath(), out2.toPath(), StandardCopyOption.REPLACE_EXISTING);
    final Document doc = new XMLGenerator(out2.toURI().toURL()).generateXML();
    final File schemaFile = new File("src/main/resources/configuration.xsd");
    final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    final Schema schema = schemaFactory.newSchema(schemaFile);
    final Validator validator = schema.newValidator();
    final Source source = new DOMSource(doc);
    validator.validate(source);
    ConfGenerator generator = new ConfGenerator(doc);
    generator.generateConf();
    // Whitespace was replaced
    assertEquals(in2.length(), out2.length(), "Different content length 2");
    // out.delete();
    // out2.delete();
    boolean windowsOS = System.getProperty("os.name").toLowerCase().contains("windows");
    Map<String, String> fileContentMap = new HashMap<>();
    if (windowsOS)
    {
      // here from 9 to 8, because Windows uses the combination CarriageReturn and LineFeed
      // in contrast to Linux (one LineFeed), so 2 characters
      assertEquals(in.length(), out.length() + 8, "Different content length");
      fileContentMap.put(getClass().getResource("tmp2.conf").getFile(), "%include \"tmp.conf\"\r\n\r\n");
      fileContentMap.put(getClass().getResource("tmp.conf").getFile(),
          "A 'X\"\"Y'\r\nB 'X\"Y'\r\nC \"X''Y\"\r\nD \"X'Y\"\r\nGUI (\r\n  Dialoge (\r\n    Dialog1 (\r\n      (TYPE \"textbox\" LABEL \"Name\")\r\n    )\r\n  )\r\n)\r\nAnredevarianten (\"Herr\", \"Frau\", \"Pinguin\")\r\n(\"Dies\", \"ist\", \"eine\", \"unbenannte\", \"Liste\")\r\nNAME \"WollMux%%%n\" # FARBSCHEMA \"Ekelig\"\r\n\r\n");
    } else
    {
      assertEquals(in.length(), out.length() + 9, "Different content length");
      fileContentMap.put(getClass().getResource("tmp2.conf").getFile(), "%include \"tmp.conf\"\n\n");
      fileContentMap.put(getClass().getResource("tmp.conf").getFile(),
          "A 'X\"\"Y'\nB 'X\"Y'\nC \"X''Y\"\nD \"X'Y\"\nGUI (\n  Dialoge (\n    Dialog1 (\n      (TYPE \"textbox\" LABEL \"Name\")\n    )\n  )\n)\nAnredevarianten (\"Herr\", \"Frau\", \"Pinguin\")\n(\"Dies\", \"ist\", \"eine\", \"unbenannte\", \"Liste\")\nNAME \"WollMux%%%n\" # FARBSCHEMA \"Ekelig\"\n\n");
    }
    Map<String, String> map = generator.generateConfMap("UTF-8");
    assertEquals(fileContentMap.size(), map.size(), "Different number of files");
    for (Entry<String, String> entry : map.entrySet())
    {
      assertTrue(fileContentMap.containsKey(entry.getKey()), "Unknown file " + entry.getKey());
      assertEquals(fileContentMap.get(entry.getKey()), entry.getValue(), "Different content");
    }
  }

  /**
   * Generate a XML-document out of a large configuration.
   *
   * @throws IOException
   *           Couldn't read the configuration.
   * @throws XMLGeneratorException
   *           Problems with generator.
   */
  @Test
  @Disabled
  public void performance() throws IOException, XMLGeneratorException
  {
    final XMLGenerator generator = new XMLGenerator(getClass().getResource("performance.conf"));
    final long start = System.currentTimeMillis();
    generator.generateXML();
    final long time = System.currentTimeMillis() - start;
    assertTrue(time < 1000, "Performance is bad: " + time + " millis");
  }

}
