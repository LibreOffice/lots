/*
 * Dateiname: MailMergeBarcodePostprocessing.java
 * Projekt  : WollMux
 * Funktion : Ersetzt in Ergebnissen des OOoBasedMailMerge enthaltende Bild-Platzhalter durch 
 *            korrekte dynamische erzeugte Barcode-Bilder.
 * 
 * Copyright (c) CIB software GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 21.07.2014 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (CIB software GmbH)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.func;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.parser.SyntaxErrorException;

/**
 * Diese Klasse führt eine Nachbearbeitung auf Ergebnissen des OOoBasedMailMerge
 * durch, bei der enthaltene Bild-Platzhalter durch dynamisch erzeugte Barcode-Bilder
 * ersetzt werden.
 * 
 * Die Nachbearbeitung findet auf Basis des OpenDocumentFormats statt, d.h. als
 * Manipulation der content.xml-Datei auf XML-Ebene.
 * 
 * @author Christoph (CIB software GmbH)
 */
public class MailMergeBarcodePostprocessing
{

  public static class ZipCopyWriter
  {
    ZipFile zipFile;

    byte[] buf = new byte[1024];

    HashMap<String, String> filename2overridedata = new HashMap<String, String>();

    public ZipCopyWriter(ZipFile zipFile)
    {
      this.zipFile = zipFile;
    }

    public void setData(String filename, String data)
    {
      filename2overridedata.put(filename, data);
    }

    public void write(File outFilename) throws IOException
    {
      ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));
      Enumeration<? extends ZipEntry> en = zipFile.entries();

      while (en.hasMoreElements())
      {
        ZipEntry entry = en.nextElement();
        entry.setCompressedSize(-1);
        out.putNextEntry(entry);

        String override = filename2overridedata.get(entry.getName());
        if (override != null)
          out.write(override.getBytes("UTF-8"));
        else
        {
          InputStream in = zipFile.getInputStream(entry);
          int len;
          while ((len = in.read(buf)) > 0)
          {
            out.write(buf, 0, len);
          }
          in.close();
        }

        out.closeEntry();
      }
      out.close();
    }

    public void close() throws IOException
    {
      zipFile.close();
    }
  }

  public static class OdtDocument
  {
    ZipFile zipFile;

    ZipCopyWriter copyWriter;

    public OdtDocument load(File file) throws ZipException, IOException
    {
      zipFile = new ZipFile(file);
      copyWriter = new ZipCopyWriter(zipFile);
      return this;
    }

    public void storeTo(File file) throws IOException
    {
      copyWriter.write(file);
    }

    public void close() throws IOException
    {
      copyWriter.close();
    }

    public List<String> getDocumentContentFiles()
    {
      ArrayList<String> list = new ArrayList<String>();
      list.add("content.xml");
      list.add("styles.xml"); // styles.xml contains header and footer content
                              // elements
      return list;
    }

    public InputStream getInputStream(String filename) throws IOException
    {
      ZipEntry e = zipFile.getEntry(filename);
      return zipFile.getInputStream(e);
    }

    public void setContent(String filename, String data)
    {
      copyWriter.setData(filename, data);
    }
  }

  public static class PictureReplacer extends DefaultHandler
  {
    private SAXParser saxParser;

    private XMLStreamWriter saxWriter;

    private ByteArrayOutputStream result = new ByteArrayOutputStream();

    private int wollMuxBarcodeInfoIgnoreLevel = 0;

    StringBuffer barcodeInfoStrBuf = null;

    ConfigThingy lastBarcodeInfo = null;

    public PictureReplacer() throws ParserConfigurationException, SAXException,
        XMLStreamException, FactoryConfigurationError
    {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      saxParser = factory.newSAXParser();
      saxWriter =
        XMLOutputFactory.newInstance().createXMLStreamWriter(result, "UTF-8");
    }

    public void execute(InputStream is)
    {
      result.reset();
      try
      {
        saxWriter.writeStartDocument("UTF-8", "1.0");
        saxWriter.writeCharacters("\n");
        saxParser.parse(is, this);
      }
      catch (Throwable err)
      {
        err.printStackTrace();
      }
    }

    public String getReplacementResult()
    {
      return result.toString();
    }

    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) throws SAXException
    {

      // ignore everything under text:span with text:style-name "WollMuxBarcodeInfo"
      // and collect the BarcodeInfo.
      if (qName.equals("text:span"))
      {
        int idx = attributes.getIndex("text:style-name");
        if (idx >= 0)
        {
          String textStyleName = attributes.getValue(idx);
          if (textStyleName.equals("WollMuxBarcodeInfo"))
          {
            wollMuxBarcodeInfoIgnoreLevel = 1;
            barcodeInfoStrBuf = new StringBuffer();
          }
        }
      }

      // replace <draw:image
      // xlink:href="Pictures/100000000000027100000271204314A4.png"> by new picture
      if (qName.equals("draw:image") && lastBarcodeInfo != null)
      {
        try
        {
          saxWriter.writeStartElement(qName);
          for (int i = 0; i < attributes.getLength(); ++i)
          {
            String attQName = attributes.getQName(i);
            if (!attQName.equals("xlink:href"))
            {
              String value = attributes.getValue(i);
              saxWriter.writeAttribute(attQName, value);
            }
          }
          saxWriter.writeAttribute("xlink:href", lastBarcodeInfo.get("CONTENT").getFirstChild().getName());
        }
        catch (XMLStreamException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        catch (NodeNotFoundException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        lastBarcodeInfo = null;
        return;
      }

      // write element if it is not ignored
      if (wollMuxBarcodeInfoIgnoreLevel <= 0)
      {
        try
        {
          saxWriter.writeStartElement(qName);
          for (int i = 0; i < attributes.getLength(); ++i)
          {
            String attQName = attributes.getQName(i);
            String value = attributes.getValue(i);
            saxWriter.writeAttribute(attQName, value);
          }
        }
        catch (XMLStreamException e)
        {
          e.printStackTrace();
        }
      }
      else
      {
        wollMuxBarcodeInfoIgnoreLevel++;
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
      if (wollMuxBarcodeInfoIgnoreLevel <= 0)
      {
        try
        {
          saxWriter.writeCharacters(ch, start, length);
        }
        catch (XMLStreamException e)
        {
          e.printStackTrace();
        }
      }
      else
      {
        barcodeInfoStrBuf.append(ch, start, length);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
        throws SAXException
    {
      if (wollMuxBarcodeInfoIgnoreLevel > 0)
      {
        wollMuxBarcodeInfoIgnoreLevel--;
        if (wollMuxBarcodeInfoIgnoreLevel == 0)
        {
          try
          {
            lastBarcodeInfo = new ConfigThingy("BCI", barcodeInfoStrBuf.toString());
            System.out.println(lastBarcodeInfo.stringRepresentation());
          }
          catch (IOException e)
          {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          catch (SyntaxErrorException e)
          {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
          barcodeInfoStrBuf = null;
        }
      }

      if (wollMuxBarcodeInfoIgnoreLevel <= 0)
      {
        try
        {
          saxWriter.writeEndElement();
        }
        catch (XMLStreamException e)
        {
          e.printStackTrace();
        }
      }
    }

  }

  public static void postprocess(File inputFile, File outputFile)
      throws ZipException, IOException, ParserConfigurationException, SAXException,
      XMLStreamException, FactoryConfigurationError
  {
    OdtDocument input = new OdtDocument();
    input.load(inputFile);
    PictureReplacer replacer = new PictureReplacer();
    for (String contentElement : input.getDocumentContentFiles())
    {
      InputStream is = input.getInputStream(contentElement);
      replacer.execute(is);
      input.setContent(contentElement, replacer.getReplacementResult());

      System.out.println(replacer.getReplacementResult());
    }
    input.storeTo(outputFile);
  }

  /**
   * Test-Methode
   * 
   * @throws Exception
   */
  public static void main(String[] args) throws Exception
  {
    File inputFile = new File("C:/temp/LHM/mm_output.odt");
    File outputFile = new File("C:/temp/LHM/mm_output_processed.odt");
    postprocess(inputFile, outputFile);
  }

}
