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
package de.muenchen.allg.itd51.wollmux.former.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.former.DuplicateIDException;
import de.muenchen.allg.itd51.wollmux.util.L;

public class IdModel
{
    /**
     * Die String-ID, die dieses Objekt repräsentiert.
     */
    private String id;

    /**
     * Die Map des verwaltenden IDManagers, in der diese ID gespeichert ist. Wird
     * verwendet, um Kollisionen zu überprüfen und das Mapping anzupassen, wenn der
     * ID-String dieses Objekts geändert wird.
     */
    private Map<String, IdModel> mapString2ID;

    /**
     * true bedeutet, dass irgendwo ein Objekt tatsächlich verwendet wird, das sich
     * mit dieser ID identifiziert. False bedeutet, dass alle Verwender dieser ID
     * damit nur ein anderes Objekt referenzieren wollen (das derzeit nicht
     * existiert).
     */
    private boolean active = false;

    /**
     * Liste von {@link WeakReference}s auf {@link IdModel.IDChangeListener}.
     */
    private List<WeakReference<IDChangeListener>> listeners = new ArrayList<>();

    /**
     * Erstellt ein neues ID Objekt, das inaktiv (siehe {@link #isActive()} ist.
     */
    public IdModel(Map<String, IdModel> mapString2ID, String id)
    {
      this.id = id;
      this.mapString2ID = mapString2ID;
    }

    /**
     * Liefert true, wenn irgendwo ein Objekt tatsächlich verwendet wird, das sich
     * mit dieser ID identifiziert. False bedeutet, dass alle Verwender dieser ID
     * damit nur ein anderes Objekt referenzieren wollen (das derzeit nicht
     * existiert).
     */
    public boolean isActive()
    {
      return active;
    }

    /**
     * Setzt diese ID auf {@link #isActive() aktiv} oder wirft
     * {@link DuplicateIDException}, falls sie es schon ist.
     */
    public void activate() throws DuplicateIDException
    {
      if (isActive()) throw new DuplicateIDException();
      active = true;
    }

    /**
     * Setzt diese ID auf {@link #isActive() inaktiv}.
     */
    public void deactivate()
    {
      active = false;
    }

    /**
     * listen wird benachrichtigt, wenn sich dieses ID-Objekt ändert. ACHTUNG! listen
     * wird nur über eine {@link java.lang.ref.WeakReference} referenziert. Das
     * heisst, dass der Aufrufer selbst eine Referenz auf den Listener am Leben
     * erhalten muss. Dafür ist der Aufruf von
     * {@link #removeIDChangeListener(IDChangeListener)} nur notwendig, wenn man
     * keine Events mehr empfangen möcht, nicht jedoch vor der Zerstörung des
     * Listeners.
     */
    public void addIDChangeListener(IDChangeListener listen)
    {
      for (WeakReference<IDChangeListener> changeListener : listeners)
      {
        IDChangeListener listener = changeListener.get();
        if (listener == null)
        {
          listeners.remove(changeListener);
        } else if (listener == listen)
        {
          return;
        }
      }

      listeners.add(new WeakReference<>(listen));
    }

    /**
     * listen wird NICHT MEHR benachrichtigt, wenn sich dieses ID-Objekt ändert.
     * ACHTUNG! listen wird von {@link #addIDChangeListener(IDChangeListener)} nur
     * über eine {@link java.lang.ref.WeakReference} referenziert. Daher ist der
     * Aufruf von {@link #removeIDChangeListener(IDChangeListener)} nur notwendig,
     * wenn man keine Events mehr empfangen möcht, nicht jedoch vor der Zerstörung
     * des Listeners.
     */
    public void removeIDChangeListener(IDChangeListener listen)
    {
      for (WeakReference<IDChangeListener> changeListener : listeners)
      {
        IDChangeListener listener = changeListener.get();

        if (listener == null || listener == listen)
        {
            listeners.remove(changeListener);
        }
      }
    }

    /**
     * Ändert die String-ID dieses Objekts auf newID und benachrichtigt alle
     * {@link IdModel.IDChangeListener}. Falls newID == {@link #getID()}, so
     * passiert nichts, es werden keine Listener benachrichtigt und es gibt keine
     * Exception. ACHTUNG! Normalerweise darf diese Funktion nur von dem Objekt
     * aufgerufen werden, das sich mit dieser ID identifiziert, nicht von Objekten
     * die diese ID nur als Referenz verwenden.
     *
     * @see #addIDChangeListener(IDChangeListener)
     * @throws DuplicateIDException
     *           wenn newID bereits im Namensraum dieses ID-Objekts verwendet wird.
     */
    public void setID(String newID) throws DuplicateIDException
    {
      if (newID.equals(id)) return;
      /*
       * Achtung! Hier wird bewusst nicht nach aktiven und inaktiven IDs
       * unterschieden. Man könnte versucht sein, Kollisionen mit inaktiven IDs
       * zuzulassen und so aufzulösen, dass die aktive ID die inaktive ID
       * "aufsammelt". Vorteile: Anlegen einer Einfügung mit nicht vergebener ID und
       * nachträgliches erzeugen eines Controls mit dieser ID würde funktionieren
       * Nachteile: - Während des Tippens einer neuen ID würde evtl. schon eine
       * ungewollte ID aufgesammelt. Beispiel: inaktive IDs "Anrede" und "Anrede2"
       * existieren parallel. Es würde bereits "Anrede" aufgesammelt während des
       * Tippens, auch wenn am Ende "Anrede2" gewünscht ist. - Eventuell noch
       * weitere. Die Folgen sind nicht so leicht abzuschätzen.
       */
      if (mapString2ID.containsKey(newID))
        throw new DuplicateIDException(L.m(
          "Collision while trying to change \"%1\" to \"%2\"", id, newID));
      mapString2ID.remove(id);
      id = newID;
      mapString2ID.put(id, this);

      for (WeakReference<IDChangeListener> changeListener : listeners)
      {
        IDChangeListener listener = changeListener.get();

        if (listener == null)
          listeners.remove(changeListener);
        else
          listener.idHasChanged(this);
      }

    }

    /**
     * Liefert die String-ID zurück, die dieses Objekt repräsentiert.
     */
    public String getID()
    {
      return id;
    }

    /**
     * wie {@link #getID()}.
     */
    @Override
    public String toString()
    {
      return id;
    }

    /**
     * Liefert true, wenn this == obj, da über den IDManager sichergestellt wird,
     * dass zu einem ID-String in einem Namensraum jeweils nur ein einziges ID-Objekt
     * existiert. Dies ist auch Voraussetzung dafür, dass die IDs ihre Funktion
     * erfüllen können.
     */
    @Override
    public boolean equals(Object obj)
    {
      return this == obj;
    }

    @Override
    public int hashCode()
    {
      return super.hashCode();
    }

    /**
     * Ein IDChangeListener wird benachrichtigt, wenn sich ein {@link IdModel}
     * Objekt ändert.
     *
     * @see #addIDChangeListener(IDChangeListener)
     */
    public interface IDChangeListener
    {
      public void idHasChanged(IdModel id);
    }
}
