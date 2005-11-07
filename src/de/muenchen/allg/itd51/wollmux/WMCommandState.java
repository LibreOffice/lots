/*
 * Dateiname: WMCommandState.java
 * Projekt  : WollMux
 * Funktion : Beschreibt den Status eines bearbeiteten WollMux-Kommandos.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 07.11.2005 | LUT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * Beschreibt den Status eines im WMCommandIterpreter ausgeführten Kommandos.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class WMCommandState
{
  /**
   * Das WM-Kommando, zu dem der Status gehört.
   */
  private ConfigThingy wmCmd;

  private static final Boolean defaultDone = new Boolean(false);

  private static final Integer defaultErrors = new Integer(0);

  // Status-Attribute:

  private Boolean done;

  private Integer errors;

  /**
   * Der Konstruktor erzeugt ein WMCommandState-Objekt, das den Status eines
   * WM-Kommandos repräsentiert. Der Konstruktor erwartet ein ConfigThingy, das
   * ein WM-Kommando und optional ein STATE-Feld enthält, d.h. ein ConfigThingy
   * mit der String-Repräsentation "WM(...) [STATE(...)]". Der Konstruktor
   * wertet dabei nur das STATE-Feld aus (falls vorhanden).
   * 
   * Ist der STATE-Knoten nicht vorhanden, so gelten Standardwerte. Erst beim
   * Überschreiben eines Standardwerts wird, wird ein entsprechender
   * STATE-Knoten erzeugt. Hinter dieser Vorgehensweise verbirgt sich die Idee,
   * dass sich das Bookmark mit dem WM-Kommando möglichst wenig ändern sollte,
   * damit der Ersteller der Vorlage einen möglichst hohen
   * Wiedererkennungs-Effekt hat.
   * 
   * @param wmCmd
   *          Ein ConfigThingy, das das WM-Kommando und optional ein STATE-Feld
   *          enthält, d.h. ein ConfigThingy mit der String-Repräsentation
   *          "WM(...) [STATE(...)]". Der Konstruktor wertet dabei nur das
   *          STATE-Feld aus.
   */
  public WMCommandState(ConfigThingy wmCmd)
  {
    this.wmCmd = wmCmd;
  }

  /**
   * Beschreibt ob das Kommando bereits abgearbeitet wurde. Ist DONE bisher noch
   * nicht definiert oder gesetzt worden, so wird der Defaultwert false
   * zurückgeliefert.
   * 
   * @return true, falls das Kommando bereits bearbeitet wurde, andernfalls
   *         false.
   */
  public boolean isDone()
  {
    if (done != null)
      return done.booleanValue();
    else if (isDefined("DONE"))
    {
      String doneStr = getKeyNode("DONE").toString();
      if (doneStr.compareToIgnoreCase("true") == 0)
        return true;
      else
        return false;
    }
    else
      return defaultDone.booleanValue();
  }

  /**
   * Setzt den Status für das Attribut Done.
   * 
   * @param done
   *          true, signalisiert, dass das Kommando bereits bearbeitet wurde,
   *          false das Gegenteil.
   */
  public void setDone(boolean done)
  {
    this.done = new Boolean(done);
  }

  /**
   * Liefert die Anzahl der Fehler, die bei der Bearbeitung des Kommandos
   * aufgetreten sind. Ist das Attribut ERRORS bisher nicht definiert oder kein
   * Fehler gesetzt worden, so wird der Defaultwert 0 zurückgliefert.
   * 
   * @return Die Anzahl der Fehler, die bei der Bearbeitung aufgetreten sind.
   */
  public int getErrors()
  {
    if (errors != null)
      return errors.intValue();
    else if (isDefined("ERRORS"))
    {
      Integer errorInt = new Integer(getKeyNode("ERRORS").toString());
      return errorInt.intValue();
    }
    else
      return defaultErrors.intValue();
  }

  /**
   * Erlaubt das explizite Setzen des Errors-Attributs.
   * 
   * @param errors
   */
  public void setErrors(int errors)
  {
    this.errors = new Integer(errors);
  }

  /**
   * Liefert ein ConfigThingy, das das WM-Kommando mit allen Statusinformationen
   * enthält. Neue Unterknoten werden dabei nur angelegt, wenn dies unbedingt
   * erforderlich ist, d.h. wenn ein Wert vom Defaultwert abweicht oder der Wert
   * bereits vorher gesetzt war.
   * 
   * @return Ein ConfigThingy, das das WM-Kommando mit allen Statusinformationen
   *         enthält.
   */
  public ConfigThingy toConfigThingy()
  {
    // DONE:
    // Falls der Knoten existiert und sich der Status geändert hat wird der neue
    // Status gesetzt. Falls der Knoten nicht existiert wird er nur erzeugt,
    // wenn der Status vom Standard abweicht.
    if (isDefined("DONE") && done != null)
    {
      setOrCreate("DONE", done.toString());
    }
    else if (isDone() != defaultDone.booleanValue())
    {
      setOrCreate("DONE", "" + isDone() + "");
    }

    // ERRORS:
    // Falls der Knoten existiert und sich der Status geändert hat wird der neue
    // Status gesetzt. Falls der Knoten nicht existiert wird er nur erzeugt,
    // wenn der Status vom Standard abweicht.
    if (isDefined("ERRORS") && errors != null)
    {
      setOrCreate("ERRORS", errors.toString());
    }
    else if (getErrors() != defaultErrors.intValue())
    {
      setOrCreate("ERRORS", "" + getErrors() + "");
    }

    return wmCmd;
  }

  /**
   * Gibt Auskunft, ob ein Key unter halb des STATE-Knotens definiert ist. z.B.
   * "WM(...) STATE (KEY '...')"
   * 
   * @param key
   * @return true, falls der Key definiert ist, andernfalls false.
   */
  private boolean isDefined(String key)
  {
    return (getKeyNode(key) != null);
  }

  /**
   * Liefert das ConfigThingy zu dem gesuchten Key key unterhalt des
   * STATE-Knotens.
   * 
   * @param key
   * @return
   */
  private ConfigThingy getKeyNode(String key)
  {
    ConfigThingy state;
    try
    {
      state = wmCmd.get("STATE");
      return state.get(key);
    }
    catch (NodeNotFoundException e1)
    {
      return null;
    }
  }

  /**
   * Setzt einen Schlüssel-Wert-Paar unterhalb des STATE-Knotens. Ist der
   * Schlüssel bereits definiert, wird der bestehende Wert überschrieben. Sind
   * der STATE-Knoten oder der Schlüssel nicht definiert, so werden die
   * entsprechenden Knoten erzeugt und der Key key erhält ein Kindknoten mit dem
   * Value value.
   * 
   * @param key
   * @param value
   */
  private void setOrCreate(String key, String value)
  {
    // gewünschte Struktur aufbauen:

    // a) STATE(...)
    ConfigThingy state;
    try
    {
      state = wmCmd.get("STATE");
    }
    catch (NodeNotFoundException e1)
    {
      state = wmCmd.add("STATE");
    }

    // b) STATE(KEY ...)
    ConfigThingy ctKey;
    try
    {
      ctKey = state.get(key);
    }
    catch (NodeNotFoundException e)
    {
      ctKey = state.add(key);
    }

    // c) STATE(KEY 'value')
    try
    {
      ctKey.getFirstChild().setName(value);
    }
    catch (NodeNotFoundException e)
    {
      ctKey.add(value);
    }
  }
}
