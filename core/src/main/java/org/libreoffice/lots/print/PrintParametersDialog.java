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
package org.libreoffice.lots.print;

import org.apache.commons.lang3.tuple.Pair;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog2;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XNumericField;
import com.sun.star.awt.XRadioButton;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.frame.DispatchResultEvent;
import com.sun.star.frame.XDispatchResultListener;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.UnoRuntime;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractActionListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractItemListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractTextListener;
import org.libreoffice.ext.unohelper.util.UnoComponent;
import org.libreoffice.lots.document.TextDocumentModel;
import org.libreoffice.lots.print.PageRange.PageRangeType;

/**
 * Dialog for configuring a print.
 */
public class PrintParametersDialog
{

  /**
   * The command indicating that the dialog has been closed by pressing "print".
   */
  public static final int CMD_SUBMIT = 1;

  /**
   * The command indicating that the dialog has been canceled by pressing "X" or "abort".
   */
  public static final int CMD_CANCEL = 0;

  private PrintParametersDialog()
  {
    // nothing to do
  }

  /**
   * Initialize and show the dialog.
   *
   * @param doc
   *          The document to print.
   * @param showCopyCount
   *          If true number of prints can be selected, otherwise not.
   * @return The copy count and page information.
   */
  public static Pair<Short, PageRange> show(TextDocumentModel doc, boolean showCopyCount)
  {
    XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
    XContainerWindowProvider provider = UNO.XContainerWindowProvider(
        UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_CONTAINER_WINDOW_PROVIDER));
    XWindow window = provider.createContainerWindow("vnd.sun.star.script:WollMux.print_parameter?location=application",
        "", peer, null);
    XControlContainer container = UNO.XControlContainer(window);
    XDialog2 dialog = UnoRuntime.queryInterface(XDialog2.class, window);

    PageRange pageRange = new PageRange(PageRangeType.ALL, null);

    XNumericField count = UNO.XNumericField(container.getControl("count"));
    UNO.XWindow(count).setVisible(showCopyCount);
    UNO.XWindow(container.getControl("countFrame")).setVisible(showCopyCount);
    UNO.XWindow(container.getControl("countLabel")).setVisible(showCopyCount);

    XFixedText printer = UNO.XFixedText(container.getControl("printer"));
    printer.setText(doc.getCurrentPrinterName());
    XButton changePrinter = UNO.XButton(container.getControl("change"));
    XDispatchResultListener dispatchListner = new XDispatchResultListener()
    {

      @Override
      public void dispatchFinished(DispatchResultEvent arg0)
      {
        UNO.XFixedText(container.getControl("printer")).setText(doc.getCurrentPrinterName());
      }

      @Override
      public void disposing(EventObject arg0)
      {
        // nothing to do
      }
    };
    AbstractActionListener changePrinterListener = event -> doc.configurePrinter(dispatchListner);
    changePrinter.addActionListener(changePrinterListener);

    XRadioButton all = UNO.XRadio(container.getControl("all"));
    AbstractItemListener allListener = event -> pageRange.setPageRangeType(PageRangeType.ALL);
    all.addItemListener(allListener);
    XRadioButton pages = UNO.XRadio(container.getControl("pages"));
    AbstractItemListener pagesListener = event -> pageRange.setPageRangeType(PageRangeType.USER_DEFINED);
    pages.addItemListener(pagesListener);
    XTextComponent pageText = UNO.XTextComponent(container.getControl("pageText"));
    AbstractTextListener additionalTextFieldListener = event -> pageRange.setPageRangeValue(pageText.getText());
    pageText.addTextListener(additionalTextFieldListener);
    XRadioButton current = UNO.XRadio(container.getControl("current"));
    AbstractItemListener currentListener = event -> pageRange.setPageRangeType(PageRangeType.CURRENT_PAGE);
    current.addItemListener(currentListener);
    XRadioButton tillEnd = UNO.XRadio(container.getControl("tillEnd"));
    AbstractItemListener tillEndListener = event -> pageRange.setPageRangeType(PageRangeType.CURRENT_AND_FOLLOWING);
    tillEnd.addItemListener(tillEndListener);

    XButton abort = UNO.XButton(container.getControl("abort"));
    AbstractActionListener abortListener = event -> dialog.endDialog(CMD_CANCEL);
    abort.addActionListener(abortListener);

    XButton print = UNO.XButton(container.getControl("print"));
    AbstractActionListener printListener = event -> dialog.endDialog(CMD_SUBMIT);
    print.addActionListener(printListener);

    short res = dialog.execute();
    if (res == CMD_SUBMIT)
    {
      return Pair.of((short) count.getValue(), pageRange);
    } else
    {
      return null;
    }
  }
}
