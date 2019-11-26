package de.muenchen.allg.itd51.wollmux.core.util;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiServiceFactory;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.afid.UnoProps;

public class Utils
{

  private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

  /**
   * Enthält die Version des eingesetzten OpenOffice.org, die mit
   * {@link Utils#getOOoVersion()} abgefragt werden kann.
   */
  private static String oooVersion = null;

  private Utils()
  {
  }

  /**
   * Diese Methode liefert die Versionsnummer von OpenOffice.org aus dem
   * Konfigurationsknoten /org.openoffice.Setup/Product/oooSetupVersionAboutBox
   * konkateniert mit /org.openoffice.Setup/Product/oooSetupExtension zurück
   * oder null, falls bei der Bestimmung der Versionsnummer Fehler auftraten.
   */
  public static String getOOoVersion()
  {
    if (oooVersion == null)
    {
      XMultiServiceFactory cfgProvider = UNO.XMultiServiceFactory(UNO.createUNOService("com.sun.star.configuration.ConfigurationProvider"));
      if (cfgProvider != null)
      {
        Optional<XNameAccess> cfgAccess;
        try
        {
          cfgAccess = Optional
              .ofNullable(UNO.XNameAccess(cfgProvider.createInstanceWithArguments("com.sun.star.configuration.ConfigurationAccess",
                  new UnoProps("nodepath", "/org.openoffice.Setup/Product").getProps())));
          cfgAccess.ifPresent(access -> {
            try
            {
              oooVersion = "" + access.getByName("ooSetupVersionAboutBox") + access.getByName("ooSetupExtension");
            } catch (NoSuchElementException | WrappedTargetException e)
            {
              LOGGER.trace("", e);
            }
          });
        } catch (Exception e)
        {
          LOGGER.trace("", e);
        }
      }
    }
    return oooVersion;
  }

  public static Object getProperty(Object o, String propName)
  {
    try
    {
      return UNO.getProperty(o, propName);
    }
    catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return null;
    }
  }

  public static Object setProperty(Object o, String propName, Object propVal)
  {
    try
    {
      return UNO.setProperty(o, propName, propVal);
    }
    catch (UnoHelperException e)
    {
      LOGGER.debug("", e);
      return null;
    }
  }
}
