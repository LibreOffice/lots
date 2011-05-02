/*
 * Dateiname: PersistentData.java
 * Projekt  : WollMux
 * Funktion : Speichert Daten persistent in einem Writer-Dokument.
 * 
 * Copyright (c) 2008 Landeshauptstadt München
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
 * 09.11.2006 | BNK | Erstellung
 * 17.05.2010 | BED | +rewriteData(dataId)
 * 14.04.2011 | LUT | Primäre alternative Speicherung über das neue
 *                    RDF-Metadaten Framework.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import com.sun.star.awt.Size;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.drawing.XShape;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.rdf.Literal;
import com.sun.star.rdf.Statement;
import com.sun.star.rdf.URI;
import com.sun.star.rdf.XDocumentMetadataAccess;
import com.sun.star.rdf.XNamedGraph;
import com.sun.star.rdf.XRepository;
import com.sun.star.rdf.XURI;
import com.sun.star.table.BorderLine;
import com.sun.star.text.HoriOrientation;
import com.sun.star.text.RelOrientation;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.text.VertOrientation;
import com.sun.star.text.WrapTextMode;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XModifiable;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * Speichert Daten persistent in einem Writer-Dokument.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD-D101)
 */
public class PersistentData
{
  /**
   * Attributname zur Einstellung des Speichermodus für persistente Daten
   */
  private static final String PERSISTENT_DATA_MODE = "PERSISTENT_DATA_MODE";

  /**
   * Wert 'annotation' des Attributs PERSISTENT_DATA_MODE
   */
  private static final String PERSISTENT_DATA_MODE_ANNOTATION = "annotation";

  /**
   * Wert 'transition' des Attributs PERSISTENT_DATA_MODE
   */
  private static final String PERSISTENT_DATA_MODE_TRANSITION = "transition";

  /**
   * Wert 'rdf' des Attributs PERSISTENT_DATA_MODE
   */
  private static final String PERSISTENT_DATA_MODE_RDF = "rdf";

  /**
   * Wert 'rdfReadLegacy' des Attributs PERSISTENT_DATA_MODE
   */
  private static final String PERSISTENT_DATA_MODE_rdfReadLegacy = "rdfReadLegacy";

  /**
   * Liefert abhängig von der Konfigurationseinstellung PERSISTENT_DATA_MODE
   * (annotation|transition|rdfReadLegacy|rdf) den dazugehörigen
   * PersistentDataContainer für das Dokument doc.
   * 
   * Die folgende Aufstellung zeigt das Verhalten der verschiedenen Einstellungen
   * bezüglich der möglichen Kombinationen von Metadaten in den Ausgangsdokumenten
   * und der Aktualisierung der Metadaten in den Ergebnisdokumenten. Ein "*"
   * symbolisiert dabei, welcher Metadatencontainer jeweils aktuell ist bzw. bei
   * Dokumentänderungen aktualisiert wird.
   * 
   * Ausgangsdokument -> bearbeitet durch -> Ergebnisdokument
   * 
   * [N*] -> annotation-Mode (WollMux-Alt) -> [N*]
   * 
   * [N*] -> transition-Mode -> [N*R*]
   * 
   * [N*] -> rdfReadLegacy-Mode -> [R*]
   * 
   * [N*] -> rdf-Mode: NICHT UNTERSTÜTZT
   * 
   * [N*R*] -> annotation-Mode (WollMux-Alt) -> [N*R]
   * 
   * [N*R*] -> transition-Mode -> [N*R*]
   * 
   * [N*R*] -> rdfReadLegacy-Mode -> [R*]
   * 
   * [N*R*] -> rdf-Mode -> [NR*]
   * 
   * [N*R] -> annotation-Mode (WollMux-Alt) -> [N*R]
   * 
   * [N*R] -> transition-Mode -> [N*R*]
   * 
   * [N*R] -> rdfReadLegacy-Mode -> [R*]
   * 
   * [N*R] -> rdf-Mode: NICHT UNTERSTÜTZT
   * 
   * [NR*] -> annotation-Mode (WollMux-Alt) : NICHT UNTERSTÜTZT
   * 
   * [NR*] -> transition-Mode: NICHT UNTERSTÜTZT
   * 
   * [NR*] -> rdfReadLegacy-Mode: NICHT UNTERSTÜTZT
   * 
   * [NR*] -> rdf -> [NR*]
   * 
   * [R*] -> annotation-Mode (WollMux-Alt): NICHT UNTERSTÜTZT
   * 
   * [R*] -> transition-Mode -> [N*R*]
   * 
   * [R*] -> rdfReadLegacy-Mode -> [R*]
   * 
   * [R*] -> rdf-Mode -> [R*]
   * 
   * Agenda: [N]=Dokument mit Notizen; [R]=Dokument mit RDF-Metadaten; [NR]=Dokument
   * mit Notizen und RDF-Metadaten; *=N/R enthält aktuellen Stand;
   * 
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  public static PersistentDataContainer createPersistentDataContainer(
      XTextDocument doc)
  {
    ConfigThingy wmConf = WollMuxSingleton.getInstance().getWollmuxConf();
    String pdMode = PERSISTENT_DATA_MODE_TRANSITION;
    try
    {
      pdMode = wmConf.query(PERSISTENT_DATA_MODE).getLastChild().toString();
    }
    catch (NodeNotFoundException e)
    {
      Logger.log(L.m("Attribut %1 nicht gefunden. Verwende Voreinstellung '%2'.",
        PERSISTENT_DATA_MODE, PERSISTENT_DATA_MODE_TRANSITION));
    }

    try
    {
      if (PERSISTENT_DATA_MODE_TRANSITION.equals(pdMode))
      {
        return new TransitionModeDataContainer(doc);
      }
      else if (PERSISTENT_DATA_MODE_rdfReadLegacy.equals(pdMode))
      {
        return new RDFReadLegacyModeDataContainer(doc);
      }
      else if (PERSISTENT_DATA_MODE_RDF.equals(pdMode))
      {
        return new RDFBasedPersistentDataContainer(doc);
      }
      else if (PERSISTENT_DATA_MODE_ANNOTATION.equals(pdMode))
      {
        return new AnnotationBasedPersistentDataContainer(doc);
      }
      else
      {
        Logger.error(L.m(
          "Ungültiger Wert '%1' für Attribut %2. Verwende Voreinstellung '%3' statt dessen.",
          pdMode, PERSISTENT_DATA_MODE, PERSISTENT_DATA_MODE_TRANSITION));
        return new TransitionModeDataContainer(doc);
      }
    }
    catch (RDFMetadataNotSupportedException e)
    {
      // TODO: Disen Zweig noch testen
      Logger.error(L.m(
        "Die Einstellung '%1' für Attribut %2 ist mit dieser OpenOffice.org-Version nicht kompatibel. Verwende Einstellung '%3' statt dessen.",
        pdMode, PERSISTENT_DATA_MODE, PERSISTENT_DATA_MODE_ANNOTATION));
      return new AnnotationBasedPersistentDataContainer(doc);
    }
  }

  /**
   * Implementiert den DatenContainer für den transition-Modus, bei dem Metadaten
   * sowohl in den Notizen als auch in den RDF-Daten aktualisiert werden. Vorrang
   * beim Lesen haben immer die in den Notizen hinterlegten Daten, damit
   * sichergestellt ist, dass Dokumente, die mit alten WollMux-Versionen bearbeitet
   * wurden, auch korrekt gelesen werden. Jede Schreibaktion führt dazu, dass neben
   * den Notizen auch in die RDF-Daten geschrieben wird (CopyOnWrite). Jede
   * Leseaktion aus einem Container (Notizen bzw. RDF) führt dazu, dass der jeweils
   * andere Container aktualisiert wird (CopyOnRead), wobei sich dabei aber der
   * Modified-Status des Dokuments nicht ändern darf.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class TransitionModeDataContainer implements
      PersistentDataContainer
  {
    private PersistentDataContainer rdfData;

    private PersistentDataContainer legacy;

    private XTextDocument doc;

    /**
     * Erzeugt einen neuen persistenten Datenspeicher im Dokument doc.
     * 
     * @throws RDFMetadataNotSupportedException
     */
    public TransitionModeDataContainer(XTextDocument doc)
        throws RDFMetadataNotSupportedException
    {
      this.rdfData = new RDFBasedPersistentDataContainer(doc);
      this.legacy = new AnnotationBasedPersistentDataContainer(doc);
      this.doc = doc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.PersistentDataContainer#getData(java.lang.String
     * )
     * 
     * TESTED
     */
    public String getData(String dataId)
    {
      String data = legacy.getData(dataId);
      if (data != null)
      {
        copyOnRead(rdfData, dataId, data);
        return data;
      }

      data = rdfData.getData(dataId);
      if (data != null)
      {
        copyOnRead(legacy, dataId, data);
      }
      return data;
    }

    /**
     * Ruft c.setData(dataId, data) auf, wobei der Modified-Status des Dokuments
     * unangetastet bleibt.
     * 
     * @author Christoph Lutz (D-III-ITD-D101) TESTED
     */
    private void copyOnRead(PersistentDataContainer c, String dataId, String data)
    {
      XModifiable mod = UNO.XModifiable(doc);
      boolean modState = false;
      if (mod != null) modState = mod.isModified();

      c.setData(dataId, data);

      if (mod != null) try
      {
        mod.setModified(modState);
      }
      catch (Exception e)
      {}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.PersistentDataContainer#setData(java.lang.String
     * , java.lang.String)
     * 
     * TESTED
     */
    public void setData(String dataId, String dataValue)
    {
      legacy.setData(dataId, dataValue);
      rdfData.setData(dataId, dataValue); // CopyOnWrite
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.PersistentDataContainer#flush()
     * 
     * TESTED
     */
    public void flush()
    {
      rdfData.flush();
      legacy.flush();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.PersistentDataContainer#removeData(java.lang.
     * String)
     */
    public void removeData(String dataId)
    {
      rdfData.removeData(dataId);
      legacy.removeData(dataId);
    }
  }

  /**
   * Implementiert den DatenContainer für den rdf-Modus. Dieser Modus verhält sich
   * wie folgt:
   * 
   * Beim Lesen: Existiert eine Notiz mit Metadaten, so wird diese vorrangig gelesen
   * (damit sichergestellt ist, dass Dokumente, die mit alten WollMux-Versionen
   * bearbeitet wurden, auch korrekt gelesen werden). Ansonsten werden die
   * RDF-Metadaten ausgelesen. Nach dem Lesen einer Notiz wird die Notiz anschließend
   * gelöscht und ohne Änderung des Modified-Status des Dokuments in die RDF-Daten
   * übertragen (CopyOnRead).
   * 
   * Beim Schreiben: Beim Schreiben wird ausschließlich in die RDF-Metadaten
   * geschrieben.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class RDFReadLegacyModeDataContainer implements
      PersistentDataContainer
  {
    private PersistentDataContainer rdfData;

    private PersistentDataContainer legacy;

    private XTextDocument doc;

    private HashSet<String> removedFromLegacy;

    /**
     * Erzeugt einen neuen persistenten Datenspeicher im Dokument doc.
     * 
     * @throws RDFMetadataNotSupportedException
     */
    public RDFReadLegacyModeDataContainer(XTextDocument doc)
        throws RDFMetadataNotSupportedException
    {
      this.rdfData = new RDFBasedPersistentDataContainer(doc);
      this.legacy = new AnnotationBasedPersistentDataContainer(doc);
      this.doc = doc;
      this.removedFromLegacy = new HashSet<String>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.PersistentDataContainer#getData(java.lang.String
     * )
     * 
     * TESTED
     */
    public String getData(String dataId)
    {
      String data = legacy.getData(dataId);
      if (data != null)
      {
        copyOnRead(rdfData, dataId, data);
        ensureRemovedFromLegacy(dataId);
      }
      else
      {
        data = rdfData.getData(dataId);
      }
      return data;
    }

    /**
     * Stellt sicher, dass dataId aus den Notizen gelöscht wurde, bzw. löscht das
     * Element falls es noch nicht gelöscht wurde.
     * 
     * @author Christoph Lutz (D-III-ITD-D101) TESTED
     */
    private void ensureRemovedFromLegacy(String dataId)
    {
      if (!removedFromLegacy.contains(dataId))
      {
        removedFromLegacy.add(dataId);
        legacy.removeData(dataId);
      }
    }

    /**
     * Ruft c.setData(dataId, data) auf, wobei der Modified-Status des Dokuments
     * unangetastet bleibt.
     * 
     * @author Christoph Lutz (D-III-ITD-D101) TESTED
     */
    private void copyOnRead(PersistentDataContainer c, String dataId, String data)
    {
      XModifiable mod = UNO.XModifiable(doc);
      boolean modState = false;
      if (mod != null) modState = mod.isModified();

      c.setData(dataId, data);

      if (mod != null) try
      {
        mod.setModified(modState);
      }
      catch (Exception e)
      {}
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.PersistentDataContainer#setData(java.lang.String
     * , java.lang.String)
     * 
     * TESTED
     */
    public void setData(String dataId, String dataValue)
    {
      rdfData.setData(dataId, dataValue);
      ensureRemovedFromLegacy(dataId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.PersistentDataContainer#flush()
     * 
     * TESTED
     */
    public void flush()
    {
      rdfData.flush();
      legacy.flush();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.PersistentDataContainer#removeData(java.lang.
     * String)
     */
    public void removeData(String dataId)
    {
      rdfData.removeData(dataId);
      ensureRemovedFromLegacy(dataId);
    }
  }

  /**
   * Wird geworfen, wenn das verwendete OpenOffice.org das RDF-Metadaten-Interface
   * noch nicht unterstützt.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static class RDFMetadataNotSupportedException extends Exception
  {
    public RDFMetadataNotSupportedException(Exception e)
    {
      super(e);
    }
  }

  /**
   * Implementiert die neue Zugriffsmethode auf persistente Daten im neuen
   * RDF-Metadatenframework
   * (http://wiki.services.openoffice.org/wiki/Documentation/DevGuide
   * /OfficeDev/RDF_metadata).
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static class RDFBasedPersistentDataContainer implements
      PersistentDataContainer
  {
    /**
     * Enthält den XML-Namespace, der WollMux-Metadaten in der RDF/XML-Datei
     * eindeutig kennzeichnet.
     */
    private static final String WM_METADATA_XMLNS =
      "http://www.wollmux.org/WollMuxMetadata#";

    /**
     * Enthält den String aus dem wollmuxDatenURI gebaut wird.
     */
    private static final String WOLLMUX_DATEN_URI_STR =
      WM_METADATA_XMLNS + "WollMuxDaten";

    /**
     * Name der rdf-Datei im ODF-Package unter dem der RDF-Graph für WollMux-Daten
     * gespeichert werden soll.
     */
    private static final String WOLLMUX_RDF_FILE = "wollmux.rdf";

    /**
     * Speichert die URI für den RDF-Graphen, der die WollMux-Daten enthält.
     */
    private static XURI wollmuxDatenURI = null;

    /**
     * Das Dokument, in dem die Daten gespeichert werden.
     */
    private XTextDocument doc;

    /**
     * Referenz auf XDocumentMetadataAccess von doc
     */
    private XDocumentMetadataAccess xDMA;

    /**
     * Referenz auf das RDF-Repository von doc
     */
    private XRepository xRepos;

    /**
     * Dient zum Cachen von bereits erzeugten URI-Objekten für verschiedene dataIDs,
     * damit diese Objekte nicht mehrfach erzeugt werden müssen.
     */
    private static final HashMap<String, XURI> mapDataIdToURI =
      new HashMap<String, XURI>();

    /**
     * Erzeugt einen neuen persistenten Datenspeicher im Dokument doc.
     * 
     * @throws Exception
     */
    public RDFBasedPersistentDataContainer(XTextDocument doc)
        throws RDFMetadataNotSupportedException
    {
      try
      {
        xDMA = UnoRuntime.queryInterface(XDocumentMetadataAccess.class, doc);
        if (xDMA == null) throw new RDFMetadataNotSupportedException(null);
        xRepos = xDMA.getRDFRepository();
        if (wollmuxDatenURI == null)
          wollmuxDatenURI = URI.create(UNO.defaultContext, WOLLMUX_DATEN_URI_STR);
        this.doc = doc;
      }
      catch (Exception e)
      {
        throw new RDFMetadataNotSupportedException(e);
      }
    }

    /**
     * Liefert den RDF-Graphen unter dem WollMux-Metadaten gespeichert sind oder
     * null, falls es diesen Graph (noch) nicht gibt.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private XNamedGraph getWollMuxDatenGraph()
    {
      try
      {
        XURI[] metadataGraphURIs = xDMA.getMetadataGraphsWithType(wollmuxDatenURI);
        if (metadataGraphURIs.length >= 1)
          return xRepos.getGraph(metadataGraphURIs[0]);
      }
      catch (Exception e)
      {
        Logger.error(L.m("Kann nicht auf den RDF-Graphen '%1' zugreifen.",
          WOLLMUX_DATEN_URI_STR), e);
      }
      return null;
    }

    /**
     * Liefert den RDF-Graphen unter dem WollMux-Metadaten gespeichert sind oder
     * erzeugt einen neuen, falls bisher keiner existiert.
     * 
     * @return Kann im Fehlerfall auch null zurück liefern.
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private XNamedGraph getOrCreateWollMuxDatenGraph()
    {
      XNamedGraph g = getWollMuxDatenGraph();
      if (g != null) return g;
      try
      {
        XURI uri =
          xDMA.addMetadataFile(WOLLMUX_RDF_FILE, new XURI[] { wollmuxDatenURI });
        return xRepos.getGraph(uri);
      }
      catch (Exception e)
      {
        Logger.error(L.m("Kann RDF-Graphen '%1' nicht erzeugen.",
          WOLLMUX_DATEN_URI_STR), e);
      }
      return null;
    }

    /**
     * Liefert die RDF-URI, die WollMux-Metadatum zu dataId kennzeichnet.
     * 
     * @throws IllegalArgumentException
     *           Wenn dataId zu einer ungültigen URI führt.
     * 
     * @author Christoph Lutz (D-III-ITD-D101)
     */
    private XURI getDataIdURI(String dataId) throws IllegalArgumentException
    {
      XURI uri = mapDataIdToURI.get(dataId);
      if (uri == null)
      {
        uri = URI.create(UNO.defaultContext, WM_METADATA_XMLNS + dataId);
        mapDataIdToURI.put(dataId, uri);
      }
      return uri;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.PersistentData.DataContainer#getData(java.lang
     * .String)
     * 
     * TESTED
     */
    public String getData(String dataId)
    {
      XNamedGraph g = getWollMuxDatenGraph();
      if (g == null) return null;
      try
      {
        XEnumeration xEnum = null;
        try
        {
          xEnum = g.getStatements(xDMA, getDataIdURI(dataId), null);
        }
        catch (NoSuchElementException x)
        {/* kann regulär vorkommen */}
        if (xEnum != null && xEnum.hasMoreElements())
          return ((Statement) xEnum.nextElement()).Object.getStringValue();
      }
      catch (Exception e)
      {
        Logger.error(L.m("Kann RDF-Metadatum zur DataID '%1' nicht auslesen.",
          dataId), e);
      }
      return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.PersistentData.DataContainer#setData(java.lang
     * .String, java.lang.String)
     * 
     * TESTED
     */
    public void setData(String dataId, String dataValue)
    {
      XNamedGraph g = getOrCreateWollMuxDatenGraph();
      if (g == null) return;

      try
      {
        XURI uri = getDataIdURI(dataId);
        try
        {
          g.removeStatements(xDMA, uri, null);
        }
        catch (NoSuchElementException x)
        {/* kann regulär auftreten */}
        g.addStatement(xDMA, uri, Literal.create(UNO.defaultContext, dataValue));
      }
      catch (Exception e)
      {
        Logger.error(
          L.m("Kann RDF-Metadatum zur DataID '%1' nicht setzen.", dataId), e);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.muenchen.allg.itd51.wollmux.PersistentData.DataContainer#removeData(java
     * .lang.String)
     * 
     * TESTED
     */
    public void removeData(String dataId)
    {
      XNamedGraph g = getWollMuxDatenGraph();
      if (g == null) return;
      try
      {
        g.removeStatements(xDMA, getDataIdURI(dataId), null);
      }
      catch (NoSuchElementException x)
      {/* kann regulär auftreten */}
      catch (Exception e)
      {
        Logger.error(
          L.m("Kann RDF-Metadatum zur DataID '%1' nicht löschen.", dataId), e);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.PersistentData.DataContainer#flush()
     * 
     * TESTED
     */
    public void flush()
    {
      try
      {
        xDMA.storeMetadataToStorage(UNO.XStorageBasedDocument(doc).getDocumentStorage());
      }
      catch (Exception e)
      {
        Logger.error(L.m("Kann RDF-Metadaten nicht persistieren."), e);
      }
    }
  }

  /**
   * Implementiert die alte Zugriffsmethode auf persistente Daten in Notizen.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static class AnnotationBasedPersistentDataContainer implements
      PersistentDataContainer
  {
    /**
     * Property von doc zur Steuerung der Änderungsverfolgung, die beim Schreiben von
     * WollMux-Metadaten temporär ausgeschaltet werden muss.
     */
    private static final String RECORD_CHANGES = "RecordChanges";

    /**
     * Der Name des Frames in dem der WollMux seine Metadaten speichert.
     */
    private static final String WOLLMUX_FRAME_NAME = "WollMuxDaten";

    /**
     * Maximale Länge von Textfeldern, die der WollMux schreibt. Die Länge 16000
     * wurde gewählt, wegen http://qa.openoffice.org/issues/show_bug.cgi?id=108709.
     */
    private static final int TEXTFIELD_MAXLEN = 16000;

    /**
     * Das Dokument, in dem die Daten gespeichert werden.
     */
    private XTextDocument doc;

    /**
     * Enthält die dataIDs, die vor dem letzten Aufruf von flush verändert wurden und
     * wird für den Workaround für OOo-Issue 100374 benötigt. Kann mit Entfernen des
     * Workarounds auch wieder entfernt werden.
     */
    private HashSet<String> modifiedDataIDs;

    /**
     * Erzeugt einen neuen persistenten Datenspeicher im Dokument doc.
     */
    public AnnotationBasedPersistentDataContainer(XTextDocument doc)
    {
      this.doc = doc;
      this.modifiedDataIDs = new HashSet<String>();
    }

    /**
     * Die Methode liefert die unter ID dataId gespeicherten Daten zurück oder null,
     * wenn keine vorhanden sind.
     */
    public String getData(String dataId)
    {
      Vector<Object> textfields = getWollMuxTextFields(dataId, false, 0);
      if (textfields.size() == 0) return null;
      Iterator<Object> iter = textfields.iterator();
      StringBuilder buffy = new StringBuilder();
      while (iter.hasNext())
      {
        buffy.append((String) UNO.getProperty(iter.next(), "Content"));
      }
      return buffy.toString();
    }

    /**
     * Liefert alle Informations-Textfelder mit Id fieldName zurück.
     * 
     * @param create
     *          falls true so werden entsprechende Felder angelegt, wenn sie nicht
     *          existieren.
     * @size falls create == true werden soviele Felder angelegt, dass darin size
     *       Zeichen aufgeteilt in TEXTFIELD_MAXLEN lange Blöcke untergebracht werden
     *       können. Eventuell vorhandene überschüssige Felder werden gelöscht. Auch
     *       bei size == 0 wird mindestens ein Block geliefert.
     * @return leeren Vector falls das Feld nicht existiert und create == false oder
     *         falls ein Fehler auftritt.
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private Vector<Object> getWollMuxTextFields(String fieldName, boolean create,
        int size)
    {
      Vector<Object> textfields = new Vector<Object>();
      XTextFramesSupplier supp = UNO.XTextFramesSupplier(doc);
      if (supp != null)
      {
        int blockCount = (size + (TEXTFIELD_MAXLEN - 1)) / TEXTFIELD_MAXLEN;
        if (blockCount == 0) blockCount = 1;
        try
        {
          XNameAccess frameAccess = supp.getTextFrames();
          XShape frame;
          if (frameAccess.hasByName(WOLLMUX_FRAME_NAME))
            frame = UNO.XShape(frameAccess.getByName(WOLLMUX_FRAME_NAME));
          else
          {
            if (!create) return textfields;

            frame =
              UNO.XShape(UNO.XMultiServiceFactory(doc).createInstance(
                "com.sun.star.text.TextFrame"));
            Size frameSize = new Size();
            frameSize.Height = 5;
            frameSize.Width = 5;
            frame.setSize(frameSize);
            UNO.setProperty(frame, "AnchorType", TextContentAnchorType.AT_PAGE);
            XText text = doc.getText();
            text.insertTextContent(text.getStart(), UNO.XTextContent(frame), false);

            UNO.setProperty(frame, "BackTransparent", Boolean.TRUE);
            UNO.setProperty(frame, "BorderDistance", Integer.valueOf(0));
            BorderLine line = new BorderLine(0, (short) 0, (short) 0, (short) 0);
            UNO.setProperty(frame, "LeftBorder", line);
            UNO.setProperty(frame, "TopBorder", line);
            UNO.setProperty(frame, "BottomBorder", line);
            UNO.setProperty(frame, "RightBorder", line);
            UNO.setProperty(frame, "TextWrap", WrapTextMode.THROUGHT);
            UNO.setProperty(frame, "HoriOrient", Short.valueOf(HoriOrientation.NONE));
            UNO.setProperty(frame, "HoriOrientPosition", Integer.valueOf(0));
            UNO.setProperty(frame, "HoriOrientRelation",
              Short.valueOf(RelOrientation.PAGE_LEFT));
            UNO.setProperty(frame, "VertOrient",
              Short.valueOf(VertOrientation.BOTTOM));
            // UNO.setProperty(frame, "VertOrientPosition", Integer.valueOf(0));
            UNO.setProperty(frame, "VertOrientRelation",
              Short.valueOf(RelOrientation.PAGE_FRAME));
            UNO.setProperty(frame, "FrameIsAutomaticHeight", Boolean.FALSE);

            XNamed frameName = UNO.XNamed(frame);
            frameName.setName(WOLLMUX_FRAME_NAME);
          }

          XEnumeration paragraphEnu =
            UNO.XEnumerationAccess(frame).createEnumeration();
          while (paragraphEnu.hasMoreElements())
          {
            Object para = paragraphEnu.nextElement();
            if (create) UNO.setProperty(para, "CharHidden", Boolean.TRUE);
            XEnumeration textportionEnu =
              UNO.XEnumerationAccess(para).createEnumeration();
            while (textportionEnu.hasMoreElements())
            {
              Object textfield =
                UNO.getProperty(textportionEnu.nextElement(), "TextField");
              String author = (String) UNO.getProperty(textfield, "Author");
              // ACHTUNG! author.equals(fieldName) wäre falsch, da author null sein
              // kann!
              if (fieldName.equals(author))
              {
                textfields.add(textfield);
              }
            }
          }

          /*
           * Falls create == true und zuviele Felder gefunden wurden, dann loesche
           * die überzähligen.
           */
          if (create && textfields.size() > blockCount)
          {
            XText frameText = UNO.XTextFrame(frame).getText();
            while (textfields.size() > blockCount)
            {
              Object textfield = textfields.remove(textfields.size() - 1);
              frameText.removeTextContent(UNO.XTextContent(textfield));
            }
          }

          /*
           * Falls create == true und zu wenige Felder gefunden wurden, dann erzeuge
           * zusätzliche.
           */
          if (create && textfields.size() < blockCount)
          {
            XText frameText = UNO.XTextFrame(frame).getText();
            while (textfields.size() < blockCount)
            {
              Object annotation =
                UNO.XMultiServiceFactory(doc).createInstance(
                  "com.sun.star.text.TextField.Annotation");
              frameText.insertTextContent(frameText.getEnd(),
                UNO.XTextContent(annotation), false);
              UNO.setProperty(annotation, "Author", fieldName);
              textfields.add(annotation);
            }
          }

        }
        catch (Exception x)
        {
          return textfields;
        }
      } // if (supp != null)
      return textfields;
    }

    /**
     * Speichert dataValue mit der id dataId persistent im Dokument. Falls bereits
     * Daten mit der selben dataId vorhanden sind, werden sie überschrieben.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    public void setData(String dataId, String dataValue)
    {
      Object recordChanges = UNO.getProperty(doc, RECORD_CHANGES);
      UNO.setProperty(doc, RECORD_CHANGES, false);
      Vector<Object> textfields =
        getWollMuxTextFields(dataId, true, dataValue.length());
      if (textfields.size() == 0)
      {
        Logger.error(L.m("Konnte WollMux-Textfeld(er) \"%1\" nicht anlegen", dataId));
        UNO.setProperty(doc, RECORD_CHANGES, recordChanges);
        return;
      }

      modifiedDataIDs.add(dataId);
      Iterator<Object> iter = textfields.iterator();
      int start = 0;
      int len = dataValue.length();
      while (iter.hasNext())
      {
        int blocksize = len - start;
        if (blocksize > TEXTFIELD_MAXLEN) blocksize = TEXTFIELD_MAXLEN;
        String str = "";
        if (blocksize > 0)
        {
          str = dataValue.substring(start, start + blocksize);
          start += blocksize;
        }

        UNO.setProperty(iter.next(), "Content", str);
      }
      UNO.setProperty(doc, RECORD_CHANGES, recordChanges);
    }

    /**
     * Entfernt die mit dataId bezeichneten Daten, falls vorhanden.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void removeData(String dataId)
    {
      Object recordChanges = UNO.getProperty(doc, RECORD_CHANGES);
      UNO.setProperty(doc, RECORD_CHANGES, false);
      Vector<Object> textfields = getWollMuxTextFields(dataId, false, 0);
      if (textfields.size() > 0)
      {
        Iterator<Object> iter = textfields.iterator();
        while (iter.hasNext())
        {
          XTextContent txt = UNO.XTextContent(iter.next());
          try
          {
            txt.getAnchor().getText().removeTextContent(txt);
          }
          catch (Exception x)
          {
            Logger.error(x);
          }
        }
      }
      UNO.setProperty(doc, RECORD_CHANGES, recordChanges);
      modifiedDataIDs.remove(dataId);
    }

    /**
     * Entfernt zuerst die mit dataId bezeichneten Daten (falls vorhanden) und
     * speichert dann den alten Wert wieder neu unter dataId. In aller Regel sollte
     * der Zustand der Daten nach Aufruf dieser Methode also völlig unverändert sein.
     * 
     * FIXME: Diese Methode wird nur für den Workaround für Issue 100374 benötigt.
     * Sie kann also theoretisch entfernt werden, sobald der Workaround rausfliegt.
     * 
     * @param dataId
     *          die ID der Daten die neu geschrieben werden sollen.
     * @author Daniel Benkmann (D-III-ITD-D101)
     */
    private void rewriteData(String dataId)
    {
      String oldValue = getData(dataId);
      if (oldValue != null)
      {
        removeData(dataId);
        setData(dataId, oldValue);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.muenchen.allg.itd51.wollmux.PersistentData.DataContainer#flush() TODO:
     * testen
     */
    public void flush()
    {
      if (Workarounds.applyWorkaroundForOOoIssue100374())
      {
        for (String dataId : modifiedDataIDs)
          rewriteData(dataId);
        modifiedDataIDs.clear();
      }
    }
  }

}
