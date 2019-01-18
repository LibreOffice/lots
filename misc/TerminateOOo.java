/*
 * Dateiname: TerminateOOo.java
 * Projekt  : n/a
 * Funktion : Kleines Tool zum Beenden von Office
 * 
 * Copyright (c) 2009 Landeshauptstadt München
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
 * 03.09.2009 | BED | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Daniel Benkmann (D-III-ITD-D101)
 * 
 */

import com.sun.star.beans.XPropertySet;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * Kleines Tool zum Beenden von Office.
 * 
 * @author Daniel Benkmann (D-III-ITD-D101)
 */
public class TerminateOOo
{

  /**
   * Stellt eine Verbindung mit Office her (und startet dabei einen
   * soffice-Prozess, falls noch keiner läuft) und beendet dann Office
   * mittels XDesktop.terminate(). Das Programm beendet die JVM mit Statuscode 0,
   * wenn das Beenden von Office erfolgreich war und mit einem Statuscode !=
   * 0, wenn Office nicht beendet wurde bzw. irgendetwas schief gelaufen ist
   * (z.B. beim Verbindungsaufbau). Zudem wird auf der Standardausgabe eine
   * entsprechende Meldung ausgegeben.
   * 
   * @param args
   *          wird nicht ausgewertet
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public static void main(String[] args)
  {
    boolean terminated = false;
    try
    {
      XMultiComponentFactory xMCF = Bootstrap.bootstrap().getServiceManager();
      XComponentContext defaultContext =
        (XComponentContext) UnoRuntime.queryInterface(
          XComponentContext.class,
          ((XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xMCF)).getPropertyValue("DefaultContext"));
      XDesktop desktop =
        (XDesktop) UnoRuntime.queryInterface(XDesktop.class,
          xMCF.createInstanceWithContext("com.sun.star.frame.Desktop",
            defaultContext));

      // Quickstarter soll das Terminieren des Prozesses nicht verhindern
      XPropertySet xPropSet =
        (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, desktop);
      xPropSet.setPropertyValue("SuspendQuickstartVeto", Boolean.TRUE);
      //ACHTUNG: Der obige Code hat keine Auswirkung auf den Quickstarter der
      //WollMuxBar! Wenn die WollMuxBar mit "--quickstarter" gestartet wird,
      //kann Office nicht beendet werden.
      

      // Versuch OOo zu beenden
      terminated = desktop.terminate();
    }
    catch (Exception e)
    {
      System.out.println("Exception occured while trying to terminate Office!");
      e.printStackTrace();
      System.exit(2);
    }

    if (terminated)
    {
      System.out.println("Office was terminated successfully!");
      System.exit(0);
    }
    else
    {
      System.out.println("Office was NOT terminated!");
      System.exit(1);
    }
  }
}
