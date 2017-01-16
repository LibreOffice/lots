/*
 * Dateiname: FrameWorker.java
 * Projekt  : WollMux
 * Funktion : Frame "disablen" während im Hintergrund gearbeitet wird
 * 
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
 * 09.03.2010 | BED | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Daniel Benkmann (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.SystemColor;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Diese Klasse stellt Methoden zur Verfügung, um zeitaufwändige Tätigkeiten in einem
 * eigenen Thread zu starten und dabei den zugehörigen Frame solange zu "disablen".
 * 
 * @author Daniel Benkmann (D-III-ITD-D101)
 */
public class FrameWorker implements Runnable
{
  /**
   * Der JFrame, der während der Worker arbeitet, "disabled" wird.
   */
  private JFrame frame;

  /**
   * Um den Zustand von {@link #frame} nach Abschluss der Arbeit des Workers
   * wiederherstellen zu können, wird die alte GlassPane von {@link frame} gesichert,
   * bevor wir Veränderungen an der GlassPane machen.
   */
  private Component oldGlassPane;

  /**
   * Enthält in seiner run()-Methode die Aktivitäten, die der Worker in einem eigenen
   * Thread ausführen soll.
   */
  private Runnable runnable;

  /**
   * Gibt an, ob während der Arbeit des Workers eine ProgressBar angezeigt werden
   * soll.
   */
  private boolean showProgressBar;

  /**
   * Konstruiert einen neuen FrameWorker für den übergebenen JFrame und das
   * übergebene Runnable-Objekt. Sichert die alte GlassPane des übergebenen JFrame in
   * {@link #oldGlassPane}.
   * 
   * @param frame
   *          {@link JFrame}, der während der Worker arbeitet, "disabled" werden
   *          soll.
   * @param runnable
   *          ein {@link Runnable}-Objekt, das in seiner run()-Methode den Code
   *          enthält, den der Worker abarbeiten soll.
   * @param showProgressBar
   *          gibt an, ob während des Arbeiten des Workers eine ProgressBar angezeigt
   *          werden soll (=true) oder nicht (=false).
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  private FrameWorker(JFrame frame, Runnable runnable, boolean showProgressBar)
  {
    this.frame = frame;
    this.runnable = runnable;
    this.showProgressBar = showProgressBar;
    this.oldGlassPane = frame.getGlassPane();
  }

  /**
   * "Disabled" den übergebenen JFrame (d.h. der Mauscursor wird in dem JFrame auf
   * einen Warte-Cursor gesetzt, es wird eine semitransparente GlassPane zu dem Frame
   * hinzugefügt, die alle Mouse- und Key-Events abfängt, und es wird (optional) eine
   * indeterminierte JProgressBar eingeblendet) und führt die run()-Methode des
   * übergebenen Runnable-Objekts in einem eigenen Thread aus. Sobald das Abarbeiten
   * der run()-Methode fertig ist, wird der Frame wieder "enabled" (d.h. die oben
   * beschriebenen Änderungen wieder rückgängig gemacht).
   * 
   * @param frame
   *          der {@link JFrame}, der "disabled" werden soll, während gearbeitet wird
   * @param runnable
   *          ein {@link Runnable}-Objekt, das in seiner run()-Methode den Code
   *          enthält, der in einem eigenen Thread ausgeführt werden soll, während
   *          der frame disabled ist.
   * @param showProgressBar
   *          gibt an, ob während des Arbeitens im Frame ein (indeterminierter)
   *          {@link JProgressBar} angezeigt werden soll (=true) oder nicht (=false).
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public static void disableFrameAndWork(JFrame frame, Runnable runnable,
      boolean showProgressBar)
  {
    // Neuen FrameWorker erschaffen und in neuem Thread ausführen
    Thread thread = new Thread(new FrameWorker(frame, runnable, showProgressBar));
    thread.start(); // siehe run()-Methode von FrameWorker
  }

  /**
   * Signalisiert, dass der Worker anfängt zu arbeiten, und "disabled" entsprechend
   * den zum Worker gehörigen Frame ({@link #frame}), d.h. der Mauscursor wird auf
   * einen Warte-Cursor gesetzt, es wird eine semitransparente GlassPane (
   * {@link WaitGlassPane}) zu dem Frame hinzugefügt, die alle Mouse- und Key-Events
   * abfängt, und es wird eine (indeterminierte) JProgressBar eingeblendet, sofern
   * {@link #showProgressBar} <code>true</code> ist.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  private void startWorking()
  {
    // GUI-Änderungen im Event Dispatch Thread ausführen!
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        // Warte-Cursor setzen
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // Semitransparente GlassPane setzen, die alle Events abfängt
        WaitGlassPane glassPane = new WaitGlassPane();
        frame.setGlassPane(glassPane);

        // ProgressBar hinzufügen, falls showProgressBar == true
        if (showProgressBar)
        {
          JProgressBar progressBar = new JProgressBar();
          progressBar.setIndeterminate(true);
          progressBar.setString(L.m("Bearbeitung läuft..."));
          progressBar.setStringPainted(true);
          JPanel progressBarPanel = new JPanel();
          progressBarPanel.add(progressBar);
          glassPane.setLayout(new GridBagLayout());
          glassPane.add(progressBarPanel, new GridBagConstraints());
        }

        // GlassPane sichtbar schalten
        glassPane.setVisible(true);

        // GlassPane braucht den Focus
        // (sonst kann man sich mit KeyEvents an ihr vorbeischummeln)
        glassPane.requestFocusInWindow();
      }
    });
  }

  /**
   * Signalisiert, dass der Worker fertig ist mit Arbeiten, und "enabled"
   * entsprechend den zum Worker gehörigen Frame ({@link #frame}) wieder, d.h. der
   * Mauscursor wird wieder auf den Default-Cursor gesetzt und die alte GlassPane
   * (gesichert in {@link #oldGlassPane} wird wieder hergestellt (wodurch auch eine
   * eventuell vorhandene ProgressBar entfernt wird, da diese an der GlassPane
   * hängt).
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  private void stopWorking()
  {
    // GUI-Änderungen im Event Dispatch Thread ausführen!
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        // Visible-Status wird beim Aufruf von setGlassPane() von voriger GlassPane
        // übernommen, daher müssen wir ihn explizit wiederherstellen
        frame.getGlassPane().setVisible(oldGlassPane.isVisible());
        // Alte GlassPane wiederherstellen
        frame.setGlassPane(oldGlassPane);
        // Cursor auf Default zurücksetzen
        frame.setCursor(Cursor.getDefaultCursor());
      }
    });
  }

  /**
   * Signalisiert dem FrameWorker, dass die Arbeit beginnt (was Änderungen an
   * {@link #frame} zur Folge hat), führt dann die run()-Methode von
   * {@link #runnable} aus, und signalisiert dann dem FrameWorker, dass die Arbeit zu
   * Ende ist (wodurch wiederum Änderungen an {@link #frame} ausgelöst werden).
   * 
   * @see java.lang.Runnable#run()
   */
  public void run()
  {
    this.startWorking();
    this.runnable.run();
    this.stopWorking();
  }

  /**
   * Eine semitransparente GlassPane, die KeyEvents und MouseEvents schluckt.
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  private class WaitGlassPane extends JComponent
  {
    private static final long serialVersionUID = 3078126237943242148L;

    /**
     * Default-Konstruktor.
     */
    public WaitGlassPane()
    {
      setOpaque(false);

      // Alle MouseEvents und KeyEvents sollen von der GlassPane geschluckt werden
      EventConsumer consumer = new EventConsumer();
      addMouseListener(consumer);
      addMouseMotionListener(consumer);
      addKeyListener(consumer);

      setFocusTraversalKeysEnabled(false);
    }

    /**
     * Um eine halbtransparente Hintergrundfarbe hinzubekommen, müssen wir
     * paintComponent überschreiben.
     * 
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g)
    {
      super.paintComponent(g);
      Color c = SystemColor.textInactiveText;
      g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 128));
      g.fillRect(0, 0, getWidth(), getHeight());
    }
  }

  /**
   * Der perfekte Konsument. Hat keinen Geschmack, denkt nicht nach, sondern
   * konsumiert einfach alles (zumindest wenn es MouseEvents oder KeyEvents sind).
   * 
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  private static class EventConsumer implements KeyListener, MouseInputListener
  {

    /**
     * Konsumiert das KeyEvent ohne etwas weiteres zu tun.
     * 
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e)
    {
      e.consume();
    }

    /**
     * Konsumiert das KeyEvent ohne etwas weiteres zu tun.
     * 
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent e)
    {
      e.consume();
    }

    /**
     * Konsumiert das KeyEvent ohne etwas weiteres zu tun.
     * 
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent e)
    {
      e.consume();
    }

    /**
     * Konsumiert das MouseEvent ohne etwas weiteres zu tun.
     * 
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e)
    {
      e.consume();
    }

    /**
     * Konsumiert das MouseEvent ohne etwas weiteres zu tun.
     * 
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e)
    {
      e.consume();
    }

    /**
     * Konsumiert das MouseEvent ohne etwas weiteres zu tun.
     * 
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e)
    {
      e.consume();
    }

    /**
     * Konsumiert das MouseEvent ohne etwas weiteres zu tun.
     * 
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e)
    {
      e.consume();
    }

    /**
     * Konsumiert das MouseEvent ohne etwas weiteres zu tun.
     * 
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e)
    {
      e.consume();
    }

    /**
     * Konsumiert das MouseEvent ohne etwas weiteres zu tun.
     * 
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent e)
    {
      e.consume();
    }

    /**
     * Konsumiert das MouseEvent ohne etwas weiteres zu tun.
     * 
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent e)
    {
      e.consume();
    }
  }
}
