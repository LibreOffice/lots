package de.muenchen.allg.itd51.wollmux.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.muenchen.allg.itd51.wollmux.core.parser.generator.xml.ConfGenerator;
import de.muenchen.allg.itd51.wollmux.core.parser.generator.xml.XMLGenerator;
import de.muenchen.allg.itd51.wollmux.core.parser.generator.xml.XMLGeneratorException;
import de.muenchen.allg.itd51.wollmux.core.parser.scanner.Scanner;
import de.muenchen.allg.itd51.wollmux.core.parser.scanner.ScannerException;
import de.muenchen.allg.itd51.wollmux.core.parser.scanner.Token;
import de.muenchen.allg.itd51.wollmux.core.parser.scanner.TokenType;

/**
 * A Test class to verify that the scanner works correctly with includes.
 *
 * @author Daniel Sikeler
 */
public class TestWithInclude
{

  /**
   * The tokens expeceted by the scan of includeTest.conf.
   */
  private final Token[] tokens = {
      new Token(getClass().getResource("includeTest.conf").getFile(), TokenType.NEW_FILE),
      new Token("file:inc/includeTest2.conf", TokenType.NEW_FILE),
      new Token("# includeTest2", TokenType.COMMENT),
      new Token("", TokenType.END_FILE),
      new Token("file:../parser/inc/includeTest2.conf", TokenType.NEW_FILE),
      new Token("# includeTest2", TokenType.COMMENT),
      new Token("", TokenType.END_FILE),
      new Token("../parser/inc/includeTest2.conf", TokenType.NEW_FILE),
      new Token("# includeTest2", TokenType.COMMENT),
      new Token("", TokenType.END_FILE),
      new Token("inc/includeTest3.conf", TokenType.NEW_FILE),
      new Token("includeTest2.conf", TokenType.NEW_FILE),
      new Token("# includeTest2", TokenType.COMMENT),
      new Token("", TokenType.END_FILE), new Token("", TokenType.END_FILE),
      new Token("", TokenType.END_FILE), };

  /**
   * Map to store the file content mapping.
   */
  private final Map<String, String> fileContentMap = new HashMap<>();

  /**
   * Test if the scanner works properly.
   *
   * @throws ScannerException
   *           Scanner problems.
   * @throws MalformedURLException
   *           Couldn't find the file with the configuration.
   */
  @Test
  public void scanWithInclude() throws ScannerException, MalformedURLException
  {
    final Scanner scanner = new Scanner(getClass().getResource("includeTest.conf"));
    int index = 0;
    while (scanner.hasNext())
    {
      final Token token = scanner.next();
      assertFalse("Tokenstream to long " + token, index >= tokens.length);
      assertEquals("Token " + index + " is wrong", tokens[index++], token);
    }
    assertFalse("Tokenstream to short", index < tokens.length);
    scanner.close();
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
  public void generateWithInclude() throws XMLGeneratorException, SAXException,
      IOException, URISyntaxException
  {
    final File in = new File(getClass().getResource("scannerTest.conf").toURI());
    final File out = new File(in.getParentFile(), "tmp.conf");
    final File in2 = new File(getClass().getResource("scannerTest2.conf").toURI());
    final File out2 = new File(in2.getParentFile(), "tmp2.conf");
    Files.copy(in.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
    Files.copy(in2.toPath(), out2.toPath(), StandardCopyOption.REPLACE_EXISTING);
    final Document doc = new XMLGenerator(out2.toURI().toURL()).generateXML();
    final File schemaFile = new File("src/main/resources/configuration.xsd");
    final SchemaFactory schemaFactory = SchemaFactory
        .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    final Schema schema = schemaFactory.newSchema(schemaFile);
    final Validator validator = schema.newValidator();
    final Source source = new DOMSource(doc);
    validator.validate(source);
    ConfGenerator generator = new ConfGenerator(doc);
    generator.generateConf();
    // Whitespace was replaced
    assertEquals("Different content length 2", in2.length(), out2.length());
    // out.delete();
    // out2.delete();
    boolean windowsOS = System.getProperty("os.name").toLowerCase().contains("windows");
    if(windowsOS)
    {
//    hier von 9 auf 8, weil Windows im Unterschied zu Linux (ein LineFeed) die Kombination CarriageReturn und LineFeed verwendet, also 2 Zeichen
      assertEquals("Different content length", in.length(), out.length() + 8);
      fileContentMap.put(getClass().getResource("tmp2.conf").getFile(), "%include \"tmp.conf\"\r\n\r\n");
      fileContentMap.put(getClass().getResource("tmp.conf").getFile(), "A 'X\"\"Y'\r\nB 'X\"Y'\r\nC \"X''Y\"\r\nD \"X'Y\"\r\nGUI (\r\n  Dialoge (\r\n    Dialog1 (\r\n      (TYPE \"textbox\" LABEL \"Name\")\r\n    )\r\n  )\r\n)\r\nAnredevarianten (\"Herr\", \"Frau\", \"Pinguin\")\r\n(\"Dies\", \"ist\", \"eine\", \"unbenannte\", \"Liste\")\r\nNAME \"WollMux%%%n\" # FARBSCHEMA \"Ekelig\"\r\n\r\n");
    }
    else
    {
      assertEquals("Different content length", in.length(), out.length() + 9);
      fileContentMap.put(getClass().getResource("tmp2.conf").getFile(), "%include \"tmp.conf\"\n\n");
      fileContentMap.put(getClass().getResource("tmp.conf").getFile(), "A 'X\"\"Y'\nB 'X\"Y'\nC \"X''Y\"\nD \"X'Y\"\nGUI (\n  Dialoge (\n    Dialog1 (\n      (TYPE \"textbox\" LABEL \"Name\")\n    )\n  )\n)\nAnredevarianten (\"Herr\", \"Frau\", \"Pinguin\")\n(\"Dies\", \"ist\", \"eine\", \"unbenannte\", \"Liste\")\nNAME \"WollMux%%%n\" # FARBSCHEMA \"Ekelig\"\n\n");
    }        
    Map<String, String> map = generator.generateConfMap("UTF-8");
    assertEquals("Different number of files", fileContentMap.size(), map.size());
    for (Entry<String, String> entry : map.entrySet())
    {
      assertTrue("Unknown file " + entry.getKey(), fileContentMap.containsKey(entry.getKey()));
      assertEquals("Different content", fileContentMap.get(entry.getKey()), entry.getValue());
    }
  }

}
