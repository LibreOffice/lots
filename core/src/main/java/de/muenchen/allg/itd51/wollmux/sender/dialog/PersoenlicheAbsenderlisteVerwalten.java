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
package de.muenchen.allg.itd51.wollmux.sender.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.ActionEvent;
import com.sun.star.awt.Key;
import com.sun.star.awt.KeyEvent;
import com.sun.star.awt.MessageBoxResults;
import com.sun.star.awt.XButton;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XKeyListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.ui.dialogs.ExecutableDialogResults;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractActionListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractKeyListener;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.sender.Sender;
import de.muenchen.allg.itd51.wollmux.sender.SenderException;
import de.muenchen.allg.itd51.wollmux.sender.SenderService;
import de.muenchen.allg.itd51.wollmux.util.L;
import org.libreoffice.ext.unohelper.util.UnoComponent;

/**
 * Dialog for adding, removing, editing of senders.
 */
public class PersoenlicheAbsenderlisteVerwalten
{
  private static final Logger LOGGER = LoggerFactory.getLogger(PersoenlicheAbsenderlisteVerwalten.class);

  private List<Sender> searchSenderList = null;

  private List<Sender> senderList = new ArrayList<>();

  private XListBox searchResultList;

  private XListBox palListe;

  private XTextComponent txtFieldNachname;

  private XTextComponent txtFieldVorname;

  private XTextComponent txtFieldOrga;

  private XTextComponent txtFieldEMail;

  private XDialog dialog;

  /**
   * Create the dialog.
   *
   * @param senderList
   *          Senders that are added to the list.
   */
  public PersoenlicheAbsenderlisteVerwalten(List<Sender> senderList)
  {
    this.senderList = senderList;

    createGUI();
  }

  private void createGUI()
  {
    XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
    XContainerWindowProvider provider = UNO.XContainerWindowProvider(
        UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_CONTAINER_WINDOW_PROVIDER));

    XWindow window = provider.createContainerWindow(
        "vnd.sun.star.script:WollMux.pal_dialog?location=application", "", peer, null);

    XControlContainer controlContainer = UNO.XControlContainer(window);

    palListe = UNO.XListBox(controlContainer.getControl("palListe"));
    palListe.setMultipleMode(true);
    XKeyListener deleteSenderListener = new AbstractKeyListener()
    {

      @Override
      public void keyReleased(KeyEvent event)
      {
        if (event.KeyCode == Key.DELETE)
        {
          short[] selectedItems = palListe.getSelectedItemsPos();
          removeSender(selectedItems);
        }
      }
    };
    UNO.XWindow(palListe).addKeyListener(deleteSenderListener);

    searchResultList = UNO.XListBox(controlContainer.getControl("searchResultList"));
    searchResultList.setMultipleMode(true);

    XButton searchBtn = UNO.XButton(controlContainer.getControl("btnSearch"));
    AbstractActionListener startSearchBtnActionListener = event -> SenderService.getInstance().find(buildSearchQuery())
        .thenAccept(result -> {
          setLdapSearchResults(result);
          showInfoDialog(result);
        });
    searchBtn.addActionListener(startSearchBtnActionListener);

    XKeyListener startSearchListener = new AbstractKeyListener()
    {

      @Override
      public void keyReleased(KeyEvent event)
      {
        if (event.KeyCode == Key.RETURN)
        {
          // focus button, so that the user knows that the search has been started
          XWindow xWnd = UNO.XWindow(searchBtn);

          if (xWnd != null)
          {
            xWnd.setFocus();
            SenderService.getInstance().find(buildSearchQuery()).thenAcceptAsync(result -> {
              setLdapSearchResults(result);
              showInfoDialog(result);
            });
          }
        }
      }
    };
    txtFieldNachname = UNO.XTextComponent(controlContainer.getControl("txtNachname"));
    UNO.XWindow(txtFieldNachname).addKeyListener(startSearchListener);
    txtFieldVorname = UNO.XTextComponent(controlContainer.getControl("txtVorname"));
    UNO.XWindow(txtFieldVorname).addKeyListener(startSearchListener);
    txtFieldEMail = UNO.XTextComponent(controlContainer.getControl("txtEmail"));
    UNO.XWindow(txtFieldEMail).addKeyListener(startSearchListener);
    txtFieldOrga = UNO.XTextComponent(controlContainer.getControl("txtOrga"));
    UNO.XWindow(txtFieldOrga).addKeyListener(startSearchListener);

    XButton newBtn = UNO.XButton(controlContainer.getControl("btnNew"));
    AbstractActionListener newBtnActionListener = event -> {
      Sender newSender = SenderService.getInstance().createNewSender();

      DatensatzBearbeitenWizard wizard = new DatensatzBearbeitenWizard(newSender,
          SenderService.getInstance().getSchema());
      short result = wizard.execute();

      if (result == ExecutableDialogResults.OK)
      {
        senderList.add(newSender);
        addPalEntriesToListBox();
      }
    };
    newBtn.addActionListener(newBtnActionListener);

    XButton editBtn = UNO.XButton(controlContainer.getControl("btnEdit"));
    AbstractActionListener editBtnActionListener = event -> {
      Sender s = new Sender(senderList.get(palListe.getSelectedItemPos()));
      DatensatzBearbeitenWizard wizard = new DatensatzBearbeitenWizard(s, SenderService.getInstance().getSchema());
      short result = wizard.execute();
      if (result == ExecutableDialogResults.OK)
      {
        senderList.set(palListe.getSelectedItemPos(), s);
        addPalEntriesToListBox();
      }
    };
    editBtn.addActionListener(editBtnActionListener);

    XButton copyBtn = UNO.XButton(controlContainer.getControl("btnCopy"));
    AbstractActionListener copyBtnActionListener = event -> copyEntry();
    copyBtn.addActionListener(copyBtnActionListener);

    XButton delBtn = UNO.XButton(controlContainer.getControl("btnDel"));
    AbstractActionListener deleteBtnActionListener = event ->
    {
      short[] selectedItemsPos = palListe.getSelectedItemsPos();
      removeSender(selectedItemsPos);
    };
    delBtn.addActionListener(deleteBtnActionListener);

    XButton addToPalBtn = UNO.XButton(controlContainer.getControl("addToPALBtn"));
    AbstractActionListener addToPalActionListener = this::addAllSelectedSenderToList;
    addToPalBtn.addActionListener(addToPalActionListener);

    addPalEntriesToListBox();

    dialog = UNO.XDialog(window);
  }

  private void addPalEntriesToListBox()
  {
    short itemToHighlightPos = 0;
    int count = 0;
    palListe.removeItems((short) 0, palListe.getItemCount());

    for (Sender ds : senderList)
    {
      boolean valueChanged = ds.isOverriden();
      if (valueChanged)
      {
        palListe.addItem("* " + ds.getDisplayString(), (short) count);
      } else
      {
        palListe.addItem(ds.getDisplayString(), (short) count);
      }

      if (ds.isSelected())
        itemToHighlightPos = (short) count;

      count++;
    }

    palListe.selectItemPos(itemToHighlightPos, true);
  }

  private void removeSender(short[] selectedItems)
  {
    boolean firstIteration = true;
    int iteration = 0;
    for (short pos : selectedItems)
    {
      if (firstIteration)
      {
        palListe.removeItems(pos, (short) 1);
        senderList.remove(pos);
        firstIteration = false;
      } else
      {
        palListe.removeItems((short) (pos - 1 - iteration), (short) 1);
        senderList.remove(pos - 1 - iteration);
        iteration++;
      }
    }
  }

  private void addAllSelectedSenderToList(ActionEvent e)
  {
    short[] selectedItemsPos = searchResultList.getSelectedItemsPos();

    for (short index : selectedItemsPos)
    {
      Sender newSender = searchSenderList.get(index);
      if (senderList.stream().noneMatch(s -> s.get("OID").equals(newSender.get("OID"))))
      {
        addSenderToList(newSender);
      } else
      {
        short res = InfoDialog.showYesNoModal(L.m("Personal sender list"),
            L.m("Data record already exists in the sender list, save data record again anyway?"));

        if (res == MessageBoxResults.YES)
        {
          addSenderToList(newSender);
        }
      }
    }
    addPalEntriesToListBox();
  }

  private void addSenderToList(Sender sender)
  {
    // check if sender has a valid ID
    String oid = sender.get("OID");
    if (oid == null || oid.isEmpty() || "*".equals(oid))
    {
      InfoDialog.showInfoModal(L.m("Missing ID"),
          L.m("The record does not contain a valid OID and "
              + "therefore will not be included in the personal sender list."));
    } else
    {
      senderList.add(sender);
    }
  }

  private void copyEntry()
  {
    short[] sel = palListe.getSelectedItemsPos();

    for (short index : sel)
    {
      try
      {
        Sender datasetCopy = new Sender(senderList.get(index));
        datasetCopy.overrideValue("Rolle", L.m("Copy"));
        senderList.add(datasetCopy);
        palListe.addItem(datasetCopy.getDisplayString(), (short) (palListe.getItemCount() + 1));
      } catch (SenderException e)
      {
        InfoDialog.showInfoModal(L.m("Error copying sender"), L.m("Sender could not be copied."));
      }
    }
  }

  private void setLdapSearchResults(List<Sender> data)
  {
    if (data == null)
    {
      LOGGER.debug("PersoenlicheAbsenderlisteVeralten: setListElements: queryresults is NULL.");
      return;
    }

    searchSenderList = new ArrayList<>();

    data.forEach(item -> searchSenderList.add(item));

    Collections.sort(searchSenderList, Sender.comparatorByColumn("Nachname"));

    if (searchResultList.getItemCount() > 0)
    {
      searchResultList.removeItems((short) 0, searchResultList.getItemCount());
    }

    for (int i = 0; i < searchSenderList.size(); i++)
    {
      searchResultList.addItem(searchSenderList.get(i).getDisplayString(), (short) i);
    }
  }

  private Map<String, String> buildSearchQuery()
  {
    Map<String, String> resultSearchQuery = new HashMap<>();

    String vorname = txtFieldVorname.getText();
    if (!vorname.isEmpty())
    {
      vorname += !vorname.endsWith("*") ? "*" : "";
      resultSearchQuery.put("Vorname", vorname);
    }
    String nachname = txtFieldNachname.getText();
    if (!nachname.isEmpty())
    {
      nachname += !nachname.endsWith("*") ? "*" : "";
      resultSearchQuery.put("Nachname", nachname);
    }
    String mail = txtFieldEMail.getText();
    if (!mail.isEmpty())
    {
      mail += !mail.endsWith("*") ? "*" : "";
      resultSearchQuery.put("Mail", mail);
    }
    String orga = txtFieldOrga.getText();
    if (!orga.isEmpty())
    {
      orga += !orga.endsWith("*") ? "*" : "";
      resultSearchQuery.put("OrgaKurz", orga);
    }

    return resultSearchQuery;
  }

  private void showInfoDialog(List<Sender> ldapSearchResults)
  {
    if (ldapSearchResults == null)
    {
      InfoDialog.showInfoModal(L.m("Search timeout"),
          L.m("Processing your search query took too long and was therefore aborted.\n"
              + "The reason for this could be a problem with the data source or with the used "
              + "search term matching too many results.\n"
              + "Please try another, more precise search query."));

    } else if (ldapSearchResults.isEmpty())
    {
      InfoDialog.showInfoModal(L.m("Search results"), L.m("No results."));
    }
  }

  /**
   * Start the dialog.
   *
   * @return The result value of the dialog.
   * @see XDialog#endExecute()
   */
  public short execute()
  {
    return dialog.execute();
  }

  public List<Sender> getSenderList()
  {
    return senderList;
  }

}
