/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.text.XTextDocument;
import com.sun.star.util.XModifiable;

import org.libreoffice.ext.unohelper.common.UNO;

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
public class TransitionModeDataContainer implements
    PersistentDataContainer
{
  private static final Logger LOGGER = LoggerFactory
    .getLogger(TransitionModeDataContainer.class);

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
   * org.libreoffice.lots.PersistentDataContainer#getData(java.lang.String
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
   * org.libreoffice.lots.PersistentDataContainer#setData(java.lang.String
   * , java.lang.String)
   *
   * TESTED
   */
  @Override
  public void setData(DataID dataId, String dataValue)
  {
    legacy.setData(dataId, dataValue);
    rdfData.setData(dataId, dataValue); // CopyOnWrite
  }

  /*
   * (non-Javadoc)
   *
   * @see org.libreoffice.lots.PersistentDataContainer#flush()
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
   * org.libreoffice.lots.PersistentDataContainer#removeData(java.lang.
   * String)
   */
  @Override
  public void removeData(DataID dataId)
  {
    rdfData.removeData(dataId);
    legacy.removeData(dataId);
  }
}
