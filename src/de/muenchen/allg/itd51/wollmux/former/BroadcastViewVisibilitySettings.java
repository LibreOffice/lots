/*
 * Dateiname: BroadcastViewVisibilitySettings.java
 * Projekt  : WollMux
 * Funktion : Änderung des ViewVisibilityDescriptors.
 * 
 * Copyright (c) 2008-2016 Landeshauptstadt München
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
 * 19.07.2007 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former;

/**
 * Änderung des
 * {@link de.muenchen.allg.itd51.wollmux.former.ViewVisibilityDescriptor}s.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class BroadcastViewVisibilitySettings implements Broadcast
{
  private ViewVisibilityDescriptor desc;

  public BroadcastViewVisibilitySettings(ViewVisibilityDescriptor desc)
  {
    this.desc = desc;
  }

  public void sendTo(BroadcastListener listener)
  {
    listener.broadcastViewVisibilitySettings(desc);
  }
}
