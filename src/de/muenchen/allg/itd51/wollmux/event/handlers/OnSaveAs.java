package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyValue;
import com.sun.star.ui.dialogs.FilePicker;
import com.sun.star.ui.dialogs.TemplateDescription;
import com.sun.star.ui.dialogs.XFilePicker3;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.itd51.wollmux.dialog.InfoDialog;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.DispatchHelper;

/**
 * Der Handler für einen abgespeckten Speichern-Unter-Dialog des WollMux, der in
 * Abängigkeit von einer gesetzten FilenameGeneratorFunction über den WollMux
 * aufgrufen und mit dem generierten Filenamen vorbelegt wird.
 *
 * Das Event wird über den DispatchHandler aufgerufen, wenn z.B. über das Menü
 * "Datei->SaveAs" oder über die Symbolleiste die dispatch-url .uno:Save bzw.
 * .uno:SaveAs abgesetzt wurde.
 */
public class OnSaveAs extends BasicEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnSaveAs.class);

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
    // FilenameGeneratorFunction auslesen und parsen
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
        LOGGER.error(L.m("Kann FilenameGeneratorFunction nicht parsen!"), e);
      }
    }

    // Original-Dispatch ausführen, wenn keine FilenameGeneratorFunction gesetzt
    if (func == null)
    {
      helper.dispatchOriginal();
      return;
    }

    File file = ensureFileHasODTSuffix(getDefaultFile(func));
    XFilePicker3 picker = FilePicker.createWithMode(UNO.defaultContext,
        TemplateDescription.FILESAVE_AUTOEXTENSION);
    picker.setDisplayDirectory(file.getParent());
    if (SystemUtils.IS_OS_WINDOWS)
    {
      picker.setDefaultName(file.getName());
    } else
    {
      picker.setDefaultName(file.getAbsolutePath());
    }
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
    String filename = func.getString(values);
    File f = new File(filename);
    if (f.isAbsolute())
      return f;

    try
    {
      Object ps = UNO.createUNOService("com.sun.star.util.PathSettings");
      URL dir = new URL(AnyConverter.toString(Utils.getProperty(ps, "Work")));
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
      InfoDialog.showInfoModal(L.m("Fehler beim Speichern"), L.m(
          "Das Speichern der Datei %1 ist fehlgeschlagen!", url));
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