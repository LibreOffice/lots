/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.mailmerge.printsettings;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.Exception;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractActionListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractXWizardPage;
import org.libreoffice.lots.dispatch.AbstractDispatchResultListener;
import org.libreoffice.lots.document.TextDocumentModel;

/**
 * A page of the mail merge wizard. Settings of the printer can be done here.
 */
public class PrintWizardPage extends AbstractXWizardPage
{

  private final XTextComponent name;
  private final XButton change;

  /**
   * Creates this page.
   *
   * @param parentWindow
   *          The containing window.
   * @param pageId
   *          The id of this page.
   * @param model
   *          The model of the document.
   * @throws Exception
   *           If the page can't be created.
   */
  public PrintWizardPage(XWindow parentWindow, short pageId, TextDocumentModel model)
      throws Exception
  {
    super(pageId, parentWindow, "vnd.sun.star.script:WollMux.seriendruck_printer?location=application");
    XControlContainer container = UNO.XControlContainer(window);
    name = UNO.XTextComponent(container.getControl("name"));
    name.setText(model.getCurrentPrinterName());
    change = UNO.XButton(container.getControl("change"));
    AbstractDispatchResultListener listener = event -> name.setText(model.getCurrentPrinterName());
    AbstractActionListener changeListener = event -> model.configurePrinter(listener);
    change.addActionListener(changeListener);
  }

  @Override
  public boolean canAdvance()
  {
    return !name.getText().isEmpty();
  }

  @Override
  public boolean commitPage(short reason)
  {
    window.setVisible(false);
    return true;
  }
}
