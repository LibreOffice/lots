package de.muenchen.allg.itd51.wollmux.core.parser.generator.xml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import de.muenchen.allg.itd51.wollmux.core.parser.Trimmer;

/**
 * Generates a configuration out of a XML-document.
 *
 * @author daniel.sikeler
 *
 */
public class ConfGenerator
{
  /**
   * The nodes of the root element of the document.
   */
  private final NodeList nodes;

  /**
   * Create a new ConfGenerator.
   *
   * @param document
   *          The document with the configuration.
   * @throws XMLGeneratorException
   *           Couldn't create the configuration.
   */
  public ConfGenerator(final Document document) throws XMLGeneratorException
  {
    final Element root = document.getDocumentElement();
    if (!XMLTags.CONFIG.getName().equals(root.getTagName()))
    {
      throw new XMLGeneratorException("No root element config in xml file");
    }
    if (root.hasChildNodes())
    {
      this.nodes = root.getChildNodes();
    } else
    {
      throw new XMLGeneratorException(
          "Root element of document has no child-elements.");
    }
  }

  /**
   * Print a file entry of the XML-document to this stream.
   *
   * @param stream
   *          The stream
   * @param file
   *          The number of the file to print (starting by 0).
   * @throws XMLGeneratorException
   *           Invalid XML-document or unable to generate the configuration
   *           file.
   */
  public void generateConf(final OutputStream stream, final int file)
      throws XMLGeneratorException
  {
    try
    {
      PrettyPrinter printer = new PrettyPrinter(new OutputStreamWriter(stream));
      printChildren(nodes.item(file).getChildNodes(), printer);
      printer.close();
    } catch (IOException e)
    {
      throw new XMLGeneratorException("Print was unsuccessful", e);
    }
  }

  /**
   * Convert the first file entry of the configuration into a string. The first
   * file is the file, which was named for generating the configuration as XML.
   * The data from other files is not part of the string.
   *
   * @param encoding
   *          The encoding of the string.
   * @return The first file of the configuration as string.
   * @throws XMLGeneratorException
   *           Invalid XML-document or unsupported encoding.
   */
  public String generateConf(final String encoding) throws XMLGeneratorException
  {
    try
    {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      generateConf(os, 0);
      return os.toString(encoding);
    } catch (UnsupportedEncodingException e)
    {
      throw new XMLGeneratorException("Unsupported encoding", e);
    }
  }

  /**
   * Convert the configuration into strings. For each file entry a separate
   * string is build.
   *
   * @param encoding
   *          The encoding of the string.
   * @return A map mapping filenames to content (as string).
   * @throws XMLGeneratorException
   *           Invalid XML-document or unsupported encoding.
   */
  public Map<String, String> generateConfMap(final String encoding)
      throws XMLGeneratorException
  {
    try
    {
      Map<String, String> map = new LinkedHashMap<>(nodes.getLength());
      for (int file = 0; file < nodes.getLength(); file++)
      {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String filename = ((Element) nodes.item(file)).getAttribute("filename");
        generateConf(os, file);
        map.put(filename, os.toString(encoding));
      }
      return map;
    } catch (UnsupportedEncodingException e)
    {
      throw new XMLGeneratorException("Unsupported encoding", e);
    }
  }

  /**
   * Generate a configuration file from an XML-document. The whole data form the
   * document is writen to the files specified in the configuration.
   *
   * @throws XMLGeneratorException
   *           Invalid XML-document or unable to generate the configuration
   *           file.
   */
  public void generateConf() throws XMLGeneratorException
  {
    for (int index = 0; index < nodes.getLength(); index++)
    {
      File file = null;
      final Element elem = (Element) nodes.item(index);
      try
      {
        file = new File(elem.getAttribute("filename"));
        final OutputStreamWriter nxtWriter = new OutputStreamWriter(
            new FileOutputStream(file), Charset.forName("UTF8"));
        printChildren(elem.getChildNodes(), new PrettyPrinter(nxtWriter));
      } catch (final IOException e)
      {
        throw new XMLGeneratorException("Print was unsuccessful.", e);
      }
    }
  }

  /**
   * Print the children of a file tag.
   *
   * @param children
   *          The children.
   * @param printer
   *          The printer to use.
   * @throws IOException
   *           Couldn't print.
   * @throws XMLGeneratorException
   *           Unsupported elements.
   */
  private void printChildren(final NodeList children,
      final PrettyPrinter printer) throws IOException, XMLGeneratorException
  {
    for (int index = 0; index < children.getLength(); index++)
    {
      final Element element = (Element) children.item(index);
      switch (XMLTags.valueOf(element.getTagName().toUpperCase()))
      {
      case KEY:
        printKey(element, false, false, printer);
        break;
      case VALUE:
        printer.add2Line("(" + Trimmer.addQuoates(element.getTextContent())
            + ")");
        printer.print();
        break;
      case GROUP:
        printGroup(element, false, printer);
        break;
      case COMMENT:
        printComment(element, printer);
        break;
      case FILEREFERENCE:
        final String content = element.getTextContent();
        printer.add2Line("%include \"" + content + "\"");
        printer.print();
        break;
      default:
        throw new XMLGeneratorException("Unsupported element");
      }
    }
    printer.print();
    printer.flush();
  }

  /**
   * Print a key element and its children.
   *
   * @param elem
   *          The key element.
   * @param grouped
   *          Was the key in a group?
   * @param whitespace
   *          Is whitespace needed after the key-element.
   * @param printer
   *          The printer.
   * @throws XMLGeneratorException
   *           Key couldn't be printed.
   * @throws IOException
   *           Print was unsuccessful.
   */
  private void printKey(final Element elem, final boolean grouped,
      final boolean whitespace, final PrettyPrinter printer)
      throws XMLGeneratorException, IOException
  {
    final Element child = (Element) elem.getFirstChild();
    printer.add2Line(elem.getAttribute("id") + " ");
    boolean isGroup = false;
    switch (XMLTags.valueOf(child.getTagName().toUpperCase()))
    {
    case VALUE:
      printer.add2Line(Trimmer.addQuoates(child.getTextContent()));
      break;
    case GROUP:
      printGroup(child, true, printer);
      isGroup = true;
      break;
    default:
      throw new XMLGeneratorException("unsupported Element for key");
    }
    if (!grouped)
    {
      if (elem.getNextSibling() != null
          && !"comment".equals(((Element) elem.getNextSibling()).getTagName()))
      {
        if (!isGroup)
        {
          printer.print();
        }
      } else
      {
        printer.add2Line(" ");
      }
    }
    if (whitespace)
    {
      printer.add2Line(" ");
    }
  }

  /**
   * Print a comment element.
   *
   * @param elem
   *          The element.
   * @param printer
   *          The printer.
   * @throws IOException
   *           Print was unsuccessful.
   */
  private void printComment(final Element elem, final PrettyPrinter printer)
      throws IOException
  {
    printer.add2Line(elem.getTextContent());
    printer.print();
  }

  /**
   * Print a value in a group.
   *
   * @param value
   *          The value.
   * @param whitespace
   *          The whitespace after the value.
   * @param last
   *          Is it the last value in this group.
   * @param printer
   *          The printer.
   */
  private void printValueOfGroup(final String value, final String whitespace,
      final boolean last, final PrettyPrinter printer)
  {
    if (last)
    {
      printer.add2Line(Trimmer.addQuoates(value) + whitespace);
    } else
    {
      printer.add2Line(Trimmer.addQuoates(value));
    }
  }

  /**
   * Print the elements of a group tag.
   *
   * @param elements
   *          The elements.
   * @param isList
   *          Is the group a list?
   * @param printer
   *          The printer.
   * @throws XMLGeneratorException
   *           Unsupported elements for groups.
   * @throws IOException
   *           Couldn't print.
   */
  private void printGroupElements(final NodeList elements,
      final boolean isList, final PrettyPrinter printer)
      throws XMLGeneratorException, IOException
  {
    for (int index = 0; index < elements.getLength(); index++)
    {
      final Element child = (Element) elements.item(index);
      switch (XMLTags.valueOf(child.getTagName().toUpperCase()))
      {
      case KEY:
        printKey(child, true, index != elements.getLength() - 1, printer);
        break;
      case VALUE:
        printValueOfGroup(child.getTextContent(), isList ? ", " : " ",
            index != elements.getLength() - 1, printer);
        break;
      case GROUP:
        printGroup(child, false, printer);
        break;
      case COMMENT:
        printComment(child, printer);
        break;
      default:
        throw new XMLGeneratorException("unsupported element for group");
      }
    }
  }

  /**
   * Print a group element.
   *
   * @param elem
   *          The element.
   * @param named
   *          Was the group element preceeded by a key?
   * @param printer
   *          The printer.
   * @throws IOException
   *           Print was unsuccessful.
   * @throws XMLGeneratorException
   *           Unsupported element for group.
   */
  private void printGroup(final Element elem, final boolean named,
      final PrettyPrinter printer) throws IOException, XMLGeneratorException
  {
    printer.add2Line("(");
    final boolean list = elem.getElementsByTagName(XMLTags.KEY.getName())
        .getLength() == 0;
    if (named && !list)
    {
      printer.print();
      printer.indent();
    }
    final NodeList children = elem.getChildNodes();
    printGroupElements(children, list, printer);
    if (named && !list)
    {
      printer.removeIndent();
    }
    printer.add2Line(")");
    printer.print();
  }

}
