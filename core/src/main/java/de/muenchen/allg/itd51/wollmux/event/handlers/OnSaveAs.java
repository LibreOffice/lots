/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyValue;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;
import com.sun.star.uno.AnyConverter;

import org.libreoffice.ext.unohelper.common.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.dispatch.DispatchHelper;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.func.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.func.Values;
import de.muenchen.allg.itd51.wollmux.util.L;
import de.muenchen.allg.itd51.wollmux.util.Utils;
import org.libreoffice.ext.unohelper.util.UnoComponent;
import org.libreoffice.ext.unohelper.util.UnoProperty;

/**
 * If a FilenameGeneratorFunction is present the default name of the file is set.
 */
public class OnSaveAs extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnSaveAs.class);

  private TextDocumentController documentController;

  private DispatchHelper helper;

  public OnSaveAs(TextDocumentController documentController,
      DispatchHelper helper)
  {
    this.documentController = documentController;
    this.helper = helper;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    FunctionLibrary lib = documentController.getFunctionLibrary();
    ConfigThingy funcConf = documentController.getFilenameGeneratorFunc();
    Function func = null;
    if (funcConf != null)
    {
      try
      {
        func = FunctionFactory.parse(funcConf, lib, null, null);
      } catch (ConfigurationErrorException e)
      {
        LOGGER.error("Cannot parse 'FilenameGeneratorFunction'.", e);
      }
    }

    if (func == null)
    {
      helper.dispatchOriginal();
      return;
    }

    File file = ensureFileHasODTSuffix(getDefaultFile(func));
    XFilePicker3 picker = FilePicker.createWithMode(UNO.defaultContext,
        TemplateDescription.FILESAVE_AUTOEXTENSION);
    picker.setDisplayDirectory(file.getParentFile().toURI().toString());
    picker.setDefaultName(file.getName());

    String filterName = "ODF Textdokument";
    picker.appendFilter(filterName, "*.odt");
    picker.appendFilter("Alle Dateien", "*");
    picker.setCurrentFilter(filterName);
    short res = picker.execute();
    if (res == com.sun.star.ui.dialogs.ExecutableDialogResults.OK)
    {
      String[] files = picker.getFiles();
      helper.dispatchFinished(saveAs(files[0]));
    }
  }

  private File getDefaultFile(Function func)
  {
    Map<String, String> fields = documentController.getModel()
        .getFormFieldValuesMap();
    Values.SimpleMap values = new Values.SimpleMap();
    for (String par : func.parameters())
    {
      String value = fields.get(par);
      if (value == null)
        value = "";
      values.put(par, value);
    }
    String filename = func.getResult(values);
    File f = new File(filename);
    if (f.isAbsolute())
      return f;

    try
    {
      Object ps = UnoComponent.createComponentWithContext(UnoComponent.CSS_UTIL_PATH_SETTINGS);
      URL dir = new URL(AnyConverter.toString(Utils.getProperty(ps, UnoProperty.WORK)));
      return new File(dir.getPath(), filename);
    } catch (com.sun.star.lang.IllegalArgumentException
        | MalformedURLException e)
    {
      LOGGER.error("", e);
    }
    return new File(filename);
  }

  private boolean saveAs(String url)
  {
    documentController.flushPersistentData();
    try
    {
      if (UNO.XStorable(documentController.getModel().doc) != null)
      {
        UNO.XStorable(documentController.getModel().doc).storeAsURL(url,
            new PropertyValue[] {});
      }
      return true;
    } catch (com.sun.star.io.IOException e)
    {
      LOGGER.error("", e);
      InfoDialog.showInfoModal(L.m("Save Error"),
          L.m("Saving the file \"{0}\" failed!", url));
    }
    return false;
  }

  private static File ensureFileHasODTSuffix(File f)
  {
    if (f != null && !f.getName().toLowerCase().endsWith(".odt"))
    {
      return new File(f.getParent(), f.getName() + ".odt");
    }
    return f;
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(" + documentController.getModel()
        + ")";
  }
}
