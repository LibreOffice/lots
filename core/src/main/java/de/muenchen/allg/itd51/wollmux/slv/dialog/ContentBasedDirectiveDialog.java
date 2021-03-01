/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.slv.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.Selection;
import com.sun.star.awt.SpinEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialog2;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XScrollBar;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperRuntimeException;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractAdjustmentListener;
import de.muenchen.allg.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.dialog.adapter.AbstractSpinListener;
import de.muenchen.allg.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.slv.print.ContentBasedDirective;
import de.muenchen.allg.util.UnoComponent;

/**
 * A dialog for printing documents with content based directives.
 */
public class ContentBasedDirectiveDialog
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ContentBasedDirectiveDialog.class);

  private static final String SPINFIELD_NAME = "Count";

  /**
   * Maximum number of chars for each {@link ContentBasedDirective}.
   */
  private static final int CONTENT_CUT = 75;

  /**
   * List of {@link ContentBasedDirective} managed by this dialog.
   */
  private List<ContentBasedDirective> items;

  /**
   * If true, printAll performs in ascending order, otherwise in descending order.
   */
  private boolean printOrderAsc = true;

  /**
   * If true, all items should be collected in one file.
   */
  private boolean collect = false;

  /**
   * List of current settings for each {@link ContentBasedDirective}.
   */
  private final List<ContentBasedDirectiveSettings> settings = new ArrayList<>();

  /**
   * The container of the control elements.
   */
  private XControlContainer container;

  /**
   * The dialog.
   */
  private XDialog2 dialog;

  /**
   * Listener of the print buttons.
   */
  private final MyActionListener[] printListener = new MyActionListener[4];

  /**
   * Listener of the copy count fields.
   */
  private final MySpinListener[] spinListener = new MySpinListener[4];

  /**
   * Create a new dialog.
   *
   * @param items
   *          List of Content Based Directives.
   */
  public ContentBasedDirectiveDialog(List<ContentBasedDirective> items)
  {
    this.items = items;

    for (int i = 0; i < items.size(); i++)
    {
      ContentBasedDirective punkt = items.get(i);
      boolean isDraft = (i + 1 == items.size());
      boolean isOriginal = (i == 0);
      settings.add(
          new ContentBasedDirectiveSettings(i + 1, (short) punkt.getNumberOfCopies(), isDraft, isOriginal));
    }

    createGUI();
  }

  public boolean getPrintOrderAsc()
  {
    return printOrderAsc;
  }

  public List<ContentBasedDirectiveSettings> getSettings()
  {
    return Collections.unmodifiableList(settings);
  }

  public boolean isCollect()
  {
    return collect;
  }

  /**
   * Initialize GUI.
   */
  private void createGUI()
  {
    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_CONTAINER_WINDOW_PROVIDER));
      XWindow window = provider.createContainerWindow(
          "vnd.sun.star.script:WollMux.slv_count?location=application", "", peer, null);
      container = UNO.XControlContainer(window);
      dialog = UNO.XDialog2(window);

      XScrollBar scrollBar = UnoRuntime.queryInterface(XScrollBar.class,
          container.getControl("ScrollBar"));
      scrollBar.setMaximum(items.size());
      AbstractAdjustmentListener scrollListener = event -> update(event.Value);
      scrollBar.addAdjustmentListener(scrollListener);

      for (int i = 1; i <= 4; i++)
      {
        spinListener[i - 1] = new MySpinListener();
        UNO.XSpinField(container.getControl(SPINFIELD_NAME + i)).addSpinListener(spinListener[i - 1]);
        UNO.XTextComponent(container.getControl(SPINFIELD_NAME + i)).addTextListener(spinListener[i - 1]);
        printListener[i - 1] = new MyActionListener();
        UNO.XButton(container.getControl("Print" + i)).addActionListener(printListener[i - 1]);
      }

      XCheckBox printOrder = UNO.XCheckBox(container.getControl("printOrderCheckbox"));
      AbstractItemListener printOrderListener = event -> printOrderAsc = printOrder.getState() == 0;
      printOrder.addItemListener(printOrderListener);

      XButton abort = UNO.XButton(container.getControl("Abort"));
      AbstractActionListener abortListener = event -> dialog.endDialog(ExecutableDialogResults.CANCEL);
      abort.addActionListener(abortListener);

      XButton collectButton = UNO.XButton(container.getControl("collectAll"));
      AbstractActionListener collectAllListener = event -> {
        collect = true;
        dialog.endDialog(ExecutableDialogResults.OK);
      };
      collectButton.addActionListener(collectAllListener);

      XButton print = UNO.XButton(container.getControl("PrintAll"));
      AbstractActionListener printAllListener = event -> {
        collect = false;
        dialog.endDialog(ExecutableDialogResults.OK);
      };
      print.addActionListener(printAllListener);

      update(0);
      updateSum();
    } catch (UnoHelperRuntimeException e)
    {
      LOGGER.error("SLV-Druck-Dialog konnte nicht angezeigt werden.", e);
    }
  }

  /**
   * Update control elements to new {@link ContentBasedDirective}.
   *
   * @param value
   *          The index of the first {@link ContentBasedDirective}.
   */
  private void update(int value)
  {
    for (int i = 1; i <= 4; i++)
    {
      int index = value - 1 + i;
      XNumericField countNum = UNO.XNumericField(container.getControl(SPINFIELD_NAME + i));
      XFixedText label = UNO.XFixedText(container.getControl("Label" + i));

      if (index < items.size())
      {
        ContentBasedDirective punkt = items.get(index);
        ContentBasedDirectiveSettings info = settings.get(index);
        label.setText(cutContent(punkt.getHeading()));
        countNum.setValue(info.getCopyCount());
        spinListener[i - 1].setInfo(info);
        printListener[i - 1].setInfo(info);
      } else
      {
        UNO.XWindow(label).setVisible(false);
        UNO.XWindow(countNum).setVisible(false);
        UNO.XWindow(container.getControl("Print" + i)).setVisible(false);
      }
    }
  }

  /**
   * Shortens a {@link String} to at most {@link #CONTENT_CUT} chars and adds "...".
   *
   * @param value
   *          The string to shorten.
   * @return The modified string.
   */
  private static String cutContent(String value)
  {
    if (value.length() > CONTENT_CUT)
      return value.substring(0, CONTENT_CUT) + " ...";
    else
      return value;
  }

  /**
   * Sum up the copy-counts of all {@link ContentBasedDirectiveSettings}. And update the dialog
   * field.
   */
  private void updateSum()
  {
    int sum = 0;
    for (ContentBasedDirectiveSettings info : settings)
    {
      sum += info.getCopyCount();
    }

    UNO.XFixedText(container.getControl("Sum")).setText("" + sum);
  }

  /**
   * Show the dialog.
   *
   * @return The result of the dialog.
   * @see XDialog#execute()
   */
  public short execute()
  {
    return dialog.execute();
  }

  /**
   * Listener for setting new copy-count-values of single {@link ContentBasedDirectiveSettings}.
   */
  private class MySpinListener extends AbstractSpinListener implements AbstractTextListener
  {
    /**
     * The {@link ContentBasedDirectiveSettings} to modify.
     */
    private ContentBasedDirectiveSettings info = null;

    /**
     * Tell this listener to use some other {@link ContentBasedDirectiveSettings}.
     *
     * @param info
     *          The new settings.
     */
    public void setInfo(ContentBasedDirectiveSettings info)
    {
      this.info = info;
    }

    @Override
    public void up(SpinEvent event)
    {
      updateCount((short) UNO.XNumericField(event.Source).getValue());
    }

    @Override
    public void down(SpinEvent event)
    {
      updateCount((short) UNO.XNumericField(event.Source).getValue());
    }

    /**
     * Update number of copies and sum.
     *
     * @param copyCount
     *          New number of copies.
     */
    private void updateCount(short copyCount)
    {
      info.setCopyCount(copyCount);
      updateSum();
    }

    @Override
    public void textChanged(TextEvent event)
    {
      try
      {
        short count = Short.parseShort(UNO.XTextComponent(event.Source).getText());
        updateCount(count);
      } catch (NumberFormatException e)
      {
        UNO.XTextComponent(event.Source).setText("0");
        Selection selection = UNO.XTextComponent(event.Source).getSelection();
        selection.Max = 1;
        UNO.XTextComponent(event.Source).setSelection(selection);
        LOGGER.error("Keine Zahl", e);
      }
    }
  }

  /**
   * Listener for print-buttons of single {@link ContentBasedDirectiveSettings}.
   */
  private class MyActionListener implements AbstractActionListener
  {
    /**
     * {@link ContentBasedDirectiveSettings} to print.
     */
    private ContentBasedDirectiveSettings info = null;

    /**
     * Tell this listener to use some other {@link ContentBasedDirectiveSettings}.
     *
     * @param info
     *          The new settings.
     */
    public void setInfo(ContentBasedDirectiveSettings info)
    {
      this.info = info;
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      // Remove all content based directive, except the one of this print button.
      settings.removeIf(s -> !s.equals(info));
      dialog.endDialog(ExecutableDialogResults.OK);
    }
  }
}
