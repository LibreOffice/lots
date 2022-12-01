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
package de.muenchen.allg.itd51.wollmux.former;

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

import de.muenchen.allg.itd51.wollmux.util.L;

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

  public ContextMenuMouseListener() {
      cutAction = new AbstractAction(L.m("Cut")) {

        private static final long serialVersionUID = -7972302689720579714L;

          @Override
          public void actionPerformed(ActionEvent ae) {
              textComponent.cut();
          }
      };

      popup.add(cutAction);

      copyAction = new AbstractAction(L.m("Copy")) {

        private static final long serialVersionUID = -3983750413260485527L;

          @Override
          public void actionPerformed(ActionEvent ae) {
              textComponent.copy();
          }
      };

      popup.add(copyAction);

      pasteAction = new AbstractAction(L.m("Insert")) {

        private static final long serialVersionUID = -1398305541404836086L;

          @Override
          public void actionPerformed(ActionEvent ae) {
              textComponent.paste();
          }
      };

      popup.add(pasteAction);
      popup.addSeparator();

      selectAllAction = new AbstractAction(L.m("Select all")) {

        private static final long serialVersionUID = 8875656666599278428L;

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
          // Nothing to do
        }
      });
  }

  @Override
  public void eventDispatched(AWTEvent event) {
      if (event instanceof MouseEvent) {
        MouseEvent  me = (MouseEvent)event;
      if (me.getID() == MouseEvent.MOUSE_CLICKED
          && me.getModifiersEx() == InputEvent.BUTTON3_DOWN_MASK)
      {
              if (!(event.getSource() instanceof JTextComponent)) {
                  return;
              }

              textComponent = (JTextComponent) event.getSource();
              textComponent.requestFocus();

              boolean enabled = textComponent.isEnabled();
              boolean editable = textComponent.isEditable();
              boolean nonempty = !(textComponent.getText() == null || textComponent.getText().isEmpty());
              boolean marked = textComponent.getSelectedText() != null;

              boolean pasteAvailable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null)
                  .isDataFlavorSupported(DataFlavor.stringFlavor);

              cutAction.setEnabled(enabled && editable && marked);
              copyAction.setEnabled(enabled && marked);
              pasteAction.setEnabled(enabled && editable && pasteAvailable);
              selectAllAction.setEnabled(enabled && nonempty);

              popup.show(me.getComponent(), me.getX(), me.getY());
        }
      }
  }
}
