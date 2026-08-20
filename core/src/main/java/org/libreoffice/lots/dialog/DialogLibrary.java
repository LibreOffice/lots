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
 * A library of named dialogs.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DialogLibrary
{
  private Map<String, Dialog> mapIdToDialog = new HashMap<>();
  private DialogLibrary baselib;


  /**
   * Creates an empty dialog library.
   */
  public DialogLibrary()
  {
    // empty dialog library
  }

  /**
   * Creates a dialog library that references (not copies!) baselib.
   * baselib is always consulted if the dialog library itself does not have one
   * Dialog contains the corresponding name.
   * @param baselib
   */
  public DialogLibrary(DialogLibrary baselib)
  {
    this.baselib = baselib;
  }

  /**
   * Adds dialog to this dialog library under the name dlg name.
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
   *  Returns the dialog named dlgName or null if no dialog
   *  known by that name. Was the dialog library with a
   *  Initialized reference to another library, this one will be used
   *  queried if the dialog library itself does not have a dialog of the corresponding
   *  Knows name.
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
