/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import de.muenchen.allg.itd51.wollmux.former.model.ID;


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

}
