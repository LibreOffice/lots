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
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Implementiert XNotifyingDispatch und kann alle Dispatch-URLs behandeln, die ein
 * DocumentModel erfordern. Nähere Infos zur Funktionsweise siehe
 * {@link BaseDispatch}.
 *
 * Der original Dispatch kann mittels Callback ausgeführt werden. Wenn der Dispatch
 * behandelt wurde lässt sich das Ergebnis mittels Callback setzen.
 *
 * @author Daniel Sikeler
 */
public class DocumentNotifyingDispatch extends NotifyingDispatch implements
    DispatchHelper
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
   * Der ResultListener. Wird gesetzt, wenn ein Dispatch mit Listener erfolgt.
   */
  private XDispatchResultListener listener = null;

  /**
   * Die Properties. Wird gesetzt wenn der Dispatch aufgerufen wird.
   */
  private PropertyValue[] props;

  /**
   * Erzeugt einen neuen DocumentDispatch.
   *
   * @param origDisp
   *          Ein XNotifyingDispatch-Objekt, das die ursprüngliche Standard-Aktion
   *          der URL url auslösen kann.
   * @param origUrl
   *          Zu origDisp passender URL um das Standardverhalten auszulösen.
   * @param frame
   *          der Frame des Textdokuments in dessen Kontext der Dispatch ausgeführt
   *          werden soll.
   */
  public DocumentNotifyingDispatch(XDispatch origDisp,
      com.sun.star.util.URL origUrl, XFrame frame)
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

  private TextDocumentController getDocumentController()
  {
    XTextDocument doc = UNO.XTextDocument(frame.getController().getModel());
    if (doc != null)
    {
      return DocumentManager.getTextDocumentController(doc);
    }
    return null;
  }

  @Override
  public void dispatchOriginal()
  {
    if (origDisp != null)
    {
      if (listener == null)
      {
        origDisp.dispatch(origUrl, props);
      }
      else
      {
        final XNotifyingDispatch nd =
          UnoRuntime.queryInterface(XNotifyingDispatch.class, origDisp);
        nd.dispatchWithNotification(origUrl, props, listener);
      }
    }
  }

  @Override
  public void dispatchFinished(boolean success)
  {
    if (listener != null)
    {
      final DispatchResultEvent dre = new DispatchResultEvent();
      dre.Source = this;
      if (success)
      {
        dre.State = DispatchResultState.SUCCESS;
      }
      else
      {
        dre.State = DispatchResultState.FAILURE;
      }
      listener.dispatchFinished(dre);
    }
  }

  public void dispatch__uno_save(String arg, PropertyValue[] props)
  {
    this.props = props;
    if (!getDocumentController().getModel().hasURL())
    {
      WollMuxEventHandler.handleSaveAs(getDocumentController(), this, isSynchronMode(props));
    }
    else
    {
      dispatchOriginal();
    }
  }

  public void dispatch__uno_save(String arg, PropertyValue[] props,
      XDispatchResultListener listener)
  {
    this.props = props;
    this.listener = listener;
    if (!getDocumentController().getModel().hasURL())
    {
      WollMuxEventHandler.handleSaveAs(getDocumentController(), this, isSynchronMode(props));
    }
    else
    {
      dispatchOriginal();
    }
  }

  public void dispatch__uno_saveas(String arg, PropertyValue[] props)
  {
    this.props = props;
    if (!getDocumentController().getModel().hasURL())
    {
      WollMuxEventHandler.handleSaveAs(getDocumentController(), this, isSynchronMode(props));
    }
    else
    {
      dispatchOriginal();
    }
  }

  public void dispatch__uno_saveas(String arg, PropertyValue[] props,
      XDispatchResultListener listener)
  {
    this.props = props;
    this.listener = listener;
    if (!getDocumentController().getModel().hasURL())
    {
      WollMuxEventHandler.handleSaveAs(getDocumentController(), this, isSynchronMode(props));
    }
    else
    {
      dispatchOriginal();
    }
  }

  public void dispatch__uno_updateinputfields(String arg, PropertyValue[] props)
  {
    this.props = props;
    WollMuxEventHandler.handleUpdateInputFields(getDocumentController(), this,
        isSynchronMode(props));
  }

  public void dispatch__uno_updateinputfields(String arg, PropertyValue[] props, XDispatchResultListener listener)
  {
    this.props = props;
    this.listener = listener;
    WollMuxEventHandler.handleUpdateInputFields(getDocumentController(), this,
        isSynchronMode(props));
  }
}
