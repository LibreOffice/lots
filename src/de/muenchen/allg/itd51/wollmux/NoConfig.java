/**
 * 
 */
package de.muenchen.allg.itd51.wollmux;

/**
 * @author 
 *
 */
public class NoConfig
{

    public static final java.lang.String NOCONFIG = "noconfig"; // schema
    private static boolean noConfigMode = false;                // modus
    private static boolean noConfigInfoShown = false;           // Anwender schon auf no config modus hingewiesen ?
  
  //ctor
    public NoConfig()
    {
   //   noConfigMode = false;
      noConfigInfoShown = false;
    }
  
    public NoConfig(boolean flag)
    {
      noConfigMode = flag;
      noConfigInfoShown = false;
    }
  
  // meth.
    public boolean isNoConfig()
    {
      return noConfigMode;
    }
  
 //   private void setNoConfig(boolean flag)
 //   {
 //     noConfigMode = flag;
 //   }
  
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
     * Zeige einen no config hinweis an.
     * @return true wenn der dialog angezeigt wurde, false falls nicht
     */
    public boolean showNoConfigInfo()
    {

      if ( isNoConfig() 
        && ! isNoConfigInfoShown())
      {
        String msg = "WollMux läuft ohne wollmux.conf !\n"  
                   + "Aus diesem Grund ist leider nicht der komplette Funktionsumfang verfügbar.\n";
        WollMuxSingleton.showInfoModal(L.m("WollMux-Hinweis - fehlende wollmux.conf"), msg);  
        setNoConfigInfoShown();
        return true;
      }
      return false;
    }
    
}
