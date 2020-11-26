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
package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.Key;
import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.MouseEvent;
import com.sun.star.awt.TextEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
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
import com.sun.star.deployment.PackageInformationProvider;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.dialog.adapter.AbstractKeyHandler;
import de.muenchen.allg.dialog.adapter.AbstractMouseListener;
import de.muenchen.allg.dialog.adapter.AbstractSelectionChangeListener;
import de.muenchen.allg.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.ui.GuiFactory;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import de.muenchen.allg.util.UnoComponent;
import de.muenchen.allg.util.UnoProperty;

/**
 * Dialog for creating if-then-else fields.
 */
public class IfThenElseDialog
{

  private static final String CONTROL_CONDITION = "txtValueIf";

  private static final String CONTROL_CBSERIENBRIEFFELD2 = "cbSerienbrieffeld2";

  private static final String CONTROL_RESULTTREE = "resultTreeControl";

  private static final String CONTROL_CBCOMPARATOR = "cbComparator";

  private static final String CONTROL_CBNOT = "cbNot";

  private static final String CONTROL_TXTVALUE = "txtValue";

  private static final String CONTROL_CBSERIENBRIEFFELD = "cbSerienbrieffeld";

  private static final Logger LOGGER = LoggerFactory.getLogger(IfThenElseDialog.class);

  private static final String NOT = "nicht";

  private static final String WENN = "WENN";

  private static final String DANN = "DANN";

  private static final String SONST = "SONST";

  private static final String IMAGE_LOCATION = PackageInformationProvider.get(UNO.defaultContext)
      .getPackageLocation(Utils.getWollMuxProperties().getProperty("extension.id")) + "/image/";

  private static final Random rand = new Random();

  private IfThenElseDialog()
  {
    // nothing to do
  }

  /**
   * Initialize the dialog.
   *
   * @param fieldNames
   *          The fields of the document.
   * @param documentController
   *          The controller of the document.
   */
  public static void startDialog(List<String> fieldNames, TextDocumentController documentController)
  {
    if (fieldNames == null || fieldNames.isEmpty())
    {
      throw new IllegalArgumentException();
    }

    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UNO.XContainerWindowProvider(
          UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_CONTAINER_WINDOW_PROVIDER));

      XWindow window = provider.createContainerWindow("vnd.sun.star.script:WollMux.if_then_else?location=application",
          "", peer, null);

      XControlContainer controlContainer = UNO.XControlContainer(window);
      XDialog dialog = UNO.XDialog(window);

      XToolkit xToolkit = UNO.XToolkit(UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_TOOLKIT));

      addKeyListener(xToolkit, controlContainer);

      XTreeControl treeControl = UNO.XTreeControl(controlContainer.getControl(CONTROL_RESULTTREE));
      XControl xTreeControl = UNO.XControl(treeControl);
      XControlModel xControlModel = xTreeControl.getModel();
      UnoProperty.setProperty(xControlModel, UnoProperty.HIDE_INACTIVE_SELECTION, Boolean.TRUE);
      UnoProperty.setProperty(xControlModel, UnoProperty.ROOT_DISPLAYED, Boolean.TRUE);
      UnoProperty.setProperty(xControlModel, UnoProperty.ROW_HEIGHT, 40); // SelectionType?
      XMutableTreeDataModel treeNodeModel = GuiFactory.createTreeModel(UNO.xMCF, UNO.defaultContext);
      XMutableTreeNode rootNode = treeNodeModel.createNode(UUID.randomUUID().toString(), false);
      rootNode.setDisplayValue("Neue Bedingung");
      rootNode.setDataValue("ROOT");
      treeNodeModel.setRoot(rootNode);
      UnoProperty.setProperty(xControlModel, UnoProperty.DATA_MODEL, treeNodeModel);
      treeControl.select(rootNode);
      treeControl.expandNode(rootNode);
      XWindow wndTreeControl = UNO.XWindow(xTreeControl);
      AbstractMouseListener mouseListener = new AbstractMouseListener()
      {
        @Override
        public void mousePressed(MouseEvent arg0)
        {
          XTreeNode node = UNO.XMutableTreeNode(treeControl.getClosestNodeForLocation(arg0.X, arg0.Y));

          if (node != null)
          {
            if (node.getChildCount() > 0)
            {
              try
              {
                treeControl.expandNode(node);
                node = node.getChildAt(0);
              } catch (IndexOutOfBoundsException | IllegalArgumentException | ExpandVetoException e)
              {
                LOGGER.error("Kein Kindknoten verfügbar", e);
              }
            }
            treeControl.select(node);
          }
        }
      };
      wndTreeControl.addMouseListener(mouseListener);
      AbstractSelectionChangeListener treeControlSelectionChangeListener = e -> nodeSelected(controlContainer);
      treeControl.addSelectionChangeListener(treeControlSelectionChangeListener);

      XButton removeConditionBtn = UNO.XButton(controlContainer.getControl("removeConditionBtn"));
      AbstractActionListener removeConditionBtnActionListener = event -> deleteCondition(
          getSelectedNode(controlContainer));
      removeConditionBtn.addActionListener(removeConditionBtnActionListener);

      XComboBox cbSerienbrieffeld = UNO.XComboBox(controlContainer.getControl(CONTROL_CBSERIENBRIEFFELD));
      AbstractItemListener cbSerienbrieffeldItemListener = e -> conditionDropDownChanged(controlContainer, e);
      cbSerienbrieffeld.addItemListener(cbSerienbrieffeldItemListener);
      fieldNames
          .forEach(fieldName -> cbSerienbrieffeld.addItem(fieldName, (short) (cbSerienbrieffeld.getItemCount() + 1)));

      XComboBox cbSerienbrieffeld2 = UNO.XComboBox(controlContainer.getControl(CONTROL_CBSERIENBRIEFFELD2));
      AbstractItemListener cbSerienbrieffeld2ItemListener = e -> contentDropDownChanged(controlContainer, e);
      cbSerienbrieffeld2.addItemListener(cbSerienbrieffeld2ItemListener);
      fieldNames
          .forEach(fieldName -> cbSerienbrieffeld2.addItem(fieldName, (short) (cbSerienbrieffeld2.getItemCount() + 1)));

      XButton okBtn = UNO.XButton(controlContainer.getControl("okBtn"));
      AbstractActionListener okBtnActionListener = e -> submit(documentController, controlContainer, dialog);
      okBtn.addActionListener(okBtnActionListener);

      XCheckBox cbWennNot = UNO.XCheckBox(controlContainer.getControl(CONTROL_CBNOT));
      AbstractItemListener notItemListener = e -> invertCondition(controlContainer, e);
      cbWennNot.addItemListener(notItemListener);

      XTextComponent txtValue = UNO.XTextComponent(controlContainer.getControl(CONTROL_TXTVALUE));
      AbstractTextListener txtValueWennListener = e -> textFieldChanged(controlContainer, e);
      txtValue.addTextListener(txtValueWennListener);
      XTextComponent txtValueIf = UNO.XTextComponent(controlContainer.getControl(CONTROL_CONDITION));
      txtValueIf.addTextListener(txtValueWennListener);

      XComboBox cbComparator = UNO.XComboBox(controlContainer.getControl(CONTROL_CBCOMPARATOR));
      Arrays.stream(TestType.values())
          .forEach(item -> cbComparator.addItem(item.label, (short) (cbComparator.getItemCount() + 1)));
      UNO.XTextComponent(cbComparator).setText(cbComparator.getItem((short) 0));
      AbstractItemListener comparatorItemListener = e -> compartorChanged(controlContainer, e);
      cbComparator.addItemListener(comparatorItemListener);

      XButton abortBtn = UNO.XButton(controlContainer.getControl("abortBtn"));
      AbstractActionListener abortBtnActionListener = event -> dialog.endExecute();
      abortBtn.addActionListener(abortBtnActionListener);

      List<String> randomImages = new ArrayList<>();
      addNodeImages(randomImages);
      XButton newConditionBtn = UNO.XButton(controlContainer.getControl("newConditionBtn"));
      AbstractActionListener newConditionBtnActionListener = e -> addNewCondition(controlContainer,
          randomImages);
      newConditionBtn.addActionListener(newConditionBtnActionListener);

      newConditionBtnActionListener.actionPerformed(new ActionEvent());
      dialog.execute();
    } catch (Exception | UnoHelperException e)
    {
      LOGGER.error("", e);
    }
  }

  private static void addNewCondition(XControlContainer controlContainer, List<String> randomImages)
  {
    int randomNumber = rand.nextInt(randomImages.size());

    String randomImageFileName = randomImages.get(randomNumber);
    XComboBox cbSerienbrieffeld = UNO.XComboBox(controlContainer.getControl(CONTROL_CBSERIENBRIEFFELD));

    try
    {
      XMutableTreeNode ifNode = createIfNode(getTreeDataModel(controlContainer), cbSerienbrieffeld,
          randomImageFileName);

      // avoid duplicates
      randomImages.remove(randomNumber);

      // refill if empty
      if (randomImages.isEmpty())
        addNodeImages(randomImages);

      XMutableTreeNode selectedNode = getSelectedNode(controlContainer);
      if (selectedNode.getParent() != null) // Test ob RootNode
      {
        String[] data = nodeDataValueToStringArray(selectedNode);
        if (DANN.equals(data[0]) || SONST.equals(data[0]))
        {
          data[2] = "";
          selectedNode.setDataValue(data);
          selectedNode.setDisplayValue(data[0] + " " + data[2]);
        }
      }

      selectedNode.appendChild(ifNode);
      selectedNode.appendChild(createThenNode(getTreeDataModel(controlContainer), "", randomImageFileName));
      selectedNode.appendChild(createElseNode(getTreeDataModel(controlContainer), "", randomImageFileName));
      XTreeControl treeControl = UNO.XTreeControl(controlContainer.getControl(CONTROL_RESULTTREE));
      treeControl.expandNode(selectedNode);
      treeControl.select(ifNode);
    } catch (com.sun.star.lang.IllegalArgumentException | ExpandVetoException | UnoHelperException e)
    {
      LOGGER.error("", e);
    }

    UNO.XTextComponent(cbSerienbrieffeld).setText(cbSerienbrieffeld.getItem((short) 0));
    UNO.XTextComponent(controlContainer.getControl(CONTROL_CONDITION)).setText("");
  }

  private static void compartorChanged(XControlContainer controlContainer, ItemEvent event)
  {
    String comparatorValue = UNO.XTextComponent(event.Source).getText();
    XTextComponent txtValue = UNO.XTextComponent(controlContainer.getControl(CONTROL_TXTVALUE));
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

    XMutableTreeNode selectedNode = getSelectedNode(controlContainer);
    if (selectedNode == null)
      return;

    String[] data = nodeDataValueToStringArray(selectedNode);

    if (WENN.equals(data[0]))
    {
      data[4] = UNO.XTextComponent(event.Source).getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    }
  }

  private static void textFieldChanged(XControlContainer controlContainer, TextEvent event)
  {
    XMutableTreeNode selectedNode = getSelectedNode(controlContainer);

    String[] data = nodeDataValueToStringArray(selectedNode);

    // text value not editable if it contains a nested condition
    if (selectedNode.getChildCount() > 0)
    {
      data[2] = "";
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(data[0] + " " + data[2]);
      return;
    }

    if (WENN.equals(data[0]))
    {
      // data[0] = condition, data[1] = id, [2] = serienbrieffeld, [3] = not, [4] = comp, [5] =
      data[5] = UNO.XTextComponent(event.Source).getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    } else
    {
      // data[0] = condition, data[1] = id, [2] = value
      data[2] = UNO.XTextComponent(event.Source).getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(data[0] + " " + data[2]);
    }
  }

  private static void invertCondition(XControlContainer controlContainer, ItemEvent event)
  {
    XMutableTreeNode selectedNode = getSelectedNode(controlContainer);
    if (selectedNode == null)
      return;

    String[] data = nodeDataValueToStringArray(selectedNode);

    if (WENN.equals(data[0]))
    {
      data[3] = UNO.XCheckBox(event.Source).getState() == 1 ? NOT : "";
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    }
  }

  private static void contentDropDownChanged(XControlContainer controlContainer, ItemEvent event)
  {
    XTextComponent txtValue = UNO.XTextComponent(controlContainer.getControl(CONTROL_TXTVALUE));
    txtValue.setText(txtValue.getText() + "{{" + UNO.XTextComponent(event.Source).getText() + "}}");
  }

  private static void conditionDropDownChanged(XControlContainer controlContainer, ItemEvent event)
  {
    XMutableTreeNode selectedNode = getSelectedNode(controlContainer);
    if (selectedNode == null)
    {
      return;
    }

    String[] data = nodeDataValueToStringArray(selectedNode);

    if (WENN.equals(data[0]))
    {
      data[2] = UNO.XTextComponent(event.Source).getText();
      selectedNode.setDataValue(data);
      selectedNode.setDisplayValue(data[0] + " " + data[2] + " " + data[3] + " " + data[4] + " " + data[5]);
    }
  }

  private static void submit(TextDocumentController documentController, XControlContainer controlContainer,
      XDialog dialog)
  {
    try
    {
      ConfigThingy resultConf = buildTrafo(getTreeDataModel(controlContainer).getRoot(), new ConfigThingy("Func"));
      documentController.replaceSelectionWithTrafoField(resultConf, "Wenn...Dann...Sonst");
      dialog.endExecute();
    } catch (UnoHelperException e)
    {
      LOGGER.error("", e);
    }
  }

  private static XMutableTreeNode getSelectedNode(XControlContainer controlContainer)
  {
    XTreeControl treeControl = UNO.XTreeControl(controlContainer.getControl(CONTROL_RESULTTREE));
    return UNO.XMutableTreeNode(treeControl.getSelection());
  }

  private static void addKeyListener(XToolkit xToolkit, XControlContainer controlContainer)
  {
    XExtendedToolkit extToolkit = UNO.XExtendedToolkit(xToolkit);
    AbstractKeyHandler keyHandler = new AbstractKeyHandler()
    {
      @Override
      public boolean keyReleased(KeyEvent event)
      {
        if (event.KeyCode == Key.DELETE)
        {
          XMutableTreeNode selectedNode = getSelectedNode(controlContainer);
          if (selectedNode == null)
          {
            return false;
          }

          deleteCondition(selectedNode);
        }

        return false;
      }
    };
    extToolkit.addKeyHandler(keyHandler);
  }

  private static void deleteCondition(XMutableTreeNode selectedNode)
  {
    if (selectedNode == null)
      return;

    try
    {
      XMutableTreeNode parentNode = UNO.XMutableTreeNode(selectedNode.getParent());

      for (int i = parentNode.getChildCount(); i > 0; i--)
      {
        parentNode.removeChildByIndex(i - 1);
      }
    } catch (IndexOutOfBoundsException | IllegalArgumentException e)
    {
      LOGGER.error("", e);
    }
  }

  private static void addNodeImages(List<String> randomImages)
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

  private static String[] nodeDataValueToStringArray(XMutableTreeNode node)
  {
    Object[] data = (Object[]) node.getDataValue();
    String[] dest = new String[data.length];
    System.arraycopy(data, 0, dest, 0, data.length);

    return dest;
  }

  private static XMutableTreeNode createElseNode(XMutableTreeDataModel treeNodeModel, String displayValue,
      String imgFileName)
  {
    String id = UUID.randomUUID().toString();

    XMutableTreeNode elseNode = treeNodeModel.createNode(UUID.randomUUID().toString(), false);
    elseNode.setDisplayValue(SONST + " " + displayValue);

    List<String> data = new ArrayList<>();
    data.add(SONST);
    data.add(id);
    data.add(displayValue);

    elseNode.setDataValue(data.toArray());
    elseNode.setNodeGraphicURL(imgFileName);

    return elseNode;
  }

  private static XMutableTreeNode createThenNode(XMutableTreeDataModel treeNodeModel, String displayValue,
      String imgFileName)
  {
    String id = UUID.randomUUID().toString();

    XMutableTreeNode thenNode = treeNodeModel.createNode(UUID.randomUUID().toString(), false);
    thenNode.setDisplayValue(DANN + " " + displayValue);

    List<String> data = new ArrayList<>();
    data.add(DANN);
    data.add(id);
    data.add(displayValue);

    thenNode.setDataValue(data.toArray());
    thenNode.setNodeGraphicURL(imgFileName);

    return thenNode;
  }

  private static XMutableTreeNode createIfNode(XMutableTreeDataModel treeNodeModel, XComboBox cbSerienbrieffeld,
      String imgFileName)
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

  private static XMutableTreeDataModel getTreeDataModel(XControlContainer controlContainer) throws UnoHelperException
  {
    XControl xTreeControl = UNO.XControl(controlContainer.getControl(CONTROL_RESULTTREE));
    XControlModel xControlModel = xTreeControl.getModel();
    return UNO.XMutableTreeDataModel(UnoProperty.getProperty(xControlModel, UnoProperty.DATA_MODEL));
  }

  private static void nodeSelected(XControlContainer controlContainer)
  {
    XMutableTreeNode selectedNode = getSelectedNode(controlContainer);

    if (selectedNode.getParent() == null)
    {
      try
      {
        UNO.XTreeControl(controlContainer.getControl(CONTROL_RESULTTREE)).select(selectedNode.getChildAt(0));
        selectedNode = getSelectedNode(controlContainer);
      } catch (com.sun.star.lang.IllegalArgumentException | IndexOutOfBoundsException e)
      {
        LOGGER.error("", e);
      }
    }

    String[] data = nodeDataValueToStringArray(selectedNode);

    // data[0] = identifier
    if (WENN.equals(data[0]))
    {
      UNO.XTextComponent(controlContainer.getControl(CONTROL_CBSERIENBRIEFFELD)).setText(data[2]);
      UNO.XCheckBox(controlContainer.getControl(CONTROL_CBNOT)).setState((short) (NOT.equals(data[3]) ? 1 : 0));
      UNO.XTextComponent(controlContainer.getControl(CONTROL_CBCOMPARATOR)).setText(data[4]);
      UNO.XTextComponent(controlContainer.getControl(CONTROL_CONDITION)).setText(data[5]);
      activeWennControls(controlContainer, true);
      changeText(controlContainer, "Wenn", "Serienbrieffeld:");
    } else
    {
      // dest[1] = id, [2] = txtfieldvalue
      UNO.XTextComponent(controlContainer.getControl(CONTROL_TXTVALUE)).setText(data[2]);
      activeWennControls(controlContainer, false);
      if (DANN.equals(data[0]))
      {
        changeText(controlContainer, "Dann", "Wert:");
      } else
      {
        changeText(controlContainer, "Sonst", "Wert:");
      }
    }

    UNO.XWindow(controlContainer.getControl("removeConditionBtn"))
        .setVisible(selectedNode.getParent().getParent() != null);
  }

  private static void changeText(XControlContainer controlContainer, String title, String label)
  {
    try
    {
      XControl frame = controlContainer.getControl("FrameControl1");
      UnoProperty.setProperty(frame.getModel(), UnoProperty.LABEL, title);
      UNO.XFixedText(controlContainer.getControl("labelVar1")).setText(label);
    } catch (UnoHelperException e)
    {
      LOGGER.error("Name der GroupBox konnte nicht geändert werden.", e);
    }
  }

  private static void activeWennControls(XControlContainer controlContainer, boolean active)
  {
    UNO.XWindow(controlContainer.getControl(CONTROL_CBNOT)).setVisible(active);
    UNO.XWindow(controlContainer.getControl(CONTROL_CBCOMPARATOR)).setVisible(active);
    UNO.XWindow(controlContainer.getControl(CONTROL_CBSERIENBRIEFFELD)).setVisible(active);
    UNO.XWindow(controlContainer.getControl("labelBed")).setVisible(active);
    UNO.XWindow(controlContainer.getControl("newConditionBtn")).setVisible(!active);
    UNO.XWindow(controlContainer.getControl("labelVar2")).setVisible(!active);
    UNO.XWindow(controlContainer.getControl(CONTROL_CBSERIENBRIEFFELD2)).setVisible(!active);
    UNO.XWindow(controlContainer.getControl(CONTROL_TXTVALUE)).setVisible(!active);
    UNO.XWindow(controlContainer.getControl(CONTROL_CONDITION)).setVisible(active);
  }

  private static ConfigThingy buildTrafo(XTreeNode currentNode, ConfigThingy rootConfig)
  {

    if (currentNode.getChildCount() < 1)
      return new ConfigThingy("");

    ConfigThingy currentConfig = rootConfig;

    for (int i = 0; i < currentNode.getChildCount(); i++)
    {
      try
      {
        XTreeNode currentChildNode = currentNode.getChildAt(i);

        String[] dataChildNode = nodeDataValueToStringArray(UNO.XMutableTreeNode(currentChildNode));

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

  private static ConfigThingy addSTRCMPBlock(ConfigThingy ifConf, String comparator, String value1, String value2)
  {
    Optional<TestType> resultTestType = Arrays.stream(TestType.values()).filter(item -> comparator.equals(item.label))
        .findFirst();

    if (resultTestType.isPresent())
    {
      ConfigThingy strCmpConf = ifConf.add(resultTestType.get().func);
      strCmpConf.add("VALUE").add(value1 == null || value1.isEmpty() ? "" : value1);
      strCmpConf.add(value2 == null || value2.isEmpty() ? "" : value2);

      return strCmpConf;
    }

    return null;
  }

  private static ConfigThingy createIf(String[] data)
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

  private static ConfigThingy createThenOrElse(String func, String[] data, XTreeNode currentChildNode)
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

  private static ConfigThingy addCAT(String value)
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
}
