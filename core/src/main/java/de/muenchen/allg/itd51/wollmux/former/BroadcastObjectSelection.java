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
package de.muenchen.allg.itd51.wollmux.former;

/**
 * Nachricht, dass in einer View ein Object ausgewählt wurde. Diese Nachricht wird
 * von anderen Views ausgewertet, um ihre Selektionen ebenfalls anzupassen.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public abstract class BroadcastObjectSelection implements Broadcast
{
  public static final int STATE_NORMAL_CLICK = 1;

  public static final int STATE_CTRL_CLICK = 0;

  public static final int STATE_SHIFT_CLICK = 2;

  /**
   * das {@link Object} das ausgewählt wurde.
   */
  private Object myObject;

  /**
   * -1 =&gt; abwählen, 1 =&gt; anwählen, 0: toggle.
   */
  private int state;

  /**
   * true =&gt; Selektion erst ganz löschen vor an/abwählen des Objektes.
   */
  private boolean clearSelection;

  /**
   * Erzeugt eine neue Nachricht.
   *
   * @param model
   *          das {@link Object} das ausgewählt wurde.
   * @param state
   *          -1 =&gt; abwählen, 1 =&gt; anwählen, 0: toggle
   * @param clearSelection
   *          true =&gt; Selektion erst ganz löschen vor an/abwählen von myObject.
   */
  protected BroadcastObjectSelection(Object model, int state, boolean clearSelection)
  {
    this.myObject = model;
    this.state = state;
    this.clearSelection = clearSelection;
  }

  public int getState()
  {
    return state;
  }

  /**
   * true =&gt; Selektion erst ganz löschen vor an/abwählen des Objekts.
   */
  public boolean getClearSelection()
  {
    return clearSelection;
  }

  /**
   * Liefert das Objekt zurück, das de/selektiert wurde.
   */
  public Object getObject()
  {
    return myObject;
  }

}
