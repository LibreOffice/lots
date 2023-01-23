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
package de.muenchen.allg.itd51.wollmux.print;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XProgressBar;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.EventObject;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractActionListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.util.L;
import org.libreoffice.ext.unohelper.util.UnoComponent;

/**
 * Progress bar of prints. If there are several print functions chained, the progress bar shows how
 * many versions each print functions creates. The progress is shown for the over all number of
 * prints.
 */
public class PrintProgressBar
{

  /**
   * Mapping from print function to number of versions produced by the function.
   */
  private HashMap<Object, Integer> maxValues;

  /**
   * Mapping from print function to produced prints. The number has to be between 0 and the maximum
   * value in {@link #maxValues}.
   */
  private HashMap<Object, Integer> currentValues;

  /**
   * List of print functions in reverse order. That means new values are added at the beginning.
   */
  private LinkedList<Object> order;

  /**
   * Listener for aborting the dialog.
   */
  private ActionListener abortListener;

  /**
   * The window.
   */
  private XWindow window;

  /**
   * The progress control.
   */
  private XProgressBar progressBar;

  /**
   * The label of the progress bar.
   */
  private XFixedText statusLabel;

  /**
   * Status of the print.
   */
  private boolean finished = false;

  /**
   * Create a new progress bar dialog and show it.
   *
   * @param message
   *          The initial message of the dialog.
   * @param abortListener
   *          Listener to be called if the dialog is canceled.
   */
  public PrintProgressBar(final String message, ActionListener abortListener)
  {
    this.order = new LinkedList<>();
    this.maxValues = new HashMap<>();
    this.currentValues = new HashMap<>();
    this.abortListener = abortListener;
    createGui();
    setMessage(message);
  }

  /**
   * Create the dialog and its children.
   */
  private void createGui()
  {
    XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());

    XContainerWindowProvider provider = UNO.XContainerWindowProvider(
        UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_CONTAINER_WINDOW_PROVIDER));

    window = provider.createContainerWindow("vnd.sun.star.script:WollMux.print_progress?location=application", "", peer,
        null);
    window.addWindowListener(new AbstractWindowListener()
    {
      @Override
      public void disposing(EventObject event)
      {
        cancel();
      }
    });
    XControlContainer controlContainer = UNO.XControlContainer(window);

    progressBar = UNO.XProgressBar(controlContainer.getControl("progress"));
    statusLabel = UNO.XFixedText(controlContainer.getControl("progressText"));
    AbstractActionListener cancelListener = event -> {
      finished = false;
      window.dispose();
    };
    UNO.XButton(controlContainer.getControl("abort")).addActionListener(cancelListener);
    window.setEnable(true);
    window.setVisible(true);
  }

  /**
   * Cancel the dialog and notify listener if necessary.
   */
  private void cancel()
  {
    if (!finished && abortListener != null)
    {
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
    }
  }

  /**
   * Dispose and close the dialog.
   */
  public void dispose()
  {
    finished = true;
    window.dispose();
  }

  /**
   * Update the text of the status label.
   *
   * @param text
   *          The new text.
   */
  public void setMessage(String text)
  {
    statusLabel.setText(text);
  }

  /**
   * Register a print function with its maximum prints. If maxValue is 0 the print function is
   * removed.
   *
   * @param key
   *          The print function.
   * @param maxValue
   *          The expected number of prints create by the function or 0 to remove the function.
   */
  public void setMaxValue(Object key, int maxValue)
  {
    if (key == null)
    {
      return;
    }

    if (maxValue == 0)
    {
      // delete key if counter is 0
      maxValues.remove(key);
      currentValues.remove(key);
      for (Iterator<Object> iter = order.iterator(); iter.hasNext();)
      {
        Object k = iter.next();
        if (k != null && k.equals(key))
        {
          iter.remove();
        }
      }
    } else
    {
      if (!maxValues.containsKey(key))
      {
        order.addFirst(key);
      }
      maxValues.put(key, maxValue);
      if (!currentValues.containsKey(key))
      {
        currentValues.put(key, 0);
      }
    }

    refresh();
  }

  /**
   * Update state of a print function.
   *
   * @param key
   *          The print function.
   * @param value
   *          Number of prints created by the function.
   */
  public void setValue(Object key, int value)
  {
    if (key == null)
    {
      return;
    }
    Integer max = maxValues.get(key);
    if (max == null)
    {
      return;
    }
    if (value > max)
    {
      value = max;
    }
    if (value < 0)
    {
      value = 0;
    }

    currentValues.put(key, value);
    refresh();
  }

  /**
   * Update the progress bar and the message in the dialog.
   */
  private void refresh()
  {
    int allMax = 1;
    int allCurrent = 0;
    StringBuilder fromMaxString = new StringBuilder();
    boolean showfms = order.size() > 1;
    if (showfms)
    {
      fromMaxString.append(" (=");
    }
    boolean first = true;

    for (Object key : order)
    {
      allCurrent += currentValues.get(key) * allMax;
      if (first)
        first = false;
      else if (showfms)
      {
        fromMaxString.append("x");
      }
      if (showfms)
      {
        fromMaxString.append(maxValues.get(key));
      }
      allMax *= maxValues.get(key);
    }
    if (showfms)
    {
      fromMaxString.append(")");
    }

    progressBar.setRange(0, allMax);
    progressBar.setValue(allCurrent);
    statusLabel.setText(L.m("{0} from {1}{2} steps", allCurrent, allMax, fromMaxString));
  }
}
