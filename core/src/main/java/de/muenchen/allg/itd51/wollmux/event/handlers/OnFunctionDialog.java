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
package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFrames;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;

import org.libreoffice.ext.unohelper.common.UNO;
import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Event for showing a functional dialog.
 *
 * The values returned by the dialog are set to the form field of the document.
 */
public class OnFunctionDialog extends WollMuxEvent
{

  private static final Logger LOGGER = LoggerFactory.getLogger(OnFunctionDialog.class);

  private TextDocumentController documentController;

  private String dialogName;

  private boolean[] lock = new boolean[] { true };

  /**
   * Create this event.
   *
   * @param documentController
   *          The document.
   * @param dialogName
   *          The name of the dialog.
   */
  public OnFunctionDialog(TextDocumentController documentController,
      String dialogName)
  {
    this.documentController = documentController;
    this.dialogName = dialogName;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    // get the dialog.
    Dialog dialog = GlobalFunctions.getInstance().getFunctionDialogs().get(dialogName);
    if (dialog == null)
    {
      throw new WollMuxFehlerException(
          L.m("Function dialog \"{0}\" is not defined.", dialogName));
    }

    try
    {
      Dialog dialogInst = dialog.instanceFor(new HashMap<>());

      setLock();
      dialogInst.show(new DialogFinishedListener(dialogInst), documentController.getFunctionLibrary(),
          documentController.getDialogLibrary());
      waitForUnlock();
    } catch (ConfigurationErrorException e)
    {
      throw new CantStartDialogException(e);
    }
  }

  /**
   * Enable all LibreOffice windows. If a window is enabled, it processes user actions like mouse
   * clicks.
   *
   * @param enabled
   *          If true the windows are enabled, otherwise not.
   */
  private static void enableAllOOoWindows(boolean enabled)
  {
    try
    {
      XFrames frames = UNO.XFramesSupplier(UNO.desktop).getFrames();
      for (int i = 0; i < frames.getCount(); i++)
      {
        XFrame frame = UNO.XFrame(frames.getByIndex(i));
        XWindow contWin = frame.getContainerWindow();
        if (contWin != null)
        {
          contWin.setEnable(enabled);
        }
      }
    } catch (IndexOutOfBoundsException | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Disables all LibreOffice windows ({@link #enableAllOOoWindows(boolean)} for a modal dialog.
   * Usage: {@code setLock(); //call dialog waitForUnlock();} {@link #waitForUnlock()} blocks the
   * thread until {#link {@link #setUnlock()} is called when the dialog is finished.
   */
  private void setLock()
  {
    enableAllOOoWindows(false);
    synchronized (lock)
    {
      lock[0] = true;
    }
  }

  /**
   * Blocks until a lock is released.
   *
   * @see #setLock()
   */
  private void waitForUnlock()
  {
    try
    {
      synchronized (lock)
      {
        while (lock[0])
        {
          lock.wait();
        }
      }
    } catch (InterruptedException e)
    {
      LOGGER.debug("Thread interrupted", e);
      Thread.currentThread().interrupt();
    }
  }

  private class DialogFinishedListener implements ActionListener
  {
    private Dialog dialogInst;

    public DialogFinishedListener(Dialog dialogInst)
    {
      this.dialogInst = dialogInst;
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      setUnlock();

      // abort if dialog wasn't finished with "OK"
      String cmd = event.getActionCommand();
      if (cmd.equalsIgnoreCase("select"))
      {
        // focus document to show changes
        documentController.getFrameController().getFrame().getContainerWindow().setFocus();

        Collection<String> schema = dialogInst.getSchema();
        for (String id : schema)
        {
          String value = dialogInst.getData(id).toString();
          documentController.addFormFieldValue(id, value);
        }
      }
    }

    /**
     * Activates all LibreOffice windows ({@link #enableAllOOoWindows(boolean)}).
     *
     * @see #setLock()
     */
    private void setUnlock()
    {
      synchronized (lock)
      {
        lock[0] = false;
        lock.notifyAll();
      }
      enableAllOOoWindows(true);
    }
  }
}
