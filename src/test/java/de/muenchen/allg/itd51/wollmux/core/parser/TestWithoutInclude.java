package de.muenchen.allg.itd51.wollmux.core.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
 * A Test class to verify that the scanner works correctly without includes.
 *
 * @author Daniel Sikeler
 */
public class TestWithoutInclude
{

  /**
   * Tokens expected by scan of scannerTest.conf.
   */
  private final Token[] tokens = {
      new Token(getClass().getResource("scannerTest.conf").getFile(), TokenType.NEW_FILE),
      new Token("A", TokenType.KEY), new Token("'X\"\"Y'", TokenType.VALUE),
      new Token("B", TokenType.KEY), new Token("'X\"Y'", TokenType.VALUE),
      new Token("C", TokenType.KEY), new Token("\"X''Y\"", TokenType.VALUE),
      new Token("D", TokenType.KEY), new Token("\"X'Y\"", TokenType.VALUE),
      new Token("GUI", TokenType.KEY),
      new Token("(", TokenType.OPENING_BRACKET),
      new Token("Dialoge", TokenType.KEY),
      new Token("(", TokenType.OPENING_BRACKET),
      new Token("Dialog1", TokenType.KEY),
      new Token("(", TokenType.OPENING_BRACKET),
      new Token("(", TokenType.OPENING_BRACKET),
      new Token("TYPE", TokenType.KEY),
      new Token("\"textbox\"", TokenType.VALUE),
      new Token("LABEL", TokenType.KEY),
      new Token("\"Name\"", TokenType.VALUE),
      new Token(")", TokenType.CLOSING_BRACKET),
      new Token(")", TokenType.CLOSING_BRACKET),
      new Token(")", TokenType.CLOSING_BRACKET),
      new Token(")", TokenType.CLOSING_BRACKET),
      new Token("Anredevarianten", TokenType.KEY),
      new Token("(", TokenType.OPENING_BRACKET),
      new Token("\"Herr\"", TokenType.VALUE),
      new Token("\"Frau\"", TokenType.VALUE),
      new Token("\"Pinguin\"", TokenType.VALUE),
      new Token(")", TokenType.CLOSING_BRACKET),
      new Token("(", TokenType.OPENING_BRACKET),
      new Token("\"Dies\"", TokenType.VALUE),
      new Token("\"ist\"", TokenType.VALUE),
      new Token("\"eine\"", TokenType.VALUE),
      new Token("\"unbenannte\"", TokenType.VALUE),
      new Token("\"Liste\"", TokenType.VALUE),
      new Token(")", TokenType.CLOSING_BRACKET),
      new Token("NAME", TokenType.KEY),
      new Token("\"WollMux%%%n\"", TokenType.VALUE),
      new Token("# FARBSCHEMA \"Ekelig\"", TokenType.COMMENT),
      new Token("", TokenType.END_FILE), };

  /**
   * The content of the file.
   */
  private final String config = "A 'X\"\"Y'\nB 'X\"Y'\nC \"X''Y\"\nD \"X'Y\"\nGUI (\n  Dialoge (\n    Dialog1 (\n      (TYPE \"textbox\" LABEL \"Name\")\n    )\n  )\n)\nAnredevarianten (\"Herr\", \"Frau\", \"Pinguin\")\n(\"Dies\", \"ist\", \"eine\", \"unbenannte\", \"Liste\")\nNAME \"WollMux%%%n\" # FARBSCHEMA \"Ekelig\"\n\n";
  private final String config_win = "A 'X\"\"Y'\r\nB 'X\"Y'\r\nC \"X''Y\"\r\nD \"X'Y\"\r\nGUI (\r\n  Dialoge (\r\n    Dialog1 (\r\n      (TYPE \"textbox\" LABEL \"Name\")\r\n    )\r\n  )\r\n)\r\nAnredevarianten (\"Herr\", \"Frau\", \"Pinguin\")\r\n(\"Dies\", \"ist\", \"eine\", \"unbenannte\", \"Liste\")\r\nNAME \"WollMux%%%n\" # FARBSCHEMA \"Ekelig\"\r\n\r\n"; 

  /**
   * Scan a file and test whether the correct tokens occur.
   *
   * @throws ScannerException
   *           Problems with scanner.
   * @throws MalformedURLException
   *           Couldn't read the configuration.
   */
  @Test
  public void scanWithoutInclude() throws ScannerException,
      MalformedURLException
  {
    final Scanner scanner = new Scanner(getClass().getResource("scannerTest.conf"));
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
   * Scan a configuration and create a new configuration out of the resulting
   * XML-document.
   *
   * @throws XMLGeneratorException
   *           Problems with generator.
   * @throws SAXException
   *           Invalid generatored XML-document.
   * @throws IOException
   *           Couldn't read or write the configuration.
   */
  @Test
  public void generateWithoutInclude() throws XMLGeneratorException,
      SAXException, IOException
  {
    final File in = new File(getClass().getResource("scannerTest.conf").getFile());
    final File out = new File(in.getParentFile(), "tmp.conf");
    Files.copy(in.toPath(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
    final Document doc = new XMLGenerator(new FileInputStream(out)).generateXML();
    final File schemaFile = new File("src/main/resources/configuration.xsd");
    final SchemaFactory schemaFactory = SchemaFactory
        .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    final Schema schema = schemaFactory.newSchema(schemaFile);
    final Validator validator = schema.newValidator();
    final Source source = new DOMSource(doc);
    validator.validate(source);
    final XPath xpath = XPathFactory.newInstance().newXPath();
    try
    {
      String name = (String) xpath.evaluate("config/file/key[@id='NAME']/value", doc, XPathConstants.STRING);
      assertEquals("name with \\n", "WollMux%\n", name);
    } catch (XPathExpressionException e)
    {
      assertFalse("No key 'NAME'", true);
    }
    ConfGenerator generator = new ConfGenerator(doc);
    generator.generateConf(new FileOutputStream(out), 0);
    // Whitespace was replaced
    boolean windowsOS = System.getProperty("os.name").toLowerCase().contains("windows");
    if(windowsOS)
    {
      assertEquals("Different content length", in.length(), out.length() + 8);
      assertEquals("wrong string", config_win, generator.generateConf("UTF-8"));
    }
    else
    {
      assertEquals("Different content length", in.length(), out.length() + 9);
      assertEquals("wrong string", config, generator.generateConf("UTF-8"));
    }    
    out.delete();    
  }

}
