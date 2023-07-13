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
package org.libreoffice.lots.former;

import java.awt.AWTEvent;
import java.awt.Font;
import java.awt.Image;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.libreoffice.lots.WollMuxFiles;
import org.libreoffice.lots.config.ConfigThingy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enthält von den Dialogen gemeinsam genutzten Code.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class Common
{

  private static final Logger LOGGER = LoggerFactory.getLogger(Common.class);

  /**
   * Spezialwert wenn eine Breite oder Höhe die maximal sinnvolle sein soll.
   */
  public static final int DIMENSION_MAX = -1;

  /**
   * Spezialwert, wenn eine Breite oder Höhe nicht angegeben wurde.
   */
  public static final int DIMENSION_UNSPECIFIED = -2;

  /**
   * Spezialwert, wenn eine X oder Y Koordinate so gesetzt werden soll, dass das
   * Fenster in der Mitte positioniert ist.
   */
  public static final int COORDINATE_CENTER = -1;

  /**
   * Spezialwert wenn eine X oder Y Koordinate die maximal sinnvolle sein soll.
   */
  public static final int COORDINATE_MAX = -2;

  /**
   * Spezialwert wenn eine X oder Y Koordinate die minimal sinnvolle sein soll.
   */
  public static final int COORDINATE_MIN = -3;

  /**
   * Spezialwert, wenn eine X oder Y Koordinate nicht angegeben wurde.
   */
  public static final int COORDINATE_UNSPECIFIED = -4;

  /**
   * Der Unit-Increment für vertikale Scrollbars, die Zeilen von Eingabefeldern
   * enthalten, wie z,B, die Steuerelemente-Ansicht des FM4000.
   */
  private static int vertical_scrollbar_unit_increment = 12;

  /**
   * Gibt an ob {@link #setLookAndFeel()} bereits aufgerufen wurde.
   */
  private static boolean lafSet = false;

  private static boolean isPopupVisible = false;

  /**
   * Speichert die ursprünglichen Fontgrößen.
   */
  private static HashMap<Object, Float> defaultFontsizes;

  private Common()
  {
    // hide public constructor
  }

  /**
   * Führt {@link #setLookAndFeel()} aus, aber nur, wenn es bisher noch nicht
   * ausgeführt wurde.
   */
  public static void setLookAndFeelOnce()
  {
    if (!lafSet) {
      setLookAndFeel();
    }
  }

  /**
   * Setzt, ob gerade ein Popup angezeigt wird.
   *
   * @param isPopupVisible
   */
  public static void setIsPopupVisible(boolean isPopupVisible) {
    Common.isPopupVisible = isPopupVisible;
  }

  /**
   * Setzt das Metal Look and Feel und ruft {@link #configureTextFieldBehaviour()}
   * auf. Das plattformspezifische LAF wird nicht verwendet, damit die Benutzer unter
   * Windows und Linux eine einheitliche Optik haben, so dass a) Schulungsvideos und
   * Unterlagen für beide Plattformen anwendbar sind und b) Benutzer sich bei der
   * Umstellung von Windows auf Linux nicht noch beim WollMux umgewöhnen müssen. Des
   * weiteren hatte zumindest als wir angefangen haben das GTK Look and Feel einige
   * Bugs. Es ist also auch ein Problem, dass wir nicht genug Ressourcen haben, um 2
   * Plattformen diesbzgl. zu testen und zu debuggen.
   *
   * Als Kompromiss ist es möglich das zu verwendende LAF über die
   * Konfiguration vorzugeben.
   *
   * alt: Setzt das System Look and Feel, falls es nicht MetalLookAndFeel ist.
   * Ansonsten setzt es GTKLookAndFeel falls möglich.
   */
  private static void setLookAndFeel()
  {
    LOGGER.debug("setLookAndFeel");

    // Das Standard-LAF für den WollMux
    String lafName = "javax.swing.plaf.metal.MetalLookAndFeel";
    try
    {

      try
      {

        // Ist der Konfig-Parameter "LAF_CLASS_NAME" gesetzt, wird das angegebene
        // LAF verwendet.
        ConfigThingy config = WollMuxFiles.getWollmuxConf();
        ConfigThingy lafConf = config.get( "LAF_CLASS_NAME" );
        lafName = lafConf.toString();

      } // try
      catch ( Exception e )
      {
        LOGGER.trace("", e);
      } // catch

      UIManager.setLookAndFeel(lafName);
    }
    catch (Exception x)
    {
      LOGGER.trace("", x);
    }

    // JFrame.setDefaultLookAndFeelDecorated(true); seems to cause problems with
    // undecorated windows in Metal LAF

    // configure behaviour of JTextFields:
    configureTextFieldBehaviour();

    defaultFontsizes = new HashMap<>();
    UIDefaults def = UIManager.getLookAndFeelDefaults();
    Enumeration<Object> keys = def.keys();
    float fontSize;
    Object key;
    Font font;
    while (keys.hasMoreElements()){
      key = keys.nextElement();
      font = def.getFont(key);
      if (font != null){
        fontSize = font.getSize();
        defaultFontsizes.put(key, fontSize);
      }
    }
    lafSet = true;
  }

  /**
   * Konfiguriert das Verhalten von JTextFields, die in der GUI der Applikation
   * benutzt werden, so dass es dem erwarteten Standard-Verhalten von Textfeldern
   * entspricht - nämlich dass eventuell vorhandener Text im Textfeld automatisch
   * selektiert wird, wenn mit der Tabulator-Taste (nicht aber mit der Maus) in das
   * Feld gewechselt wird, so dass man einfach lostippen kann um den Inhalt zu
   * überschreiben.
   *
   * Dazu installieren wir im aktuellen {@link KeyboardFocusManager} einen
   * {@link KeyEventPostProcessor}, der beim Loslassen ({@link KeyEvent#KEY_RELEASED}
   * ) der Tabulator-Taste überprüft, ob das KeyEvent von einem JTextField ausgelöst
   * wurde und in diesem Fall allen Text in dem Textfeld selektiert.
   *
   * Sollte irgendwann mal RFE 4493590
   * (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4493590) umgesetzt werden,
   * kann man das ganze vielleicht besser lösen.
   *
   * Außerdem wird ein Swing-Problem korrigiert, durch das es vorkommen kann, dass in
   * einem JTextField selektierter Text auch nachdem des Textfeld den Focus verloren
   * hat noch als selektiert angezeigt wird.
   */
  private static void configureTextFieldBehaviour()
  {
    // Hier wird konfiguriert das rechts Mouse click in ein TextField. Das Mouse listener
    // ist als AWTEventListener implementiert um eine globaler event listener zu haben,
    // er reagiert an eine event in alle Komponente.
    Toolkit.getDefaultToolkit().addAWTEventListener(new ContextMenuMouseListener(), AWTEvent.MOUSE_EVENT_MASK);

    KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();

    // Wir fügen dem aktuellen KeyboardFocusManager einen
    // KeyEventPostProcessor hinzu, der beim Loslassen (KeyEvent.KEY_RELEASED)
    // der Tabulator-Taste überprüft, ob das KeyEvent von einem JTextField ausgelöst
    // wurde und in diesem Fall allen Text in dem Textfeld selektiert.
    kfm.addKeyEventPostProcessor(e -> {
      if (e.getKeyCode() == KeyEvent.VK_TAB && e.getID() == KeyEvent.KEY_RELEASED
          && e.getComponent() instanceof JTextField)
      {
        JTextField textField = (JTextField) e.getComponent();
        textField.selectAll();
      }
      return false;
    });

    // Wir melden am aktuellen KeyboardFocusManager einen PropertyChangeListener an,
    // der bemerkt, ob ein JTextField den Focus verloren hat und dessen Selektion
    // löscht.
    // Das darf nicht passieren wenn in ein JTextField das recht Mouse Taste geklickt ist
    // und ein PopUp Menu angezeigt wird.
    kfm.addPropertyChangeListener("focusOwner", evt -> {
      if (evt.getOldValue() instanceof JTextField && !isPopupVisible)
      {
        JTextField textField = (JTextField) evt.getOldValue();

        // Aufruf von setCaretPosition löscht die Selektion.
        textField.setCaretPosition(textField.getCaretPosition());
      }
    });
  }

  /**
   * Wertet die FONT_ZOOM-Direktive des Dialoge-Abschnitts aus und zoomt die Fonts falls
   * erforderlich.
   *
   * @param config
   *          a valid ConfigThingy configuration.
   * @return {@link Double} returns Zoom factor value.
   */
  public static double getFontZoomFactor(ConfigThingy config)
  {
    if (config == null)
    {
      LOGGER.debug(
          "Common: getFontZoomFactor(): ConfigThingy is NULL. Returning with default value 1 for zoom.");
      return 1;
    }

    Common.setLookAndFeelOnce();

    ConfigThingy zoom = config.query("Dialoge").query("FONT_ZOOM", 2);
    if (zoom.count() > 0)
    {
      try
      {
        return Double.parseDouble(zoom.getLastChild().toString());
      } catch (Exception x)
      {
        LOGGER.error("", x);
      }
    }

    return 1;
  }

  /**
   * Multipliziert alle Font-Größen mit zoomFactor. ACHTUNG! Nach jedem Aufruf von setLookAndFeel()
   * kann diese Funktion genau einmal verwendet werden und hat in folgenden Aufrufen keine Wirkung
   * mehr, bis wieder setLookAndFeel() aufgerufen wird (was den Zoom wieder zurücksetzt).
   *
   * @param zoomFactor
   *          Value for font zoom.
   */
  public static void zoomFonts(double zoomFactor)
  {
    if (zoomFactor < 0.5 || zoomFactor > 3)
    {
      LOGGER.error("Invalid FONT_ZOOM Value: {} . Zoom has been set to default value.", zoomFactor);

      return;
    }

    LOGGER.debug("zoomFonts({})", zoomFactor);
    UIDefaults def = UIManager.getLookAndFeelDefaults();
    Set<Map.Entry<Object, Float>> mappings;
    mappings = defaultFontsizes.entrySet();
    Iterator<Entry<Object, Float>> mappingEntries = mappings.iterator();
    Entry<Object,Float> mappingEntry;
    Object key;
    Float size;
    Font oldFnt;
    Font newFont;
    int changedFonts = 0;
    while (mappingEntries.hasNext())
    {
      try
      {
        mappingEntry = mappingEntries.next();
        key = mappingEntry.getKey();
        size = mappingEntry.getValue();
        oldFnt = def.getFont(key);
        if (oldFnt != null) {
          newFont = oldFnt.deriveFont((float)(size * zoomFactor));
          def.put(key, newFont);
          ++changedFonts;
        }
      }
      catch (Exception ex)
      {
        LOGGER.debug("", ex);
      }
    }

    LOGGER.debug("{} Fontgrößen verändert!", changedFonts);
  }

  public static int getVerticalScrollbarUnitIncrement()
  {
    return vertical_scrollbar_unit_increment;
  }

  /**
   * Parst WIDTH, HEIGHT, X und Y aus fensterConf und liefert ein entsprechendes
   * Rectangle. Spezialwerte wie {@link #COORDINATE_CENTER} und
   * {@link #DIMENSION_MAX} werden verwendet.
   */
  public static Rectangle parseDimensions(ConfigThingy fensterConf)
  {
    Rectangle r = new Rectangle();
    r.x = COORDINATE_UNSPECIFIED;

    String xStr = fensterConf.getString("X", "0");
    if ("center".equalsIgnoreCase(xStr))
    {
      r.x = COORDINATE_CENTER;
    }
    else if ("max".equalsIgnoreCase(xStr))
    {
      r.x = COORDINATE_MAX;
    }
    else if ("min".equalsIgnoreCase(xStr))
    {
      r.x = COORDINATE_MIN;
    }
    else if ("auto".equalsIgnoreCase(xStr))
    {
      /* nothing */}
    else
    {
      r.x = Integer.parseInt(xStr);
      // Ja, das folgende ist eine Einschränkung, aber
      // negative Koordinaten gehen in KDE eh nicht und kollidieren mit
      // obigen Festlegungen
      if (r.x < 0)
      {
        r.x = 0;
      }
    }

    r.y = COORDINATE_UNSPECIFIED;
    String yStr = fensterConf.getString("Y", "0");

    if ("center".equalsIgnoreCase(yStr))
    {
      r.y = COORDINATE_CENTER;
    }
    else if ("max".equalsIgnoreCase(yStr))
    {
      r.y = COORDINATE_MAX;
    }
    else if ("min".equalsIgnoreCase(yStr))
    {
      r.y = COORDINATE_MIN;
    }
    else if ("auto".equalsIgnoreCase(yStr))
    {
      /* nothing */}
    else
    {
      r.y = Integer.parseInt(yStr);
      // Ja, das folgende ist eine Einschränkung, aber
      // negative Koordinaten gehen in KDE eh nicht und kollidieren mit
      // obigen Festlegungen
      if (r.y < 0)
      {
        r.y = 0;
      }
    }

    r.width = DIMENSION_UNSPECIFIED;
    String widthStr = fensterConf.getString("WIDTH", "-2");

    if ("max".equalsIgnoreCase(widthStr))
    {
      r.width = DIMENSION_MAX;
    }
    else if ("auto".equalsIgnoreCase(widthStr))
    {
      /* nothing */
    }
    else
    {
      r.width = Integer.parseInt(widthStr);
      if (r.width < 0)
      {
        r.width = DIMENSION_UNSPECIFIED;
      }
    }

    r.height = DIMENSION_UNSPECIFIED;
    String heightStr = fensterConf.getString("HEIGHT", "-2");

    if ("max".equalsIgnoreCase(heightStr))
    {
      r.height = DIMENSION_MAX;
    }
    else if ("auto".equalsIgnoreCase(heightStr))
    {
      /* nothing */
    }
    else
    {
      r.height = Integer.parseInt(heightStr);
      if (r.height < 0)
      {
        r.height = DIMENSION_UNSPECIFIED;
      }
    }

    return r;
  }

  /**
   * Sets the icon of the passed in JFrame to the WollMux icon.
   *
   * @param myFrame
   *          JFrame which should get the WollMux icon
   */
  public static void setWollMuxIcon(JFrame myFrame)
  {
    try
    {
      URL url = Common.class.getClassLoader().getResource("wollmux_icon32x32.png");

      if (url == null) {
        LOGGER.error("Could not retrieve Image from resource.");
    	
        return;
      }

      Image image = Toolkit.getDefaultToolkit().createImage(url);
      myFrame.setIconImage(image);
    }
    catch (Throwable e)
    {
    	LOGGER.error("", e);
    }
  }
}
