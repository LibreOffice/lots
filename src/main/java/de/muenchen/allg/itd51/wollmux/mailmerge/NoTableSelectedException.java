package de.muenchen.allg.itd51.wollmux.mailmerge;

import de.muenchen.allg.itd51.wollmux.mailmerge.ds.DatasourceModel;

/**
 * Exception for accessing a {@link DatasourceModel} without selection of a table.
 */
public class NoTableSelectedException extends Exception
{
  private static final long serialVersionUID = 495666967644874471L;

  @Override
  public String getMessage()
  {
    return "Es wurde keine Tabelle ausgewählt.";
  }
}