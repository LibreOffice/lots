/*
 * Dateiname: Workarounds.java
 * Projekt  : WollMux
 * Funktion : Referenziert alle temporären Workarounds an einer zentralen Stelle
 *
 * Copyright (c) 2009-2019 Landeshauptstadt München
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
 * 01.04.2009 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 * @version 1.0
 *
 */package de.muenchen.allg.itd51.wollmux;

import java.awt.Toolkit;
import java.net.URL;
import java.net.URLClassLoader;

import com.sun.star.drawing.XDrawPageSupplier;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.text.XTextTablesSupplier;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;

/**
 * Diese Klasse referenziert alle temporären Workarounds, die im WollMux aufgenommen
 * wurden, an einer zentralen Stelle. Sie definiert Methoden, die die Steuerung
 * übernehmen, ob ein Workaround anzuwenden ist oder nicht.
 *
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class Workarounds
{
  private static Boolean workaround89783 = null;

  private static Boolean workaroundToolbarHoverFreeze = null;

  private static Boolean workaround102164 = null;

  private static Boolean workaround73229 = null;

  private static ClassLoader workaround102164CL = null;

  private static Boolean workaroundWMClass = null;

  public static Boolean applyWorkaround(String issueNumber)
  {
    Logger.debug("Workaround für Issue "
      + issueNumber
      + " aktiv. Bestimmte Features sind evtl. nicht verfügbar. Die Performance kann ebenfalls leiden.");
    return Boolean.TRUE;
  }

  /**
   * Beim Einfügen eines Dokuments mit einer Section und einem Seitenumbruch in ein 
   * anderes wird eine leere Seite am Anfang eingefügt.
   *  
   * Issue #73229 betrifft den WollMux-Seriendruck in ein Gesamtdokument und ist
   * aktuell für OOo Later priorisiert - wird also nicht in absehbarer Zeit behoben
   * sein.
   *
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static boolean applyWorkaroundForOOoIssue73229()
  {
    if (workaround73229 == null)
    {
      workaround73229 = applyWorkaround("73229");
    }
    return workaround73229.booleanValue();
  }

  /**
   * Beim Erzeugen eines neuen Threads wird der Classloader nicht gesetzt und muss
   * explizit gesetzt werden.
   * 
   * Issue #102164 betrifft OOo 3.2. Es ist unklar, wann der Workaround entfernt
   * werden kann, da er aufgrund eines Bugs in der Swing-Implementierung von Java 6
   * zurückgeht.
   *
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public static void applyWorkaroundForOOoIssue102164()
  {
    if (workaround102164 == null)
    {
      String version = Utils.getOOoVersion();
      if (version != null && !version.startsWith("3.1") && !version.startsWith("2")
        && !version.startsWith("3.0"))
      {
        workaround102164 = applyWorkaround("102164");
      }
      else
        workaround102164 = Boolean.FALSE;
    }

    if (workaround102164.booleanValue())
    {
      if (workaround102164CL == null)
        workaround102164CL = Thread.currentThread().getContextClassLoader();
      if (workaround102164CL == null)
        workaround102164CL = Workarounds.class.getClassLoader();
      if (workaround102164CL == null)
        workaround102164CL = ClassLoader.getSystemClassLoader();
      if (workaround102164CL == null)
        workaround102164CL = new URLClassLoader(new URL[] {});
      Thread.currentThread().setContextClassLoader(workaround102164CL);
    }
  }

  /**
   * Interne Begrenzung von Arrays im Seriendruck auf 65536.
   * 
   * Wegen https://bugs.documentfoundation.org/show_bug.cgi?id=89783 muss der
   * OOoMailMerge in mehrere Pakete aufgeteilt werden, wenn das
   * Seriendruck-Hauptdokument doc viele der im Issue genannten Elemente (z.B.
   * Rahmen, PageStyles, ...) enthält. Betroffen davon sind alle aktuell bekannten
   * Versionen von OOo, AOO und LO.
   *
   * @param doc
   *         Das Seriendruck-Hauptdokument
   *
   * @return Der Rückgabewert dieser Methode beschreibt, wie viele Datensätze zu doc
   *         ohne Einfrierer von der aktuell genutzen Office-Version verarbeitet
   *         werden können. Der Rückgabewert kann auch null sein, dann soll der der
   *         Workaround nicht angewendet werden.
   *
   * @author Christoph Lutz (CIB software GmbH)
   */
  public static Integer workaroundForTDFIssue89783(XTextDocument doc)
  {
    String version = Utils.getOOoVersion();

    if (workaround89783 == null && (version.startsWith("4") || version.startsWith("5.0")))
    {
      Logger.debug(L.m("Workaround für TDF Issue 89783 aktiv."));
      workaround89783 = true;
    } else {
      workaround89783 = false;
    }

    if(workaround89783)
    {
      int maxCritElements = 1;
      // zähle Sections:
      XTextSectionsSupplier tss = UNO.XTextSectionsSupplier(doc);
      if (tss != null)
      {
        String[] names = tss.getTextSections().getElementNames();
        if (names.length > maxCritElements) maxCritElements = names.length;
      }

      // zähle DrawPage-Objekte (TextFrames + Pictures + DrawObjects):
      XDrawPageSupplier dps = UNO.XDrawPageSupplier(doc);
      if (dps != null)
      {
        int drawPageElements = dps.getDrawPage().getCount();
        if (drawPageElements > maxCritElements) maxCritElements = drawPageElements;
      }

      // count TextTables
      XTextTablesSupplier tts =
        UnoRuntime.queryInterface(XTextTablesSupplier.class, doc);
      if (tts != null)
      {
        String[] names = tts.getTextTables().getElementNames();
        if (names.length > maxCritElements) maxCritElements = names.length;
      }

      // Maximalwert des mit 16-Bit adressierbaren Bereichs / maxCritElements - 1
      // (zu Sicherheit)
      return ((1 << 16) / maxCritElements) - 1;
    }

    return null;
  }

  /**
   * Unter Linux kann kein Schnellstarter erstellt werden, da
   * @see http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6528430.
   * Damit es funktioniert, muss das Attribut WM_Class gesetzt werden, da dann
   * eine korrekte Verknüpfung zwischen dem Schnellstarter und der .desktop
   * Datei entsteht.
   *
   * @return true wenn WM_Class gesetzt wurde.
   */
  public static boolean applyWorkaroundForWMClass()
  {
    if (workaroundWMClass == null)
    {
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      Class<?> xtoolkit = toolkit.getClass();
      if ("sun.awt.X11.XToolkit".equals(xtoolkit.getName()))
      {
        try
        {
          java.lang.reflect.Field awtAppClassNameField = xtoolkit.getDeclaredField("awtAppClassName");
          awtAppClassNameField.setAccessible(true);
          awtAppClassNameField.set(null, "WollMux");
          workaroundWMClass = applyWorkaround("WMClass");
        } catch (Exception e) {
          Logger.error(L.m("WMClass konnte nicht gesetzt werden."), e);
          workaroundWMClass = Boolean.FALSE;
        }
      } else {
        workaroundWMClass = Boolean.FALSE;
      }
    }
    return workaroundWMClass.booleanValue();
  }

}
