/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Map;

import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.func.FunctionLibrary;

/**
 * A dialog that allows the user to set different values.
 */
public interface Dialog
{
  /**
   * Returns the instance of this dialog for the given context (newly created,
   * if not used yet).
   *
   * @param context
   *          For each context, the dialog keeps an independent copy of its
   *          state before. In this way, the dialogue can take place in different ways
   *          Use positions independently of each other. DANGER! This map will not
   *          used as a key, but values ​​are stored in it.
   * @throws ConfigurationErrorException
   *           if the dialog was initialized with erroneous data (and the
   *           error could only be diagnosed during instantiation).
   */
  public Dialog instanceFor(Map<Object, Object> context);

  /**
   * Returns the value of the dialog identified by id. If the dialogue still
   * was not called, a default value is returned (typically the empty
   * string). The return value null is also possible and indicates that the
   * Dialog does not and never will have the corresponding field. The return
   * of zero is not mandatory in this case, it is
   * the empty string is also possible. However, returning null should
   * take place if it is somehow possible for the dialogue.
   *
   * This function may only be called for instances created with instanceFor()
   * become. Otherwise it always returns zero. This function is thread safe.
   * In particular, it does not have to be called in EDT. She can
   * be called both during and after the call to show(), even after the
   * Dialog has already been closed.
   */
  public Object getData(String id);

  /**
   * Returns a set of ids for which {@link #getData(String)} is never null
   * delivers. This is not necessarily a complete list of all ids for which
   * the dialog can return values. It is also not guaranteed that the
   * Dialogue always displays something other than the empty string for one of these ids
   * returns. This function can be called before instanceFor(), it
   * However, it is possible that when called for an instance created with instanceFor()
   * more information (i.e. a larger amount) is returned. The
   * Returned object may be changed. This has no effect
   * the dialogue.
   */
  public Collection<String> getSchema();

  /**
   * Displays the dialog. This function may only be used for files created with instanceFor()
   * Instances are called. Otherwise she does nothing.
   *
   * @param dialogEndListener
   *          if not null, the
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *          Method called (on the event dispatching thread) after the dialog
   *          has been closed. The actionCommand of the ActionEvent returns the action
   *          that caused the dialog to end.
   * @param funcLib
   *          If the dialog evaluates functions, references are made to
   *          Functions resolved with this library.
   * @param dialogLib
   *          if the dialog in turn supports function dialogs, then
   *          Resolved references to function dialogs about this library.
   * @throws ConfigurationErrorException
   *           if the dialog was initialized with erroneous data (and the
   *          error could only be diagnosed when the display was displayed).
   */
  public void show(ActionListener dialogEndListener, FunctionLibrary funcLib, DialogLibrary dialogLib);
}
