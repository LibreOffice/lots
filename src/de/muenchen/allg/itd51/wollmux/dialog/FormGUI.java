/*
 * Dateiname: FormGUI.java
 * Projekt  : WollMux
 * Funktion : managed die Fenster (Writer und FormController) der FormularGUI.
 *
 * Copyright (c) 2010-2018 Landeshauptstadt München
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
 * 27.01.2006 | BNK | Erstellung
 * 30.01.2006 | BNK | Office-Bean Einbindung
 * 31.01.2006 | BNK | Bean im Preview-Modus aufrufen
 * 01.02.2006 | BNK | etwas rumgedoktore mit LayoutManager
 * 02.02.2006 | BNK | Fenster zusammengeklebt
 * 05.05.2006 | BNK | Condition -> Function, besser kommentiert
 * 05.07.2006 | BNK | optische Verbesserungen, insbes. bzgl. arrangeWindows()
 * 19.07.2006 | BNK | mehrere übelste Hacks, damit die Formular-GUI nie unsinnige Größe annimmt beim Starten
 * 14.09.2006 | BNK | üble Hacks hoffentlich robuster gemacht
 * 17.11.2006 | BNK | +getController()
 * 26.02.2010 | BED | WollMux-Icon für FormGUI-Frame
 * 02.06.2010 | BED | +saveTempAndOpenExt
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.formmodel.SingleDocumentFormModel;

/**
 * Managed die Fenster (Writer und FormController) der FormularGUI.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormGUI
{

  private static final Logger LOGGER = LoggerFactory.getLogger(FormGUI.class);

  /**
   * Die (vom Betriebssystem gelieferten) Ränder des Formular-GUI-Fensters. Wird
   * benötigt, um die exakte Platzierung der FormGUI und des Writer-Fensters zu
   * bewerkstelligen.
   */
  private Insets windowInsets;

  /**
   * Der maximal durch ein Fenster nutzbare Bereich, d,h, Bildschirmgroesse
   * minus Taskbar undsoweiter.
   */
  private Rectangle maxWindowBounds;

  /**
   * Die Größe, die durch pack() für den Frame eingestellt wird.
   */
  private Rectangle naturalFrameBounds;

  /**
   * Das Fenster der Formular-GUI. Hier wird der FormController eingebettet.
   * Auch das Office-Bean wäre hier eingebettet worden, wenn nicht die
   * Entscheidung gegen seine Verwendung gefallen wäre.
   */
  private JFrame myFrame;

  /**
   * Das zum Formular gehörende Writer-Dokument (als FormModel gekapselt).
   */
  private SingleDocumentFormModel myDoc;

  /**
   * Ein Timer, der dafür sorgt, dass (insbesondere beim manuellen Resizen des
   * Fensters) nicht unnötig viele Resizes des OOo-Fensters durchgeführt werden.
   * Dies schadet einerseits der Stabilität von OOo und andererseits ist es ein
   * Performance-Problem.
   */
//  private WindowPosSizeSetter windowPosSizeSetter = new WindowPosSizeSetter();

  /**
   * Der Titel des Formulars (falls nicht anderweitig spezifiziert).
   */
  private String formTitle = L.m("Unbenanntes Formular");

  /**
   * Der Titel des FormularGUI-Fensters (falls nicht anderweitig spezifiziert).
   */
  private String formGUITitle = formTitle;

  /**
   * Gibt die Lage und Größe des Fensters der FormGUI an, so wie sie von
   * {@link Common#parseDimensions(ConfigThingy)} geliefert wird.
   */
  private Rectangle formGUIBounds;

  /**
   * wird getriggert bei windowClosing() Event oder für Buttons mit der ACTION "abort".
   */
  private ActionListener closeAction = e -> abort();

  /**
   * Der {@link FormController} dieser FormGUI.
   */
  private FormController formController;

  /**
   * Der Titel des FormModels, hier gespeichert, um Veränderungen zu bemerken.
   */
  private String frameTitle;

  /**
   * Zeigt eine neue Formular-GUI an.
   *
   * @param formFensterConf
   *          Der Formular-Unterabschnitt des Fenster-Abschnitts von wollmux.conf.
   * @param conf
   *          der Formular-Knoten, der die Formularbeschreibung enthält.
   * @param doc
   *          das zum Formular gehörende Writer-Dokument (gekapselt als FormModel)
   * @param mapIdToPresetValue
   *          bildet IDs von Formularfeldern auf Vorgabewerte ab. Falls hier ein Wert für ein
   *          Formularfeld vorhanden ist, so wird dieser allen anderen automatischen Befüllungen
   *          vorgezogen. Wird das Objekt {@link TextDocumentModel#FISHY} als Wert für ein Feld
   *          übergeben, so wird dieses Feld speziell markiert als ungültig bis der Benutzer es
   *          manuell ändert.
   * @param functionContext
   *          der Kontext für Funktionen, die einen benötigen.
   * @param funcLib
   *          die Funktionsbibliothek, die zur Auswertung von Plausis etc. herangezogen werden soll.
   * @param dialogLib
   *          die Dialogbibliothek, die die Dialoge bereitstellt, die für automatisch zu befüllende
   *          Formularfelder benötigt werden.
   * @param visible
   *          false zeigt an, dass die FormGUI unsichtbar bleiben soll.
   */
  public FormGUI() {}
  public FormGUI(final ConfigThingy formFensterConf, final ConfigThingy conf,
      SingleDocumentFormModel doc,
      final Map<String, String> mapIdToPresetValue, final Map<Object, Object> functionContext,
      final FunctionLibrary funcLib, final DialogLibrary dialogLib, final boolean visible)
  {
    myDoc = doc;

    formGUIBounds = Common.parseDimensions(formFensterConf);

    formTitle = conf.getString("TITLE", L.m("Unbenanntes Formular"));

    frameTitle = doc.getWindowTitle();
    if (frameTitle != null)
        formGUITitle = frameTitle + " - " + formTitle;

    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    Runnable runner = () -> {
      try
      {
        createGUI(conf, mapIdToPresetValue, functionContext, funcLib, dialogLib, visible);
      } catch (Exception x)
      {
        LOGGER.error("", x);
      }
    };
    if (SwingUtilities.isEventDispatchThread())
      runner.run();
    else
      SwingUtilities.invokeLater(runner);
  }

  private void createGUI(ConfigThingy conf,
      Map<String, String> mapIdToPresetValue,
      Map<Object, Object> functionContext, FunctionLibrary funcLib,
      DialogLibrary dialogLib, boolean visible)
  {
    Common.setLookAndFeelOnce();

    // Create and set up the window.
    myFrame = new JFrame(formGUITitle);
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    MyWindowListener oehrchen = new MyWindowListener();
    // der WindowListener sorgt dafür, dass auf windowClosing mit abort reagiert
    // wird
    myFrame.addWindowListener(oehrchen);
    // der ComponentListener sorgt dafür dass bei Verschieben/Größenänderung das
    // Writer-Fenster ebenfalls angepasst wird.
    myFrame.addComponentListener(oehrchen);

    // WollMux-Icon für das FormGUI-Fenster
    Common.setWollMuxIcon(myFrame);

    try
    {
      formController = new FormController(conf, myDoc, mapIdToPresetValue,
          functionContext, funcLib, dialogLib, closeAction);
    } catch (ConfigurationErrorException x)
    {
      LOGGER.error("", x);
      return;
    }

    myFrame.getContentPane().add(formController.JComponent());

    /*
     * Leider kann wegen
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4737732 nicht auf
     * einfache Weise die nutzbare Bildschirmflaeche bestimmt werden, deshalb
     * der folgende Hack:
     *
     * o maxWindowBounds initialisieren (so wie es eigentlich reichen sollte
     * aber unter KDE leider nicht tut) minus Sicherheitsabzug fuer KDE. Die
     * Initialisierung ist erforderlich, weil vor den folgenden Events schon
     * Events kommen können, die ein arrangeWindows() erforderlich machen. - Wir
     * registrieren einen WindowStateListener
     *
     * o Wir maximieren das Fenster
     *
     * o Sobald der Event-Handler das erfolgte Maximieren anzeigt, lesen wir die
     * Größe aus und setzen wieder auf normal.
     *
     * o Sobald das Normalsetzen beendet ist deregistriert sich der
     * WindowStateListener und die Fenster werden arrangiert.
     *
     * Bemerkung: Den Teil mit Normalsetzen kann man vermutlich entfernen, da
     * das Setzen der Fenstergröße ohnehin den maximierten Zustand verlässt.
     */

    GraphicsEnvironment genv = GraphicsEnvironment
        .getLocalGraphicsEnvironment();
    maxWindowBounds = genv.getMaximumWindowBounds();

    myFrame.pack();
    myFrame.setResizable(true);
    myFrame.setVisible(visible);

    naturalFrameBounds = myFrame.getBounds();

    /*
     * Bestimmen der Breite des Fensterrahmens.
     */
    windowInsets = myFrame.getInsets();
    
    setFormGUISizeAndLocation();
    arrangeWindows();
  }

  /**
   * Setzt Größe und Ort der FormGUI.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void setFormGUISizeAndLocation()
  {
    Rectangle frameBounds = new Rectangle(naturalFrameBounds);
    LOGGER.debug("setFormGUISizeAndLocation: frameBounds=" + frameBounds);

    switch (formGUIBounds.width)
    {
    case Common.DIMENSION_UNSPECIFIED: // natural width
      if (frameBounds.width > (0.66 * maxWindowBounds.width))
        frameBounds.width = (int) (0.66 * maxWindowBounds.width);
      break;
    case Common.DIMENSION_MAX: // max
      frameBounds.width = maxWindowBounds.width;
      break;
    default: // specified width
      frameBounds.width = formGUIBounds.width;
      break;
    }

    switch (formGUIBounds.height)
    {
    case Common.DIMENSION_UNSPECIFIED: // natural height
      break;
    case Common.DIMENSION_MAX: // max
      frameBounds.height = maxWindowBounds.height;
      break;
    default: // specified height
      frameBounds.height = formGUIBounds.height;
      break;
    }

    switch (formGUIBounds.x)
    {
    case Common.COORDINATE_CENTER: // center
      frameBounds.x = maxWindowBounds.x
          + (maxWindowBounds.width - frameBounds.width) / 2;
      break;
    case Common.COORDINATE_MAX: // max
      frameBounds.x = maxWindowBounds.x + maxWindowBounds.width
          - frameBounds.width;
      break;
    case Common.COORDINATE_MIN: // min
      frameBounds.x = maxWindowBounds.x;
      break;
    case Common.COORDINATE_UNSPECIFIED: // kein Wert angegeben
      frameBounds.x = maxWindowBounds.x;
      break;
    default: // Wert angegeben, wird nur einmal berücksichtigt.
      frameBounds.x = formGUIBounds.x;
      formGUIBounds.x = Common.COORDINATE_UNSPECIFIED;
      break;
    }

    switch (formGUIBounds.y)
    {
    case Common.COORDINATE_CENTER: // center
      frameBounds.y = maxWindowBounds.y
          + (maxWindowBounds.height - frameBounds.height) / 2;
      break;
    case Common.COORDINATE_MAX: // max
      frameBounds.y = maxWindowBounds.y + maxWindowBounds.height
          - frameBounds.height;
      break;
    case Common.COORDINATE_MIN: // min
      frameBounds.y = maxWindowBounds.y;
      break;
    case Common.COORDINATE_UNSPECIFIED: // kein Wert angegeben
      frameBounds.y = maxWindowBounds.y;
      break;
    default: // Wert angegeben, wird nur einmal berücksichtigt.
      frameBounds.y = formGUIBounds.y;
      formGUIBounds.y = Common.COORDINATE_UNSPECIFIED;
      break;
    }

    /*
     * Workaround für Bug in Java: Standardmaessig werden die
     * MaximumWindowBounds nicht berücksichtigt beim ersten Layout (jedoch
     * schon, wenn sich die Taskleiste verändert).
     */
    if (frameBounds.y + frameBounds.height > maxWindowBounds.y
        + maxWindowBounds.height)
      frameBounds.height = maxWindowBounds.y + maxWindowBounds.height
          - frameBounds.y;

    myFrame.setBounds(frameBounds);
    myFrame.validate(); // ohne diese wurde in Tests manchmal nicht neu
                        // gezeichnet
    myFrame.toFront();
  }

  /**
   * Arrangiert das Writer Fenster so, dass es neben dem Formular-Fenster sitzt.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void arrangeWindows()
  {
    Rectangle frameBounds = new Rectangle(myFrame.getBounds());
    LOGGER.debug("Maximum window bounds " + maxWindowBounds + "| window insets "
        + windowInsets + "| frame bounds " + frameBounds);

    /*
     * Das Addieren von windowInsets.left und windowInsets.right ist eine
     * Heuristik. Da sich setWindowPosSize() unter Windows und Linux anders
     * verhält, gibt es keine korrekte Methode (die mir bekannt ist), um die
     * richtige Ausrichtung zu berechnen.
     */
    int docX = frameBounds.width + frameBounds.x + windowInsets.left;
    int docWidth = maxWindowBounds.width - frameBounds.width - frameBounds.x
        - windowInsets.right;
    if (docWidth < 0)
    {
      docX = maxWindowBounds.x;
      docWidth = maxWindowBounds.width;
    }
    int docY = maxWindowBounds.y + windowInsets.top;
    /*
     * Das Subtrahieren von 2*windowInsets.bottom ist ebenfalls eine Heuristik.
     * (siehe weiter oben)
     */
    int docHeight = maxWindowBounds.y + maxWindowBounds.height - docY
        - 2 * windowInsets.bottom;

   	myDoc.setWindowPosSize(docX, docY, docWidth, docHeight);
  }

  /**
   * Ein WindowListener, der auf den JFrame registriert wird, damit als Reaktion
   * auf den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird, sowie ein
   * ComponentListener, der beim Verschieben und Verändern der Größe dafür
   * sorgt, dass das Writer-Fenster entsprechend mitverändert.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyWindowListener extends WindowAdapter
      implements ComponentListener
  {
    @Override
    public void windowActivated(WindowEvent e)
    {
      updateTitle();
    }

    private void updateTitle()
    {
      try
      {
        String frameTitle2 = myDoc.getWindowTitle();
        if (frameTitle2 != frameTitle)
        {
          frameTitle = frameTitle2;
          formGUITitle = frameTitle + " - " + formTitle;
          myFrame.setTitle(formGUITitle);
        }
      } catch (Exception x)
      {
      }
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
      closeAction.actionPerformed(null);
      ((JFrame) e.getSource()).dispose();
    }

    @Override
    public void windowDeiconified(WindowEvent e)
    {
      arrangeWindows();
    }

    @Override
    public void componentResized(ComponentEvent e)
    {
      arrangeWindows();
    }

    @Override
    public void componentMoved(ComponentEvent e)
    {
      arrangeWindows();
    }

    @Override
    public void componentShown(ComponentEvent e)
    {
    }

    @Override
    public void componentHidden(ComponentEvent e)
    {
    }
  }

  /**
   * Implementiert die gleichnamige ACTION.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    myDoc.close();
    // Achtung: Der Frame darf hier nicht disposed werden, da bei einem
    // modifizierten
    // Writer-Dokument zuerst die Sicherheitsabfrage
    // "Speichern/Verwerfen/Abbrechen?"
    // kommt und im Abbrechen-Fall die Form-GUI nicht geschlossen werden soll.
  }

  /**
   * Liefert den {@link FormController} zu dieser FormGUI.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormController getController()
  {
    return formController;
  }

  /**
   * Schliesst die FormGUI und alle zugehörigen Fenster.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void dispose()
  {
    try
    {
      javax.swing.SwingUtilities.invokeLater(() -> {
        try
        {
          myFrame.dispose();
        } catch (Exception x)
        {
        }
      });
    } catch (Exception x)
    {
    }
  }
}
