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

import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.func.PrintFunctionLibrary;

public class PrintFunctionDialog extends JDialog
{
  private static final long serialVersionUID = 1L;
  
  private TextDocumentModel doc;
  PrintFunctionLibrary printFunctionLibrary;

  public PrintFunctionDialog(Frame owner, boolean modal, TextDocumentModel doc, PrintFunctionLibrary printFuncLib)
  {
    super(owner, modal);
    
    this.doc = doc;
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
        new JList<String>(new Vector<String>(doc.getPrintFunctions()));
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
            doc.removePrintFunction("" + o);
          printFunctionCurrentList.setListData(new Vector<String>(
            doc.getPrintFunctions()));
        }
      };

      ActionListener addFunc = new ActionListener()
      {
        @Override
        public void actionPerformed(ActionEvent e)
        {
          String newFunctionName = printFunctionComboBox.getSelectedItem().toString();
          doc.addPrintFunction(newFunctionName);
          printFunctionCurrentList.setListData(new Vector<String>(
            doc.getPrintFunctions()));
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
