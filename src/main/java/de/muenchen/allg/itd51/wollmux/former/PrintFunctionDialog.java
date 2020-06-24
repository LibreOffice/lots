/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.print.PrintFunctionLibrary;

public class PrintFunctionDialog extends JDialog
{
  private static final long serialVersionUID = 1L;
  
  private TextDocumentController documentController;
  PrintFunctionLibrary printFunctionLibrary;

  public PrintFunctionDialog(Frame owner, boolean modal, TextDocumentController documentController, PrintFunctionLibrary printFuncLib)
  {
    super(owner, modal);
    
    this.documentController = documentController;
    this.printFunctionLibrary = printFuncLib;
    
    setTitle(L.m("Druckfunktion setzen"));
    
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
        new JList<String>(new Vector<String>(documentController.getModel().getPrintFunctions()));
      JPanel printFunctionEditorContentPanel = new JPanel(new BorderLayout());
      printFunctionEditorContentPanel.add(printFunctionCurrentList,
        BorderLayout.CENTER);

      final JComboBox<String> printFunctionComboBox = new JComboBox<String>(printFunctionLibrary.getFunctionNames().toArray(new String[]{}));
      printFunctionComboBox.setEditable(true);

      printFunctionEditorContentPanel.add(printFunctionComboBox, BorderLayout.NORTH);

      ActionListener removeFunc = new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          for (Object o : printFunctionCurrentList.getSelectedValuesList())
            documentController.removePrintFunction("" + o);
          printFunctionCurrentList.setListData(new Vector<String>(
            documentController.getModel().getPrintFunctions()));
        }
      };

      ActionListener addFunc = new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          String newFunctionName = printFunctionComboBox.getSelectedItem().toString();
          documentController.addPrintFunction(newFunctionName);
          printFunctionCurrentList.setListData(new Vector<String>(
            documentController.getModel().getPrintFunctions()));
        }
      };

      JButton wegDamit = new JButton(L.m("Entfernen"));
      wegDamit.addActionListener(removeFunc);

      JButton machDazu = new JButton(L.m("Hinzufügen"));
      machDazu.addActionListener(addFunc);

      JButton ok = new JButton(L.m("OK"));
      ok.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          dispose();
        }
      });

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
