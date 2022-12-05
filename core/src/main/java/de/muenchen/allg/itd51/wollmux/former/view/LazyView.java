/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former.view;

/**
 * Eine View die erst wenn sie das erste mal angezeigt wird initialisiert wird.
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public interface LazyView extends View
{
  /**
   * Muss aufgerufen werden, wenn die View angezeigt wird. Dies veranlasst die View,
   * sich komplett zu initialisieren. Vor dem Aufruf dieser Funktion ist nicht
   * garantiert, dass in der View tatsächlich Inhalt angezeigt wird.
   */
  public void viewIsVisible();

  /**
   * Sollte (muss aber nicht) aufgerufen werden, wenn die View nicht mehr angezeigt
   * wird. Sie kann dann falls möglich Ressourcen freigeben.
   */
  public void viewIsNotVisible();
}
