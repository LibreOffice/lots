package de.muenchen.allg.itd51.wollmux.core.functions;

/**
 * Ein Wert, der als verschiedene Datentypen abrufbar ist
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Value
{
  /**
   * Der aktuelle Wert als String. Falls es sich um einen booleschen Wert handelt,
   * wird der String "true" oder "false" zurückgeliefert.
   */
  public String getString();

  /**
   * Der aktuelle Wert als boolean. Falls der Wert seiner Natur nach ein String ist,
   * so ist das Ergebnis abhängig von der konkreten Implementierung.
   */
  public boolean getBoolean();
}
