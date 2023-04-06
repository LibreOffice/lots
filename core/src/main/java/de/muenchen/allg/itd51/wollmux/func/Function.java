/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.func;

import java.util.Collection;

/**
 * A function that calculates a value depending on parameters.
 */
public interface Function
{
  /**
   * Returns the names of the parameters expected by the function.
   * The order is undefined. No name can appear more than once.
   */
  public String[] parameters();

  /**
   * The names of all function dialogs that use this
   * Function referenced.
   */
  public void getFunctionDialogReferences(Collection<String> set);

  /**
   * Calls the function with arguments from parameters and returns that
   * Function result as a string. If it is a boolean value
   * is used, the string "true" or "false" is returned.
   * If an error occurs during execution, it may (this
   * depends on the function) the String object
   * {@link FunctionLibrary#ERROR} (== comparable) returned.
   * @param parameters should match any of those returned by {@link #parameters()}
   *        Names contain a string value.
   */
  public String getResult(Values parameters);

   /**
   * Calls the function with arguments from parameters and returns that
   * Function result as boolean. If the value by its nature a
   * is a string, true is returned if it (disregarding
   * case sensitive) matches the string "true".
   * If an error occurs during execution, false is returned.
   * @param parameters should match any of those returned by {@link #parameters()}
   *        Names contain a string value.
   */
  public boolean getBoolean(Values parameters);

}
