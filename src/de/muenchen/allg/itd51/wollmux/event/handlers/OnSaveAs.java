package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyValue;
import com.sun.star.uno.AnyConverter;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.DispatchHelper;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;

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

    public OnSaveAs(TextDocumentController documentController, DispatchHelper helper)
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
      if (funcConf != null) {
        try
        {
          func = FunctionFactory.parse(funcConf, lib, null, null);
        }
        catch (ConfigurationErrorException e)
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

      boolean done = false;
      File file = ensureFileHasODTSuffix(getDefaultFile(func));
      JFileChooser fc = createODTFileChooser(file);
      while (!done)
      {
        done = true;
        if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
        {
          boolean save = true;
          File f = ensureFileHasODTSuffix(fc.getSelectedFile());

          // Sicherheitsabfage vor Überschreiben
          if (f.exists())
          {
            save = false;
            int res =
              JOptionPane.showConfirmDialog(
                null,
                L.m("Datei %1 existiert bereits. Soll sie überschrieben werden?",
                  f.getName()), L.m("Überschreiben?"),
                JOptionPane.YES_NO_CANCEL_OPTION);
            if (res == JOptionPane.NO_OPTION)
            {
              done = false;
            }
            if (res == JOptionPane.OK_OPTION)
            {
              save = true;
            }
          }

          if (save)
          {
            helper.dispatchFinished(saveAs(f));
          }
        }
      }
    }

    private File getDefaultFile(Function func)
    {
      Map<String, String> fields = documentController.getModel().getFormFieldValuesMap();
      Values.SimpleMap values = new Values.SimpleMap();
      for (String par : func.parameters())
      {
        String value = fields.get(par);
        if (value == null) value = "";
        values.put(par, value);
      }
      String filename = func.getString(values);
      File f = new File(filename);
      if (f.isAbsolute()) return f;

      try
      {
        Object ps = UNO.createUNOService("com.sun.star.util.PathSettings");
        URL dir = new URL(AnyConverter.toString(UNO.getProperty(ps, "Work")));
        return new File(dir.getPath(), filename);
      }
      catch (com.sun.star.lang.IllegalArgumentException e)
      {
        LOGGER.error("", e);
      }
      catch (MalformedURLException e)
      {
        LOGGER.error("", e);
      }
      return new File(filename);
    }

    private JFileChooser createODTFileChooser(File file)
    {
      JFileChooser fc = new JFileChooser()
      {
        private static final long serialVersionUID = 1560806929064954454L;

        // Laut Didi kommt der JFileChooser unter Windows nicht im Vordergrund.
        // Deshalb das Überschreiben der createDialog-Methode und Setzen von
        // alwaysOnTop(true)
        @Override
        protected JDialog createDialog(Component parent) throws HeadlessException
        {
          JDialog dialog = super.createDialog(parent);
          dialog.setAlwaysOnTop(true);
          return dialog;
        }
      };
      fc.setMultiSelectionEnabled(false);
      fc.setFileFilter(new FileFilter()
      {
        @Override
        public String getDescription()
        {
          return L.m("ODF Textdokument");
        }

        @Override
        public boolean accept(File f)
        {
          return f.getName().toLowerCase().endsWith(".odt") || f.isDirectory();
        }
      });
      fc.setSelectedFile(file);
      return fc;
    }

    private boolean saveAs(File f)
    {
      documentController.flushPersistentData();
      try
      {
        String url = UNO.getParsedUNOUrl(f.toURI().toURL().toString()).Complete;
        if (UNO.XStorable(documentController.getModel().doc) != null)
        {
          UNO.XStorable(documentController.getModel().doc).storeAsURL(url, new PropertyValue[] {});
        }
        return true;
      }
      catch (MalformedURLException e)
      {
        LOGGER.error(L.m("das darf nicht passieren"), e);
      }
      catch (com.sun.star.io.IOException e)
      {
        LOGGER.error("", e);
        JOptionPane.showMessageDialog(null, L.m(
          "Das Speichern der Datei %1 ist fehlgeschlagen!\n\n%2", f.toString(),
          e.getLocalizedMessage()), L.m("Fehler beim Speichern"),
          JOptionPane.ERROR_MESSAGE);
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
      return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
    }
  }