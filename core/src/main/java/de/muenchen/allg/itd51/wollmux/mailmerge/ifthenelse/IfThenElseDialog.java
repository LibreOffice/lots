/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.mailmerge.ifthenelse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.Key;
import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.MouseEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialog2;
import com.sun.star.awt.XExtendedToolkit;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.awt.tree.ExpandVetoException;
import com.sun.star.awt.tree.XMutableTreeDataModel;
import com.sun.star.awt.tree.XMutableTreeNode;
import com.sun.star.awt.tree.XTreeControl;
import com.sun.star.deployment.PackageInformationProvider;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.uno.AnyConverter;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractActionListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractItemListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractKeyHandler;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractMouseListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import org.libreoffice.ext.unohelper.ui.GuiFactory;
import org.libreoffice.ext.unohelper.util.UnoComponent;
import org.libreoffice.ext.unohelper.util.UnoProperty;

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

  private static final String IMAGE_LOCATION = PackageInformationProvider.get(UNO.defaultContext)
      .getPackageLocation(Utils.getWollMuxProperties().getProperty("extension.id")) + "/image/";

  private static final Random rand = new Random();

  private final List<String> randomImages = new ArrayList<>();

  private IfThenElseModel model;

  XControlContainer controlContainer;
  XDialog2 dialog;

  /**
   * Initialize the dialog.
   *
   * @param fieldNames
   *          The fields of the document.
   * @param model
   *          The model of the dialog.
   */
  public IfThenElseDialog(List<String> fieldNames, IfThenElseModel model)
  {
    this.model = model;

    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UNO.XContainerWindowProvider(
          UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_CONTAINER_WINDOW_PROVIDER));

      XWindow window = provider.createContainerWindow("vnd.sun.star.script:WollMux.if_then_else?location=application",
          "", peer, null);

      controlContainer = UNO.XControlContainer(window);
      dialog = UNO.XDialog2(window);

      addKeyListener();
      addTreeControl();
      addNewConditionButton();
      addRemoveConditionButton();
      addFieldDropdown(fieldNames);
      addContentDropdown(fieldNames);
      addInvertConditionCheckbox();
      addContentTextfield();
      addCompartorDropdown();
      addFinishButtons();
    } catch (UnoHelperException | IllegalArgumentException | ExpandVetoException | IndexOutOfBoundsException e)
    {
      LOGGER.error("", e);
    }
  }

  private void addKeyListener()
  {
    XExtendedToolkit extToolkit = UNO
        .XExtendedToolkit(UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_TOOLKIT));
    AbstractKeyHandler keyHandler = new AbstractKeyHandler()
    {
      @Override
      public boolean keyReleased(KeyEvent event)
      {
        if (event.KeyCode == Key.DELETE)
        {
          deleteCondition();
        }

        return false;
      }
    };
    extToolkit.addKeyHandler(keyHandler);
  }

  private void addRemoveConditionButton()
  {
    XButton removeConditionBtn = UNO.XButton(controlContainer.getControl("removeConditionBtn"));
    AbstractActionListener removeConditionBtnActionListener = event -> deleteCondition();
    removeConditionBtn.addActionListener(removeConditionBtnActionListener);
  }

  private void deleteCondition()
  {
    XMutableTreeNode selectedNode = getSelectedNode();
    if (selectedNode != null)
    {
      XMutableTreeNode parentNode = UNO.XMutableTreeNode(selectedNode.getParent());
      IfThenElseBaseModel node = model.getById(getDataValueFrom(selectedNode));
      if (!(node instanceof IfModel))
      {
        selectedNode = parentNode;
        parentNode = UNO.XMutableTreeNode(selectedNode.getParent());
      }
      int index = parentNode.getIndex(selectedNode);
      try
      {
        deleteChildrenFrom(selectedNode);
        parentNode.removeChildByIndex(index);
        IfThenElseBaseModel newNode = model.deleteCondition(node.getId());
        parentNode.insertChildByIndex(index,
            createNode(newNode.getId(), newNode.toString(), parentNode.getNodeGraphicURL()));
      } catch (IllegalArgumentException | IndexOutOfBoundsException | UnoHelperException e)
      {
        LOGGER.error("", e);
      }
    }
  }

  private void addTreeControl()
      throws UnoHelperException, IndexOutOfBoundsException, ExpandVetoException
  {
    XTreeControl treeControl = UNO.XTreeControl(controlContainer.getControl(CONTROL_RESULTTREE));
    XControl xTreeControl = UNO.XControl(treeControl);
    XControlModel xControlModel = xTreeControl.getModel();
    UnoProperty.setProperty(xControlModel, UnoProperty.HIDE_INACTIVE_SELECTION, Boolean.TRUE);
    UnoProperty.setProperty(xControlModel, UnoProperty.ROOT_DISPLAYED, Boolean.TRUE);
    UnoProperty.setProperty(xControlModel, UnoProperty.ROW_HEIGHT, 40); // SelectionType?
    XMutableTreeDataModel treeNodeModel = GuiFactory.createTreeModel(UNO.xMCF, UNO.defaultContext);
    UnoProperty.setProperty(xControlModel, UnoProperty.DATA_MODEL, treeNodeModel);
    XWindow wndTreeControl = UNO.XWindow(xTreeControl);
    AbstractMouseListener mouseListener = new AbstractMouseListener()
    {
      @Override
      public void mousePressed(MouseEvent event)
      {
        XMutableTreeNode node = UNO.XMutableTreeNode(treeControl.getClosestNodeForLocation(event.X, event.Y));
        try
        {
          nodeSelected(node);
        } catch (ExpandVetoException | IndexOutOfBoundsException e)
        {
          LOGGER.debug("", e);
        }
      }
    };
    wndTreeControl.addMouseListener(mouseListener);
    XMutableTreeNode newNode = createConditionNodes(model.getFunction());
    XMutableTreeNode rootNode = createNode(UUID.randomUUID().toString(), "Neue Bedingung", "");
    rootNode.appendChild(newNode);
    treeNodeModel.setRoot(rootNode);
    nodeSelected(rootNode);
  }

  private void addNewConditionButton()
  {
    XButton newConditionBtn = UNO.XButton(controlContainer.getControl("newConditionBtn"));
    AbstractActionListener newConditionBtnActionListener = e ->
    {
      XMutableTreeNode node = getSelectedNode();
      XMutableTreeNode parent = UNO.XMutableTreeNode(node.getParent());
      int index = parent.getIndex(node);
      UNO.XTreeControl(controlContainer.getControl(CONTROL_RESULTTREE)).clearSelection();
      try
      {
        deleteChildrenFrom(node);
        parent.removeChildByIndex(index);
        IfModel newModel = model.createNewCondition(getDataValueFrom(node));
        XMutableTreeNode newNode = createConditionNodes(newModel);
        parent.insertChildByIndex(index, newNode);
        nodeSelected(newNode);
      } catch (IllegalArgumentException | IndexOutOfBoundsException | ExpandVetoException ex)
      {
        LOGGER.error("", ex);
      }
    };
    newConditionBtn.addActionListener(newConditionBtnActionListener);
  }

  private void addFieldDropdown(List<String> fieldNames)
  {
    AbstractItemListener cbSerienbrieffeldItemListener = event ->
    {
      XMutableTreeNode selectedNode = getSelectedNode();
      if (selectedNode == null)
      {
        return;
      }

      String id = getDataValueFrom(selectedNode);
      IfThenElseBaseModel data = model.getById(id);

      if (data instanceof IfModel)
      {
        ((IfModel) data).setField(UNO.XTextComponent(event.Source).getText());
        selectedNode.setDisplayValue(data.toString());
      }
    };

    XComboBox cbSerienbrieffeld = UNO.XComboBox(controlContainer.getControl(CONTROL_CBSERIENBRIEFFELD));
    cbSerienbrieffeld.addItemListener(cbSerienbrieffeldItemListener);
    fieldNames
        .forEach(fieldName -> cbSerienbrieffeld.addItem(fieldName, (short) (cbSerienbrieffeld.getItemCount() + 1)));
  }

  private void addContentDropdown(List<String> fieldNames)
  {
    AbstractItemListener cbSerienbrieffeld2ItemListener = event -> {
      XTextComponent txtValue = UNO.XTextComponent(controlContainer.getControl(CONTROL_TXTVALUE));
      txtValue.setText(txtValue.getText() + "{{" + UNO.XTextComponent(event.Source).getText() + "}}");
    };

    XComboBox cbSerienbrieffeld2 = UNO.XComboBox(controlContainer.getControl(CONTROL_CBSERIENBRIEFFELD2));
    cbSerienbrieffeld2.addItemListener(cbSerienbrieffeld2ItemListener);
    fieldNames
        .forEach(fieldName -> cbSerienbrieffeld2.addItem(fieldName, (short) (cbSerienbrieffeld2.getItemCount() + 1)));
  }

  private void addInvertConditionCheckbox()
  {
    XCheckBox cbWennNot = UNO.XCheckBox(controlContainer.getControl(CONTROL_CBNOT));
    AbstractItemListener notItemListener = event ->
    {
      XMutableTreeNode selectedNode = getSelectedNode();
      if (selectedNode == null)
        return;

      String id = getDataValueFrom(selectedNode);
      IfThenElseBaseModel data = model.getById(id);

      if (data instanceof IfModel)
      {
        ((IfModel) data).setNot(UNO.XCheckBox(event.Source).getState() == 1);
        selectedNode.setDisplayValue(data.toString());
      }
    };
    cbWennNot.addItemListener(notItemListener);
  }

  private void addContentTextfield()
  {
    XTextComponent txtValue = UNO.XTextComponent(controlContainer.getControl(CONTROL_TXTVALUE));
    AbstractTextListener txtValueWennListener = event -> {
      XMutableTreeNode selectedNode = getSelectedNode();

      String id = getDataValueFrom(selectedNode);
      IfThenElseBaseModel data = model.getById(id);

      if (data != null)
      {
        data.setValue(UNO.XTextComponent(event.Source).getText());
        selectedNode.setDisplayValue(data.toString());
      }
    };
    txtValue.addTextListener(txtValueWennListener);
    XTextComponent txtValueIf = UNO.XTextComponent(controlContainer.getControl(CONTROL_CONDITION));
    txtValueIf.addTextListener(txtValueWennListener);
  }

  private void addCompartorDropdown()
  {
    XComboBox cbComparator = UNO.XComboBox(controlContainer.getControl(CONTROL_CBCOMPARATOR));
    Arrays.stream(TestType.values())
        .forEach(item -> cbComparator.addItem(item.getLabel(), (short) (cbComparator.getItemCount() + 1)));
    UNO.XTextComponent(cbComparator).setText(cbComparator.getItem((short) 0));
    AbstractItemListener comparatorItemListener = event -> {
      String comparatorValue = UNO.XTextComponent(event.Source).getText();
      XTextComponent txtValue = UNO.XTextComponent(controlContainer.getControl(CONTROL_TXTVALUE));
      if (txtValue.getText().isEmpty())
      {
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
      }

      XMutableTreeNode selectedNode = getSelectedNode();
      if (selectedNode == null)
        return;

      String id = getDataValueFrom(selectedNode);
      IfThenElseBaseModel data = model.getById(id);

      if (data instanceof IfModel)
      {
        String comparator = UNO.XTextComponent(event.Source).getText();
        Optional<TestType> resultTestType = Arrays.stream(TestType.values())
            .filter(item -> comparator.equals(item.getLabel())).findFirst();
        ((IfModel) data).setComparator(resultTestType.orElse(null));
        selectedNode.setDisplayValue(data.toString());
      }
    };
    cbComparator.addItemListener(comparatorItemListener);
  }

  private void addFinishButtons()
  {
    XButton okBtn = UNO.XButton(controlContainer.getControl("okBtn"));
    AbstractActionListener okBtnActionListener = event -> dialog.endDialog(ExecutableDialogResults.OK);
    okBtn.addActionListener(okBtnActionListener);

    XButton abortBtn = UNO.XButton(controlContainer.getControl("abortBtn"));
    AbstractActionListener abortBtnActionListener = event -> dialog.endDialog(ExecutableDialogResults.CANCEL);
    abortBtn.addActionListener(abortBtnActionListener);
  }

  /**
   * Start the dialog.
   *
   * @return {@link XDialog#execute()}
   */
  public short execute()
  {
    return dialog.execute();
  }

  private String getRandomImage()
  {
    // refill if empty
    if (randomImages.isEmpty())
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
    int randomNumber = rand.nextInt(randomImages.size());
    // avoid duplicates
    return randomImages.remove(randomNumber);
  }

  private XMutableTreeNode createConditionNodes(IfModel condition)
  {
    try
    {
      String imageURL = getRandomImage();
      XMutableTreeNode ifNode = createNode(condition.getId(), condition.toString(), imageURL);
      ifNode.setHasChildrenOnDemand(true);

      XMutableTreeNode thenNode = null;
      if (condition.getThenModel() instanceof IfModel)
      {
        thenNode = createConditionNodes((IfModel) condition.getThenModel());
      }
      if (thenNode == null)
      {
        thenNode = createNode(condition.getThenModel().getId(), condition.getThenModel().toString(), imageURL);
      }
      ifNode.appendChild(thenNode);

      XMutableTreeNode elseNode = null;
      if (condition.getElseModel() instanceof IfModel)
      {
        elseNode = createConditionNodes((IfModel) condition.getElseModel());
      }
      if (elseNode == null)
      {
        elseNode = createNode(condition.getElseModel().getId(), condition.getElseModel().toString(), imageURL);
      }
      ifNode.appendChild(elseNode);
      return ifNode;
    } catch (UnoHelperException e)
    {
      LOGGER.error("", e);
      return null;
    }
  }

  private XMutableTreeNode getSelectedNode()
  {
    XTreeControl treeControl = UNO.XTreeControl(controlContainer.getControl(CONTROL_RESULTTREE));
    return UNO.XMutableTreeNode(treeControl.getSelection());
  }

  private String getDataValueFrom(XMutableTreeNode node)
  {
    return AnyConverter.toString(node.getDataValue());
  }

  private XMutableTreeNode createNode(String dataValue, String displayValue, String imageURL) throws UnoHelperException
  {
    XControl xTreeControl = UNO.XControl(controlContainer.getControl(CONTROL_RESULTTREE));
    XControlModel xControlModel = xTreeControl.getModel();
    XMutableTreeDataModel treeNodeModel = UNO
        .XMutableTreeDataModel(UnoProperty.getProperty(xControlModel, UnoProperty.DATA_MODEL));
    XMutableTreeNode node = treeNodeModel.createNode(displayValue, false);
    node.setDataValue(dataValue);
    node.setNodeGraphicURL(imageURL);
    return node;
  }

  private void deleteChildrenFrom(XMutableTreeNode node) throws IndexOutOfBoundsException
  {
    for (int i = node.getChildCount() - 1; i >= 0; i--)
    {
      deleteChildrenFrom(UNO.XMutableTreeNode(node.getChildAt(i)));
      node.removeChildByIndex(i);
    }
  }

  private void nodeSelected(XMutableTreeNode selectedNode)
      throws IndexOutOfBoundsException, ExpandVetoException
  {
    if (selectedNode == null)
    {
      return;
    }

    UNO.XTreeControl(controlContainer.getControl(CONTROL_RESULTTREE)).expandNode(selectedNode);

    if (selectedNode.getParent() == null)
    {
      selectedNode = UNO.XMutableTreeNode(selectedNode.getChildAt(0));
    }

    UNO.XTreeControl(controlContainer.getControl(CONTROL_RESULTTREE)).expandNode(selectedNode);
    UNO.XTreeControl(controlContainer.getControl(CONTROL_RESULTTREE)).select(selectedNode);

    String id = getDataValueFrom(selectedNode);
    IfThenElseBaseModel data = model.getById(id);
    boolean isRootCondition = data.getParent() == null;
    if (data instanceof IfModel)
    {
      IfModel cond = (IfModel) data;
      UNO.XTextComponent(controlContainer.getControl(CONTROL_CBSERIENBRIEFFELD)).setText(cond.getField());
      UNO.XCheckBox(controlContainer.getControl(CONTROL_CBNOT)).setState((short) (cond.isNot() ? 1 : 0));
      UNO.XTextComponent(controlContainer.getControl(CONTROL_CBCOMPARATOR)).setText(cond.getComparator().getLabel());
      UNO.XTextComponent(controlContainer.getControl(CONTROL_CONDITION)).setText(cond.getValue());
      activeWennControls(true);
      changeText("Wenn", "Serienbrieffeld:");
    } else
    {
      UNO.XTextComponent(controlContainer.getControl(CONTROL_TXTVALUE)).setText(data.getValue());
      activeWennControls(false);
      if (data instanceof ThenModel)
      {
        changeText("Dann", "Wert:");
      } else
      {
        changeText("Sonst", "Wert:");
      }
      isRootCondition = data.getParent().getParent() == null;
    }
    UNO.XWindow(controlContainer.getControl("removeConditionBtn")).setVisible(!isRootCondition);
  }

  private void changeText(String title, String label)
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

  private void activeWennControls(boolean active)
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
}
