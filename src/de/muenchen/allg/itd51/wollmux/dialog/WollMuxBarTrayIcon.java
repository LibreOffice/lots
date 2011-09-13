/*
 * Dateiname: WollMuxBarTrayIcon.java
 * Projekt  : WollMux
 * Funktion : TrayIcon für die WollMuxBar
 * 
 * Copyright (c) 2011 Landeshauptstadt München
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
 * 29.05.2011 | BED | Erstellung
 * 02.09.2011 | BED | Überarbeitung; Erste eingecheckte Version
 * 03.09.2011 | BED | Focus-Probleme behoben
 * -------------------------------------------------------------------
 *
 * @author Daniel Benkmann (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import de.muenchen.allg.itd51.wollmux.L;
import de.muenchen.allg.itd51.wollmux.UnavailableException;

/**
 * Tray-Icon für die WollMuxBar.
 * 
 * @author Daniel Benkmann (D-III-ITD-D101)
 */
public class WollMuxBarTrayIcon
{
  /**
   * Das TrayIcon der WollMuxBar, das in der System Tray angezeigt wird.
   */
  private TrayIcon icon;

  /**
   * Das Kontextmenü des TrayIcons. Kann auch <code>null</code> sein, wenn es kein
   * Kontextmenü gibt.
   */
  private JPopupMenu popupMenu;

  /**
   * Programmfenster (in diesem Fall in aller Regel die WollMuxBar), das beim Klicken
   * auf das Tray-Icon wieder de-ikonifiziert und in den Vordergrund gebracht werden
   * soll. Kann auch <code>null</code> sein, wenn kein Deikonifizieren beim Klick
   * gewünscht ist!
   */
  private JFrame iconifiableFrame;

  /**
   * Der Default-Tooltip des TrayIcons, der verwendet wird, wenn kein expliziter
   * Tooltip für das WollMuxBarTrayIcon angegeben wurde.
   */
  private static final String DEFAULT_TOOLTIP = "WollMux";

  /**
   * Hilfsmethode, die versucht die Größe von TrayIcons auf dem aktuellen System
   * festzustellen und ein entsprechendes Default-Bild für die Verwendung als
   * WollMuxBar-TrayIcon zurückliefert, das von seiner Größe möglichst nahe an der
   * TrayIcon-Größe des Systems dran ist (um unschöne Bildskalierungen zu vermeiden).
   * Wird vom Klassen-Konstruktor verwendet, dem nicht explizit das Icon mitgegeben
   * wird.
   * 
   * WARNUNG: Enthält hardcodierte Pfade auf Bilddateien! Außerdem behandelt der Code
   * nicht den Fall, dass die Bilddateien nicht gefunden werden können.
   * 
   * @return ein {@link Image}-Objekt, das als TrayIcon für die WollMuxBar verwendet
   *         werden kann und von seiner Größe möglichst nahe an der richtigen Größe
   *         für TrayIcons auf dem aktuellen System dran ist.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  private static Image getRightSizedDefaultImage()
  {
    Image img =
      Toolkit.getDefaultToolkit().createImage(
        WollMuxBarTrayIcon.class.getClassLoader().getResource(
          "data/wollmux_icon32x32_gelb.png")); // 32x32 als Default

    if (SystemTray.isSupported())
    {
      int iconWidth = (int) SystemTray.getSystemTray().getTrayIconSize().getWidth();
      if (iconWidth <= 16)
      {
        img =
          Toolkit.getDefaultToolkit().createImage(
            WollMuxBarTrayIcon.class.getClassLoader().getResource(
              "data/wollmux_icon16x16_gelb.png"));
      }
      else if (iconWidth <= 24)
      {
        img =
          Toolkit.getDefaultToolkit().createImage(
            WollMuxBarTrayIcon.class.getClassLoader().getResource(
              "data/wollmux_icon24x24_gelb.png"));
      }
    }

    return img;
  }

  /**
   * Konstruiert ein WollMuxBarTrayIcon, an welches das übergebene {@link JPopupMenu}
   * als Kontextmenü angehängt wird und das beim Linksklicken den übergebenen
   * {@link JFrame} deikonifiziert und in den Vordergrund bringt sowie den Frame auf
   * das Tray-Icon ikonifiziert, wenn der Frame minimiert wird. Als Tooltip für das
   * TrayIcon wird der Default-Tooltip verwendet und als Image das Default-Image
   * (möglichst in der richtigen Auflösung).
   * 
   * @param popupMenu
   *          das Kontextmenü, welches an das TrayIcon angehängt werden soll. Wird
   *          <code>null</code> übergeben, hat das TrayIcon kein Kontextmenü.
   * @param iconifiableFrame
   *          das Programmfenster, dass beim Klicken auf das Tray-Icon deikonifiziert
   *          und in den Vordergrund gebracht werden soll. Soll kein Programmfenster
   *          deikonifiziert und in den Vordergrund gebracht werden, so kann
   *          <code>null</code> übergeben werden.
   * @param iconifyToTray
   *          gibt an, ob der übergebene <code>iconifiableFrame</code> automatisch
   *          auf die Tray iconifiziert werden soll, wenn der Frame minimiert wird.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public WollMuxBarTrayIcon(JPopupMenu popupMenu, JFrame iconifiableFrame,
      boolean iconifyToTray)
  {
    this(getRightSizedDefaultImage(), DEFAULT_TOOLTIP, popupMenu, iconifiableFrame,
      iconifyToTray);
  }

  /**
   * Konstruiert ein WollMuxBarTrayIcon, an welches das übergebene {@link JPopupMenu}
   * als Kontextmenü angehängt wird und das beim Linksklicken den übergebenen
   * {@link JFrame} deikonifiziert und in den Vordergrund bringt sowie optional den
   * Frame auf das Tray-Icon ikonifiziert, wenn der Frame minimiert wird; als Tooltip
   * wird der übergebene String verwendet und als Bild für das Tray-Icon das
   * übergebene {@link Image}-Objekt.
   * 
   * @param icon
   *          das als Icon zu verwendende Bild.
   * @param tooltip
   *          der Tooltip, der für das TrayIcon angezeigt werden soll.
   * @param popupMenu
   *          das Kontextmenü, welches an das TrayIcon angehängt werden soll. Wird
   *          <code>null</code> übergeben, hat das TrayIcon kein Kontextmenü.
   * @param iconifiableFrame
   *          das Programmfenster, dass beim Klicken auf das Tray-Icon deikonifiziert
   *          und in den Vordergrund gebracht werden soll. Soll kein Programmfenster
   *          deikonifiziert und in den Vordergrund gebracht werden, so kann
   *          <code>null</code> übergeben werden.
   * @param iconifyToTray
   *          gibt an, ob der übergebene <code>iconifiableFrame</code> automatisch
   *          auf die Tray iconifiziert werden soll, wenn der Frame minimiert wird.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public WollMuxBarTrayIcon(Image icon, String tooltip, JPopupMenu popupMenu,
      JFrame iconifiableFrame, boolean iconifyToTray)
  {
    this.popupMenu = popupMenu;
    this.iconifiableFrame = iconifiableFrame;
    this.icon = new TrayIcon(icon, tooltip, null);
    this.icon.setImageAutoSize(true);

    MyTrayIconPopupListener trayIconListener = new MyTrayIconPopupListener();
    this.icon.addMouseListener(trayIconListener);

    if (this.popupMenu != null)
    {
      this.popupMenu.addPopupMenuListener(trayIconListener);
    }

    if (iconifyToTray && this.iconifiableFrame != null)
    {
      this.iconifiableFrame.addWindowListener(new WindowAdapter()
      {
        public void windowIconified(WindowEvent e)
        {
          iconifyFrameToTray();
        }
      });
    }
  }

  /**
   * Fügt dieses WollMuxBarTrayIcon zur SystemTray hinzu (sofern die SystemTray auf
   * dem aktuellen System supportet ist).
   * 
   * @throws UnavailableException
   *           wenn die SystemTray nicht verfügbar ist.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public void addToTray() throws UnavailableException
  {
    if (!SystemTray.isSupported())
    {
      throw new UnavailableException(L.m("System Tray ist nicht verfügbar!"));
    }
    SystemTray tray = SystemTray.getSystemTray();
    try
    {
      tray.add(icon);
    }
    catch (AWTException e)
    {
      throw new UnavailableException(L.m("System Tray ist nicht verfügbar!"), e);
    }
  }

  /**
   * Entfernt dieses WollMuxBarTrayIcon aus der SystemTray.
   * 
   * @throws UnavailableException
   *           wenn die SystemTray nicht verfügbar ist.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public void removeFromTray() throws UnavailableException
  {
    if (!SystemTray.isSupported())
    {
      throw new UnavailableException(L.m("System Tray ist nicht verfügbar!"));
    }
    SystemTray tray = SystemTray.getSystemTray();
    tray.remove(icon);
  }

  /**
   * Ikonifiziert den diesem WollMuxBarTrayIcon zugewiesenen JFrame auf die Tray.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public void iconifyFrameToTray()
  {
    if (iconifiableFrame.getExtendedState() != Frame.ICONIFIED)
    {
      iconifiableFrame.setExtendedState(Frame.ICONIFIED);
    }
    iconifiableFrame.setVisible(false);
  }

  /**
   * De-Ikonifiziert den diesem WollMuxBarTrayIcon zugewiesenen JFrame von der Tray
   * und bringt ihn in den Vordergrund.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public void deiconifyFrameFromTray()
  {
    // Frame deikonifizieren (falls ikonifiziert - ansonsten lassen wir den
    // Status in Ruhe)
    if (iconifiableFrame.getExtendedState() == Frame.ICONIFIED)
    {
      iconifiableFrame.setExtendedState(Frame.NORMAL);
    }
    // Frame sichtbar machen und in den Vordergrund bringen (unabhängig davon ob er
    // vorher ikonifiziert war oder nicht)
    iconifiableFrame.setVisible(true);
    iconifiableFrame.toFront();
  }

  /**
   * Kombinierter MouseListener und PopupMenuListener, der sich bei den richtigen
   * Events darum kümmert, dass das PopupMenu angezeigt wird bzw. verschwindet und
   * die (De-)Ikonifizierung ausgelöst wird.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  private class MyTrayIconPopupListener implements MouseListener, PopupMenuListener
  {

    /**
     * Die JWindow dient als Komponente, in deren Space das Popup-Menü erscheint.
     * Wird für die show()-Methode des Popup-Menüs benötigt, brauchen wir ansonsten
     * aber für nichts und bleibt unsichtbar.
     */
    private JWindow invoker;

    /**
     * Zeigt das Kontext-Popup-Menü des TrayIcons an. Wenn kein Popup-Menü vorhanden
     * ist, tut diese Methode nichts.
     * 
     * @param e
     *          das MouseEvent, welches das Anzeigen des Popup-Menüs ausgelöst hat.
     * 
     * @author Daniel Benkmann (D-III-ITD-D101)
     */
    private void showPopup(MouseEvent e)
    {
      if (popupMenu != null)
      {
        if (invoker == null)
        {
          invoker = new JWindow(new JFrame()
          {
            // Ganz fieser Trick! JWindow muss focussierbar sein, damit unser
            // PopupMenu beim Verlust des Focus wieder korrekt gecancelt wird.
            // Allerdings ist JWindow nur dann focussierbar, wenn sein Parent
            // focussierbar und sichtbar ist. Einen JFrame (oder JDialog) als Parent
            // des JWindows darf ich aber nicht sichtbar schalten, da ich ansonsten
            // in der Taskleiste einen entsprechenden Programmeintrag bekomme und den
            // will ich ja unbedingt vermeiden (sonst hätte ich von vorneherein nicht
            // JWindow genommen). Also überschreibe ich mit folgendem Hack einfach
            // die isShowing()-Methode des Parents (einem JFrame), so dass sie mir
            // true zurückliefert, obwohl der JFrame eigentlich gar nicht angezeigt
            // wird. Da ich den JFrame ansonsten für nichts sinnvolles verwende,
            // richte ich mit dem Hack hoffentlich auch keinen Schaden an. Alles
            // ziemlich umständlich, aber nach sehr viel Trial&Error und Recherche
            // scheint es einen einfachereren Weg (der auch verlässlich funktioniert)
            // wohl nicht zu geben.
            public boolean isShowing()
            {
              return true;
            }
          });

          // JWindow noch mal explizit focussierbar machen (dank obigen Hack steht
          // dem nichts mehr im Wege)
          invoker.setFocusable(true);
        }
        invoker.setAlwaysOnTop(true);
        Dimension trayMenuSize = popupMenu.getPreferredSize();
        invoker.setLocation(e.getX(), e.getY() - trayMenuSize.height);
        invoker.setVisible(true);
        invoker.toFront(); // invoker muss Focus haben, damit Popup angezeigt wird
        popupMenu.show(invoker.getContentPane(), 0, 0);
      }
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e)
    {
    // Do nothing special
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
    {
      if (invoker != null)
      {
        invoker.dispose();
        invoker = null;
      }
    }

    public void popupMenuCanceled(PopupMenuEvent e)
    {
      if (invoker != null)
      {
        invoker.dispose();
        invoker = null;
      }
    }

    public void mouseClicked(MouseEvent e)
    {
      if (e.getButton() == MouseEvent.BUTTON1 && iconifiableFrame != null)
      {
        deiconifyFrameFromTray();
      }
    }

    public void mousePressed(MouseEvent e)
    {
    // Eigentlich wäre unter den meisten Linux-Plattformen das mousePressed-Event
    // der richtige Trigger, um ein Popup-Menü zu starten und man sollte um das
    // ganze plattformunabhängig zu machen mittels e.isPopupTrigger() sowohl bei
    // mousePressed als auch mouseReleased (dem üblichen Popup-Trigger auf
    // Windows-Plattformen) prüfen, ob das Event der Popup-Trigger der Plattform
    // ist und nur dann das Popup auslösen. Dies tun wir hier nicht, da es unter
    // Linux (zumindest Debian + Ubuntu) einen nervigen Bug gibt, der dafür sorgt,
    // dass wenn wir das Popup-Menü korrekterweise beim mousePressed-Event
    // auslösen, es manchmal(!) beim mouseReleased-Event dann gleich wieder
    // verschwindet, weil wahrscheinlich irgendwo der Focus verloren geht. Daher
    // war die einfachste Lösung, die auf allen Plattformen funktioniert, das Popup
    // konsistent beim mouseReleased-Event mit Button 2 oder 3 zu triggern - selbst
    // wenn dies nicht überall das plattformgerechte Verhalten ist.
    }

    public void mouseReleased(MouseEvent e)
    {
      // Normalerweise wäre ein "if (e.isPopupTrigger())" angebracht, aber siehe
      // Kommentar unter mousePressed.
      if (e.getButton() == MouseEvent.BUTTON2 || e.getButton() == MouseEvent.BUTTON3)
      {
        showPopup(e);
      }
    }

    public void mouseEntered(MouseEvent e)
    {
    // Do nothing
    }

    public void mouseExited(MouseEvent e)
    {
    // Do nothing
    }
  }

  /**
   * Main-Methode zum Testen der Klasse.
   * 
   * @throws MalformedURLException
   * @throws UnavailableException
   */
  public static void main(String[] args) throws MalformedURLException,
      UnavailableException
  {
    // JPopupMenu konstruieren
    JPopupMenu menu = new JPopupMenu();
    menu.add(new JMenuItem("Testeintrag 1"));
    menu.add(new JMenuItem("Testeintrag 2"));
    menu.add(new JMenuItem("Testeintrag 3"));
    menu.add(new JMenuItem("Testeintrag 1"));
    JMenu submenu = new JMenu("Untermenü 1");
    submenu.add(new JMenuItem("Untermenüeintrag 1"));
    submenu.add(new JMenuItem("Untermenüeintrag 2"));
    menu.add(submenu);

    // menu.addPropertyChangeListener(new PropertyChangeListener()
    // {
    //
    // public void propertyChange(PropertyChangeEvent evt)
    // {
    // System.out.println(evt.getPropertyName());
    // }
    // });

    // Bild für Icon
    Image img = new ImageIcon("src/data/wollmux_icon24x24_gelb.png").getImage();

    // Test-Frame
    JFrame frame = new JFrame("Dies ist ein Testframe");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    frame.getContentPane().add(new JButton("Button"));

    // Icon erstellen
    WollMuxBarTrayIcon icon =
      new WollMuxBarTrayIcon(img, "TrayIcon Test", menu, frame, true);

    icon.addToTray();

    frame.setPreferredSize(new Dimension(400, 300));
    frame.setSize(400, 300);
    frame.pack();
    frame.setVisible(true);
  }
}
