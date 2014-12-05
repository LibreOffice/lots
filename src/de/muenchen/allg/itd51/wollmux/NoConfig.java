/**
 * 
 */
package de.muenchen.allg.itd51.wollmux;

/**
 * Speichert Informationen darüber, ob der WollMux sich im NoConfig-Modus
 * befindet.
 */
public class NoConfig
{

  /**
   * Dummy-Schema für Datenquellen.
   */
  public static final java.lang.String NOCONFIG = "noconfig";

  private static boolean noConfigMode = false;

  private static boolean noConfigInfoShown = false; // Anwender schon auf no config
                                                    // modus hingewiesen ?

  public NoConfig()
  {
    noConfigMode = false;
    noConfigInfoShown = false;
  }

  public NoConfig(boolean flag)
  {
    noConfigMode = flag;
    noConfigInfoShown = false;
  }

  /**
   * Läuft wollmux im no config Modus ?
   * 
   * @return true wenn no config Modus, false falls nicht
   */
  public boolean isNoConfig()
  {
    return noConfigMode;
  }

  private boolean isNoConfigInfoShown()
  {
    return noConfigInfoShown;
  }

  /**
   * setze NoConfigInfoShown auf true, i.e. Dialog wurde Benutzer angezeigt
   */
  private void setNoConfigInfoShown()
  {
    noConfigInfoShown = true;
  }

  /**
   * Zeige - einmalig - einen no config hinweis an.
   * 
   * @return true wenn der dialog angezeigt wurde, false falls nicht
   */
  public boolean showNoConfigInfo()
  {

    if (isNoConfig() && !isNoConfigInfoShown())
    {
      WollMuxSingleton.showInfoModal(
        L.m("WollMux-Hinweis - fehlende wollmux.conf"),
        L.m("WollMux läuft ohne wollmux.conf !\n"
          + "Aus diesem Grund ist leider nicht der komplette Funktionsumfang verfügbar.\n"));
      setNoConfigInfoShown();
      return true;
    }
    return false;
  }

}
