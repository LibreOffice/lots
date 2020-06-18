package de.muenchen.allg.itd51.wollmux.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.frame.XFrame;
import com.sun.star.text.XTextDocument;
import com.sun.star.view.DocumentZoomType;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.util.UnoProperty;

public class FrameController
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FrameController.class);

  private XTextDocument doc;

  public FrameController(XTextDocument doc)
  {
    this.doc = doc;
  }

  /**
   * Liefert den Frame zu diesem TextDocument oder null, wenn der Frame nicht bestimmt werden kann.
   *
   * @return Den Frame des TextDocuments, oder null.
   */
  public synchronized XFrame getFrame()
  {
    try
    {
      return doc.getCurrentController().getFrame();
    }
    catch (java.lang.Exception e)
    {
      return null;
    }
  }

  /**
   * Setzt das Fensters des TextDokuments auf Sichtbar (visible==true) oder
   * unsichtbar (visible == false).
   *
   * @param visible
   */
  public synchronized void setWindowVisible(boolean visible)
  {
    XFrame frame = getFrame();
    if (frame != null)
    {
      frame.getContainerWindow().setVisible(visible);
    }
  }

  /**
   * Diese Methode setzt den ZoomTyp bzw. den ZoomValue der Dokumentenansicht des Dokuments auf den
   * neuen Wert den das ConfigThingy conf im Knoten ZOOM angibt, der entwender eine ganzzahliger
   * Prozentwert (ohne "%"-Zeichen") oder einer der Werte "Optimal", "PageWidth", "PageWidthExact"
   * oder "EntirePage" ist.
   *
   * @param conf
   * @throws ConfigurationErrorException
   */
  public synchronized void setDocumentZoom(ConfigThingy conf)
  {
    try
    {
      setDocumentZoom(conf.getString("ZOOM", null));
    }
    catch (ConfigurationErrorException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Diese Methode setzt den ZoomTyp bzw. den ZoomValue der Dokumentenansicht des
   * Dokuments auf den neuen Wert zoom, der entwender eine ganzzahliger Prozentwert
   * (ohne "%"-Zeichen") oder einer der Werte "Optimal", "PageWidth",
   * "PageWidthExact" oder "EntirePage" ist.
   *
   * @param zoom
   * @throws ConfigurationErrorException
   */
  private void setDocumentZoom(String zoom) throws ConfigurationErrorException
  {
    Short zoomType = null;
    Short zoomValue = null;

    if (zoom != null)
    {
      // ZOOM-Argument auswerten:
      if ("Optimal".equalsIgnoreCase(zoom))
        zoomType = Short.valueOf(DocumentZoomType.OPTIMAL);

      if ("PageWidth".equalsIgnoreCase(zoom))
        zoomType = Short.valueOf(DocumentZoomType.PAGE_WIDTH);

      if ("PageWidthExact".equalsIgnoreCase(zoom))
        zoomType = Short.valueOf(DocumentZoomType.PAGE_WIDTH_EXACT);

      if ("EntirePage".equalsIgnoreCase(zoom))
        zoomType = Short.valueOf(DocumentZoomType.ENTIRE_PAGE);

      if (zoomType == null)
      {
        try
        {
          zoomValue = Short.valueOf(zoom);
        }
        catch (NumberFormatException e)
        {
          LOGGER.debug("", e);
        }
      }
    }

    // ZoomType bzw ZoomValue setzen:
    Object viewSettings = null;
    try
    {
      viewSettings =
        UNO.XViewSettingsSupplier(doc.getCurrentController()).getViewSettings();
    }
    catch (java.lang.Exception e)
    {
      LOGGER.debug("", e);
    }
    if (zoomType != null)
      Utils.setProperty(viewSettings, UnoProperty.ZOOM_TYPE, zoomType);
    else if (zoomValue != null)
      Utils.setProperty(viewSettings, UnoProperty.ZOOM_VALUE, zoomValue);
    else
      throw new ConfigurationErrorException(L.m("Ungültiger ZOOM-Wert '%1'", zoom));
  }

  /**
   * Liefert den Titel des Dokuments, wie er im Fenster des Dokuments angezeigt wird,
   * ohne den Zusatz " - OpenOffice.org Writer" oder "NoTitle", wenn der Titel nicht
   * bestimmt werden kann. TextDocumentModel('<title>')
   */
  public synchronized String getTitle()
  {
    String title = "NoTitle";
    try
    {
      title = UnoProperty.getProperty(getFrame(), UnoProperty.TITLE).toString();
      // "Untitled1 - OpenOffice.org Writer" -> cut " - OpenOffice.org Writer"
      int i = title.lastIndexOf(" - ");
      if (i >= 0) {
        title = title.substring(0, i);
      }
    }
    catch (java.lang.Exception e)
    {
      LOGGER.debug("", e);
    }
    return title;
  }
}
