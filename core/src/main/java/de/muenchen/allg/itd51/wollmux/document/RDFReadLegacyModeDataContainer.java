/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextDocument;
import com.sun.star.util.XModifiable;

import de.muenchen.allg.afid.UNO;

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
public class RDFReadLegacyModeDataContainer implements
    PersistentDataContainer
{

  private static final Logger LOGGER = LoggerFactory.getLogger(RDFReadLegacyModeDataContainer.class);

  private PersistentDataContainer rdfData;

  private PersistentDataContainer legacy;

  private XTextDocument doc;

  private HashSet<DataID> removedFromLegacy;

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
    this.removedFromLegacy = new HashSet<>();
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
  @Override
  public String getData(DataID dataId)
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
  private void ensureRemovedFromLegacy(DataID dataId)
  {
    if (!removedFromLegacy.contains(dataId))
    {
      XModifiable mod = UNO.XModifiable(doc);
      boolean modState = false;
      if (mod != null)
      {
        modState = mod.isModified();
      }

      removedFromLegacy.add(dataId);
      legacy.removeData(dataId);

      if (mod != null)
      {
        try
        {
          mod.setModified(modState);
        } catch (Exception e)
        {
          LOGGER.trace("", e);
        }
      }
    }
  }

  /**
   * Ruft c.setData(dataId, data) auf, wobei der Modified-Status des Dokuments
   * unangetastet bleibt.
   *
   * @author Christoph Lutz (D-III-ITD-D101) TESTED
   */
  private void copyOnRead(PersistentDataContainer c, DataID dataId, String data)
  {
    XModifiable mod = UNO.XModifiable(doc);
    boolean modState = false;
    if (mod != null) {
      modState = mod.isModified();
    }

    c.setData(dataId, data);

    if (mod != null) {
      try
      {
        mod.setModified(modState);
      }
      catch (Exception e)
      {
        LOGGER.trace("", e);
      }
    }
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
  @Override
  public void setData(DataID dataId, String dataValue)
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
  @Override
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
  @Override
  public void removeData(DataID dataId)
  {
    rdfData.removeData(dataId);
    ensureRemovedFromLegacy(dataId);
  }
}
