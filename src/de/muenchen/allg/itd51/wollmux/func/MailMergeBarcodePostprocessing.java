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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Vector;
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

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.L;

/**
 * Diese Klasse führt eine Nachbearbeitung auf Ergebnissen des OOoBasedMailMerge
 * durch, bei der enthaltene Bild-Platzhalter durch dynamisch erzeugte Barcode-Bilder
 * ersetzt werden.
 * 
 * Die Nachbearbeitung findet auf Basis des OpenDocumentFormats statt, d.h. als
 * Manipulation der content.xml-Datei auf XML-Ebene. Die Barcode-Bilder werden
 * dynamisch mit Hilfe der java-Bibliothek zxing erzeugt.
 * 
 * @author Christoph (CIB software GmbH)
 */
public class MailMergeBarcodePostprocessing
{
  static byte[] buf = new byte[1024];

  /**
   * Liest das ODT-Dokument inputFile, führt die Nachbearbeitung durch und schreibt
   * das Ergebnis in das File outputFile.
   * 
   * @throws ZipException
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws XMLStreamException
   * @throws FactoryConfigurationError
   * @throws WriterException
   * @throws NodeNotFoundException
   */
  public static void execute(File inputFile, File outputFile) throws ZipException,
      IOException, ParserConfigurationException, SAXException, XMLStreamException,
      FactoryConfigurationError, WriterException, NodeNotFoundException
  {
    ZipFile inputZip = new ZipFile(inputFile);
    ZipOutputStream outputZip =
      new ZipOutputStream(new FileOutputStream(outputFile));
    try
    {
      Enumeration<? extends ZipEntry> en = inputZip.entries();
      PictureReplacer replacer = new PictureReplacer();
      Manifest manifest = new Manifest();
      Vector<ConfigThingy> barcodeInfos = new Vector<ConfigThingy>();

      while (en.hasMoreElements())
      {
        ZipEntry entry = en.nextElement();
        String name = entry.getName();

        if (name.equals("content.xml") || name.equals("styles.xml") /* header&footer */)
        {
          InputStream is = inputZip.getInputStream(entry);
          entry.setCompressedSize(-1);
          outputZip.putNextEntry(entry);
          replacer.execute(is, outputZip, barcodeInfos);
          outputZip.closeEntry();
          continue;
        }

        else if (name.equals("META-INF/manifest.xml"))
        {
          InputStream is = inputZip.getInputStream(entry);
          manifest.parse(is);
          continue;
        }

        else if (name.equals("mimetype"))
        {
          entry.setMethod(ZipEntry.STORED);
        }

        // copy entry to output
        outputZip.putNextEntry(entry);
        InputStream in = inputZip.getInputStream(entry);
        int len;
        while ((len = in.read(buf)) > 0)
        {
          outputZip.write(buf, 0, len);
        }
        in.close();

        outputZip.closeEntry();
      }

      // create barcode pictures and add to document package
      HashSet<String> pathsCreated = new HashSet<String>();
      for (ConfigThingy barcodeInfo : barcodeInfos)
      {
        String path = barcodeInfo.get("PATH").getLastChild().getName();
        if (pathsCreated.contains(path)) continue;
        pathsCreated.add(path);
        
        String barcodeContent = barcodeInfo.get("CONTENT").getLastChild().getName();
        String type = barcodeInfo.get("TYPE").getLastChild().getName();

        ZipEntry entry = new ZipEntry(path);
        entry.setCompressedSize(-1);
        outputZip.putNextEntry(entry);

        if (type.equals("QR"))
        {
          QRCodeWriter writer = new QRCodeWriter();
          BitMatrix bitMatrix =
            writer.encode(barcodeContent, BarcodeFormat.QR_CODE, 300, 300);
          MatrixToImageWriter.writeToStream(bitMatrix, "png", outputZip);
        }
        else
        {
          throw new IllegalArgumentException(L.m(
            "Nicht unterstützter Barcode-Typ '%1'", type));
        }

        outputZip.closeEntry();
        manifest.addEntry(path, null, "image/png");
      }

      // write new manifest file (which will contain newly added pictures)
      ZipEntry entry = new ZipEntry("META-INF/manifest.xml");
      entry.setCompressedSize(-1);
      entry.setMethod(ZipEntry.DEFLATED);
      outputZip.putNextEntry(entry);
      manifest.write(outputZip);
      outputZip.closeEntry();
    }
    finally
    {
      outputZip.close();
      inputZip.close();
    }
  }

  /**
   * Der PictureReplacer ist ein SAX-XML Filter, der ein Eingangsdokument (XML)
   * liest, enthaltene '<text:span
   * text:style-name="WollMuxBarcodeInfo">BARCODEINFO(TYPE "QR" CONTENT "..."
   * )</text:span>' Zeilen sammelt (und ignoriert), das erste darauf folgende Bild
   * durch den Verweis auf ein neues, dynamisches Barcode-Bild ersetzt und das
   * Ergebnis (XML) in einen OutputStream schreibt.
   * 
   * @author Christoph (CIB software GmbH)
   */
  public static class PictureReplacer extends DefaultHandler
  {
    private XMLStreamWriter saxWriter;

    private int wollMuxBarcodeInfoIgnoreLevel = 0;

    StringBuffer barcodeInfoStrBuf = null;

    ConfigThingy lastBarcodeInfo = null;

    Vector<ConfigThingy> barcodeInfos;

    public void execute(InputStream is, OutputStream out,
        Vector<ConfigThingy> barcodeInfos) throws ParserConfigurationException,
        SAXException, XMLStreamException, FactoryConfigurationError, IOException
    {
      this.barcodeInfos = barcodeInfos;
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      saxWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(out, "UTF-8");
      saxWriter.writeStartDocument("UTF-8", "1.0");
      saxWriter.writeCharacters("\n");
      saxParser.parse(is, this);
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

          String barcodeContent = "";
          try
          {
            barcodeContent =
              lastBarcodeInfo.get("CONTENT").getFirstChild().getName();
          }
          catch (NodeNotFoundException x)
          {}

          String barcodeType = "NO_TYPE";
          try
          {
            barcodeType = lastBarcodeInfo.get("TYPE").getFirstChild().getName();
          }
          catch (NodeNotFoundException x)
          {}

          // Erzeuge neuen eindeutigen Elementnamen für das Barcode-Bild
          String md5sum = getMD5Sum(barcodeType + ":" + barcodeContent);
          String picturePath = "Pictures/" + md5sum + ".png";
          ConfigThingy path = new ConfigThingy("PATH");
          path.addChild(new ConfigThingy(picturePath));
          lastBarcodeInfo.addChild(path);

          saxWriter.writeAttribute("xlink:href", picturePath);
          barcodeInfos.add(lastBarcodeInfo);
        }
        catch (XMLStreamException e)
        {
          handleXMLStreamException(e);
        }
        catch (NoSuchAlgorithmException e)
        {
          throw new SAXException(
            L.m("Kann keinen eindeutigen Elementnamen für das neue Bild erzeugen"),
            e);
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
          handleXMLStreamException(e);
        }
      }
      else
      {
        wollMuxBarcodeInfoIgnoreLevel++;
      }
    }

    /**
     * Diese Helper-Methode liefert die MD5Sum zum String content zurück.
     * 
     * @throws NoSuchAlgorithmException
     *           wenn MD5-Algorithmus nicht verfügbar
     */
    private String getMD5Sum(String content) throws NoSuchAlgorithmException
    {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(content.getBytes());
      byte[] digest = md.digest();
      BigInteger bigInt = new BigInteger(1, digest);
      String hashtext = bigInt.toString(16);

      // padding with zeros until 32 characters.
      while (hashtext.length() < 32)
      {
        hashtext = "0" + hashtext;
      }

      return hashtext;
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
          handleXMLStreamException(e);
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
          }
          catch (Exception e)
          {
            throw new SAXException(L.m("Kann WollMuxBarcodeInfo nicht parsen"), e);
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
          handleXMLStreamException(e);
        }
      }
    }

    private void handleXMLStreamException(XMLStreamException e) throws SAXException
    {
      throw new SAXException(L.m("Fehler beim Schreiben des Ergebnisdokuments"), e);
    }

  }

  /**
   * Repräsentiert und schreibt einen Eintrag einer ODF-Manifest Datei.
   * 
   * @author Christoph (CIB software GmbH)
   */
  public static class ManifestEntry
  {
    private String fullPath = null;

    private String version = null;

    private String mediaType = null;

    public void setFullPath(String fullPath)
    {
      this.fullPath = fullPath;
    }

    public void setVersion(String version)
    {
      this.version = version;
    }

    public void setMediaType(String mediaType)
    {
      this.mediaType = mediaType;
    }

    public void write(XMLStreamWriter writer) throws XMLStreamException
    {
      writer.writeStartElement("manifest:file-entry");
      if (fullPath != null)
      {
        writer.writeAttribute("manifest:full-path", fullPath);
      }
      if (version != null)
      {
        writer.writeAttribute("manifest:version", version);
      }
      if (mediaType != null)
      {
        writer.writeAttribute("manifest:media-type", mediaType);
      }
      writer.writeEndElement();
    }
  }

  /**
   * Parst, repräsentiert und schreibt eine ODF-Manifest Datei und ermöglicht es,
   * neue Elemente hinzuzufügen.
   * 
   * @author Christoph (CIB software GmbH)
   */
  public static class Manifest extends DefaultHandler
  {
    private HashMap<String, String> manifestAtts = new HashMap<String, String>();

    private Vector<ManifestEntry> entries = new Vector<ManifestEntry>();

    public void parse(InputStream is) throws ParserConfigurationException,
        SAXException, IOException
    {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(is, this);
    }

    @Override
    public void startElement(String uri, String localName, String qName,
        Attributes attributes) throws SAXException
    {
      // store only attributes for manifest-nodes
      if (qName.equals("manifest:manifest"))
      {
        for (int i = 0; i < attributes.getLength(); ++i)
        {
          String attQName = attributes.getQName(i);
          String value = attributes.getValue(i);
          manifestAtts.put(attQName, value);
        }
      }

      else if (qName.equals("manifest:file-entry"))
      {
        ManifestEntry e = new ManifestEntry();
        for (int i = 0; i < attributes.getLength(); ++i)
        {
          String attQName = attributes.getQName(i);
          if (attQName.equals("manifest:full-path"))
          {
            e.setFullPath(attributes.getValue(i));
          }
          if (attQName.equals("manifest:version"))
          {
            e.setVersion(attributes.getValue(i));
          }
          if (attQName.equals("manifest:media-type"))
          {
            e.setMediaType(attributes.getValue(i));
          }
        }
        entries.add(e);
      }
    }

    public void addEntry(String fullPath, String version, String mediaType)
    {
      ManifestEntry e = new ManifestEntry();
      if (fullPath != null) e.setFullPath(fullPath);
      if (version != null) e.setVersion(version);
      if (mediaType != null) e.setMediaType(mediaType);
      entries.add(e);
    }

    public void write(OutputStream os) throws XMLStreamException,
        FactoryConfigurationError
    {
      XMLStreamWriter saxWriter =
        XMLOutputFactory.newInstance().createXMLStreamWriter(os, "UTF-8");
      saxWriter.writeStartDocument("UTF-8", "1.0");
      saxWriter.writeCharacters("\n");
      write(saxWriter);
    }

    private void write(XMLStreamWriter writer) throws XMLStreamException
    {
      writer.writeStartElement("manifest:manifest");
      for (Entry<String, String> att : manifestAtts.entrySet())
      {
        writer.writeAttribute(att.getKey(), att.getValue());
      }
      for (ManifestEntry entry : entries)
      {
        entry.write(writer);
      }
      for (ManifestEntry entry : entries)
      {
        entry.write(writer);
      }
      writer.writeEndElement();
    }
  }

  /**
   * Test-Methode
   * 
   * @throws Exception
   */
  public static void main(String[] args) throws Exception
  {
    File inputFile = new File("testdata/mailmerge_barcode/mm_output.odt");
    File outputFile = new File("testdata/mailmerge_barcode/mm_output_processed.odt");
    execute(inputFile, outputFile);
  }

}
