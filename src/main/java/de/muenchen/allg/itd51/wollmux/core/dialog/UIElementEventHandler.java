/*
 * Dateiname: UIElementEventHandler.java
 * Projekt  : WollMux
 * Funktion : Interface für Klassen, die auf Events reagieren, die von UIElements verursacht werden.
 * 
 * Copyright (c) 2009-2015 Landeshauptstadt München
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
 * 11.01.2006 | BNK | Erstellung
 * 24.04.2006 | BNK | Kommentiert.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.dialog;

import de.muenchen.allg.itd51.wollmux.core.dialog.controls.UIElement;

/**
 * Interface für Klassen, die auf Events reagieren, die von UIElements verursacht
 * werden.
 */
public interface UIElementEventHandler
{
  /**
   * Wird aufgerufen, wenn auf einem UIElement ein Event registriert wird.
   * 
   * @param source
   *          das UIElement auf dem der Event registriert wurde.
   * @param eventType
   *          die Art des Events. Zur Zeit werden folgende Typen unterstützt (diese
   *          Liste kann erweitert werden, auch für existierende UIElemente; ein
   *          Handler sollte also zwingend den Typ überprüfen und unbekannte Typen
   *          ohne Fehlermeldung ignorieren):
   *          <dl>
   *          <dt>action</dt>
   *          <dd>Eine ACTION wurde ausgelöst (normalerweise durch einen Button).
   *          Das Array args enthält als erstes Element den Namen der ACTION. Falls
   *          die ACTION weitere Parameter benötigt, so werden diese in den folgenden
   *          Arrayelementen übergeben.</dd>
   *          <dt>valueChanged</dt>
   *          <dd>Wird von Elementen ausgelöst, die der Benutzer bearbeiten kann
   *          (zum Beispiel TextFields), wenn der Wert geändert wurde. Achtung! Es
   *          ist nicht garantiert, dass der Wert sich tatsächlich geändert hat.
   *          Dieser Event wird auch ausgelöst, wenn der Benutzer aus einer Auswahl
   *          (z.B. ComboBox) ein Element ausgewählt hat.</dd>
   *          <dt>focus</dt>
   *          <dd>Wird von Elementen ausgelöst, wenn sie den Focus bekommen oder
   *          verloren haben. Das Array args enthält als erstes Element entweder den
   *          String "lost", falls der Focus verloren wurde, oder "gained", falls das
   *          Element den Focus bekommen hat.</dd>
   *          </dl>
   */
  public void processUiElementEvent(UIElement source, String eventType, Object[] args);
}
