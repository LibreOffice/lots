package de.muenchen.allg.itd51.wollmux.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XUnoControlDialog;
import com.sun.star.awt.XWindow;
import com.sun.star.container.XNameContainer;
import com.sun.star.awt.XTextComponent;
import com.sun.star.ui.XUIElement;
import com.sun.star.ui.dialogs.XWizardController;
import com.sun.star.ui.dialogs.XWizardPage;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;

public class DatensatzBearbeitenWizardController implements XWizardController
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(DatensatzBearbeitenWizardController.class);
  private static final int PAGE_COUNT = 3;
  private PAGE_ID currentPage = PAGE_ID.PERSON;
  private XWizardPage[] pages = new XWizardPage[PAGE_COUNT];
  private DJDataset dataset;

  public static final short[] PATHS = { 0, 1, 2 };

  private enum PAGE_ID
  {
    PERSON,
    ORGA,
    FUSSZEILE
  }

  private String[] title = { "Person", "Orga", "Fusszeile" };

  public DatensatzBearbeitenWizardController(DJDataset dataset)
  {
    this.dataset = dataset;
  }

  @Override
  public boolean canAdvance()
  {
    return true;
  }

  @Override
  public boolean confirmFinish()
  {
    XWizardPage page = pages[currentPage.ordinal()];
    XControlContainer controlContainer = UnoRuntime.queryInterface(XControlContainer.class,
        page.getWindow());

    switch (currentPage)
    {
    case PERSON:

      try
      {
        XTextComponent rolle = UnoRuntime.queryInterface(XTextComponent.class,
            controlContainer.getControl("txtRolle"));
        XComboBox anrede = UnoRuntime.queryInterface(XComboBox.class,
            controlContainer.getControl("cbAnrede"));
        XTextComponent vorname = UNO.XTextComponent(controlContainer.getControl("txtVorname"));
        XTextComponent nachname = UNO.XTextComponent(controlContainer.getControl("txtNachname"));
        XTextComponent titel = UNO.XTextComponent(controlContainer.getControl("txtTitel"));
        XTextComponent tel = UNO.XTextComponent(controlContainer.getControl("txtTel"));
        XTextComponent fax = UNO.XTextComponent(controlContainer.getControl("txtFax"));
        XTextComponent zimmerNr = UNO.XTextComponent(controlContainer.getControl("txtZimmerNr"));
        XTextComponent str = UNO.XTextComponent(controlContainer.getControl("txtStr"));
        XTextComponent dienstgebPLZ = UNO
            .XTextComponent(controlContainer.getControl("txtDienstgebPLZ"));
        XTextComponent dienstgebOrt = UNO
            .XTextComponent(controlContainer.getControl("txtDienstgebOrt"));
        XTextComponent email = UNO.XTextComponent(controlContainer.getControl("txtEmail"));
        XTextComponent zustaendigkeit = UNO
            .XTextComponent(controlContainer.getControl("txtZustaendigkeit"));
        XTextComponent funktion = UNO.XTextComponent(controlContainer.getControl("txtFunktion"));
        XTextComponent dienstBezKurz = UNO
            .XTextComponent(controlContainer.getControl("txtDienstBezKurz"));
        XTextComponent dienstBezLang = UNO
            .XTextComponent(controlContainer.getControl("txtDienstBezLang"));

        dataset.set("Rolle", rolle.getText());
        dataset.set("Anrede", anrede.getItems()[0]); //!
        dataset.set("Vorname", vorname.getText());
        dataset.set("Nachname", nachname.getText());
        dataset.set("Titel", titel.getText());
        dataset.set("Telefon", tel.getText());
        dataset.set("Mail", email.getText());
        dataset.set("Fax", fax.getText());
        dataset.set("Zimmer", zimmerNr.getText());
        dataset.set("Dienstgebaeude", str.getText());
        dataset.set("DienstgebaeudePLZ", dienstgebPLZ.getText());
        dataset.set("DienstgebaeudeOrt", dienstgebOrt.getText());
        dataset.set("Zustaendigkeit", zustaendigkeit.getText());
        dataset.set("Funktion", funktion.getText());
        dataset.set("DienstBezKurz", dienstBezKurz.getText());
        // dienstBezLang in datenquellen.conf?
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }

      break;

    case ORGA:
      try
      {
        XTextComponent orgraReferat = UNO.XTextComponent(controlContainer.getControl("txtReferat"));
        XTextComponent orgaKurz = UNO.XTextComponent(controlContainer.getControl("txtOrgaKurz"));
        XTextComponent orgaLang = UNO.XTextComponent(controlContainer.getControl("txtOrgaLang"));
        XTextComponent orgaDienststelle = UNO
            .XTextComponent(controlContainer.getControl("txtDienststelle"));
        XTextComponent orgaStr = UNO.XTextComponent(controlContainer.getControl("txtStr"));
        XTextComponent orgaPLZ = UNO.XTextComponent(controlContainer.getControl("txtPLZ"));
        XTextComponent orgaOrt = UNO.XTextComponent(controlContainer.getControl("txtOrt"));
        XTextComponent orgaEMail = UNO.XTextComponent(controlContainer.getControl("txtEMail"));
        XTextComponent orgaTelefon = UNO.XTextComponent(controlContainer.getControl("txtTelefon"));
        XTextComponent orgaFax = UNO.XTextComponent(controlContainer.getControl("txtFax"));

        dataset.set("Referat", orgraReferat.getText());
        dataset.set("OrgaKurz", orgaKurz.getText());
        dataset.set("OrgaLang", orgaLang.getText());
        dataset.set("Dienstgebaeude", orgaDienststelle.getText()); // ?
        dataset.set("Postanschrift", orgaStr.getText()); // ?
        dataset.set("PostPLZ", orgaPLZ.getText());
        dataset.set("PostOrt", orgaOrt.getText());
        dataset.set("OrgaEmail", orgaEMail.getText());
        dataset.set("OrgaTelefon", orgaTelefon.getText());
        dataset.set("OrgaFax", orgaFax.getText());
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }
      break;

    case FUSSZEILE:
      try
      {
        XTextComponent txtSpalte1Teil1 = UNO
            .XTextComponent(controlContainer.getControl("txtSpalte1Teil1"));
        XTextComponent txtSpalte1Teil2 = UNO
            .XTextComponent(controlContainer.getControl("txtSpalte1Teil2"));
        XTextComponent txtSpalte2Teil1 = UNO
            .XTextComponent(controlContainer.getControl("txtSpalte2Teil1"));
        XTextComponent txtSpalte2Teil2 = UNO
            .XTextComponent(controlContainer.getControl("txtSpalte2Teil2"));
        XTextComponent txtSpalte3Teil1 = UNO
            .XTextComponent(controlContainer.getControl("txtSpalte3Teil1"));
        XTextComponent txtSpalte3Teil2 = UNO
            .XTextComponent(controlContainer.getControl("txtSpalte3Teil2"));
        XTextComponent txtSpalte4Teil1 = UNO
            .XTextComponent(controlContainer.getControl("txtSpalte4Teil1"));
        XTextComponent txtSpalte4Teil2 = UNO
            .XTextComponent(controlContainer.getControl("txtSpalte4Teil2"));

        dataset.set("FussSpalte1", txtSpalte1Teil1.getText());
        dataset.set("FussSpalte12", txtSpalte1Teil2.getText());
        dataset.set("FussSpalte2", txtSpalte2Teil1.getText());
        dataset.set("FussSpalte22", txtSpalte2Teil2.getText());
        dataset.set("FussSpalte3", txtSpalte3Teil1.getText());
        dataset.set("FussSpalte32", txtSpalte3Teil2.getText());
        dataset.set("FussSpalte4", txtSpalte4Teil1.getText());
        dataset.set("FussSpalte42", txtSpalte4Teil2.getText());
      } catch (ColumnNotFoundException e)
      {
        LOGGER.error("", e);
      }

      break;
    }

    return true;
  }

  @Override
  public XWizardPage createPage(XWindow arg0, short arg1)
  {
    arg0.setPosSize(0, 0, 1000, 800, PosSize.POSSIZE);
    LOGGER.debug("createPage");
    XWizardPage page = null;
    try
    {
      switch (getPageId(arg1))
      {
      case PERSON:
        page = new DatensatzBearbeitenPersonWizardPage(arg0, arg1);
        XControlContainer controlContainerPerson = UnoRuntime.queryInterface(XControlContainer.class,
            page.getWindow());
        UNO.XTextComponent(controlContainerPerson.getControl("txtRolle")).setText(dataset.get("Rolle"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtRolle")).setText(dataset.get("Anrede"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtEmail")).setText(dataset.get("Mail"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtVorname")).setText(dataset.get("Vorname"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtNachname")).setText(dataset.get("Nachname"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtTitel")).setText(dataset.get("Titel"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtTel")).setText(dataset.get("Telefon"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtFax")).setText(dataset.get("Fax"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtZimmerNr")).setText(dataset.get("Zimmer"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtStr")).setText(dataset.get("Postanschrift"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtDienstgebPLZ")).setText(dataset.get("PostPLZ"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtDienstgebOrt")).setText(dataset.get("PostOrt"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtZustaendigkeit")).setText(dataset.get("Zustaendigkeit"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtFunktion")).setText(dataset.get("Funktion"));
        UNO.XTextComponent(controlContainerPerson.getControl("txtDienstBezKurz")).setText(dataset.get("DienstBezKurz"));
        
        break;
      case ORGA:
        page = new DatensatzBearbeitenOrgaWizardPage(arg0, arg1);
        XControlContainer controlContainerOrga = UnoRuntime.queryInterface(XControlContainer.class,
            page.getWindow());
        UNO.XTextComponent(controlContainerOrga.getControl("txtReferat")).setText(dataset.get("Referat"));
        UNO.XTextComponent(controlContainerOrga.getControl("txtOrgaKurz")).setText(dataset.get("OrgaKurz"));
        UNO.XTextComponent(controlContainerOrga.getControl("txtOrgaLang")).setText(dataset.get("OrgaLang"));
        UNO.XTextComponent(controlContainerOrga.getControl("txtDienststelle")).setText(dataset.get("OrgaName"));
        UNO.XTextComponent(controlContainerOrga.getControl("txtStr")).setText(dataset.get("Dienstgebaeude"));
        UNO.XTextComponent(controlContainerOrga.getControl("txtPLZ")).setText(dataset.get("DienstgebaeudePLZ"));
        UNO.XTextComponent(controlContainerOrga.getControl("txtOrt")).setText(dataset.get("DienstgebaeudeOrt"));
        UNO.XTextComponent(controlContainerOrga.getControl("txtEMail")).setText(dataset.get("OrgaEmail"));
        UNO.XTextComponent(controlContainerOrga.getControl("txtTelefon")).setText(dataset.get("OrgaTelefon"));
        UNO.XTextComponent(controlContainerOrga.getControl("txtFax")).setText(dataset.get("OrgaFax"));
        
        break;
      case FUSSZEILE:
        page = new DatensatzBearbeitenFusszeileWizardPage(arg0, arg1);
        XControlContainer controlContainerFusszeile = UnoRuntime.queryInterface(XControlContainer.class,
            page.getWindow());
        UNO.XTextComponent(controlContainerFusszeile.getControl("txtSpalte1Teil1")).setText(dataset.get("FussSpalte1"));
        UNO.XTextComponent(controlContainerFusszeile.getControl("txtSpalte1Teil2")).setText(dataset.get("FussSpalte12"));
        UNO.XTextComponent(controlContainerFusszeile.getControl("txtSpalte2Teil1")).setText(dataset.get("FussSpalte2"));
        UNO.XTextComponent(controlContainerFusszeile.getControl("txtSpalte2Teil2")).setText(dataset.get("FussSpalte22"));
        UNO.XTextComponent(controlContainerFusszeile.getControl("txtSpalte3Teil1")).setText(dataset.get("FussSpalte3"));
        UNO.XTextComponent(controlContainerFusszeile.getControl("txtSpalte3Teil2")).setText(dataset.get("FussSpalte32"));
        UNO.XTextComponent(controlContainerFusszeile.getControl("txtSpalte4Teil1")).setText(dataset.get("FussSpalte4"));
        UNO.XTextComponent(controlContainerFusszeile.getControl("txtSpalte4Teil2")).setText(dataset.get("FussSpalte42"));
        
        break;
      }
      pages[arg1] = page;
    } catch (Exception ex)
    {
      LOGGER.error("Page {} konnte nicht erstellt werden", arg1);
      LOGGER.error("", ex);
    }
    return page;
  }

  @Override
  public String getPageTitle(short arg0)
  {
    return title[arg0];
  }

  @Override
  public void onActivatePage(short arg0)
  {
    currentPage = getPageId(arg0);
    pages[arg0].activatePage();
  }

  @Override
  public void onDeactivatePage(short arg0)
  {
    // TODO Auto-generated method stub

  }

  private PAGE_ID getPageId(short pageId)
  {
    return PAGE_ID.values()[pageId];
  }
}
