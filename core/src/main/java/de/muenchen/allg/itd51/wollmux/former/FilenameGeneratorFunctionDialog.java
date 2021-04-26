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
package de.muenchen.allg.itd51.wollmux.former;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.former.model.ID;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.util.L;

public class FilenameGeneratorFunctionDialog extends JDialog
{
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FilenameGeneratorFunctionDialog.class);


  private transient TextDocumentController documentController;
  private transient AdjustorFunction func;
  private String functionName;
  private transient IDManager idManager;

  /**
   * A new dialog for editing the filename generator function.
   *
   * @param owner
   *          The owning frame.
   * @param modal
   *          If true the dialog is modal.
   * @param documentController
   *          The controller of the document.
   * @param idManager
   *          The id manager.
   */
  public FilenameGeneratorFunctionDialog(Frame owner, boolean modal, TextDocumentController documentController,
      IDManager idManager)
  {
    super(owner, modal);

    this.documentController = documentController;
    this.idManager = idManager;
    func = parseAdjustorFunction(documentController.getFilenameGeneratorFunc());
    if (func != null)
    {
      functionName = func.functionName;
    }
    
    createGui();
    
    setTitle(L.m("Dateiname vorgeben"));
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    pack();

    int frameWidth = getWidth();
    int frameHeight = getHeight();
    if (frameHeight < 200) {
      frameHeight = 200;
    }

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    setBounds(x, y, frameWidth, frameHeight);
  }

  private void createGui()
  {
    Box vbox = Box.createVerticalBox();
    vbox.setBorder(new EmptyBorder(8, 5, 10, 5));

    JTextField tf = new JTextField();
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(tf);
    final TextComponentTags tt = new TextComponentTags(tf);
    if (func != null)
    {
      tt.setContent(TextComponentTags.CAT_VALUE_SYNTAX, func.cat);
    }
    
    Collection<ID> idsCol = idManager.getAllIDs(FormularMax4kController.NAMESPACE_FORMCONTROLMODEL);
    List<String> ids = new ArrayList<>();
    for (ID id : idsCol)
      ids.add(id.getID());
    JPotentiallyOverlongPopupMenuButton insertFieldButton =
      new JPotentiallyOverlongPopupMenuButton(L.m("ID"),
        TextComponentTags.makeInsertFieldActions(ids, tt));
    insertFieldButton.setFocusable(false);
    Box hbox = Box.createHorizontalBox();
    hbox.add(new JLabel(L.m("Dateiname"), JLabel.LEFT));
    hbox.add(Box.createHorizontalGlue());
    hbox.add(insertFieldButton);
    vbox.add(hbox);
    vbox.add(tf);

    final List<String> adjustFuncs = new ArrayList<>();
    adjustFuncs.add(L.m("-- keine --"));
    int sel = 0;
    for (String fName : documentController.getFunctionLibrary().getFunctionNames())
    {
      Function f = documentController.getFunctionLibrary().get(fName);
      if (f != null && f.parameters().length == 1
        && f.parameters()[0].equals("Filename"))
      {
        if (functionName != null && functionName.equals(fName))
          sel = adjustFuncs.size();
        adjustFuncs.add(fName);
      }
    }
    vbox.add(Box.createVerticalStrut(5));
    hbox = Box.createHorizontalBox();
    hbox.add(new JLabel(L.m("Nachträgliche Anpassung")));
    hbox.add(Box.createHorizontalGlue());
    vbox.add(hbox);
    final JComboBox<String> adjustFuncCombo = new JComboBox<>(new Vector<>(adjustFuncs));
    if (sel > 0)
    {
      adjustFuncCombo.setSelectedIndex(sel);
    }
    else if (functionName != null)
    {
      adjustFuncCombo.addItem(functionName);
      adjustFuncCombo.addItemListener(e -> {
        if (adjustFuncCombo.getSelectedIndex() == adjustFuncs.size())
        {
          adjustFuncCombo.setBackground(Color.red);
          adjustFuncCombo.setToolTipText(L.m("Achtung: Funktion nicht definiert!"));
        } else
        {
          adjustFuncCombo.setBackground(null);
          adjustFuncCombo.setToolTipText(null);
        }

      });
      adjustFuncCombo.setSelectedIndex(adjustFuncs.size());
    }
    DimAdjust.maxHeightIsPrefMaxWidthUnlimited(adjustFuncCombo);
    vbox.add(adjustFuncCombo);

    JButton cancel = new JButton(L.m("Abbrechen"));
    cancel.addActionListener(e -> dispose());

    ActionListener submitActionListener = e -> {
      try
      {
        ConfigThingy functionConf = createFilenameGeneratorFunctionConf(tt, adjustFuncCombo);
        documentController.setFilenameGeneratorFunc(functionConf);
      } catch (Exception e1)
      {
        LOGGER.error("", e1);
      }
      dispose();
    };

    JButton ok = new JButton(L.m("OK"));
    ok.addActionListener(submitActionListener);
    tf.addActionListener(submitActionListener);

    Box buttons = Box.createHorizontalBox();
    buttons.add(cancel);
    buttons.add(Box.createHorizontalGlue());
    buttons.add(ok);
    vbox.add(Box.createVerticalGlue());
    vbox.add(buttons, BorderLayout.SOUTH);
    add(vbox);    
  }
  
  private static class AdjustorFunction
  {
    final ConfigThingy cat;

    final String functionName;

    AdjustorFunction(ConfigThingy cat, String functionName)
    {
      this.cat = cat;
      this.functionName = functionName;
    }
  }

  private static AdjustorFunction parseAdjustorFunction(ConfigThingy func)
  {
    if (func == null) return null;
    if (func.getName().equals("CAT") && isCatFuncOk(func))
      return new AdjustorFunction(func, null);
    if (!func.getName().equals("BIND") || func.count() != 2) return null;
    Iterator<ConfigThingy> bindIter = func.iterator();
    ConfigThingy n = bindIter.next();
    if (n == null || !n.getName().equals("FUNCTION") || n.count() != 1) return null;
    String bindFunctionName = n.iterator().next().getName();
    n = bindIter.next();
    if (n == null || !n.getName().equals("SET") || n.count() != 2) return null;
    Iterator<ConfigThingy> setIter = n.iterator();
    n = setIter.next();
    if (n == null || !n.getName().equals("Filename") || n.count() != 0) return null;
    n = setIter.next();
    if (n == null || !n.getName().equals("CAT") || !isCatFuncOk(n)) return null;
    return new AdjustorFunction(n, bindFunctionName);
  }

  private static boolean isCatFuncOk(ConfigThingy catFunc)
  {
    for (ConfigThingy c : catFunc)
    {
      boolean invalid = true;
      if (c.count() == 0) invalid = false;
      if (c.getName().equals("VALUE") && c.count() == 1) invalid = false;
      if (invalid) return false;
    }
    return true;
  }
  
  private static ConfigThingy createFilenameGeneratorFunctionConf(
      TextComponentTags tt, JComboBox<String> adjustFuncCombo)
  {
    if (tt.getJTextComponent().getText().trim().length() == 0) return null;
    ConfigThingy catFunc = tt.getContent(TextComponentTags.CAT_VALUE_SYNTAX);
    ConfigThingy bindFunc = null;
    if (adjustFuncCombo.getSelectedIndex() != 0)
    {
      bindFunc = new ConfigThingy("BIND");
      ConfigThingy function = new ConfigThingy("FUNCTION");
      function.add(adjustFuncCombo.getSelectedItem().toString());
      bindFunc.addChild(function);
      ConfigThingy set = new ConfigThingy("SET");
      set.add("Filename");
      set.addChild(catFunc);
      bindFunc.addChild(set);
      return bindFunc;
    }
    else
      return catFunc;
  }
}
