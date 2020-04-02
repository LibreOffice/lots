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
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.MouseEvent;
import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XExtendedToolkit;
import com.sun.star.awt.XFixedText;
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
import com.sun.star.lang.EventObject;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.dialog.adapter.AbstractKeyHandler;
import de.muenchen.allg.dialog.adapter.AbstractMouseListener;
import de.muenchen.allg.dialog.adapter.AbstractSelectionChangeListener;
import de.muenchen.allg.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Erlaubt die Bearbeitung der Funktion eines Wenn-Dann-Sonst-Feldes.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class IfThenElseDialog
{

  private static final Logger LOGGER = LoggerFactory.getLogger(IfThenElseDialog.class);

  private static final String NOT = "nicht";

  private static final String WENN = "WENN";

  private static final String DANN = "DANN";

  private static final String SONST = "SONST";

  private static final String EXTENSION_ID = "de.muenchen.allg.d101.wollmux";

  // Zugriff auf Ressourcen
  private static final String IMAGE_LOCATION = PackageInformationProvider.get(UNO.defaultContext)
      .getPackageLocation(EXTENSION_ID) + "/image/";

  private XControlContainer controlContainer;

  private XDialog dialog;

  private XComboBox cbComparator;

  private XCheckBox cbWennNot;

  private XTextComponent txtValue;

  private XComboBox cbSerienbrieffeld;

  private XComboBox cbSerienbrieffeld2;

  private XFixedText labelVar1;

  private XFixedText labelVar2;

  private XFixedText labelBed;

  private XMutableTreeNode selectedNode;

  private XMutableTreeNode rootNode;

  private XControlModel xControlModel;

  private XTreeControl treeControl;

  private XButton newConditionBtn;

  private XButton removeConditionBtn;

  private List<String> randomImages = new ArrayList<>();

  private XMutableTreeDataModel treeNodeModel;

  private TextDocumentController documentController;

  private int firstY;
  private int lastY;

  public IfThenElseDialog(List<String> fieldNames, TextDocumentController documentController)
  {
    this.documentController = documentController;
    if (fieldNames == null || fieldNames.isEmpty())
      throw new IllegalArgumentException();

    addNodeImages();

    buildGUI(fieldNames);
  }

  private void addNodeImages()
  {
    randomImages.add(IMAGE_LOCATION + "if.png");
    randomImages.add(IMAGE_LOCATION + "else.png");
    randomImages.add(IMAGE_LOCATION + "then.png");
    randomImages.add(IMAGE_LOCATION + "brown.png");
    randomImages.add(IMAGE_LOCATION + "grey.png");
    randomImages.add(IMAGE_LOCATION + "ocker.png");
    randomImages.add(IMAGE_LOCATION + "orange.png");
    randomImages.add(IMAGE_LOCATION + "red.png");
    randomImages.add(IMAGE_LOCATION + "navy_blue.png");
    randomImages.add(IMAGE_LOCATION + "light_blue.png");
    randomImages.add(IMAGE_LOCATION + "lila.png");
    randomImages.add(IMAGE_LOCATION + "dark_green.png");
  }

  private void buildGUI(List<String> fieldNames)
  {
    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));

      XWindow window = provider.createContainerWindow(
          "vnd.sun.star.script:WollMux.if_then_else?location=application", "", peer, null);

      Object toolkit = UNO.xMCF.createInstanceWithContext("com.sun.star.awt.Toolkit",
          UNO.defaultContext);
      XToolkit xToolkit = UNO.XToolkit(toolkit);
      XExtendedToolkit extToolkit = UnoRuntime.queryInterface(XExtendedToolkit.class, xToolkit);
      extToolkit.addKeyHandler(keyHandler);

      controlContainer = UnoRuntime.queryInterface(XControlContainer.class, window);

      removeConditionBtn = UNO.XButton(controlContainer.getControl("removeConditionBtn"));
      removeConditionBtn.addActionListener(removeConditionBtnActionListener);

      newConditionBtn = UNO.XButton(controlContainer.getControl("newConditionBtn"));
      newConditionBtn.addActionListener(newConditionBtnActionListener);

      XButton okBtn = UNO.XButton(controlContainer.getControl("okBtn"));
      okBtn.addActionListener(okBtnActionListener);

      XButton abortBtn = UNO.XButton(controlContainer.getControl("abortBtn"));
      abortBtn.addActionListener(abortBtnActionListener);

      labelVar1 = UNO.XFixedText(controlContainer.getControl("labelVar1"));

      cbSerienbrieffeld = UNO.XComboBox(controlContainer.getControl("cbSerienbrieffeld"));
      cbSerienbrieffeld.addItemListener(cbSerienbrieffeldItemListener);
      firstY = UNO.XWindow(cbSerienbrieffeld).getPosSize().Y;

      labelBed = UNO.XFixedText(controlContainer.getControl("labelBed"));

      fieldNames.forEach(fieldName -> cbSerienbrieffeld.addItem(fieldName,
          (short) (cbSerienbrieffeld.getItemCount() + 1)));

      cbComparator = UNO.XComboBox(controlContainer.getControl("cbComparator"));
      Arrays.stream(TestType.values()).forEach(
          item -> cbComparator.addItem(item.label, (short) (cbComparator.getItemCount() + 1)));
      // default wert setzen
      UNO.XTextComponent(cbComparator).setText(cbComparator.getItem((short) 0));
      cbComparator.addItemListener(comparatorItemListener);

      cbWennNot = UNO.XCheckBox(controlContainer.getControl("cbNot"));
      cbWennNot.addItemListener(notItemListener);

      txtValue = UNO.XTextComponent(controlContainer.getControl("txtValue"));
      txtValue.addTextListener(txtValueWennListener);
      lastY = UNO.XWindow(txtValue).getPosSize().Y;

      labelVar2 = UNO.XFixedText(controlContainer.getControl("labelVar2"));

      cbSerienbrieffeld2 = UNO.XComboBox(controlContainer.getControl("cbSerienbrieffeld2"));
      cbSerienbrieffeld2.addItemListener(cbSerienbrieffeld2ItemListener);

      fieldNames.forEach(fieldName -> cbSerienbrieffeld2.addItem(fieldName,
          (short) (cbSerienbrieffeld2.getItemCount() + 1)));

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

      newConditionBtnActionListener.actionPerformed(new ActionEvent());

      dialog = UnoRuntime.queryInterface(XDialog.class, window);
      dialog.execute();
    } catch (Exception e)
    {
      LOGGER.error("", e);
    }
  }

  private AbstractActionListener removeConditionBtnActionListener = event -> removeNode();

  private AbstractActionListener okBtnActionListener = event -> {
    ConfigThingy resultConf = buildTrafo(rootNode, new ConfigThingy("Func"));
    documentController.replaceSelectionWithTrafoField(resultConf, "Wenn...Dann...Sonst");
    dialog.endExecute();
  };

  private AbstractActionListener abortBtnActionListener = event -> dialog.endExecute();

  private AbstractActionListener newConditionBtnActionListener = event -> {
    Random rand = new Random();
    int randomNumber = rand.nextInt(randomImages.size());

    String randomImageFileName = randomImages.get(randomNumber);
    XMutableTreeNode ifNode = createIfNode(randomImageFileName);

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
        XPropertySet xTreeModelProperty = UnoRuntime.queryInterface(XPropertySet.class,
            xControlModel);
        xTreeModelProperty.setPropertyValue("DataModel", treeNodeModel);
        selectedNode = root;
        treeControl.expandNode(root);
      } catch (UnknownPropertyException | PropertyVetoException | WrappedTargetException
          | IllegalArgumentException | ExpandVetoException e)
      {
        LOGGER.error("", e);
      }
    } else if (selectedNode.getParent() != null) // Test ob RootNode
    {
      // falls durch den Benutzer bereits ein (Text)-Wert für einen "WENN" oder "SONST"-Node gesetzt wurde,
      // -> nicht erlaubt, daher zurücksetzen.
      String[] data = nodeDataValueToStringArray(selectedNode);
      if (DANN.equals(data[0]) || SONST.equals(data[0]))
      {
        data[2] = "";
        selectedNode.setDataValue(data);
        selectedNode.setDisplayValue(data[0] + " " + data[2]);
      }
    }

    selectedNode.appendChild(ifNode);
    selectedNode.appendChild(createThenNode("", randomImageFileName));
    selectedNode.appendChild(createElseNode("", randomImageFileName));

    try
    {
      treeControl.expandNode(selectedNode);
      treeControl.clearSelection();
      treeControl.addSelection(ifNode);
    } catch (com.sun.star.lang.IllegalArgumentException | ExpandVetoException e)
    {
      LOGGER.error("", e);
    }

    UNO.XTextComponent(cbSerienbrieffeld).setText(cbSerienbrieffeld.getItem((short) 0));
  };

  private AbstractMouseListener mouseListener = new AbstractMouseListener()
  {
    @Override
    public void mousePressed(MouseEvent arg0)
    {
      XTreeNode node = UnoRuntime.queryInterface(XTreeNode.class,
          treeControl.getClosestNodeForLocation(arg0.X, arg0.Y));

      if (node != null)
      {
        treeControl.clearSelection();
        if (node.getChildCount() > 0)
        {
          try
          {
            treeControl.expandNode(node);
            node = node.getChildAt(0);
          } catch (IndexOutOfBoundsException | com.sun.star.lang.IllegalArgumentException
              | ExpandVetoException e)
          {
            LOGGER.error("Kein Kindknoten verfügbar", e);
          }
        }
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

      // data[0] = identifier
      if (WENN.equals(data[0]))
      {
        UNO.XTextComponent(cbSerienbrieffeld).setText(data[2]);
        cbWennNot.setState((short) (NOT.equals(data[3]) ? 1 : 0));
        UNO.XTextComponent(cbComparator).setText(data[4]);
        txtValue.setText(data[5]);
        activeWennControls(true);
        setPosition(true);
        changeText("Wenn", "Serienbrieffeld:");
      } else
      {
        // dest[1] = id, [2] = txtfieldvalue
        txtValue.setText(data[2]);
        activeWennControls(false);
        setPosition(false);
        if (DANN.equals(data[0]))
        {
          changeText("Dann", "Wert:");
        } else
        {
          changeText("Sonst", "Wert:");
        }
      }

      // Löschen-Button ausblenden wenn nur eine Bedingung vorhanden ist.
      UNO.XWindow(removeConditionBtn).setVisible(selectedNode.getParent().getParent() != null);
    }

    private void changeText(String title, String label)
    {
      try
      {
        XControl frame = controlContainer.getControl("FrameControl1");
        XPropertySet props = UNO.XPropertySet(frame.getModel());
        props.setPropertyValue("Label", title);
        labelVar1.setText(label);
      } catch (com.sun.star.lang.IllegalArgumentException | UnknownPropertyException
          | PropertyVetoException | WrappedTargetException e)
      {
        LOGGER.error("Name der GroupBox konnte nicht geändert werden.", e);
      }
    }

    /*
     * Control tauschen um unnötigen freien Bereich bei ausgeblendeten Controls zu vermeiden.
     *
     * WENN-Layout ([----] = Control): [----] (Serienbrieffeld) [----] (NOT-Combobox) [----]
     * (Comparator-ComboBox) [----] (TextFeld) ...
     *
     * DANN/SONST-Layout: [----] (TextFeld) ... Serienbrieffeld, NOT-CB und Comparator-CB sind
     * ausgeblendet, TextFeld wird an erste Position verschoben.
     */
    private void setPosition(boolean active)
    {
      Rectangle rectText = UNO.XWindow(txtValue).getPosSize();
      Rectangle rectLabel = UNO.XWindow(labelVar2).getPosSize();
      Rectangle rectCb = UNO.XWindow(cbSerienbrieffeld2).getPosSize();
      if (!active)
      {
        UNO.XWindow(txtValue).setPosSize(rectText.X, rectText.Y - (rectText.Y - firstY), 0, 0,
            PosSize.POS);
        UNO.XWindow(labelVar2).setPosSize(rectLabel.X, rectLabel.Y - (rectText.Y - firstY), 0, 0,
            PosSize.POS);
        UNO.XWindow(cbSerienbrieffeld2).setPosSize(rectCb.X, rectCb.Y - (rectText.Y - firstY), 0, 0,
            PosSize.POS);
      } else
      {
        UNO.XWindow(txtValue).setPosSize(rectText.X, rectText.Y + (lastY - rectText.Y), 0, 0,
            PosSize.POS);
        UNO.XWindow(labelVar2).setPosSize(rectLabel.X, rectLabel.Y + (lastY - rectText.Y), 0, 0,
            PosSize.POS);
        UNO.XWindow(cbSerienbrieffeld2).setPosSize(rectCb.X, rectCb.Y + (lastY - rectText.Y), 0, 0,
            PosSize.POS);
      }
    }

    private void activeWennControls(boolean active)
    {
      UNO.XWindow(cbWennNot).setVisible(active);
      UNO.XWindow(cbComparator).setVisible(active);
      UNO.XWindow(cbSerienbrieffeld).setVisible(active);
      UNO.XWindow(labelBed).setVisible(active);
      UNO.XWindow(newConditionBtn).setVisible(!active);
      UNO.XWindow(labelVar2).setVisible(!active);
      UNO.XWindow(cbSerienbrieffeld2).setVisible(!active);
    }
  };

  private String[] nodeDataValueToStringArray(XMutableTreeNode node) {
    Object[] data = (Object[]) node.getDataValue();
    String[] dest = new String[data.length];
    System.arraycopy(data, 0, dest, 0, data.length);

    return dest;
  }

  private AbstractTextListener txtValueWennListener = event -> {
    // DisplayValue aus DataValue generieren, Änderungen speichern.
    String[] data = nodeDataValueToStringArray(selectedNode);

    // Bedingung 'dann' oder 'sonst' darf nicht mit Text-Wert editierbar
    // sein wenn weitere Bedingung (IF) gesetzt ist.
    if (selectedNode.getChildCount() > 0) {
      //default setzen falls zuvor schon editiert wurde.
      data[2] = "";
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(data[0] + " " + data[2]);
      return;
    }

    if (WENN.equals(data[0]))
    {
      // data[0] = condition, data[1] = id, [2] = serienbrieffeld, [3] = not, [4] = comp, [5] =
      data[5] = txtValue.getText();
      selectedNode.setDataValue(data);
      selectedNode
          .setDisplayValue(data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    } else
    {
      // data[0] = condition, data[1] = id, [2] = value
      data[2] = txtValue.getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(data[0] + " " + data[2]);
    }
  };

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

  private XMutableTreeNode createIfNode(String imgFileName)
  {
    String id = UUID.randomUUID().toString();
    XMutableTreeNode ifNode = treeNodeModel.createNode(id, false);
    ifNode.setDisplayValue(WENN);

    List<String> data = new ArrayList<>();
    data.add(WENN);
    data.add(id);
    data.add(cbSerienbrieffeld.getItem((short) 0)); // mailmerge Field
    data.add(""); // Not
    data.add(TestType.STRCMP.label); // compartor
    data.add(""); // value

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

  private AbstractItemListener notItemListener = event -> {
    if (selectedNode == null)
      return;

    String[] data = nodeDataValueToStringArray(selectedNode);

    if (WENN.equals(data[0])) {
      data[3] = cbWennNot.getState() == 1 ? NOT : "";
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(
          data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    }
  };

  private AbstractItemListener cbSerienbrieffeldItemListener = event -> {
    if (selectedNode == null)
      return;

    String[] data = nodeDataValueToStringArray(selectedNode);

    if (WENN.equals(data[0]))
    {
      data[2] = UNO.XTextComponent(cbSerienbrieffeld).getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(
          data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    }
  };

  private AbstractItemListener cbSerienbrieffeld2ItemListener = event -> txtValue
      .setText(txtValue.getText() + "{{" + UNO.XTextComponent(cbSerienbrieffeld2).getText() + "}}");

  private AbstractItemListener comparatorItemListener = event -> {
    // Usability. Setzt je nach selektiertem Vergleichsoperator
    // den zu eingebenden Wert im txtValue-TextFeld.
    String comparatorValue = UNO.XTextComponent(cbComparator).getText();
    if (comparatorValue.equals("genau ="))
    {
      txtValue.setText("Text");
    } else if (comparatorValue.equals("regulärer Ausdruck"))
    {
      txtValue.setText("Regulärer Ausdruck");
    } else
    {
      txtValue.setText("Numerischer Wert");
    }

    if (selectedNode == null)
      return;

    // Vergleichsoperator speichern
    String[] data = nodeDataValueToStringArray(selectedNode);

    if (WENN.equals(data[0]))
    {
      data[4] = UNO.XTextComponent(cbComparator).getText();
      selectedNode.setDataValue(data);
      selectedNode
          .setDisplayValue(data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    }
  };

  private ConfigThingy buildTrafo(XTreeNode currentNode, ConfigThingy rootConfig)
  {

    if (currentNode.getChildCount() < 1)
      return new ConfigThingy("");

    ConfigThingy currentConfig = rootConfig;

    for (int i = 0; i < currentNode.getChildCount(); i++)
    {
      try
      {
        XTreeNode currentChildNode = currentNode.getChildAt(i);

        String[] dataChildNode = nodeDataValueToStringArray(
            UnoRuntime.queryInterface(XMutableTreeNode.class, currentChildNode));

        String conditionType = dataChildNode[0];

        if (WENN.equals(conditionType))
        {
          ConfigThingy ifConfig = createIf(dataChildNode);
          currentConfig.addChild(ifConfig);
          currentConfig = ifConfig;
        } else if (DANN.equals(conditionType))
        {
          ConfigThingy thenConf = createThenOrElse("THEN", dataChildNode, currentChildNode);
          currentConfig.addChild(thenConf);
        } else if (SONST.equals(conditionType))
        {
          ConfigThingy elseConf = createThenOrElse("ELSE", dataChildNode, currentChildNode);
          currentConfig.addChild(elseConf);
        }
      } catch (IndexOutOfBoundsException e)
      {
        LOGGER.error("", e);
      }
    }

    return rootConfig;
  }

  private ConfigThingy addSTRCMPBlock(ConfigThingy ifConf, String comparator, String value1,
      String value2)
  {
    Optional<TestType> resultTestType = Arrays.stream(TestType.values())
        .filter(item -> comparator.equals(item.label)).findFirst();

    if (resultTestType.isPresent())
    {
      ConfigThingy strCmpConf = ifConf.add(resultTestType.get().func);
      strCmpConf.add("VALUE").add(value1 == null || value1.isEmpty() ? "" : value1);
      strCmpConf.add(value2 == null || value2.isEmpty() ? "" : value2);

      return strCmpConf;
    }

    return null;
  }

  private ConfigThingy createIf(String[] data)
  {
    ConfigThingy conf = new ConfigThingy("IF");
    if (NOT.equals(data[3]))
    {
      ConfigThingy notConf = new ConfigThingy("NOT");
      addSTRCMPBlock(notConf, data[4], data[2], data[5]);
      conf.addChild(notConf);
    } else
    {
      addSTRCMPBlock(conf, data[4], data[2], data[5]);
    }
    return conf;
  }

  private ConfigThingy createThenOrElse(String func, String[] data, XTreeNode currentChildNode)
  {
    ConfigThingy conf = new ConfigThingy(func);

    if (currentChildNode.getChildCount() > 0)
      buildTrafo(currentChildNode, conf);
    else
    {
      ConfigThingy catConf = addCAT(data[2]);
      conf.addChild(catConf);
    }
    return conf;
  }

  private ConfigThingy addCAT(String value)
  {
    ConfigThingy catConf = new ConfigThingy("CAT");
    if (value.isEmpty())
    {
      catConf.add(value);
    } else
    {
      boolean finished = false;
      int index = 0;
      do
      {
        int startIndex = value.indexOf("{{", index);
        int endIndex = value.indexOf("}}", index);
        finished = index >= value.length();
        if (!finished)
        {
          if (startIndex > -1 && endIndex > -1)
          {
            catConf.add(value.substring(index, startIndex));
            ConfigThingy valueConf = new ConfigThingy("VALUE");
            valueConf.add(value.substring(startIndex + 2, endIndex));
            catConf.addChild(valueConf);
            index = endIndex + 2;
          } else
          {
            catConf.add(value.substring(index));
            index = value.length();
          }
        }
      } while (!finished);
    }

    return catConf;
  }

  private enum TestType
  {
    STRCMP("genau =", "STRCMP"),
    NUMCMP("numerisch =", "NUMCMP"),
    LT("numerisch <", "LT"),
    LE("numerisch <=", "LE"),
    GT("numerisch >", "GT"),
    GE("numerisch >=", "GE"),
    MATCH("regulärer Ausdruck", "MATCH");

    private final String label;

    private final String func;

    private TestType(String label, String func)
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

  private AbstractKeyHandler keyHandler = new AbstractKeyHandler()
  {
    @Override
    public boolean keyReleased(KeyEvent arg0)
    {
      if (arg0.KeyCode == 1286) // ENTF
      {
        if (selectedNode == null)
          return false;

        removeNode();
      }

      return false;
    }
  };

  // Entfernt Wenn/Dann/Sonst node.
  private void removeNode()
  {
    if (selectedNode == null)
      return;

    try
    {
      XMutableTreeNode parentNode = UnoRuntime.queryInterface(XMutableTreeNode.class,
          selectedNode.getParent());

      int childCount = parentNode.getChildCount();

      // letztes child zuerst entfernen, TreeView kommt bei
      // node.removeChildByIndex(0)
      // node.removeChildByIndex(1)
      // node.removeChildByIndex(2)
      // durcheinander da der interne Index der TreeView bei Aufruf von removeChildByIndex()
      // dekrementiert wird.
      // Prüfung der children, dann oder sonst kann bereits gelöscht sein.
      for (int i = childCount; i > 0; i--)
      {
        parentNode.removeChildByIndex(i - 1);
      }
    } catch (IndexOutOfBoundsException | IllegalArgumentException e)
    {
      LOGGER.error("", e);
    }
  }
}

