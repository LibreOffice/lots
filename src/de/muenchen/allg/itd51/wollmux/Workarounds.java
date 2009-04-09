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

  private static Boolean workaround100374 = null;

  private static Boolean workaround100718 = null;

  private static Boolean applyWorkaround(String issueNumber)
  {
    Logger.error("Workaround für Issue "
      + issueNumber
      + " aktiv. Bestimmte Features sind evtl. nicht verfügbar. Die Performance kann ebenfalls leiden.");
    return Boolean.TRUE;
  }

  /**
   * Issue #100374 betrifft OOo 3.0.x bis voraussichtlich OOo 3.2. Der Workaround
   * kann entfernt werden, wenn voraussichtlich OOo 3.2 flächendeckend eingesetzt
   * wird.
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
   * Issue #100718 betrifft OOo 3.1 bis voraussichtlich OOo 3.2. Der Workaround kann
   * entfernt werden, wenn voraussichtlich OOo 3.2 flächendeckend eingesetzt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public static boolean applyWorkaroundForOOoIssue100718()
  {
    if (workaround100718 == null)
    {
      // auskommentiert, da Bug voraussichtlich in 3.1 doch behoben sein wird.
      // String version = getOOoVersion();
      // // -100718 ist der Marker für unsere selbst gepatchten Versionen ohne den
      // // Fehler
      // if (version != null && (version.startsWith("3.1"))
      // && !version.contains("-100718"))
      // {
      // workaround100718 = applyWorkaround("100718");
      // }
      // else
      workaround100718 = Boolean.FALSE;
    }

    return workaround100718.booleanValue();
  }

  /**
   * Diese Methode liefert die Versionsnummer von OpenOffice.org aus dem
   * Konfigurationsknoten /org.openoffice.Setup/Product/oooSetupVersionAboutBox
   * konkateniert mit /org.openoffice.Setup/Product/oooSetupExtension zurück oder
   * null, falls bei der Bestimmung der Versionsnummer Fehler auftraten.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  private static String getOOoVersion()
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
}
