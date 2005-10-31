/*
 * Dateiname: Event.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert ein für den WollMux relevantes Event.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 24.10.2005 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;

/**
 * TODO: dokumentieren von Event.java
 * 
 * @author lut
 * 
 */
public class Event
{
  public static final int UNKNOWN = -1;

  public static final int ON_LOAD = 1;

  public static final int ON_NEW = 2;

  public static final int ON_MODIFIED = 10;

  public static final int ON_ABSENDERDATEN_BEARBEITEN = 20;

  public static final int ON_OPENFRAG = 21;

  public static final int ON_DIALOG_BACK = 30;

  public static final int ON_DIALOG_ABORT = 31;

  private int event = UNKNOWN;

  private Object source = null;

  private String argument = "";

  private String getEventName()
  {
    if (event == UNKNOWN)
    {
      return "UNKNOWN";
    }
    if (event == ON_ABSENDERDATEN_BEARBEITEN)
    {
      return "ON_ABSENDERDATEN_BEARBEITEN";
    }
    if (event == ON_LOAD)
    {
      return "ON_LOAD";
    }
    if (event == ON_NEW)
    {
      return "ON_NEW";
    }
    if (event == ON_MODIFIED)
    {
      return "ON_MODIFIED";
    }
    if (event == ON_OPENFRAG)
    {
      return "ON_OPENFRAG";
    }
    if (event == ON_DIALOG_BACK)
    {
      return "ON_DIALOG_BACK";
    }
    if (event == ON_DIALOG_ABORT)
    {
      return "ON_DIALOG_ABORT";
    }
    else
      return "namenlos";
  }

  public Event(int event)
  {
    this.event = event;
  }

  public Event(int event, String argument)
  {
    this.event = event;
    this.argument = argument;
  }

  public Event(com.sun.star.document.EventObject docEvent)
  {
    this.event = UNKNOWN;
    this.source = (XInterface) UnoRuntime.queryInterface(
        XInterface.class,
        docEvent.Source);

    // Bekannte Event-Typen rausziehen:
    if (docEvent.EventName.equals("OnLoad")) this.event = ON_LOAD;
    if (docEvent.EventName.equals("OnNew")) this.event = ON_NEW;
  }

  public Event(com.sun.star.lang.EventObject modifyEvent)
  {
    this.event = ON_MODIFIED;
    this.source = (XInterface) UnoRuntime.queryInterface(
        XInterface.class,
        modifyEvent.Source);
  }

  public int getEvent()
  {
    return event;
  }

  public Object getSource()
  {
    return source;
  }

  public String getArgument()
  {
    return argument;
  }

  public String toString()
  {
    return "Event(" + getEventName() + ")";
  }
}
