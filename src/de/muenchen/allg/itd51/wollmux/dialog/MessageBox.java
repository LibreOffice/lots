/*
 * Dateiname: MessageBox.java
 * Projekt  : n/a
 * Funktion : TODO Funktionsbeschreibung
 * 
 * Copyright (c) 2010 Landeshauptstadt München
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
 * 10.03.2010 | ERT | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Andor Ertsey (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import com.sun.star.awt.VclWindowPeerAttribute;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;

public class MessageBox
{
  public final static String MESSAGE = "messbox";

  public final static String INFO = "infobox";

  public final static String ERROR = "errorbox";

  public final static String WARNING = "warningbox";

  public final static String QUERY = "querybox";

  public static void show(XWindowPeer parent, String msg, String title)
  {
    show(parent, msg, title, MESSAGE, VclWindowPeerAttribute.OK);
  }

  public static short show(XWindowPeer parent, String msg, String title,
      String type, int buttons)
  {
    try
    {
      WindowDescriptor desc = new WindowDescriptor();
      desc.Type = WindowClass.MODALTOP;
      desc.WindowServiceName = type;
      desc.ParentIndex = -1;
      desc.Parent = parent;
      desc.WindowAttributes =
        WindowAttribute.BORDER | WindowAttribute.CLOSEABLE
          | WindowAttribute.MOVEABLE | buttons;

      XToolkit toolkit =
        UNO.XToolkit(UNO.createUNOService("com.sun.star.awt.Toolkit"));
      XMessageBox msgbox =
        (XMessageBox) UnoRuntime.queryInterface(XMessageBox.class,
          toolkit.createWindow(desc));
      msgbox.setMessageText(msg);
      msgbox.setCaptionText(title);

      return msgbox.execute();
    }
    catch (com.sun.star.lang.IllegalArgumentException ex)
    {
      return -1;
    }
  }
}
