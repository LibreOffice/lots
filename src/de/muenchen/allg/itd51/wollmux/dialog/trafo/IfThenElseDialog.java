/*
 * Dateiname: IfThenElseDialog.java
 * Projekt  : WollMux
 * Funktion : Erlaubt die Bearbeitung der Funktion eines Wenn-Dann-Sonst-Feldes.
 * 
 * Copyright (c) 2008-2019 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 01.02.2008 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.MouseEvent;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XExtendedToolkit;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tree.ExpandVetoException;
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.awt.tree.XMutableTreeNode;
import com.sun.star.awt.tree.XTreeControl;
import com.sun.star.awt.tree.XTreeNode;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.deployment.PackageInformationProvider;
import com.sun.star.deployment.XPackageInformationProvider;
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractKeyHandler;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractMouseListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractSelectionChangeListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Erlaubt die Bearbeitung der Funktion eines Wenn-Dann-Sonst-Feldes.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class IfThenElseDialog
{
  private static final Logger LOGGER = LoggerFactory.getLogger(IfThenElseDialog.class);

  private XControlContainer controlContainer;

  private XDialog dialog;

  private XWindow window;

  private XComboBox cbWennComperator;

  private XComboBox cbWennNot;

  private XTextComponent txtValueWenn;

  private XTextComponent txtValueDann;

  private XTextComponent txtValueSonst;

  private XComboBox cbWennSerienbrieffeld;

  private XComboBox cbDannSerienbrieffeld;

  private XComboBox cbSonstSerienbrieffeld;

  private XMutableTreeNode selectedNode;
  
  private XMutableTreeNode rootNode;
  
  private XControlModel xControlModel;

  private XTreeControl treeControl;
  
  private List<String> randomImages = new ArrayList<>();
  
  private XMutableTreeDataModel treeNodeModel;
  
  private TextDocumentController documentController;

  private static final String WENN = "WENN";
  
  private static final String DANN = "DANN";
  
  private static final String SONST = "SONST";

  private static final String EXTENSION_ID = "de.muenchen.allg.d101.wollmux";

  /**
   * Das Objekt, das den Startinhalt des Dialogs spezifiziert (und am Ende verwendet wird, um den
   * Rückgabewert zu speichern).
   */
  private TrafoDialogParameters params;

  public IfThenElseDialog(TrafoDialogParameters params, TextDocumentController documentController)
  {
    this.documentController = documentController;
    this.params = params;
    if (!params.isValid || params.fieldNames == null
        || params.fieldNames.size() == 0)
      throw new IllegalArgumentException();

    params.isValid = false; // erst bei Beendigung mit Okay werden sie wieder valid
    
    addNodeImages();
    
    buildGUI(params);
  }
  
  private void addNodeImages()
  {
    XPackageInformationProvider xPackageInformationProvider = PackageInformationProvider
        .get(UNO.defaultContext);
    String imgLocation = xPackageInformationProvider.getPackageLocation(EXTENSION_ID) + "/image/";

    randomImages.add(imgLocation + "if.png");
    randomImages.add(imgLocation + "else.png");
    randomImages.add(imgLocation + "then.png");
    randomImages.add(imgLocation + "brown.png");
    randomImages.add(imgLocation + "grey.png");
    randomImages.add(imgLocation + "ocker.png");
    randomImages.add(imgLocation + "orange.png");
    randomImages.add(imgLocation + "red.png");
    randomImages.add(imgLocation + "navy_blue.png");
    randomImages.add(imgLocation + "light_blue.png");
    randomImages.add(imgLocation + "lila.png");
    randomImages.add(imgLocation + "dark_green.png");
  }
  
  private void buildGUI(TrafoDialogParameters params)
  {

    XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
    XContainerWindowProvider provider = null;

    try
    {
      provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));
    } catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }

    window = provider.createContainerWindow(
        "vnd.sun.star.script:WollMux.if_then_else2?location=application", "", peer, null);

    try
    {
      Object toolkit = UNO.xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit",
          UNO.defaultContext);
      XToolkit xToolkit = UNO.XToolkit(toolkit);
      XExtendedToolkit extToolkit = UnoRuntime.queryInterface(XExtendedToolkit.class, xToolkit);
      extToolkit.addKeyHandler(keyHandler);
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }

    controlContainer = UnoRuntime.queryInterface(XControlContainer.class, window);
    
    window.setPosSize(30, 30, 600, 600, PosSize.SIZE);

    cbWennSerienbrieffeld = UNO
        .XComboBox(controlContainer.getControl("cbWennSerienbrieffeld"));
    cbWennSerienbrieffeld.addItemListener(cbWennSerienbrieffeldItemListener);
    cbDannSerienbrieffeld = UNO
        .XComboBox(controlContainer.getControl("cbDannSerienbrieffeld"));
    cbSonstSerienbrieffeld = UNO
        .XComboBox(controlContainer.getControl("cbSonstSerienbrieffeld"));
    UNO.XTextComponent(cbWennSerienbrieffeld).setText("Serienbrieffeld");
    UNO.XTextComponent(cbDannSerienbrieffeld).setText("Serienbrieffeld");
    UNO.XTextComponent(cbSonstSerienbrieffeld).setText("Serienbrieffeld");

    params.fieldNames.forEach(fieldName -> {
      cbWennSerienbrieffeld.addItem(fieldName, (short) (cbWennSerienbrieffeld.getItemCount() + 1));
      cbDannSerienbrieffeld.addItem(fieldName, (short) (cbWennSerienbrieffeld.getItemCount() + 1));
      cbSonstSerienbrieffeld.addItem(fieldName, (short) (cbWennSerienbrieffeld.getItemCount() + 1));
    });

    cbWennComperator = UNO.XComboBox(controlContainer.getControl("cbWennComporator"));
    testTypes.forEach(item -> cbWennComperator.addItem(item.label,
        (short) (cbWennComperator.getItemCount() + 1)));
    cbWennComperator.addItemListener(comparatorItemListener);

    cbWennNot = UNO.XComboBox(controlContainer.getControl("cbNot"));
    cbWennNot.addItem("", (short) 0);
    cbWennNot.addItem("NOT", (short) 1);
    cbWennNot.addItemListener(notItemListener);

    XButton wennButton = UNO.XButton(controlContainer.getControl("tbnWenn"));
    wennButton.addActionListener(wennActionListener);

    XButton dannButton = UNO.XButton(controlContainer.getControl("btnDann"));
    dannButton.addActionListener(dannActionListener);

    XButton sonstButton = UNO.XButton(controlContainer.getControl("btnSonst"));
    sonstButton.addActionListener(sonstActionListener);

    txtValueWenn = UNO.XTextComponent(controlContainer.getControl("txtValueWenn"));
    txtValueWenn.addTextListener(txtValueWennListener);
    
    txtValueDann = UNO.XTextComponent(controlContainer.getControl("txtValueDann"));
    txtValueDann.addTextListener(txtValueDannListener);
    
    txtValueSonst = UNO.XTextComponent(controlContainer.getControl("txtValueSonst"));
    txtValueSonst.addTextListener(txtValueSonstListener);

    try
    {
      treeControl = UnoRuntime.queryInterface(XTreeControl.class,
          controlContainer.getControl("resultTreeControl"));
      XControl treeCtrl = UNO.XControl(treeControl);
      XPropertySet props = UNO.XPropertySet(treeCtrl.getModel());

      props.setPropertyValue("HideInactiveSelection", Boolean.TRUE);
      props.setPropertyValue("RootDisplayed", Boolean.TRUE);
      props.setPropertyValue("RowHeight", 40); // SelectionType?

      XControl xTreeControl = UNO.XControl(treeControl);
      XWindow wndTreeControl = UNO.XWindow(xTreeControl);
      wndTreeControl.addMouseListener(mouseListener);
      xControlModel = xTreeControl.getModel();

      treeControl.addSelectionChangeListener(treeControlSelectionChangeListener);
      
      treeNodeModel = UnoRuntime.queryInterface(XMutableTreeDataModel.class,
      UNO.xMSF.createInstance("com.sun.star.awt.tree.MutableTreeDataModel"));

    } catch (Exception e)
    {
      LOGGER.error("", e);
    }

    dialog = UnoRuntime.queryInterface(XDialog.class, window);
    short result = dialog.execute();

    if (result != ExecutableDialogResults.OK)
    {
      ConfigThingy resultConf = buildRec(rootNode, new ConfigThingy("Func"));
      updateTrafoConf(resultConf);
      dialog.endExecute();
    } 
  }
  
  private AbstractMouseListener mouseListener = new AbstractMouseListener()
  {
    @Override
    public void mousePressed(MouseEvent arg0)
    {
      XMutableTreeNode node = UnoRuntime.queryInterface(XMutableTreeNode.class,
          treeControl.getClosestNodeForLocation(arg0.X, arg0.Y));

      if (node != null)
      {
        treeControl.clearSelection();
        treeControl.addSelection(node);
      }
    }
  };
  
  private AbstractSelectionChangeListener treeControlSelectionChangeListener = new AbstractSelectionChangeListener()
  {
    @Override
    public void selectionChanged(EventObject arg0)
    {
      selectedNode = UnoRuntime.queryInterface(XMutableTreeNode.class,
              treeControl.getSelection());
      
      String[] data = nodeDataValueToStringArray(selectedNode);

      String identifier = data[0];

      switch (identifier)
      {
        case WENN:
          // data[1] = id, [2] = serienbrieffeld, [3] = not, [4] = comp, [5] = textfieldvalue
          UNO.XTextComponent(cbWennSerienbrieffeld).setText(data[2]);
          UNO.XTextComponent(cbWennNot).setText(data[3]);
          UNO.XTextComponent(cbWennComperator).setText(data[4]);
          txtValueWenn.setText(data[5]);
          break;
         
        case DANN:
          // dest[1] = id, [2] = txtfieldvalue
          txtValueDann.setText(data[2]);
          break;
          
        case SONST:
          // dest[1] = id, [2] = txtfieldvalue
          txtValueSonst.setText(data[2]);
          break;

      default:
        break;
      }
    }
  };
  
  private String[] nodeDataValueToStringArray(XMutableTreeNode node) {
    Object[] data = (Object[]) node.getDataValue();
    String[] dest = new String[data.length];
    System.arraycopy(data, 0, dest, 0, data.length);
    
    return dest;
  }
  
  /**
   * 
   * @param currentNode
   * @param currentConfig
   * @return
   */
  private ConfigThingy buildRec(XTreeNode currentNode, ConfigThingy currentConfig) {
    
    if (currentNode.getChildCount() < 1)
      return new ConfigThingy("");

    for (int i = 0; i < currentNode.getChildCount(); i++) {
      XTreeNode currentChildNode = null;
      try
      {
        currentChildNode = currentNode.getChildAt(i);
      } catch (IndexOutOfBoundsException e)
      {
        LOGGER.error("", e);
      }
      
      if (currentChildNode == null)
        continue;
      
      String[] dataChildNode = nodeDataValueToStringArray(
          UnoRuntime.queryInterface(XMutableTreeNode.class, currentChildNode));
      
      String conditionType = dataChildNode[0];
      
      if (WENN.equals(conditionType)) {
        currentConfig = addIf(currentConfig);
        
        if (!UNO.XTextComponent(cbWennNot).getText().isEmpty())
          currentConfig = addNot(currentConfig);

        addSTRCMPBlock(currentConfig, dataChildNode[4], dataChildNode[2], dataChildNode[5]);
        if (currentChildNode.getChildCount() > 0)
          buildRec(currentChildNode, currentConfig);

      } else if (DANN.equals(conditionType)) {        
        ConfigThingy thenConfig = addThen(currentConfig);
        
        if (currentChildNode.getChildCount() > 0)
          buildRec(currentChildNode, thenConfig);
        else {
          ConfigThingy catConf = addCAT(thenConfig, dataChildNode[2]);
          thenConfig.addChild(catConf);
        }
      } else if (SONST.equals(conditionType)) {
        ConfigThingy elseConf = addElse(currentConfig);
        
        if (currentChildNode.getChildCount() > 0)
          buildRec(currentChildNode, elseConf);
        else {
          ConfigThingy catConf = addCAT(elseConf, dataChildNode[2]);
          elseConf.addChild(catConf);
        }
      }
    }

    return currentConfig;
  }

  private AbstractTextListener txtValueWennListener = event -> {    
    // DisplayValue aus DataValue generieren, Änderungen speichern.
    String[] data = nodeDataValueToStringArray(selectedNode);
    
    //data[0] = condition, data[1] = id, [2] = serienbrieffeld, [3] = not, [4] = comp, [5] = textfieldvalue
    if (WENN.equals(data[0])) {
      data[5] = txtValueWenn.getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(
          data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    }
  };
  
  private AbstractTextListener txtValueDannListener = event -> {    
    // DisplayValue aus DataValue generieren, Änderungen speichern.
    String[] data = nodeDataValueToStringArray(selectedNode);
    
    //data[0] = condition, data[1] = id, [2] = value
    if (DANN.equals(data[0])) {
      data[2] = txtValueDann.getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(
          data[0] + " " + data[2]);
    }
  };
  
  private AbstractTextListener txtValueSonstListener = event -> {    
    // DisplayValue aus DataValue generieren, Änderungen speichern.
    String[] data = nodeDataValueToStringArray(selectedNode);
    
    //data[0] = condition, data[1] = id, [2] = value
    if (SONST.equals(data[0])) {
      data[2] = txtValueSonst.getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(
          data[0] + " " + data[2]);
    }
  };

  private AbstractActionListener sonstActionListener = event -> 
    selectedNode.appendChild(createElseNode(txtValueSonst.getText(), ""));

  private XMutableTreeNode createElseNode(String displayValue, String imgFileName)
  {
    String id = UUID.randomUUID().toString();
    
    XMutableTreeNode elseNode = treeNodeModel.createNode(UUID.randomUUID().toString(), false);
    elseNode.setDisplayValue(SONST + " " + displayValue);
    
    List<String> data = new ArrayList<>();
    data.add(SONST);
    data.add(id);
    data.add(displayValue);
    
    elseNode.setDataValue(data.toArray());
    elseNode
        .setNodeGraphicURL(imgFileName);

    return elseNode;
  }

  private XMutableTreeNode createThenNode(String displayValue, String imgFileName)
  {
    String id = UUID.randomUUID().toString();
    
    XMutableTreeNode thenNode = treeNodeModel.createNode(UUID.randomUUID().toString(), false);
    thenNode.setDisplayValue(DANN + " " + displayValue);
    
    List<String> data = new ArrayList<>();
    data.add(DANN);
    data.add(id);
    data.add(displayValue);
    
    thenNode.setDataValue(data.toArray());
    thenNode
        .setNodeGraphicURL(imgFileName);

    return thenNode;
  }

  private XMutableTreeNode createIfNode(String serienbriefFeld, String not, String comparatorValue,
      String value, String imgFileName)
  {
    String id = UUID.randomUUID().toString();
    XMutableTreeNode ifNode = treeNodeModel.createNode(id, false);
    ifNode.setDisplayValue(
        WENN + " " + serienbriefFeld + " " + not + " " + comparatorValue + " " + value);

    List<String> data = new ArrayList<>();
    data.add(WENN);
    data.add(id);
    data.add(serienbriefFeld);
    data.add(not);
    data.add(comparatorValue);
    data.add(value);
    
    ifNode.setDataValue(data.toArray());
    ifNode.setNodeGraphicURL(imgFileName);

    return ifNode;
  }

  private XMutableTreeNode createRootNode()
  {
    rootNode = treeNodeModel.createNode(UUID.randomUUID().toString(), false);
    rootNode.setDisplayValue("Neue Bedingung");
    rootNode.setDataValue("ROOT");

    return rootNode;
  }

  private AbstractActionListener wennActionListener = event -> {
    String not = cbWennNot.getItem((short) 0);
    String value = txtValueWenn.getText();
    String serienBriefFeld = UNO.XTextComponent(cbWennSerienbrieffeld).getSelectedText();

    XTextComponent comparator = UNO.XTextComponent(controlContainer.getControl("cbWennComporator"));
    String comparatorValue = comparator.getSelectedText();

    Random rand = new Random();
    int randomNumber = rand.nextInt(randomImages.size());
    
    String randomImageFileName = randomImages.get(randomNumber);
    XMutableTreeNode ifNode = createIfNode(serienBriefFeld, not, comparatorValue, value,
        randomImageFileName);
    
    // duplikate vermeiden. nach Verwendung löschen, nicht endlich, daher wieder füllen.
    randomImages.remove(randomNumber);
    
    if (randomImages.isEmpty())
      addNodeImages();
   
    if (selectedNode == null)
    {
      try
      {
        XMutableTreeNode root = createRootNode();
        treeNodeModel.setRoot(root);
        selectedNode = root;
        selectedNode.appendChild(ifNode);
        selectedNode.appendChild(createThenNode("", randomImageFileName));
        selectedNode.appendChild(createElseNode("", randomImageFileName));

        XPropertySet xTreeModelProperty = UnoRuntime.queryInterface(
            XPropertySet.class, xControlModel);
        xTreeModelProperty.setPropertyValue("DataModel", treeNodeModel);
        
        treeControl.expandNode(root);
      } catch (UnknownPropertyException | PropertyVetoException 
          | WrappedTargetException | IllegalArgumentException | ExpandVetoException e)
      {
        LOGGER.error("", e);
      }
    } else
    {      
      selectedNode.appendChild(ifNode);
      selectedNode.appendChild(createThenNode("", randomImageFileName));
      selectedNode.appendChild(createElseNode("", randomImageFileName));
    }
  };

  private AbstractActionListener dannActionListener = event ->
    selectedNode.appendChild(createThenNode(txtValueDann.getText(), ""));

  private AbstractItemListener notItemListener = event -> {
    String[] data = nodeDataValueToStringArray(selectedNode);
    
    if (WENN.equals(data[0])) {
      data[3] = UNO.XTextComponent(cbWennNot).getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(
          data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    }
  };
  
  private AbstractItemListener cbWennSerienbrieffeldItemListener = event -> {
    String[] data = nodeDataValueToStringArray(selectedNode);
    
    if (WENN.equals(data[0])) {
      data[2] = UNO.XTextComponent(cbWennSerienbrieffeld).getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(
          data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    }
  };

  private AbstractItemListener comparatorItemListener = event -> {   
    String[] data = nodeDataValueToStringArray(selectedNode);
    
    if (WENN.equals(data[0])) {
      data[4] = UNO.XTextComponent(cbWennComperator).getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(
          data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    }
  };

  private void addSTRCMPBlock(ConfigThingy ifConf, String comparator, String value1,
      String value2)
  {
    ConfigThingy strCmpConf = ifConf.add("STRCMP");
    strCmpConf.add("VALUE").add(value1 == null || value1.isEmpty() ? "" : value1);
    strCmpConf.add(value2 == null || value2.isEmpty() ? "" : value2);
  }
  
  private ConfigThingy addNot(ConfigThingy rootNode)
  {
    return rootNode.add("NOT");
  }

  private ConfigThingy addIf(ConfigThingy conf)
  {
      
      return conf.add("IF");
  }

  private ConfigThingy addThen(ConfigThingy conf)
  {
    return conf.add("THEN");
  }

  private ConfigThingy addElse(ConfigThingy conf)
  {
    return conf.add("ELSE");
  }

  private ConfigThingy addCAT(ConfigThingy rootNode, String value)
  {
    ConfigThingy catConf = new ConfigThingy("CAT");
    catConf.add(value);
    
    
    return catConf;
  }

  /**
   * Aktualisiert {@link #params},conf anhand des aktuellen Dialogzustandes und setzt params,isValid
   * auf true.
   * 
   */
  private void updateTrafoConf(ConfigThingy conf)
  {
    params.conf = conf;
    params.isValid = true;
    System.out.println(params.conf.stringRepresentation());
    documentController.replaceSelectionWithTrafoField(params.conf, "Wenn...Dann...Sonst");
  }

  private static class TestType
  {
    public String label;

    public String func;

    public TestType(String label, String func)
    {
      this.label = label;
      this.func = func;
    }

    @Override
    public String toString()
    {
      return label;
    }
  }

  private static final List<TestType> testTypes = Arrays.asList(
      new TestType(L.m("="), "STRCMP"), new TestType(L.m("="), "NUMCMP"),
      new TestType(L.m("<"), "LT"), new TestType(L.m("<="), "LE"), new TestType(L.m(">"), "GT"),
      new TestType(L.m(">="), "GE"),
      new TestType(L.m("regulärer A."), "MATCH"));

  private AbstractKeyHandler keyHandler = new AbstractKeyHandler()
  {
    @Override
    public boolean keyReleased(KeyEvent arg0)
    {
      if (arg0.KeyCode == 1286) // ENTF
      {
        if (selectedNode == null)
          return false;

        int nodeToDelIndex = selectedNode.getParent().getIndex(selectedNode);
        try
        {
          XMutableTreeNode parentNode = UnoRuntime.queryInterface(XMutableTreeNode.class, selectedNode.getParent());
          String[] data = nodeDataValueToStringArray(parentNode);
          String[] dataSelectedNode = nodeDataValueToStringArray(selectedNode);
          
          if (WENN.equals(data[0]) && WENN.equals(dataSelectedNode[0])) {
            
            int childCount = parentNode.getChildCount();
            
            // letztes child zuerst entfernen, TreeView kommt bei
            // node.removeChildByIndex(0)
            // node.removeChildByIndex(1)
            // node.removeChildByIndex(2)
            // durcheinander da der interne Index der TreeView bei Aufruf von removeChildByIndex()
            // dekrementiert wird.
            // Prüfung der children, dann oder sonst kann bereits gelöscht sein.         
            for (int i = childCount; i >= 0; i--)
            {
              parentNode.removeChildByIndex(i - 1);
            }
          } else {
            parentNode.removeChildByIndex(nodeToDelIndex);
          }
        } catch (IndexOutOfBoundsException |
            IllegalArgumentException e)
        {
          LOGGER.error("", e);
        }
      }

      return false;
    }
  };
}

