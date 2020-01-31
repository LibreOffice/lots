package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Exception if a dialog can't start.
 */
public class CantStartDialogException extends WollMuxFehlerException
{
  private static final long serialVersionUID = -1130975078605219254L;

  /**
   * New exception.
   *
   * @param e
   *          The original exception.
   */
  public CantStartDialogException(Exception e)
  {
    super(
        L.m("Der Dialog konnte nicht gestartet werden!\n\nBitte kontaktieren Sie Ihre Systemadministration."),
        e);
  }
}