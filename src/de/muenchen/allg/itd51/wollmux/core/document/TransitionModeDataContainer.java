package de.muenchen.allg.itd51.wollmux.core.document;

import com.sun.star.text.XTextDocument;
import com.sun.star.util.XModifiable;

import de.muenchen.allg.afid.UNO;

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
  @Override
  public void setData(DataID dataId, String dataValue)
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
    legacy.removeData(dataId);
  }
}