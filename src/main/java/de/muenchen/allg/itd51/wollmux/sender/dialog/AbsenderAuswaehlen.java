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
package de.muenchen.allg.itd51.wollmux.sender.dialog;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialog2;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.sender.Sender;
import de.muenchen.allg.itd51.wollmux.sender.SenderService;

/**
 * Dialog for selecting a sender.
 */
public class AbsenderAuswaehlen
{
  private static final Logger LOGGER = LoggerFactory.getLogger(AbsenderAuswaehlen.class);

  private List<Sender> senderList;

  private XListBox absAuswahl;

  private XDialog2 dialog;

  private Sender selected;

  /**
   * A new dialog.
   *
   * @param senderList
   *          The list of sender.
   */
  public AbsenderAuswaehlen(List<Sender> senderList)
  {
    this.senderList = senderList;

    createUNOGUI();
  }

  private void createUNOGUI()
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

    if (provider == null)
      return;

    XWindow window = provider.createContainerWindow(
        "vnd.sun.star.script:WollMux.absender_auswahl?location=application", "", peer, null);
    XControlContainer controlContainer = UNO.XControlContainer(window);

    absAuswahl = UNO.XListBox(controlContainer.getControl("absAuswahl"));

    XButton okBtn = UNO.XButton(controlContainer.getControl("okBtn"));
    AbstractActionListener okAction = this::endDialog;
    okBtn.addActionListener(okAction);

    XButton editBtn = UNO.XButton(controlContainer.getControl("editBtn"));
    AbstractActionListener editActionListener = event -> SenderService
        .getInstance().showManageSenderListDialog(this::setListElements);
    editBtn.addActionListener(editActionListener);

    XButton abortBtn = UNO.XButton(controlContainer.getControl("abortBtn"));
    AbstractActionListener abortActionListener = event -> dialog.endExecute();
    abortBtn.addActionListener(abortActionListener);

    if (senderList.isEmpty())
    {
      SenderService.getInstance().showManageSenderListDialog(this::setListElements);
    } else
    {
      setListElements(senderList);
    }

    dialog = UNO.XDialog2(window);
  }

  private void endDialog(ActionEvent event)
  {
    Sender selectedElement = senderList.get(absAuswahl.getSelectedItemPos());

    if (selectedElement == null)
    {
      LOGGER.debug("AbsenderAuswaehlen: itemStateChanged: selectedDataset is NULL.");
      dialog.endDialog(ExecutableDialogResults.CANCEL);
      dialog.endExecute();
      return;
    }
    selected = selectedElement;
    dialog.endDialog(ExecutableDialogResults.OK);
  }

  private void setListElements(List<Sender> senderList)
  {
    this.senderList = senderList;

    if (absAuswahl.getItemCount() > 0)
      absAuswahl.removeItems((short) 0, absAuswahl.getItemCount());

    short itemToHightlightPos = 0;
    int count = 0;

    for (Sender ds : senderList)
    {

      boolean valueChanged = ds.isOverriden();
      if (valueChanged)
      {
        absAuswahl.addItem("* " + ds.getDisplayString(), (short) count);
      } else
      {
        absAuswahl.addItem(ds.getDisplayString(), (short) count);
      }

      if (ds.isSelected())
        itemToHightlightPos = (short) count;

      count++;
    }

    absAuswahl.selectItemPos(itemToHightlightPos, true);
  }

  /**
   * Start the dialog.
   *
   * @return The execution result.
   * @see XDialog#execute()
   */
  public short execute()
  {
    return dialog.execute();
  }

  public Sender getSelectedSender()
  {
    return selected;
  }
}
