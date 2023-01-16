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
package de.muenchen.allg.itd51.wollmux.former.group;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.group.model.GroupModel;
import de.muenchen.allg.itd51.wollmux.former.model.IdModel;
import de.muenchen.allg.itd51.wollmux.former.view.LineView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

public class OneGroupLineView extends LineView
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(OneGroupLineView.class);

  /**
   * Das Model zu dieser View.
   */
  private GroupModel model;

  /**
   * Typischerweise ein Container, der die View enthält und daher über Änderungen auf
   * dem Laufenden gehalten werden muss.
   */
  private ViewChangeListener bigDaddy;

  /**
   * Wird auf alle Teilkomponenten dieser View registriert.
   */
  private MyMouseListener myMouseListener = new MyMouseListener();

  /**
   * Wird vor dem Ändern eines Attributs des Models gesetzt, damit der rekursive
   * Aufruf des ChangeListeners nicht unnötigerweise das Feld updatet, das wir selbst
   * gerade gesetzt haben.
   */
  private boolean ignoreAttributeChanged = false;

  /**
   * Das TextField, das die ID der Gruppe anzeigt und ändern lässt.
   */
  private JTextField idBox;

  /**
   * Erzeugt eine neue View für model.
   */
  public OneGroupLineView(GroupModel model, ViewChangeListener bigDaddy,
      FormularMax4kController formularMax4000)
  {
    this.model = model;
    this.bigDaddy = bigDaddy;
    this.formularMax4000 = formularMax4000;
    myPanel = new JPanel();
    myPanel.setOpaque(true);
    myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.X_AXIS));
    myPanel.setBorder(BorderFactory.createEmptyBorder(BORDER, BORDER, BORDER, BORDER));
    myPanel.addMouseListener(myMouseListener);
    myPanel.add(makeIDView());
    unmarkedBackgroundColor = myPanel.getBackground();
    model.addListener(new MyModelChangeListener());
    myPanel.validate();
    // Notwendig, um BoxLayout beim Aufziehen daran zu hindern, die Textfelder
    // hässlich
    // hoch zu machen.
    myPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,
      myPanel.getPreferredSize().height));
  }

  /**
   * Liefert eine Komponente, die die ID anzeigt und Änderungen an das Model
   * weitergibt.
   */
  private JComponent makeIDView()
  {
    idBox = new JTextField(20);

    idBox.setText(model.getID().toString());
    final JTextComponent tc = idBox;
    final Document comboDoc = tc.getDocument();
    final Color defaultBackground = tc.getBackground();

    comboDoc.addDocumentListener(new DocumentListener()
    {
      public void update()
      {
        ignoreAttributeChanged = true;
        try
        {
          model.setID(comboDoc.getText(0, comboDoc.getLength()));
          tc.setBackground(defaultBackground);
        }
        catch (BadLocationException x)
        {
          LOGGER.error("", x);
        }
        catch (Exception x)
        {
          tc.setBackground(Color.RED);
        }
        ignoreAttributeChanged = false;
      }

      @Override
      public void insertUpdate(DocumentEvent e)
      {
        update();
      }

      @Override
      public void removeUpdate(DocumentEvent e)
      {
        update();
      }

      @Override
      public void changedUpdate(DocumentEvent e)
      {
        update();
      }
    });

    idBox.addMouseListener(myMouseListener);

    return idBox;
  }

  /**
   * Liefert das {@link GroupModel} das zu dieser View gehört.
   */
  public GroupModel getModel()
  {
    return model;
  }

  private void idChangedDueToExternalReasons(IdModel newId)
  {
    idBox.setText(newId.toString());
  }

  private class MyModelChangeListener implements GroupModel.ModelChangeListener
  {
    @Override
    public void modelRemoved(GroupModel model)
    {
      bigDaddy.viewShouldBeRemoved(OneGroupLineView.this);
    }

    @Override
    public void attributeChanged(GroupModel model, int attributeId, Object newValue)
    {
      if (!ignoreAttributeChanged && attributeId == GroupModel.ID_ATTR)
      {
        idChangedDueToExternalReasons((IdModel) newValue);
      }
    }
  }

  /**
   * Wird auf alle Teilkomponenten der View registriert. Setzt MousePressed-Events um
   * in Broadcasts, die signalisieren, dass das entsprechende Model selektiert wurde.
   * Je nachdem ob CTRL gedrückt ist oder nicht wird die Selektion erweitert oder
   * ersetzt.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyMouseListener extends MouseAdapter
  {

    @Override
    public void mousePressed(MouseEvent e)
    {
      int state = BroadcastObjectSelection.STATE_NORMAL_CLICK;
      if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK)
        state = BroadcastObjectSelection.STATE_CTRL_CLICK;
      else if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == InputEvent.SHIFT_DOWN_MASK)
        state = BroadcastObjectSelection.STATE_SHIFT_CLICK;

      formularMax4000.broadcast(new BroadcastObjectSelection(getModel(), state,
        state == BroadcastObjectSelection.STATE_NORMAL_CLICK)
      {
        @Override
        public void sendTo(BroadcastListener listener)
        {
          listener.broadcastGroupModelSelection(this);
        }
      });
    }
  }
}
