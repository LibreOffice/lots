package de.muenchen.allg.itd51.wollmux.core.parser.generator.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.LinkedList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.muenchen.allg.itd51.wollmux.core.parser.PathProcessor;
import de.muenchen.allg.itd51.wollmux.core.parser.Trimmer;
import de.muenchen.allg.itd51.wollmux.core.parser.scanner.Scanner;
import de.muenchen.allg.itd51.wollmux.core.parser.scanner.ScannerException;
import de.muenchen.allg.itd51.wollmux.core.parser.scanner.Token;
import de.muenchen.allg.itd51.wollmux.core.parser.scanner.TokenType;

/**
 * Generates a XML-document from a configuration.
 * 
 * Use case:
 * 
 * <pre>
 * <code>
 * //create a new Generator with a stream. If you use a stream includes may not be resolved correctly.
 * XMLGenerator generator = new XMLGenerator(new FileInputStream("someFile"));
 * //or create a Generator with a URL.
 * XMLGenerator generator = new XMLGenerator(new URL("file:someFile"));
 * //create XML-document
 * Document doc = generator.generateXML();
 * //print XML-document to original files
 * new ConfGenerator(doc).generateConf();
 * //print the first file of the XML-document to a stream.
 * new ConfGenerator(doc).generateConf(new FileOutputStream("someOtherFile"), 0);
 * </code>
 * </pre>
 * 
 * @author Daniel Sikeler
 */
public class XMLGenerator
{

  /**
   * The document.
   */
  private final Document document;
  /**
   * The root element of the document.
   */
  private final Element config;
  /**
   * The scanner to get the configuration from.
   */
  private final Scanner scanner;
  /**
   * A stack with the produced elements, which can contain other elements.
   */
  private final Deque<Element> files;

  /**
   * Create a new generator.
   * 
   * @param url
   *          The URL of the configuration file.
   * @throws XMLGeneratorException
   *           Couldn't create a document or read the stream.
   */
  public XMLGenerator(final URL url) throws XMLGeneratorException
  {
    try
    {
      scanner = new Scanner(url);
      this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
          .newDocument();
      this.config = document.createElement(XMLTags.CONFIG.getName());
      document.appendChild(config);
      files = new LinkedList<>();
    } catch (final ParserConfigurationException e)
    {
      throw new XMLGeneratorException("Couldn't create document.", e);
    } catch (final ScannerException e)
    {
      throw new XMLGeneratorException("Couldn't read from stream.", e);
    }
  }

  /**
   * Create a new generator. If there are inclue-instructions in the
   * configuration, they may not be resolved correctly. Use XMLGenerator(URL)
   * instead.
   * 
   * @param stream
   *          The stream of the configuration file.
   * @throws XMLGeneratorException
   *           Couldn't create a document or read the stream.
   */
  public XMLGenerator(final InputStream stream) throws XMLGeneratorException
  {
    try
    {
      scanner = new Scanner(stream);
      this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
          .newDocument();
      this.config = document.createElement(XMLTags.CONFIG.getName());
      document.appendChild(config);
      files = new LinkedList<>();
    } catch (final ParserConfigurationException e)
    {
      throw new XMLGeneratorException("Couldn't create document.", e);
    } catch (final ScannerException e)
    {
      throw new XMLGeneratorException("Couldn't read from stream.", e);
    }
  }

  /**
   * Create the XML-document and return it. If it was already created, it is
   * deleted and created from scratch.
   * 
   * @return The document.
   * @throws XMLGeneratorException
   *           Couldn't create the document.
   */
  public Document generateXML() throws XMLGeneratorException
  {
    try
    {
      if (config.hasChildNodes())
      {
        NodeList nodes = config.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index++)
        {
          config.removeChild(nodes.item(index));
        }
        while (!files.isEmpty())
        {
          files.removeFirst();
        }
      }
      createDocument();
      return document;
    } catch (final ScannerException ex)
    {
      throw new XMLGeneratorException("Problems while scaning", ex);
    } catch (MalformedURLException e)
    {
      throw new XMLGeneratorException("Problems to create new file reference",
          e);
    }
  }

  /**
   * Create the XML-document.
   * 
   * @return The XML-document.
   * @throws ScannerException
   *           Thrown if the scanner has problems or unknown tokens are
   *           recognized.
   * @throws XMLGeneratorException
   *           Unknown tokens
   * @throws MalformedURLException
   */
  private void createDocument() throws ScannerException, XMLGeneratorException,
      MalformedURLException
  {
    Token token;
    files.push(config);
    while (scanner.hasNext())
    {
      token = scanner.next();
      switch (token.getType())
      {
      case KEY:
        createKey(token);
        break;
      case VALUE:
        createValue(token);
        break;
      case OPENING_BRACKET:
        createOpen();
        break;
      case CLOSING_BRACKET:
        createClose();
        break;
      case COMMENT:
        createComment(token);
        break;
      case NEW_FILE:
        createFile(token);
        break;
      case END_FILE:
        files.pop();
        break;
      default:
        throw new XMLGeneratorException("Unknown token");
      }
    }
    scanner.close();
  }

  /**
   * Process a {@link TokenType}.{@link KEY}.
   * 
   * @param token
   *          The token.
   */
  private void createKey(final Token token)
  {
    final Element element = document.createElement(XMLTags.KEY.getName());
    element.setAttribute("id", token.getContent());
    files.peek().appendChild(element);
    files.push(element);
  }

  /**
   * Process a {@link TokenType}.{@link VALUE}.
   * 
   * @param token
   *          The token.
   */
  private void createValue(final Token token)
  {
    final Element element = document.createElement(XMLTags.VALUE.getName());
    element.setTextContent(Trimmer.trimQuotes(token.getContent()));
    files.peek().appendChild(element);
    if (XMLTags.KEY.getName().equals(files.peek().getNodeName()))
    {
      files.pop();
    }
  }

  /**
   * Process a {@link TokenType}.{@link OPENING_BRACKET}.
   * 
   * @param token
   *          The token.
   */
  private void createOpen()
  {
    final Element element = document.createElement(XMLTags.GROUP.getName());
    files.peek().appendChild(element);
    files.push(element);
  }

  /**
   * Process a {@link TokenType}.{@link CLOSING_BRACKET}.
   * 
   * @param token
   *          The token.
   */
  private void createClose()
  {
    files.pop();
    if (XMLTags.KEY.getName().equals(files.peek().getNodeName()))
    {
      files.pop();
    }
  }

  /**
   * Process a {@link TokenType}.{@link COMMENT}.
   * 
   * @param token
   *          The token.
   */
  private void createComment(final Token token)
  {
    final Element element = document.createElement(XMLTags.COMMENT.getName());
    element.setTextContent(token.getContent());
    files.peek().appendChild(element);
  }

  /**
   * Process a {@link TokenType}.{@link NEW_FILE}.
   * 
   * @param token
   *          The token.
   * @throws MalformedURLException
   *           Couldn't create the URL of the new element.
   */
  private void createFile(final Token token) throws MalformedURLException
  {
    final String FILE_NAME = "filename";
    Element element;
    if (!XMLTags.CONFIG.getName().equals(files.peek().getTagName()))
    {
      element = document.createElement(XMLTags.FILEREFERENCE.getName());
      element.setTextContent(token.getContent());
      files.peek().appendChild(element);
    }
    URL context = new URL("file:" + files.peek().getAttribute(FILE_NAME));
    String newFile = PathProcessor.processInclude(token.getContent());
    Path path = Paths.get(newFile);
    element = document.createElement(XMLTags.FILE.getName());
    boolean windowsOS = System.getProperty("os.name").toLowerCase().contains("windows");
    if(windowsOS)
    {
      if(path.toFile().exists())
      {        
        element.setAttribute(FILE_NAME, new URL(context, "/" + newFile).getPath()); 
      }
      else
      {
        element.setAttribute(FILE_NAME, new URL(context, newFile).getPath());
      }
    }
    else
    {
      element.setAttribute(FILE_NAME, new URL(context, newFile).getPath());  
    }
    config.appendChild(element);
    files.push(element);
  }

  /**
   * Print a document to some output stream.
   * 
   * @param doc
   *          The document to print.
   * @param out
   *          The stream to use.
   * @throws TransformerException
   *           The XML-document couldn't be transformed.
   */
  public static void printDocument(final Document doc, final OutputStream out)
      throws TransformerException
  {
    final TransformerFactory tf = TransformerFactory.newInstance();
    tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    final Transformer transformer = tf.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
        "4");
    transformer.transform(new DOMSource(doc), new StreamResult(
        new OutputStreamWriter(out)));
  }

}
