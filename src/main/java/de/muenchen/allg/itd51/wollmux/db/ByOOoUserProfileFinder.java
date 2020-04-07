package de.muenchen.allg.itd51.wollmux.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.util.UnoConfiguration;

/**
 * Ein konkreter DataFinder, der für die Auflösung der Variable in getValueForKey im Benutzerprofil
 * der OOo Registry nachschaut (das selbe wie
 * Extras-&gt;Optionen-&gt;LibreOffice-&gt;Benutzerdaten).
 *
 * @author christoph.lutz
 */
public class ByOOoUserProfileFinder extends DataFinder
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ByOOoUserProfileFinder.class);

  public ByOOoUserProfileFinder(DatasourceJoiner dsj)
  {
    super(dsj);
  }

  @Override
  protected String getValueForKey(String key)
  {
    try
    {
      return UnoConfiguration.getConfiguration("/org.openoffice.UserProfile/Data", key).toString();
    } catch (Exception e)
    {
      LOGGER.error(L.m("Konnte den Wert zum Schlüssel '%1' des OOoUserProfils nicht bestimmen:", key), e);
    }
    return "";
  }
}
