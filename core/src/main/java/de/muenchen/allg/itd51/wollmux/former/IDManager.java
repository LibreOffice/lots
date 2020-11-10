/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * verwaltet Objekte, die ID-Strings repräsentieren. Die ID-Objekte können an
 * mehreren Stellen verwendet werden und da jedes ID-Objekt alle seine Verwender
 * kennt (wenn sie sich als Listener registrieren) können Änderungen an der ID allen
 * Verwendern mitgeteilt werden.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class IDManager
{
  private Map<Object, HashMap<String, ID>> mapNamespace2mapString2ID = new HashMap<>();

  /**
   * Liefert ein {@link IDManager.ID}-Objekt zur String-ID id im Namensraum
   * namespace. Falls dieser Manager zu dieser String-ID noch kein Objekt hatte, wird
   * ein neues angelegt, ansonsten das bereits existierende zurückgeliefert. Wird ein
   * neues ID-Objekt angelegt, so ist dieses inaktiv (siehe
   * {@link IDManager.ID#isActive()}). Diese Funktion darf also nur von Aufrufern
   * verwendet werden, die die ID als Referenz auf ein anderes Objekt benötigen.
   * Aufrufer, die sich selbst mit der ID identifizieren wollen müssen
   * {@link #getActiveID(Object, String)} verwenden.
   *
   * @param namespace
   *          ein beliebiger Identifikator für den gewünschten Namensraum.
   */
  public ID getID(Object namespace, String id)
  {
    if (!mapNamespace2mapString2ID.containsKey(namespace))
      mapNamespace2mapString2ID.put(namespace, new HashMap<String, ID>());

    Map<String, ID> mapString2ID = mapNamespace2mapString2ID.get(namespace);

    if (!mapString2ID.containsKey(id))
      mapString2ID.put(id, new ID(mapString2ID, id));

    return mapString2ID.get(id);
  }

  /**
   * Falls dieser Manager im Namensraum namespace ein Objekt mit String-ID id hat, so
   * wird dieses zurückgeliefert, ansonsten null.
   */
  public ID getExistingID(Object namespace, String id)
  {
    if (!mapNamespace2mapString2ID.containsKey(namespace)) return null;

    Map<String, ID> mapString2ID = mapNamespace2mapString2ID.get(namespace);

    if (!mapString2ID.containsKey(id)) return null;

    return mapString2ID.get(id);
  }

  /**
   * Falls im angegebenen namespace bereits ein ID Objekt für die String-ID id
   * existiert und dieses {@link IDManager.ID#isActive()} aktiv ist, so wird eine
   * {@link DuplicateIDException} geworfen, ansonsten wird das existierende ID Objekt
   * aktiviert oder (falls noch keins existierte) ein aktiviertes ID Objekt neu
   * angelegt und dann zurückgeliefert. Diese Funktion ist dafür vorgesehen, von
   * Aufrufern verwendet zu werden, die sich selbst mit der ID identifizieren wollen.
   * Aufrufer, die die ID als Referenz auf ein anderes Objekt verwenden, müssen
   * {@link #getID(Object, String)} verwenden.
   *
   * @param namespace
   *          ein beliebiger Identifikator für den gewünschten Namensraum.
   */
  public ID getActiveID(Object namespace, String id) throws DuplicateIDException
  {
    ID idO = getID(namespace, id);
    idO.activate();
    return idO;
  }

  /**
   * Liefert eine {@link Collection} mit allen {@link IDManager.ID} Objekten, die im
   * Namensraum namespace registriert sind. ACHTUNG! Die zurückgegebene Collection
   * darf nicht geändert oder gespeichert werden, da sie direkt eine interne
   * Datenstruktur ist!
   */
  public Collection<ID> getAllIDs(Object namespace)
  {
    if (!mapNamespace2mapString2ID.containsKey(namespace))
      return new ArrayList<>();

    Map<String, ID> mapString2ID = mapNamespace2mapString2ID.get(namespace);
    return mapString2ID.values();
  }

  /**
   * Liefert eine sortierte {@link Collection} mit allen {@link IDManager.ID} Objekten, die im
   * Namensraum namespace registriert sind. ACHTUNG! Die zurückgegebene Collection
   * darf nicht geändert oder gespeichert werden, da sie direkt eine interne
   * Datenstruktur ist!
   */
  public Collection<ID> getAllIDsSorted(Object namespace)
  {
    if (!mapNamespace2mapString2ID.containsKey(namespace))
      return new ArrayList<>();

    Map<String, ID> mapString2ID = new TreeMap<>(mapNamespace2mapString2ID.get(namespace));

    return mapString2ID.values();
  }

  /**
   * Ein Objekt, das eine String-ID repräsentiert.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static class ID
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
    private Map<String, ID> mapString2ID;

    /**
     * true bedeutet, dass irgendwo ein Objekt tatsächlich verwendet wird, das sich
     * mit dieser ID identifiziert. False bedeutet, dass alle Verwender dieser ID
     * damit nur ein anderes Objekt referenzieren wollen (das derzeit nicht
     * existiert).
     */
    private boolean active = false;

    /**
     * Liste von {@link WeakReference}s auf {@link IDManager.IDChangeListener}.
     */
    private List<WeakReference<IDChangeListener>> listeners = new ArrayList<>();

    /**
     * Erstellt ein neues ID Objekt, das inaktiv (siehe {@link #isActive()} ist.
     */
    private ID(Map<String, ID> mapString2ID, String id)
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
      Iterator<WeakReference<IDChangeListener>> iter = listeners.iterator();
      while (iter.hasNext())
      {
        Reference<IDChangeListener> ref = iter.next();
        IDChangeListener listen2 = ref.get();
        if (listen2 == null)
          iter.remove();
        else if (listen2 == listen) return;
      }
      listeners.add(new WeakReference<IDChangeListener>(listen));
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
      Iterator<WeakReference<IDChangeListener>> iter = listeners.iterator();
      while (iter.hasNext())
      {
        Reference<IDChangeListener> ref = iter.next();
        IDChangeListener listen2 = ref.get();
        if (listen2 == null || listen2 == listen) iter.remove();
      }
    }

    /**
     * Ändert die String-ID dieses Objekts auf newID und benachrichtigt alle
     * {@link IDManager.IDChangeListener}. Falls newID == {@link #getID()}, so
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
          "Kollision beim Versuch ID von \"%1\" auf \"%2\" zu ändern", id, newID));
      mapString2ID.remove(id);
      id = newID;
      mapString2ID.put(id, this);
      Iterator<WeakReference<IDChangeListener>> iter = listeners.iterator();
      while (iter.hasNext())
      {
        Reference<IDChangeListener> ref = iter.next();
        IDChangeListener listen = ref.get();
        if (listen == null)
          iter.remove();
        else
          listen.idHasChanged(this);
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
  }

  /**
   * Ein IDChangeListener wird benachrichtigt, wenn sich ein {@link IDManager.ID}
   * Objekt ändert.
   *
   * @see IDManager.ID#addIDChangeListener(IDChangeListener)
   */
  public interface IDChangeListener
  {
    public void idHasChanged(ID id);
  }
}
