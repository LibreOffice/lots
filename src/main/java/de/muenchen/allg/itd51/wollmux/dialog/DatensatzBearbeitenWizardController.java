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
package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.XWindow;
import com.sun.star.ui.dialogs.XWizardController;
import com.sun.star.ui.dialogs.XWizardPage;

import de.muenchen.allg.itd51.wollmux.core.db.LocalOverrideStorageStandardImpl.LOSDJDataset;

public class DatensatzBearbeitenWizardController implements XWizardController
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(DatensatzBearbeitenWizardController.class);
  private static final int PAGE_COUNT = 3;
  private XWizardPage[] pages = new XWizardPage[PAGE_COUNT];
  private LOSDJDataset dataset;
  private List<String> dbSchema;

  protected static final short[] PATHS = { 0, 1, 2 };

  private enum PAGE_ID
  {
    PERSON,
    ORGA,
    FUSSZEILE
  }

  private String[] title = { "Person", "Orga", "Fusszeile" };

  public DatensatzBearbeitenWizardController(LOSDJDataset dataset,
      List<String> dbSchema)
  {
    this.dataset = dataset;
    this.dbSchema = dbSchema;
  }

  @Override
  public boolean canAdvance()
  {
    return true;
  }

  @Override
  public boolean confirmFinish()
  {
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
        page = new DatensatzBearbeitenPersonWizardPage(arg0, arg1, dataset, dbSchema);
        break;
        
      case ORGA:
        page = new DatensatzBearbeitenOrgaWizardPage(arg0, arg1, dataset, dbSchema);
        break;
        
      case FUSSZEILE:
        page = new DatensatzBearbeitenFusszeileWizardPage(arg0, arg1, dataset, dbSchema);
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
    pages[arg0].activatePage();
  }

  @Override
  public void onDeactivatePage(short arg0)
  {
    //not used
  }

  private PAGE_ID getPageId(short pageId)
  {
    return PAGE_ID.values()[pageId];
  }

}
