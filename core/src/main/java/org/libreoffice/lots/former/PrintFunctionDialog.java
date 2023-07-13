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
package org.libreoffice.lots.former;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.libreoffice.lots.document.TextDocumentController;
import org.libreoffice.lots.print.PrintFunctionLibrary;
import org.libreoffice.lots.util.L;

public class PrintFunctionDialog extends JDialog
{
  private static final long serialVersionUID = 1L;

  private transient TextDocumentController documentController;
  transient PrintFunctionLibrary printFunctionLibrary;

  /**
   * A new dialog for editing the print function.
   *
   * @param owner
   *          The owning frame.
   * @param modal
   *          If true the dialog is modal.
   * @param documentController
   *          The controller of the document.
   * @param printFuncLib
   *          The print function library.
   */
  public PrintFunctionDialog(Frame owner, boolean modal, TextDocumentController documentController,
      PrintFunctionLibrary printFuncLib)
  {
    super(owner, modal);

    this.documentController = documentController;
    this.printFunctionLibrary = printFuncLib;

    setTitle(L.m("Print functions"));

    createGui();

    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    pack();
    int frameWidth = getWidth();
    int frameHeight = getHeight();
    if (frameHeight < 200)
    {
      frameHeight = 200;
    }

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    setBounds(x, y, frameWidth, frameHeight);
  }

  private void createGui()
  {
    final JList<String> printFunctionCurrentList =
        new JList<>(new Vector<>(documentController.getModel().getPrintFunctions()));
      JPanel printFunctionEditorContentPanel = new JPanel(new BorderLayout());
      printFunctionEditorContentPanel.add(printFunctionCurrentList,
        BorderLayout.CENTER);

      final JComboBox<String> printFunctionComboBox = new JComboBox<>(
          printFunctionLibrary.getFunctionNames().toArray(new String[] {}));
      printFunctionComboBox.setEditable(true);

      printFunctionEditorContentPanel.add(printFunctionComboBox, BorderLayout.NORTH);

      ActionListener removeFunc = e -> {
        for (Object o : printFunctionCurrentList.getSelectedValuesList())
        {
          documentController.removePrintFunction("" + o);
        }
        printFunctionCurrentList.setListData(new Vector<>(documentController.getModel().getPrintFunctions()));
      };

      ActionListener addFunc = e -> {
        String newFunctionName = printFunctionComboBox.getSelectedItem().toString();
        documentController.addPrintFunction(newFunctionName);
        printFunctionCurrentList.setListData(new Vector<>(documentController.getModel().getPrintFunctions()));
      };

      JButton wegDamit = new JButton(L.m("Remove"));
      wegDamit.addActionListener(removeFunc);

      JButton machDazu = new JButton(L.m("Add"));
      machDazu.addActionListener(addFunc);

      JButton ok = new JButton(L.m("OK"));
      ok.addActionListener(e -> dispose());

      Box buttons = Box.createHorizontalBox();
      buttons.add(wegDamit);
      buttons.add(Box.createHorizontalGlue());
      buttons.add(machDazu);
      buttons.add(Box.createHorizontalGlue());
      buttons.add(ok);
      printFunctionEditorContentPanel.add(buttons, BorderLayout.SOUTH);

      add(printFunctionEditorContentPanel);
  }
}
