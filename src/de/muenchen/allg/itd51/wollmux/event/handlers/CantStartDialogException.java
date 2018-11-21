package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

public class CantStartDialogException extends WollMuxFehlerException
{
  private static final long serialVersionUID = -1130975078605219254L;

  public CantStartDialogException(java.lang.Exception e)
  {
    super(
      L.m("Der Dialog konnte nicht gestartet werden!\n\nBitte kontaktieren Sie Ihre Systemadministration."),
      e);
  }
}