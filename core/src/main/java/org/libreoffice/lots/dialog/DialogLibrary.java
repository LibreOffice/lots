/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package org.libreoffice.lots.dialog;

import java.util.HashMap;
import java.util.Map;

import org.libreoffice.lots.util.L;

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
      throw new NullPointerException(L.m("Neither dialog name nor the dialog itself must be null"));
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
