package de.muenchen.allg.itd51.wollmux.sidebar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.frame.XController;
import com.sun.star.frame.XController2;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.ui.XDeck;
import com.sun.star.ui.XDecks;
import com.sun.star.ui.XSidebarProvider;

import de.muenchen.allg.afid.UNO;

public class SidebarHelper
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SidebarHelper.class);

  public static final String WM_FORM_GUI = "FormularGuiDeck";

  public static final String WM_NAVIGATION = "WollMuxDeck";

  public static final String WM_MAILMERGE = "WollMuxSeriendruckDeck";

  private SidebarHelper()
  {
  }

  /**
   * Get all decks known by LO.
   * 
   * @param xController
   * @return returns NULL if XSidebarProvider returns no decks.
   */
  private static XDecks getDecks(XController xController)
  {
    XController2 controller2 = UNO.XController2(xController);

    if (controller2 == null)
    {
      LOGGER.trace("XController2 is NULL.");
      return null;
    }

    XSidebarProvider sideBarProvider = controller2.getSidebar();

    if (sideBarProvider == null)
    {
      LOGGER.trace("XSidebarProvider is NULL.");
      return null;
    }

    return sideBarProvider.getDecks();
  }

  public static XDeck getDeckByName(String name, XController xController)
  {
    XDecks xDecks = getDecks(xController);

    if (xDecks == null)
    {
      LOGGER.trace("XDecks is NULL.");
      return null;
    }

    try
    {
      return UNO.XDeck(xDecks.getByName(name));
    } catch (NoSuchElementException | WrappedTargetException e)
    {
      LOGGER.trace("Cast to XDeck fails.", e);
    }

    return null;
  }

}
