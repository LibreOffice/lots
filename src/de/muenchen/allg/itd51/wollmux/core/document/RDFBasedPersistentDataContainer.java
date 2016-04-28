package de.muenchen.allg.itd51.wollmux.core.document;

import java.util.HashMap;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.rdf.Literal;
import com.sun.star.rdf.Statement;
import com.sun.star.rdf.URI;
import com.sun.star.rdf.XDocumentMetadataAccess;
import com.sun.star.rdf.XNamedGraph;
import com.sun.star.rdf.XRepository;
import com.sun.star.rdf.XURI;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.document.PersistentDataContainer.DataID;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Implementiert die neue Zugriffsmethode auf persistente Daten im neuen
 * RDF-Metadatenframework
 * (http://wiki.services.openoffice.org/wiki/Documentation/DevGuide
 * /OfficeDev/RDF_metadata).
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class RDFBasedPersistentDataContainer implements
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
  private static final HashMap<DataID, XURI> mapDataIdToURI =
    new HashMap<DataID, XURI>();

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
      xDMA = UNO.XDocumentMetadataAccess(doc);
      if (xDMA == null) throw new RDFMetadataNotSupportedException();
      xRepos = xDMA.getRDFRepository();
      if (wollmuxDatenURI == null)
        wollmuxDatenURI = URI.create(UNO.defaultContext, WOLLMUX_DATEN_URI_STR);
      this.doc = doc;
    }
    catch (Throwable e)
    {
      throw new RDFMetadataNotSupportedException();
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
  private XURI getDataIdURI(DataID dataId) throws IllegalArgumentException
  {
    XURI uri = mapDataIdToURI.get(dataId);
    if (uri == null)
    {
      uri =
        URI.create(UNO.defaultContext, WM_METADATA_XMLNS + dataId.getDescriptor());
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
  @Override
  public String getData(DataID dataId)
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
  @Override
  public void setData(DataID dataId, String dataValue)
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
  @Override
  public void removeData(DataID dataId)
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
  @Override
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