/*
 * Dateiname: Workarounds.java
 * Projekt  : WollMux
 * Funktion : Referenziert alle temporären Workarounds an einer zentralen Stelle
 * 
 * Copyright (c) 2009 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0.
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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;

import com.sun.star.container.XNameAccess;
import com.sun.star.lang.XMultiServiceFactory;

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

  private static Boolean workaround103137 = null;

  private static Boolean workaround102164 = null;

  private static ClassLoader workaround102164CL = null;

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
}
