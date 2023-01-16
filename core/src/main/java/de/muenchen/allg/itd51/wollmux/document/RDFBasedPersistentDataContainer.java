/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.document;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
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
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.afid.UnoIterator;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Implementiert die neue Zugriffsmethode auf persistente Daten im neuen
 * RDF-Metadatenframework
 * (https://wiki.documentfoundation.org/Documentation/DevGuide/Office_Development#RDF_metadata).
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class RDFBasedPersistentDataContainer implements
    PersistentDataContainer
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(RDFBasedPersistentDataContainer.class);

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
  private XURI wollmuxDatenURI = null;

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
  private final Map<DataID, XURI> mapDataIdToURI = new EnumMap<>(DataID.class);

  /**
   * Erzeugt einen neuen persistenten Datenspeicher im Dokument doc.
   */
  public RDFBasedPersistentDataContainer(XTextDocument doc)
      throws RDFMetadataNotSupportedException
  {
    try
    {
      xDMA = UNO.XDocumentMetadataAccess(doc);
      if (xDMA == null) {
        throw new RDFMetadataNotSupportedException();
      }
      xRepos = xDMA.getRDFRepository();
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
      LOGGER.error(L.m("Cannot access the RDF graph \"{0}\".",
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
    if (g != null) {
      return g;
    }
    try
    {
      XURI uri =
        xDMA.addMetadataFile(WOLLMUX_RDF_FILE, new XURI[] { wollmuxDatenURI });
      return xRepos.getGraph(uri);
    }
    catch (Exception e)
    {
      LOGGER.error(L.m("Cannot create the RDF graph \"{0}\".",
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
  private XURI getDataIdURI(DataID dataId)
  {
    return mapDataIdToURI.computeIfAbsent(dataId,
        key -> URI.create(UNO.defaultContext, WM_METADATA_XMLNS + key.getDescriptor()));
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
    if (g == null) {
      return null;
    }
    try
    {
      UnoIterator<Statement> statements = null;
      try
      {
        statements = UnoIterator.create(g.getStatements(xDMA, getDataIdURI(dataId), null), Statement.class);
      }
      catch (NoSuchElementException x)
      {
        /* kann regulär vorkommen */
        LOGGER.trace("", x);
      }
      if (statements != null && statements.hasNext())
        return statements.next().Object.getStringValue();
    }
    catch (Exception e)
    {
      LOGGER.error(L.m("Cannot read RDF metadata for DataID \"{0}\".",
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
    if (g == null) {
      return;
    }

    try
    {
      XURI uri = getDataIdURI(dataId);
      try
      {
        g.removeStatements(xDMA, uri, null);
      }
      catch (NoSuchElementException x)
      {
        /* kann regulär vorkommen */
        LOGGER.trace("", x);
      }
      g.addStatement(xDMA, uri, Literal.create(UNO.defaultContext, dataValue));
    }
    catch (Exception e)
    {
      LOGGER.error(
        L.m("Cannot set RDF metadata for DataID \"{0}\".", dataId), e);
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
    if (g == null) {
      return;
    }
    try
    {
      g.removeStatements(xDMA, getDataIdURI(dataId), null);
    }
    catch (NoSuchElementException x)
    {
      /* kann regulär vorkommen */
      LOGGER.trace("", x);
    }
    catch (Exception e)
    {
      LOGGER.error(
        L.m("Cannot delete RDF metadata for DataID \"{0}\".", dataId), e);
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
      XNamedGraph g = getWollMuxDatenGraph();
      if(g==null)
      {
        //Workaround, der Fehler liegt in libreoffice:
        //Wollmuxdaten werden im Dokument durch Einfügen von RTF-formatiertem Text gelöscht.
        //Daher werden die Wollmuxdaten neu aufgebaut
        xDMA = UNO.XDocumentMetadataAccess(doc);
        if (xDMA == null) {
          throw new RDFMetadataNotSupportedException();
        }
        xRepos = xDMA.getRDFRepository();
        wollmuxDatenURI = URI.create(UNO.defaultContext, WOLLMUX_DATEN_URI_STR);
        getOrCreateWollMuxDatenGraph();

        TextDocumentController documentController = DocumentManager.getTextDocumentController(doc);
        setData(DataID.SETTYPE, "formDocument");
        setData(DataID.TOUCH_WOLLMUXVERSION, WollMuxSingleton.getVersion());
        setData(DataID.TOUCH_OOOVERSION, UNO.getOOoVersion());
        setData(DataID.FORMULARWERTE, documentController.getFormFieldValuesString());
        documentController.storeCurrentFormDescription();
      }

      xDMA.storeMetadataToStorage(UNO.XStorageBasedDocument(doc).getDocumentStorage());
    }
    catch (Exception e)
    {
      LOGGER.error(L.m("Cannot persist RDF metadata."), e);
    }
  }
}
