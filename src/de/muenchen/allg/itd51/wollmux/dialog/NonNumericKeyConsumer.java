/*
 * Dateiname: NonNumericKeyConsumer.java
 * Projekt  : WollMux
 * Funktion : Verschlingt alle KeyEvents die keine Ziffern oder Editierbefehle sind.

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
 * 11.07.2008 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Verschlingt alle KeyEvents die keine Ziffern oder Editierbefehle sind.
 */
public class NonNumericKeyConsumer implements KeyListener
{
  /**
   * Instanz des {@link NonNumericKeyConsumer}s.
   */
  public static final NonNumericKeyConsumer instance = new NonNumericKeyConsumer();

  public void keyTyped(KeyEvent e)
  {
    char c = e.getKeyChar();
    if (!((Character.isDigit(c) || (c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE))))
    {
      e.consume();
    }
  }

  public void keyPressed(KeyEvent e)
  {}

  public void keyReleased(KeyEvent e)
  {}
}
