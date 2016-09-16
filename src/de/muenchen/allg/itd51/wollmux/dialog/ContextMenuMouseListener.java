/*
 * Dateiname: ContextMenuMouseListener.java
 * Projekt  : WollMux
 * Funktion : MouseListener um ein Kontextmenü beim rechts mouse Klick zu erzeugen.
 * 
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
 * 06.05.2015 | SL | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Simona Loi (I23)
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Enthält ein MouseListener für das recht Mouse Klick. Das Mouse listener ist als AWTEventListener implementiert 
 * um eine globaler event listener zu haben, er reagiert an eine event in alle Komponente.
 * 
 * @author Simona Loi (I23)
 */


public class ContextMenuMouseListener implements AWTEventListener {
  private JPopupMenu popup = new JPopupMenu();

  private Action cutAction;
  private Action copyAction;
  private Action pasteAction;
  private Action selectAllAction;

  private JTextComponent textComponent;
  
  private enum Actions { CUT, COPY, PASTE, SELECT_ALL };

  public ContextMenuMouseListener() {
      cutAction = new AbstractAction(L.m("Ausschneiden")) {

          @Override
          public void actionPerformed(ActionEvent ae) {
              textComponent.cut();
          }
      };

      popup.add(cutAction);

      copyAction = new AbstractAction(L.m("Kopieren")) {

          @Override
          public void actionPerformed(ActionEvent ae) {
              textComponent.copy();
          }
      };

      popup.add(copyAction);

      pasteAction = new AbstractAction(L.m("Einfügen")) {

          @Override
          public void actionPerformed(ActionEvent ae) {
              textComponent.paste();
          }
      };

      popup.add(pasteAction);
      popup.addSeparator();

      selectAllAction = new AbstractAction(L.m("Alles markieren")) {

          @Override
          public void actionPerformed(ActionEvent ae) {
              textComponent.selectAll();
          }
      };

      popup.add(selectAllAction);
      
      popup.addPopupMenuListener(new PopupMenuListener()
      {
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e)
        {
          Common.setIsPopupVisible(true);
        }
        
        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
        {
          Common.setIsPopupVisible(false);
        }
        
        @Override
        public void popupMenuCanceled(PopupMenuEvent e)
        {
        }
      });
  }

  @Override
  public void eventDispatched(AWTEvent event) {   
      if (event instanceof MouseEvent) {
        MouseEvent  me = (MouseEvent)event;
        if(me.getID() == MouseEvent.MOUSE_CLICKED && me.getModifiers() == InputEvent.BUTTON3_MASK){
              if (!(event.getSource() instanceof JTextComponent)) {
                  return;
              }

              textComponent = (JTextComponent) event.getSource();
              textComponent.requestFocus();

              boolean enabled = textComponent.isEnabled();
              boolean editable = textComponent.isEditable();
              boolean nonempty = !(textComponent.getText() == null || textComponent.getText().equals(""));
              boolean marked = textComponent.getSelectedText() != null;

              boolean pasteAvailable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).isDataFlavorSupported(DataFlavor.stringFlavor);

              cutAction.setEnabled(enabled && editable && marked);
              copyAction.setEnabled(enabled && marked);
              pasteAction.setEnabled(enabled && editable && pasteAvailable);
              selectAllAction.setEnabled(enabled && nonempty);

              popup.show(me.getComponent(), me.getX(), me.getY());         
        }
      }
  }
}