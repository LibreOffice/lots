/*
 * Dateiname: DocumentDispatch.java
 * Projekt  : WollMux
 * Funktion : Implementiert XDispatch und kann alle Dispatch-URLs behandeln, die ein DocumentModel erfordern.
 * 
 * Copyright (c) 2009-2015 Landeshauptstadt München
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
 * 05.11.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux.event;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.DispatchResultEvent;
import com.sun.star.frame.DispatchResultState;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XNotifyingDispatch;
import com.sun.star.frame.XStatusListener;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.DocumentManager;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;

/**
 * Implementiert XDispatch und kann alle Dispatch-URLs behandeln, die ein
 * DocumentModel erfordern. Nähere Infos zur Funktionsweise siehe {@link Dispatch}.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class DocumentDispatch extends Dispatch
{
  /**
   * Ein XDispatch-Objekt, das die ursprüngliche Standard-Aktion der URL url ist.
   * 
   */
  private XDispatch origDisp;

  /**
   * Zu origDisp passender URL um das Standardverhalten auszulösen.
   */
  private com.sun.star.util.URL origUrl;

  /**
   * Der Frame des Textdokuments das sich alle Dispatches beziehen.
   */
  private XFrame frame;

  /**
   * Erzeugt einen neuen DocumentDispatch.
   * 
   * @param origDisp
   *          Ein XDispatch-Objekt, das die ursprüngliche Standard-Aktion der URL url
   *          auslösen kann.
   * @param origUrl
   *          Zu origDisp passender URL um das Standardverhalten auszulösen.
   * @param frame
   *          der Frame des Textdokuments in dessen Kontext der Dispatch ausgeführt
   *          werden soll.
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public DocumentDispatch(XDispatch origDisp, com.sun.star.util.URL origUrl,
      XFrame frame)
  {
    this.origDisp = origDisp;
    this.origUrl = origUrl;
    this.frame = frame;
  }

  /**
   * Wenn wir ein Original-Dispatch-Objekt haben, überlassen wir diesem das managen
   * des Status.
   * 
   * @see #removeStatusListener(XStatusListener, URL)
   */
  @Override
  public void addStatusListener(XStatusListener listener, URL url)
  {
    if (origDisp != null)
      origDisp.addStatusListener(listener, url);
    else
      super.addStatusListener(listener, url);
  }

  /**
   * Wenn wir ein Original-Dispatch-Objekt haben, überlassen wir diesem das managen
   * des Status.
   * 
   * @see #addStatusListener(XStatusListener, URL)
   */
  @Override
  public void removeStatusListener(XStatusListener listener, URL url)
  {
    if (origDisp != null)
      origDisp.removeStatusListener(listener, url);
    else
      super.removeStatusListener(listener, url);
  }

  private TextDocumentModel getModel()
  {
    XTextDocument doc = UNO.XTextDocument(frame.getController().getModel());
    if (doc != null)
    {
      return DocumentManager.getTextDocumentModel(doc);
    }
    return null;
  }

  public void dispatch__uno_print(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handlePrint(getModel(), origDisp, origUrl, props);
  }

  public void dispatch__uno_save(String arg, PropertyValue[] props)
  {
    dispatch__uno_saveas(arg, props);
  }

  public void dispatch__uno_save(String arg, PropertyValue[] props,
      XDispatchResultListener listener)
  {
    dispatch__uno_saveas(arg, props, listener);
  }

  public void dispatch__uno_saveas(String arg, PropertyValue[] props)
  {
    if (!getModel().hasURL())
    {
      WollMuxEventHandler.handleSaveAs(getModel(), new DocumentDispatchHelper(
        null, origDisp, origUrl, props, this));
    }
    else
      origDisp.dispatch(origUrl, props);
  }

  public void dispatch__uno_saveas(String arg, PropertyValue[] props,
      XDispatchResultListener listener)
  {
    if (!getModel().hasURL())
    {
      WollMuxEventHandler.handleSaveAs(getModel(), new DocumentDispatchHelper(
        listener, origDisp, origUrl, props, this));
    }
    else
    {
      final XNotifyingDispatch nd =
        UnoRuntime.queryInterface(XNotifyingDispatch.class, origDisp);
      nd.dispatchWithNotification(origUrl, props, listener);
    }
  }

  public void dispatch__uno_printdefault(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handlePrint(getModel(), origDisp, origUrl, props);
  }

  public void dispatch_wollmux_functiondialog(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleFunctionDialog(getModel(), arg);
  }

  public void dispatch_wollmux_formularmax4000(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleFormularMax4000Show(getModel());
  }

  public void dispatch_wollmux_ziffereinfuegen(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleButtonZifferEinfuegenPressed(getModel());
  }

  public void dispatch_wollmux_abdruck(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleButtonAbdruckPressed(getModel());
  }

  public void dispatch_wollmux_zuleitungszeile(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleButtonZuleitungszeilePressed(getModel());
  }

  public void dispatch_wollmux_markblock(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleMarkBlock(getModel(), arg);
  }

  public void dispatch_wollmux_textbausteineinfuegen(String arg,
      PropertyValue[] props)
  {
    WollMuxEventHandler.handleTextbausteinEinfuegen(getModel(), true);
  }

  public void dispatch_wollmux_platzhalteranspringen(String arg,
      PropertyValue[] props)
  {
    WollMuxEventHandler.handleJumpToPlaceholder(getModel());
  }

  public void dispatch_wollmux_textbausteinverweiseinfuegen(String arg,
      PropertyValue[] props)
  {
    WollMuxEventHandler.handleTextbausteinEinfuegen(getModel(), false);
  }

  public void dispatch_wollmux_seriendruck(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleSeriendruck(getModel(), false);
  }

  public void dispatch_wollmux_test(String arg, PropertyValue[] props)
  {
    if (WollMuxFiles.installQATestHandler()) TestHandler.doTest(getModel(), arg);
  }

  /**
   * Helperklasse für XNotifyingDispatch. Der ResultStatus kann beim Listener gesetzt
   * werden und der original Dispatch weitergereicht werden.
   *
   * @author daniel.sikeler
   *
   */
  public class DocumentDispatchHelper
  {

    /**
     * Der Listener des XNotifiyingDispatch.
     */
    private final XDispatchResultListener listener;

    /**
     * Der original Dispatch.
     */
    private final XDispatch origDisp;

    /**
     * Die URL des original Dispatch.
     */
    private final com.sun.star.util.URL origUrl;

    /**
     * Die Parameter des original Dispatch.
     */
    private final PropertyValue[] origArgs;

    /**
     * Ein Result, dass an den Listener übergeben wird.
     */
    private final DispatchResultEvent dre = new DispatchResultEvent();

    /**
     * Erzeugt einen neuen Helper für XNotifyingDispatches.
     *
     * @param listener
     *          Der Listener für den Dispatch.
     * @param origDisp
     *          Der original Dispatch.
     * @param origUrl
     *          Die URL des original Dispatch.
     * @param origArgs
     *          Die Parameter des original Dispatch.
     * @param dispatch
     *          Der Dispatch.
     */
    private DocumentDispatchHelper(XDispatchResultListener listener,
        XDispatch origDisp, com.sun.star.util.URL origUrl, PropertyValue[] origArgs,
        XDispatch dispatch)
    {
      this.listener = listener;
      this.origDisp = origDisp;
      this.origUrl = origUrl;
      this.origArgs = origArgs;
      dre.Source = dispatch;
    }

    /**
     * Sendet einen Result an den Listener und zeigt damit an, dass der Dispatch
     * beendet ist.
     *
     * @param success
     *          War der Dispatch erfolgreich?
     */
    public void dispatchFinished(final boolean success)
    {
      if (listener != null)
      {
        if (success)
        {
          dre.State = DispatchResultState.SUCCESS;
        }
        else
        {
          dre.State = DispatchResultState.FAILURE;
        }
        this.listener.dispatchFinished(dre);
      }
    }

    /**
     * Führt den Dispatch mit der original URL und en original Parametern aus.
     */
    public void dispatch()
    {
      if (origDisp != null)
      {
        if (listener == null)
        {
          origDisp.dispatch(origUrl, origArgs);
        }
        else
        {
          final XNotifyingDispatch nd =
            UnoRuntime.queryInterface(XNotifyingDispatch.class, origDisp);
          nd.dispatchWithNotification(origUrl, origArgs, listener);
        }
      }
    }
  }

}
