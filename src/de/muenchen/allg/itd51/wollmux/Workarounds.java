/*
 * Dateiname: Workarounds.java
 * Projekt  : WollMux
 * Funktion : Referenziert alle temporären Workarounds an einer zentralen Stelle
 * 
 * Copyright (c) 2009-2015 Landeshauptstadt München
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

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import com.sun.star.container.XNameAccess;
import com.sun.star.drawing.XDrawPageSupplier;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.text.XTextTablesSupplier;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;

/**
 * Diese Klasse referenziert alle temporären Workarounds, die im WollMux aufgenommen
 * wurden, an einer zentralen Stelle. Sie definiert Methoden, die die Steuerung
 * übernehmen, ob ein Workaround anzuwenden ist oder nicht.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class Workarounds
{
  /**
   * Enthält die Version des eingesetzten OpenOffice.org, die mit
   * {@link #getOOoVersion()} abgefragt werden kann.
   */
  private static String oooVersion = null;

  /*
   * Das ".*" nach dem \\A dürfte da eigentlich nicht sein, aber wenn es nicht da
   * ist, wird bei der Abtretungserklärung gemeckert wegen des Issues 101249.
   */
  private static final Pattern INSERTFORMVALUE_BOOKMARK_TEXT_THAT_CAN_BE_SAFELY_DELETED_WORKAROUND =
    Pattern.compile("\\A.*[<\\[{].*[\\]>}]\\z");

  private static Boolean workaround100374 = null;

  private static Pattern workaround101249 = null;

  private static String workaround101283 = null;

  private static Boolean workaround89783 = null;

  private static Boolean workaround103137 = null;

  private static Boolean workaroundToolbarHoverFreeze = null;

  private static Boolean workaround102164 = null;

  private static Boolean workaround73229 = null;

  private static Boolean workaround96281 = null;

  private static Boolean workaround102619 = null;

  private static ClassLoader workaround102164CL = null;

  private static Boolean workaround68261 = null;

  private static Boolean workaroundSunJavaBug4737732 = null;

  private static Boolean applyWorkaround(String issueNumber)
  {
    Logger.debug("Workaround für Issue "
      + issueNumber
      + " aktiv. Bestimmte Features sind evtl. nicht verfügbar. Die Performance kann ebenfalls leiden.");
    return Boolean.TRUE;
  }

  /**
   * Issue #100374 betrifft OOo 3.0.x. Der Workaround kann entfernt werden, wenn
   * voraussichtlich OOo 3.1 flächendeckend eingesetzt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public static boolean applyWorkaroundForOOoIssue100374()
  {
    if (workaround100374 == null)
    {
      String version = getOOoVersion();
      // -100374 ist der Marker für unsere selbst gepatchten Versionen ohne den
      // Fehler
      if (version != null && version.startsWith("3.0")
        && !version.contains("-100374"))
      {
        workaround100374 = applyWorkaround("100374");
      }
      else
        workaround100374 = Boolean.FALSE;
    }

    return workaround100374.booleanValue();
  }

  /**
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
      String version = getOOoVersion();
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
   * Issue #103137 betrifft OOo 3.0 und 3.1. Der Workaround kann entfernt werden,
   * wenn keine Dokumente mehr im Umlauf sind, deren Generator OOo 2 ist und in denen
   * Textstellen mindestens einmal aus- und wieder eingeblendet wurden. Notfalls muss
   * man vor der Deaktivierung einen Mechanismus über die Dokumentablagen der
   * Referate laufen lassen der dafür sorgt, dass der Altbestand der von OOo 2
   * erzeugten Dokumente von sämtlichen text:display="none"-Stellen befreit wurde.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static boolean applyWorkaroundForOOoIssue103137()
  {
    if (workaround103137 == null)
    {
      String version = getOOoVersion();
      if (version != null
        && (version.startsWith("3.0") || version.startsWith("3.1")))
      {
        workaround103137 = applyWorkaround("103137");
      }
      else
        workaround103137 = Boolean.FALSE;
    }

    return workaround103137.booleanValue();
  }

  /**
   * Issue #96281 betrifft OOo 3.1 und 3.2. Ob es in 3.3 gelöst sein wird wissen wir
   * nicht. Seien wir einfach mal pessimistisch.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public static boolean applyWorkaroundForOOoIssue96281()
  {
    if (workaround96281 == null)
    {
      String version = getOOoVersion();
      if (version != null
        && (version.startsWith("3.1") || version.startsWith("3.2") || version.startsWith("3.3")))
      {
        workaround96281 = applyWorkaround("96281");
      }
      else
        workaround96281 = Boolean.FALSE;
    }

    return workaround96281.booleanValue();
  }

  /**
   * Folgender Workaround bezieht sich auf das SUN-JRE Issue
   * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4737732 und ist in der Klasse
   * d.m.a.i.wollmux.dialog.FormGUI implementiert. Der Workaround ist notwendig, wenn
   * WollMux mit einem SunJRE < 1.7 gestartet wird. Den Java-Bug konnten wir bislang
   * vor allem unter Linux mit KDE feststellen. Der Java-Bug ist in Oracle JRE 1.7
   * behoben und auch mit dem aktuellen openJDK 1.7 tritt er nicht mehr auf. Der
   * Workaround war von 2006 bis 2014 immer standardmäßig aktiv, führt aber mit
   * Basisclient 5.0 verstärkt zum Issue trac#11494. Deshalb wird der Workaround 
   * jetzt nur noch mit SunJRE <= 1.6 angewendet. Er kann vermutlich komplett
   * entfernt werden (wenn WollMux unter Linux nicht mehr mit SunJRE 1.6 genutzt wird)
   */
  public static boolean applyWorkaroundForSunJavaBug4737732()
  {
    if(workaroundSunJavaBug4737732 == null)
    {
      String javaVendor = System.getProperty("java.vendor");
      String javaVersion = System.getProperty("java.version");
      if(javaVendor.equals("Sun Microsystems Inc.") && (javaVersion.startsWith("1.6.") || javaVersion.startsWith("1.5.")))
      {
        Logger.debug("Workaround für Sun-JRE Issue 4737732 aktiv.");
        workaroundSunJavaBug4737732  = Boolean.TRUE;
      }
      else
      {
        workaroundSunJavaBug4737732 = Boolean.FALSE;
      }
    }
    return workaroundSunJavaBug4737732;
  }
  
  /**
   * Diese Methode liefert die Versionsnummer von OpenOffice.org aus dem
   * Konfigurationsknoten /org.openoffice.Setup/Product/oooSetupVersionAboutBox
   * konkateniert mit /org.openoffice.Setup/Product/oooSetupExtension zurück oder
   * null, falls bei der Bestimmung der Versionsnummer Fehler auftraten.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static String getOOoVersion()
  {
    if (oooVersion == null)
    {
      XMultiServiceFactory cfgProvider =
        UNO.XMultiServiceFactory(UNO.createUNOService("com.sun.star.configuration.ConfigurationProvider"));
      if (cfgProvider != null)
      {
        XNameAccess cfgAccess = null;
        try
        {
          cfgAccess =
            UNO.XNameAccess(cfgProvider.createInstanceWithArguments(
              "com.sun.star.configuration.ConfigurationAccess", new UnoProps(
                "nodepath", "/org.openoffice.Setup/Product").getProps()));
        }
        catch (Exception e)
        {}
        if (cfgAccess != null)
        {
          try
          {
            oooVersion =
              "" + cfgAccess.getByName("ooSetupVersionAboutBox")
                + cfgAccess.getByName("ooSetupExtension");
          }
          catch (Exception e)
          {}
        }
      }
    }
    return oooVersion;
  }

  /**
   * Wegen http://qa.openoffice.org/issues/show_bug.cgi?id=101249 muss ein laxeres
   * Pattern verwendet werden, zum Test, ob ein Text in einem insertFormValue
   * Bookmark problematisch ist.
   * 
   * @return das Pattern das zum Testen verwendet werden soll
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public static Pattern workaroundForIssue101249()
  {
    if (workaround101249 == null)
    {
      Logger.debug(L.m("Workaround für Issue 101249 aktiv."));
      workaround101249 =
        INSERTFORMVALUE_BOOKMARK_TEXT_THAT_CAN_BE_SAFELY_DELETED_WORKAROUND;
    }
    return workaround101249;
  }

  /**
   * Wegen http://qa.openoffice.org/issues/show_bug.cgi?id=101283 muss der Inhalt des
   * Bookmarks durch ein Leerzeichen an Stelle des gewünschten Leerstrings ersetzt
   * werden.
   * 
   * @return Der String, der an Stelle des gewünschten Leerstrings zur Behebung des
   *         Workarounds verwendet werden muss.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static String workaroundForIssue101283()
  {
    if (workaround101283 == null)
    {
      Logger.debug(L.m("Workaround für Issue 101283 aktiv."));
      workaround101283 = " ";
    }
    return workaround101283;
  }

  /**
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
    if (workaround89783 == null)
    {
      Logger.debug(L.m("Workaround für TDF Issue 89783 aktiv."));
      workaround89783 = true;
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
   * Wenn bestimmte Aktionen getätigt werden (z.B. setWindowPosSize()) während der
   * Mauszeiger über einer OOo-Toolbar schwebt, dann friert OOo 3.0 und 3.1 unter
   * Windows ein.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public static boolean workaroundForToolbarHoverFreeze()
  {
    if (workaroundToolbarHoverFreeze == null)
    {
      String version = getOOoVersion();
      if (version == null) return false;
      if ((version.startsWith("3.0") || version.startsWith("3.1"))
        && System.getProperty("os.name").contains("Windows"))
      {
        workaroundToolbarHoverFreeze = applyWorkaround("ToolbarHoverFreeze");
      }
      else
        workaroundToolbarHoverFreeze = Boolean.FALSE;
    }

    if (workaroundToolbarHoverFreeze)
    {
      try
      {
        Robot robot = new Robot();
        PointerInfo info = MouseInfo.getPointerInfo();
        Point p = info.getLocation();
        robot.mouseMove(p.x, 0);
        Thread.sleep(100);
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }

    return workaroundToolbarHoverFreeze;
  }

  /**
   * Prüft, ob das Programm mit Java 5 ausgeführt wird, und liefert <code>true</code>
   * zurück, wenn dies der Fall ist. Wenn showMessage=true übergeben wird, wird
   * außerdem ein Meldungsdialog angezeigt, der darauf hinweist dass das gewünschte
   * Feature nicht mit Java 5 ausgeführt werden kann und es wird eine entsprechende
   * Meldung geloggt.
   * 
   * Es wird explizit nur auf Java 1.5 geprüft, da Java-Versionen kleiner 1.5 ohnehin
   * nicht vom WollMux unterstützt werden. Der Check auf die Systemproperty
   * "java.version" ist freilich nicht der sicherste, da diese Variable z.B. auch vom
   * Benutzer überschrieben werden kann, aber für unsere Zwecke sollte er in 99,9%
   * der Fälle ausreichend sein.
   * 
   * @param featureName
   *          Name des Features, das nicht mit Java 5 ausgeführt werden kann. Dieser
   *          String wird in der Log-Meldung verwendet.
   * @param showMessage
   *          Falls <code>true</code> wird eine benutzersichtbare Meldung angezeigt,
   *          die darauf hinweist, dass ein Feature nicht mit Java 5 ausführbar ist
   *          und eine Meldung geloggt.
   * @return <code>true</code>, wenn das Programm mit Java 5 ausgeführt wird,
   *         <code>false</code> sonst
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public static boolean workaroundForJava5(String featureName, boolean showMessage)
  {
    if (System.getProperty("java.version").startsWith("1.5"))
    {
      if (showMessage)
      {
        Logger.debug(L.m(
          "Versuch das Feature \"%1\" mit Java 5 zu starten wurde verhindert.",
          featureName));
        JOptionPane.showMessageDialog(
          null,
          L.m("Dieses Feature ist nur verfügbar, wenn Java 6 oder höher eingesetzt wird."),
          L.m("Inkompatible Java-Version"), JOptionPane.ERROR_MESSAGE);
      }
      return true;
    }
    return false;
  }

  /**
   * Trac-Ticket 5031 beschreibt mehrere Fälle, in denen WollMux-Formulardokumente
   * aus bisher unbekanntem Grund ihren Formularwerteabschnitt verloren haben. In
   * einem Fall enthielt ein Formulardokument gar keine Notiz "WollMuxFormularwerte",
   * ein anderes Formulardokument enthielt eine solche Notiz aber mit leerem
   * Dateninhalt, wiederum eine andere Notiz enthielt eine leere Notiz ohne
   * dc:creator-Attribut, die mutmaßlich früher einmal eine
   * WollMuxFormularwerte-Notiz war.
   * 
   * Falls das Dokument vor dieser Verarbeitung durch den WollMux bereits einmal
   * durch den WollMux als Formulardokument markiert worden ist, entsteht ein nicht
   * zulässiger Zustand, wenn der Formularwerte-Abschnitt fehlt. Diese Methode
   * liefert true, wenn der Fall vorliegt und ein entsprechender Workaround
   * angewendet werden soll.
   * 
   * Da die Ursache, die zu dem nicht zulässigen Stand geführt hat derzeit noch nicht
   * bekannt ist, und die Symptome auch in drei Fällen unterschiedlich waren (s.o.),
   * kann dieser Workaround derzeit leider nicht zeitlich beschränkt werden. Die
   * Vermutung liegt nahe, dass der Fehler im Zusammenhang mit den OOo-Issues 100374
   * und 108709 (also den Änderungen an OOo 3.0 bezüglich des Notizen-Systems)
   * zusammen hängt. Die Hoffnung ist, dass solche kaputten Dokumente nicht mehr
   * auftreten, wenn diese beiden Fehler gefixed sind.
   * 
   * @param werteStr
   *          Den Wert den PersistantData zur ID "WollMuxFormularwerte" zurück gibt
   *          (kann null sein, wenn die Notiz bisher nicht existiert, oder != null,
   *          wenn sie bereits existiert).
   * @param alreadyTouchedAsFormDocument
   *          Gibt an, ob das Dokument vor dieser Verarbeitung durch den WollMux
   *          bereits Formulardokument-Merkmale enthielt (z.B. eine Notiz SetType mit
   *          dem Wert "formularDokument")
   * @return true, wenn der Workaround anzuwenden ist.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static boolean applyWorkaroundForTracTicket5031(String werteStr,
      boolean alreadyTouchedAsFormDocument)
  {
    boolean apply =
      alreadyTouchedAsFormDocument && (werteStr == null || werteStr.length() == 0);
    if (apply)
      Logger.log(L.m("Formulardokument ohne Formularwerte-Abschnitt gefunden! Workaround zur Rekonstruierung der Formularwerte aktiv."));
    return apply;
  }

  /**
   * Issue 102619 (calling dispatch .uno:GotoNextPlacemarker crashes OOo) beschreibt
   * einen Bug in OOo < 3.2.1 bei dem OOo crashed, wenn mittels des Dispatches
   * .uno:GotoNextPlacemarker in eine Tabelle navigiert wird.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static boolean applyWorkaroundForOOoIssue102619()
  {
    if (workaround102619 == null)
    {
      String version = getOOoVersion();
      if (version != null
        && (version.startsWith("2.") || version.startsWith("3.0") || version.startsWith("3.1")))
      {
        workaround102619 = applyWorkaround("102619");
      }
      else
        workaround102619 = Boolean.FALSE;
    }

    return workaround102619.booleanValue();
  }

  /**
   * Issue 68261 (XEnumeration.nextElement() throws despite hasMoreElements()==true)
   * beschreibt einen Bug in OOo < 3.0 (das genaue Target der 2er-Serie kann ich
   * leider nicht mehr ermitteln) bei dem OOo Exceptions schmeißt beim Iterieren über
   * Inhalte in Tabellen.
   * 
   * Achtung: Sollte der Workaround einmal entfernt werden, dann bitte darauf achten,
   * dass sich der Workaround nicht nur auf den im Code markierten Block bezieht,
   * sondern sich durch die ganze Logik in der Klasse {@link FormFieldFactory}
   * durchzieht. Die Logik dieser Klasse kann an einigen Stellen sicherlich deutlich
   * vereinfacht werden ohne diesen Workaround. Evtl. bietet sich sogar ein
   * Neu-Schreiben an.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static boolean applyWorkaroundForOOoIssue68261()
  {
    if (workaround68261 == null)
    {
      String version = getOOoVersion();
      if (version != null && (version.startsWith("2.")))
      {
        workaround68261 = applyWorkaround("68261");
      }
      else
        workaround68261 = Boolean.FALSE;
    }
    return workaround68261.booleanValue();
  }

}
