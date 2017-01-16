/*
 * Dateiname: TrafoDialogParameters.java
 * Projekt  : WollMux
 * Funktion : Speichert diverse Parameter für den Aufruf von TrafoDialogen.
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
 * 01.02.2008 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

/**
 * Speichert diverse Parameter für den Aufruf von TrafoDialogen.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class TrafoDialogParameters
{
  /**
   * Gibt an, ob der Inhalt dieses Objekts gültig ist. Dieser Parameter hat nur eine
   * Bedeutung, wenn ein Dialog die TrafoDialogParameters bei seiner Beendigung an
   * den wartenden {@link #closeAction} übergibt. Wurde der Dialog abgebrochen ohne
   * dass gültige Änderungen vorgenommen wurden, ist dieses Feld auf false gesetzt
   * und die Daten dürfen nicht übernommen werden.
   */
  public boolean isValid = true;

  /**
   * Die Beschreibung der Funktion, mit der der Dialog vorbelegt werden soll.
   * Oberster Knoten ist ein beliebiger Bezeichner (typischerweise der
   * Funktionsname). Das ConfigThingy kann also direkt aus einem Funktionen-Abschnitt
   * eines Formulars übernommen werden.
   */
  public ConfigThingy conf;

  /**
   * Für Dialoge, die Feldnamen zur Auswahl stellen gibt diese Liste an, welche Namen
   * angeboten werden sollen.
   */
  public List<String> fieldNames;

  /**
   * Falls nicht null, so wird bei Beendigung des Dialogs dieser Listener aufgerufen,
   * wobei als source der TrafoDialog übergeben wird.
   */
  public ActionListener closeAction;

  public String toString()
  {
    StringBuilder buffy = new StringBuilder();
    buffy.append("isValid: ");
    buffy.append(isValid);
    buffy.append('\n');
    buffy.append("conf: ");
    if (conf == null)
      buffy.append("null");
    else
      buffy.append(conf.stringRepresentation());

    buffy.append("\nfieldNames: ");

    if (fieldNames == null)
      buffy.append("null");
    else
    {
      Iterator<String> iter = fieldNames.iterator();
      while (iter.hasNext())
      {
        buffy.append(iter.next());
        if (iter.hasNext()) buffy.append(", ");
      }
    }
    return buffy.toString();
  }
}
