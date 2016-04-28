package de.muenchen.allg.itd51.wollmux.core.parser;

import de.muenchen.allg.itd51.wollmux.core.util.L;

public class InvalidIdentifierException extends Exception
{
  private static final long serialVersionUID = 495666967644874471L;

  private String invalidId;

  public InvalidIdentifierException(String invalidId)
  {
    this.invalidId = invalidId;
  }

  @Override
  public String getMessage()
  {
    return L.m(
      "Der Bezeichner '%1' ist ungültig, und darf nur die Zeichen a-z, A-Z, _ und 0-9 enthalten, wobei das erste Zeichen keine Ziffer sein darf.",
      invalidId);
  }
}