/*
 * Dateiname: AbsenderAuswaehlen.java
 * Projekt  : WollMux
 * Funktion : Implementiert den Absenderdaten auswählen Dialog des BKS
 *
 * Copyright (c) 2010-2019 Landeshauptstadt München
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
 * 25.10.2005 | BNK | Erstellung
 * 27.10.2005 | BNK | back + CLOSEACTION
 * 02.11.2005 | BNK | Absenderliste nicht mehr mit Vorname = M* befüllen,
 *                    weil jetzt der TestDJ schon eine Absenderliste
 *                    mit Einträgen hat.
 * 22.11.2005 | BNK | Common.setLookAndFeel() verwenden
 * 03.01.2005 | BNK | Bug korrigiert;  .gridy = x  sollte .gridx = x sein.
 * 19.05.2006 | BNK | [R1898]Wenn die Liste leer ist, dann gleich den PAL Verwalten Dialog aufrufen
 * 26.02.2010 | BED | WollMux-Icon für das Fenster
 * 08.04.2010 | BED | [R52334] Anzeige über DISPLAY-Attribut konfigurierbar
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageStandardImpl.LOSDJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPALChangedNotify;

/**
 * Diese Klasse stellt einen Dialog zum Auswählen eines Eintrages aus der Persönlichen Absenderliste
 * bereit.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1), Björn Ranft
 */
public class AbsenderAuswaehlen
{
  private static final Logger LOGGER = LoggerFactory.getLogger(AbsenderAuswaehlen.class);

  /**
   * Der DatasourceJoiner, den dieser Dialog anspricht.
   */
  private DatasourceJoiner dj;

  private List<DJDataset> elements = null;

  private XListBox absAuswahl;

  private XDialog dialog;

  /**
   * Erzeugt einen neuen Dialog.
   *
   * @param dj
   *          der DatasourceJoiner, der die PAL verwaltet.
   */
  public AbsenderAuswaehlen(DatasourceJoiner dj)
  {
    this.dj = dj;

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
    XControlContainer controlContainer = UnoRuntime.queryInterface(XControlContainer.class, window);

    absAuswahl = UNO.XListBox(controlContainer.getControl("absAuswahl"));

    XButton okBtn = UNO.XButton(controlContainer.getControl("okBtn"));
    okBtn.addActionListener(okActionListener);

    XButton editBtn = UNO.XButton(controlContainer.getControl("editBtn"));
    editBtn.addActionListener(editActionListener);

    XButton abortBtn = UNO.XButton(controlContainer.getControl("abortBtn"));
    abortBtn.addActionListener(abortActionListener);

    QueryResults palEntries = dj.getLOS();
    if (palEntries.isEmpty())
    {
      new PersoenlicheAbsenderlisteVerwalten(dj, palListener);
    } else
    {
      setListElements();
    }

    dialog = UnoRuntime.queryInterface(XDialog.class, window);
    dialog.execute();
  }

  private AbstractActionListener abortActionListener = event -> dialog.endExecute();

  private AbstractNotifier palListener = new AbstractNotifier()
  {
    @Override
    public void dialogClosed()
    {
      setListElements();
    }
  };

  private AbstractActionListener okActionListener = event -> {
    DJDataset selectedElement = elements.get(absAuswahl.getSelectedItemPos());

    if (selectedElement == null)
    {
      LOGGER.debug("AbsenderAuswaehlen: itemStateChanged: selectedDataset is NULL.");
      dialog.endExecute();
      return;
    }

    selectedElement.select();
    new OnPALChangedNotify().emit();
    dialog.endExecute();
  };

  private AbstractActionListener editActionListener = event -> {
    dialog.endExecute();
    new PersoenlicheAbsenderlisteVerwalten(dj, palListener);
  };

  private void setListElements()
  {
    elements = new ArrayList<>();

    if (absAuswahl.getItemCount() > 0)
      absAuswahl.removeItems((short) 0, absAuswahl.getItemCount());

    short itemToHightlightPos = 0;
    int count = 0;

    for (Dataset dataset : dj.getLOS())
    {
      boolean valueChanged = false;
      LOSDJDataset ds = (LOSDJDataset) dataset;

      if (ds.getLOS() != null && !ds.getLOS().isEmpty())
      {
        for (String attribute : dj.getMainDatasourceSchema())
        {
          if (ds.isDifferentFromLdapDataset(attribute, ds))
          {
            valueChanged = true;
            break;
          } else
          {
            valueChanged = false;
          }
        }

      }

      if (valueChanged)
      {
        absAuswahl.addItem("* " + ds.toString(), (short) count);
      } else
      {
        absAuswahl.addItem(ds.toString(), (short) count);
      }

      elements.add(ds);
      if (ds.isSelectedDataset())
        itemToHightlightPos = (short) count;

      count++;
    }

    absAuswahl.selectItemPos(itemToHightlightPos, true);
  }
}
