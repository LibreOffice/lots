package de.muenchen.allg.itd51.wollmux.core.util;

import com.sun.star.container.XNameAccess;
import com.sun.star.lang.XMultiServiceFactory;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;

public class Utils
{

  /**
   * Enthält die Version des eingesetzten OpenOffice.org, die mit
   * {@link Utils#getOOoVersion()} abgefragt werden kann.
   */
  private static String oooVersion = null;

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

}
