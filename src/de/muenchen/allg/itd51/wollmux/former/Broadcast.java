/*
 * Dateiname: Broadcast.java
 * Projekt  : WollMux
 * Funktion : Interface für Nachrichten auf dem globalen Broadcast-Kanal
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
 * 04.09.2006 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former;

/**
 * Interface für Nachrichten auf dem globalen Broadcast-Kanal. Siehe auch
 * {@link de.muenchen.allg.itd51.wollmux.former.BroadcastListener}.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface Broadcast
{
  /**
   * Sendet diese Broadcast-Nachricht an listener.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void sendTo(BroadcastListener listener);
}
