/*
* Dateiname: DialogLibrary.java
* Projekt  : WollMux
* Funktion : Eine Bibliothek von benannten Dialogs
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
* 03.05.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
*
*/
package de.muenchen.allg.itd51.wollmux.core.dialog;

import java.util.HashMap;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Eine Bibliothek von benannten Dialogs.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DialogLibrary
{
  private Map<String, Dialog> mapIdToDialog = new HashMap<>();
  private DialogLibrary baselib;


  /**
   * Erzeugt eine leere Dialogsbibliothek.
   */
  public DialogLibrary()
  {
    // empty dialog library
  }

  /**
   * Erzeugt eine Dialogsbibliothek, die baselib referenziert (nicht kopiert!).
   * baselib wird immer dann befragt, wenn die Dialogsbibliothek selbst keinen
   * Dialog des entsprechenden Namens enthält.
   * @param baselib
   */
  public DialogLibrary(DialogLibrary baselib)
  {
    this.baselib = baselib;
  }

  /**
   * Fügt dialog dieser Dialogsbibliothek unter dem Namen dlgName hinzu.
   */
  public void add(String dlgName, Dialog dialog)
  {
    if (dialog == null || dlgName == null)
    {
      throw new NullPointerException(L.m("Weder Dialogname noch Dialog darf null sein"));
    }
    mapIdToDialog.put(dlgName, dialog);
  }

  /**
   * Liefert den Dialog namens dlgName zurück oder null, falls kein Dialog
   * mit diesem Namen bekannt ist. Wurde die Dialogsbibliothek mit einer
   * Referenz auf eine andere Bibliothek initialisiert, so wird diese
   * befragt, falls die Dialogsbibliothek selbst keinen Dialog des entsprechenden
   * Namens kennt.
   */
  public Dialog get(String dlgName)
  {
    Dialog dialog = mapIdToDialog.get(dlgName);
    if (dialog == null && baselib != null)
    {
      dialog = baselib.get(dlgName);
    }
    return dialog;
  }

}
