/*
 * Dateiname: Dialog.java
 * Projekt  : WollMux
 * Funktion : Ein Dialog, der dem Benutzer erlaubt verschiedenen Werte zu setzen.
 * 
 * Copyright (c) 2008-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 04.05.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.dialog;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

/**
 * Ein Dialog, der dem Benutzer erlaubt verschiedenen Werte zu setzen.
 */
public interface Dialog
{
  /**
   * Liefert die Instanz dieses Dialogs für den gegebenen context (neu erstellt,
   * falls bisher noch nicht verwendet).
   * 
   * @param context
   *          Für jeden Kontext hält der Dialog eine unabhängige Kopie von seinem
   *          Zustand vor. Auf diese Weise lässt sich der Dialog an verschiedenen
   *          Stellen unabhängig voneinander einsetzen. ACHTUNG! Diese Map wird nicht
   *          als Schlüssel verwendet, sondern in ihr werden Werte abgelegt.
   * @throws ConfigurationErrorException
   *           wenn der Dialog mit fehlerhaften Daten initialisiert wurde (und der
   *           Fehler erst bei der Instanziierung diagnostiziert werden konnte).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Dialog instanceFor(Map<Object, Object> context)
      throws ConfigurationErrorException;

  /**
   * Liefert den durch id identifizierten Wert des Dialogs. Falls der Dialog noch
   * nicht aufgerufen wurde wird ein Standardwert geliefert (typischerweise der leere
   * String). Der Rückgabewert null ist ebenfalls möglich und signalisiert, dass der
   * Dialog das entsprechende Feld nicht hat und auch nie haben wird. Die Rückgabe
   * von null ist in diesem Fall allerdings nicht verpflichtend, sondern es ist
   * ebenfalls der leere String möglich. Die Rückgabe von null sollte jedoch
   * erfolgen, falls es dem Dialog irgendwie möglich ist.
   * 
   * Diese Funktion darf nur für mit instanceFor() erzeugte Instanzen aufgerufen
   * werden. Ansonsten liefert sie immer null. Diese Funktion ist Thread-safe.
   * Insbesondere muss sie nicht im EDT aufgerufen werden. Sie kann sowohl vor,
   * während als auch nach dem Aufruf von show() aufgerufen werden, auch nachdem der
   * Dialog schon geschlossen wurde.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getData(String id);

  /**
   * Liefert eine Menge von ids, für die {@link #getData(String)} niemals null
   * liefert. Dies ist nicht zwangsweise eine vollständige Liste aller ids, für die
   * der Dialog Werte zurückliefern kann. Es ist ebenfalls nicht garantiert, dass der
   * Dialog jemeils für eine dieser ids etwas anderes als den leeren String
   * zurückliefert. Diese Funktion kann schon vor instanceFor() aufgerufen werden, es
   * ist jedoch möglich, dass bei Aufruf für eine mit instanceFor() erzeugte Instanz
   * mehr Information (d.h. eine größere Menge) zurückgeliefert wird. Das
   * zurückgelieferte Objekt darf verändert werden. Dies hat keine Auswirkungen auf
   * den Dialog.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Collection<String> getSchema();

  /**
   * Zeigt den Dialog an. Diese Funktion darf nur für mit instanceFor() erzeugte
   * Instanzen aufgerufen werden. Ansonsten tut sie nichts.
   * 
   * @param dialogEndListener
   *          falls nicht null, wird die
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *          Methode aufgerufen (im Event Dispatching Thread), nachdem der Dialog
   *          geschlossen wurde. Das actionCommand des ActionEvents gibt die Aktion
   *          an, die das Beenden des Dialogs veranlasst hat.
   * @param funcLib
   *          falls der Dialog Funktionen auswertet, so werden Referenzen auf
   *          Funktionen mit dieser Bibliothek aufgelöst.
   * @param dialogLib
   *          falls der Dialog wiederum Funktionsdialoge unterstützt, so werden
   *          Referenzen auf Funktionsdialoge über diese Bibliothek aufgelöst.
   * @throws ConfigurationErrorException
   *           wenn der Dialog mit fehlerhaften Daten initialisiert wurde (und der
   *           Fehler erst bei der Anzeige diagnostiziert werden konnte).
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void show(ActionListener dialogEndListener, FunctionLibrary funcLib,
      DialogLibrary dialogLib) throws ConfigurationErrorException;
}
