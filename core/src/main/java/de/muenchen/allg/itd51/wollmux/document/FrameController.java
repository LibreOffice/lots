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
package de.muenchen.allg.itd51.wollmux.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.DeviceInfo;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.text.XTextDocument;
import com.sun.star.view.DocumentZoomType;

import org.libreoffice.ext.unohelper.common.UNO;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import org.libreoffice.ext.unohelper.util.UnoProperty;

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
   * Diese Methode liest die (optionalen) Attribute X, Y, WIDTH, HEIGHT und ZOOM aus
   * dem übergebenen Konfigurations-Abschnitt settings und setzt die
   * Fenstereinstellungen des Dokuments entsprechend um. Bei den Pärchen X/Y bzw.
   * SIZE/WIDTH müssen jeweils beide Komponenten im Konfigurationsabschnitt angegeben
   * sein.
   *
   * @param settings
   *          der Konfigurationsabschnitt, der X, Y, WIDHT, HEIGHT und ZOOM als
   *          direkte Kinder enthält.
   */
  public synchronized void setWindowViewSettings(ConfigThingy settings)
  {
    // Fenster holen (zum setzen der Fensterposition und des Zooms)
    XWindow window = null;
    try
    {
      window = getFrame().getContainerWindow();
    }
    catch (java.lang.Exception e)
    {
      LOGGER.debug("", e);
    }

    // Insets bestimmen (Rahmenmaße des Windows)
    int insetLeft = 0;
    int insetTop = 0;
    int insetRight = 0;
    int insetButtom = 0;

    if (UNO.XDevice(window) != null)
    {
      DeviceInfo di = UNO.XDevice(window).getInfo();
      insetButtom = di.BottomInset;
      insetTop = di.TopInset;
      insetRight = di.RightInset;
      insetLeft = di.LeftInset;
    }

    // Position setzen:
    try
    {
      int xPos = Integer.parseInt(settings.get("X").toString());
      int yPos = Integer.parseInt(settings.get("Y").toString());
      if (window != null)
      {
        window.setPosSize(xPos + insetLeft, yPos + insetTop, 0, 0, PosSize.POS);
      }
    }
    catch (NumberFormatException | NodeNotFoundException e)
    {
      LOGGER.debug("", e);
    }
    // Dimensions setzen:
    try
    {
      int width = Integer.parseInt(settings.get("WIDTH").toString());
      int height = Integer.parseInt(settings.get("HEIGHT").toString());
      if (window != null)
        window.setPosSize(0, 0, width - insetLeft - insetRight, height - insetTop
          - insetButtom, PosSize.SIZE);
    }
    catch (NumberFormatException | NodeNotFoundException e)
    {
      LOGGER.debug("", e);
    }

    // Zoom setzen:
    setDocumentZoom(settings);
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
      throw new ConfigurationErrorException(L.m("Invalid ZOOM value \"{0}\"", zoom));
  }

  /**
   * Liefert den Titel des Dokuments, wie er im Fenster des Dokuments angezeigt wird,
   * ohne den Zusatz " - OpenOffice.org Writer" oder "NoTitle", wenn der Titel nicht
   * bestimmt werden kann. TextDocumentModel('&lt;title&gt;')
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
