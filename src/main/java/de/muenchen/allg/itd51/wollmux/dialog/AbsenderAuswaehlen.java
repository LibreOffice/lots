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
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageStandardImpl.LOSDJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
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
    XControlContainer controlContainer = UNO.XControlContainer(window);

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

    dialog = UNO.XDialog(window);
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
