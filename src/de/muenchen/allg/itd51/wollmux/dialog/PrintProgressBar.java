/*
 * Dateiname: PrintProgressBar.java
 * Projekt  : WollMux
 * Funktion : Implementiert eine Fortschrittsanzeige für den WollMux-Komfortdruck
 *
 * Copyright (c) 2010-2019 Landeshauptstadt München
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
 * 04.07.2008 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XProgressBar;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Diese Klasse repräsentiert eine Fortschrittsanzeige für den WollMux-Komfortdruck, die damit
 * zurecht kommt, dass potentiell mehrere Druckfunktionen hintereinander geschaltet sind und die
 * Zahl insgesamt zu druckender Versionen sich daher aus der Multiplikation der Werte der einzelnen
 * Druckfunktionen ergibt. Im Fall, dass mehrere Druckfunktionen ihren Status an die
 * PrintProgressBar berichten, erfolgt auch z.B. eine Anzeige des Druckstatus in der Form "3 von 10
 * (=2x5)", aus der hervorgeht, dass zwei Druckfunktionen beteiligt sind, von denen die eine 2
 * Versionen und die andere 5 Versionen erstellen wird.
 *
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class PrintProgressBar
{

  private static final Logger LOGGER = LoggerFactory.getLogger(PrintProgressBar.class);

  /**
   * Enthält eine Zuordnung eines Schlüssels, der eine Komfortdruckfunktion repräsentiert, auf den
   * Maximalwert von dieser Komfortdruckfunktion zu erwartender Versionen.
   */
  private HashMap<Object, Integer> maxValues;

  /**
   * Enthält eine Zuordnung eines Schlüssels, der eine Komfortdruckfunktion repräsentiert, auf den
   * Bearbeitungsstatus der entsprechenden Komfortdruckfunktion. Für den Bearbeitungsstatus gilt: 0
   * <= currentValue <= maxValue.
   */
  private HashMap<Object, Integer> currentValues;

  /**
   * Enthält die Schlüssel der gerade aktiven Komfortdruckfunktionen in zeitlich gesehen umgekehrter
   * Registrierungsreihenfolge. D.h. ein maxValue, der zu einem späteren Zeitpunkt mittels
   * setMaxValue(key, maxValue) registriert wird, wird immer am Anfang der LinkedList eingefügt.
   */
  private LinkedList<Object> order;

  /**
   * Enthält den Listener der aufgerufen wird, wenn der "X"- oder der "Abbrechen"-Button betätigt
   * wurde.
   */
  private ActionListener abortListener;

  private XWindow window;

  /**
   * Enthält die ProgressBar zur Darstellung des Gesamtdruckverlaufs
   */
  private XProgressBar progressBar;

  /**
   * Enthält das Label mit dem Statustext (z.B. "3 von 10 (=2x5)")
   */
  private XFixedText statusLabel;

  private boolean finished = false;

  /**
   * Erzeugt ein neues PrintProgressBar-Objekt und zeigt das entsprechende Fenster mit der
   * Verlaufsinformation sofort sichtbar an.
   *
   * @param title
   *          Enthält den initial in der Titel-Leiste anzuzeigenden Titel
   * @param abortListener
   *          Der abortListener wird informiert, wenn der "X"-Button oder der "Abbrechen"-Knopf des
   *          Fensters betätigt wurde.
   */
  public PrintProgressBar(final String title, ActionListener abortListener)
  {
    this.order = new LinkedList<>();
    this.maxValues = new HashMap<>();
    this.currentValues = new HashMap<>();
    this.abortListener = abortListener;
    createGui();
    setMessage(title);
  }

  /**
   * Erzeugt das Fenster und alle enthaltenen Elemente und schaltet es sichtbar.
   */
  private void createGui()
  {
    XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());

    try
    {
      XContainerWindowProvider provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));

      window = provider.createContainerWindow(
          "vnd.sun.star.script:WollMux.print_progress?location=application", "", peer, null);
      window.addWindowListener(new AbstractWindowListener()
      {
        @Override
        public void disposing(EventObject event)
        {
          cancel();
        }
      });
      XControlContainer controlContainer = UnoRuntime.queryInterface(XControlContainer.class, window);

      progressBar = UnoRuntime.queryInterface(XProgressBar.class, controlContainer.getControl("progress"));
      statusLabel = UNO.XFixedText(controlContainer.getControl("progressText"));
      UNO.XButton(controlContainer.getControl("abort")).addActionListener(new AbstractActionListener()
      {
        @Override
        public void actionPerformed(com.sun.star.awt.ActionEvent arg0)
        {
          finished = false;
          window.dispose();
        }
      });
      window.setEnable(true);
      window.setVisible(true);
    } catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("Fortschrittsanzeige für den Seriendruck konnte nicht gestartet werden.", e);
    }
  }

  /**
   * Wird aufgerufen, wenn der "X"- oder "Abbrechen"-Knopf gedrückt wurde und sorgt dafür, dass das
   * Fenster vollständig disposed und der abortListener informiert wird.
   */
  private void cancel()
  {
    if (!finished && abortListener != null)
    {
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
    }
  }

  public void dispose()
  {
    finished = true;
    window.dispose();
  }

  public void setMessage(String text)
  {
    statusLabel.setText(text);
  }

  /**
   * Registriert eine Komfortdruckfunktion (vertreten durch den Schlüssel key) mit der Maximalzahl
   * der von dieser Komfortdruckfunktion zu erwartenden Versionen oder entfernt eine bereits
   * registrierte Druckfunktion, wenn maxValue==0 ist. Beim Registrieren wird die Zahl der von der
   * Druckfunktion bereits gedruckten Versionen mit 0 initialisiert, wenn die Druckfunktion bisher
   * noch nicht bekannt war.
   *
   * @param key
   *          wird gehashed und repräsentiert die Komfortdruckfunktion, von der maxValue Versionen
   *          zu erwarten sind.
   * @param maxValue
   *          die Anzahl der von der Druckfunktion zu erwartenden Versionen oder 0 zum
   *          deregistrieren einer Druckfunktion.
   */
  public void setMaxValue(Object key, int maxValue)
  {
    if (key == null)
    {
      return;
    }

    if (maxValue == 0)
    {
      // Zähler für key löschen, wenn maxValue==0
      maxValues.remove(key);
      currentValues.remove(key);
      for (Iterator<Object> iter = order.iterator(); iter.hasNext();)
      {
        Object k = iter.next();
        if (k != null && k.equals(key))
        {
          iter.remove();
        }
      }
    } else
    {
      // neuen maxWert setzen, Reihenfolge festhalten und currentValue
      // initialisieren
      if (!maxValues.containsKey(key))
      {
        order.addFirst(key);
      }
      maxValues.put(key, maxValue);
      if (!currentValues.containsKey(key))
      {
        currentValues.put(key, 0);
      }
    }

    refresh();
  }

  /**
   * Informiert die PrintProgressBar über den Fortschritt value einer Druckfunktion, die durch key
   * repräsentiert wird.
   *
   * @param key
   *          repräsentiert eine Druckfunktion, die über den neuen Fortschritt informiert.
   * @param value
   *          enthält die aktuellen Anzahl der Versionen, die bereits von der Druckfunktion gedruckt
   *          wurden und muss damit im Bereich 0 &lt;= value &lt;= maxValue (siehe setMaxValue(...))
   *          liegen.
   */
  public void setValue(Object key, int value)
  {
    if (key == null)
    {
      return;
    }
    Integer max = maxValues.get(key);
    if (max == null)
    {
      return;
    }
    if (value > max)
    {
      value = max;
    }
    if (value < 0)
    {
      value = 0;
    }

    currentValues.put(key, value);
    refresh();
  }

  /**
   * Baut die Ansicht der PrintProgressBar neu auf. Eine der Hauptaufgaben von refresh ist es dabei,
   * den status-String (z.B. "1 von 4 Schritten" oder bei mehr als einer registrierten Druckfunktion
   * "3 von 10 (=2x5) Schritten") zusammen zu setzen und die Gesamtzahl zu erwartender Versionen und
   * den aktuellen Fortschrittswert zu berechnen. Die Gesamtzahl ergibt sich aus der Multiplikation
   * der einzelnen Maximal-Werte der registrierten Druckfunktionen. Bei der Berechnung des aktuellen
   * Druckfortschritts spielt die Reihenfolge der registrierten Druckfunktionen eine Rolle, da das
   * Erhöhen einer früher registrierten Druckfunktion einschließt, dass die später registrierten
   * Druckfunktionen damit auch schon entsprechend oft durchlaufen wurden.
   */
  private void refresh()
  {
    int allMax = 1;
    int allCurrent = 0;
    StringBuilder fromMaxString = new StringBuilder();
    boolean showfms = order.size() > 1;
    if (showfms)
    {
      fromMaxString.append(" (=");
    }
    boolean first = true;

    for (Object key : order)
    {
      allCurrent += currentValues.get(key) * allMax;
      if (first)
        first = false;
      else if (showfms)
      {
        fromMaxString.append("x");
      }
      if (showfms)
      {
        fromMaxString.append(maxValues.get(key));
      }
      allMax *= maxValues.get(key);
    }
    if (showfms)
    {
      fromMaxString.append(")");
    }

    refresh(allCurrent, allMax, fromMaxString.toString());
  }

  /**
   * Enthält die Teile von refresh, die über den Swing-EDT aufgerufen werden.
   *
   * @param allCurrent
   *          gesamtzahl aller zu erwartenden Versionen
   * @param allMax
   *          gesamtzahl aller bereits gedruckten Versionen
   * @param fromMaxString
   *          Darstellung abhängig von der Anzahl registrierter Druckfunktionen entweder "" oder
   *          "(=2x5)"
   */
  private void refresh(final int allCurrent, final int allMax, final String fromMaxString)
  {
    progressBar.setRange(0, allMax);
    progressBar.setValue(allCurrent);
    statusLabel.setText(L.m(" %1 von %2%3 Schritten", allCurrent, allMax, fromMaxString));
  }
}
