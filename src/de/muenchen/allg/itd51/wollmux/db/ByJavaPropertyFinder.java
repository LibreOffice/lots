package de.muenchen.allg.itd51.wollmux.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Ein konkreter DataFinder, der für die Auflösung der Variable in getValueForKey
 * die Methode System.getProperty(key) verwendet.
 *
 * @author christoph.lutz
 */
public class ByJavaPropertyFinder extends DataFinder
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ByJavaPropertyFinder.class);
  
  public ByJavaPropertyFinder(DatasourceJoiner dsj)
  {
    super(dsj);
  }

  @Override
  protected String getValueForKey(String key)
  {
    try
    {
      return System.getProperty(key);
    } catch (java.lang.Exception e)
    {
      LOGGER.error(
          L.m("Konnte den Wert der JavaProperty '%1' nicht bestimmen:", key),
          e);
    }
    return "";
  }
}
