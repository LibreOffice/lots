package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.sidebar.SeriendruckSidebarContent;

/**
 * Event for initializing the mailmerge sidebar.
 */
public class OnInitMailMergeSidebar extends BasicEvent
{
  private SeriendruckSidebarContent sidebar;

  /**
   * Create a new event.
   * 
   * @param sidebar
   *          The sidebar to initialize.
   */
  public OnInitMailMergeSidebar(SeriendruckSidebarContent sidebar)
  {
    this.sidebar = sidebar;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    sidebar.init();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName();
  }
}