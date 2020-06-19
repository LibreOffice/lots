package de.muenchen.allg.itd51.wollmux.former.insertion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.UnknownIDException;
import de.muenchen.allg.itd51.wollmux.former.BroadcastListener;
import de.muenchen.allg.itd51.wollmux.former.BroadcastObjectSelection;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.view.LineView;
import de.muenchen.allg.itd51.wollmux.former.view.ViewChangeListener;

public class OneInsertionLineView extends LineView
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(OneInsertionLineView.class);

  /**
   * InsertValue erhält eine graue Hintergrundfarbe.
   */
  private Color insertValueBackground=Color.LIGHT_GRAY;

  /**
   * Das Model zu dieser View.
   */
  private InsertionModel model;

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
   * Die ComboBox, die DB_SPALTE bzw, ID anzeigt und ändern lässt.
   */
  private JComboBox<String> idBox;

  /**
   * Erzeugt eine neue View für model.
   */
  public OneInsertionLineView(InsertionModel model, ViewChangeListener bigDaddy,
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
   * Liefert eine Komponente, die je nach Art der Einfügung das DB_SPALTE oder ID
   * Attribut anzeigt und Änderungen an das Model weitergibt.
   */
  private JComponent makeIDView()
  {
    idBox = new JComboBox<>();

    if (!(this.model instanceof InsertionModel4InsertXValue))
    {
      idBox.setEditable(true);
      idBox.setSelectedItem("Spezialfeld");
      idBox.setEditable(false);
      idBox.setEnabled(false);
    }
    else
    {
      final InsertionModel4InsertXValue model =
        (InsertionModel4InsertXValue) this.model;
      idBox.setEditable(true);
      idBox.setSelectedItem(model.getDataID());
      final JTextComponent tc =
        ((JTextComponent) idBox.getEditor().getEditorComponent());
      final Document comboDoc = tc.getDocument();
      final Color defaultBackground;

      if(model.getSourceType()==0)
        defaultBackground = insertValueBackground;
      else
        defaultBackground = tc.getBackground();

      tc.setBackground(defaultBackground);

      tc.addMouseListener(myMouseListener);

      comboDoc.addDocumentListener(new DocumentListener()
      {
        public void update()
        {
          ignoreAttributeChanged = true;
          try
          {
            model.setDataID(comboDoc.getText(0, comboDoc.getLength()));
            tc.setBackground(defaultBackground);
          }
          catch (BadLocationException x)
          {
            LOGGER.error("", x);
          }
          catch (UnknownIDException x)
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
    }

    idBox.addMouseListener(myMouseListener);

    return idBox;
  }

  /**
   * Liefert das {@link InsertionModel} das zu dieser View gehört.
   */
  public InsertionModel getModel()
  {
    return model;
  }

  private void idChangedDueToExternalReasons(IDManager.ID newId)
  {
    idBox.setSelectedItem(newId.toString());
  }

  private class MyModelChangeListener implements InsertionModel.ModelChangeListener
  {
    @Override
    public void modelRemoved(InsertionModel model)
    {
      bigDaddy.viewShouldBeRemoved(OneInsertionLineView.this);
    }

    @Override
    public void attributeChanged(InsertionModel model, int attributeId,
        Object newValue)
    {
      if (ignoreAttributeChanged) return;
      switch (attributeId)
      {
        case InsertionModel4InsertXValue.ID_ATTR:
          idChangedDueToExternalReasons((IDManager.ID) newValue);
          break;
      }
    }
  }

  /**
   * Wird auf alle Teilkomponenten der View registriert. Setzt MousePressed-Events um
   * in Broadcasts, die signalisieren, dass das entsprechende Model selektiert wurde.
   * Je nachdem ob CTRL gedrückt ist oder nicht wird die Selektion erweitert oder
   * ersetzt.
   */
  private class MyMouseListener implements MouseListener
  {
    @Override
    public void mouseClicked(MouseEvent e)
    {}

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
          listener.broadcastInsertionModelSelection(this);
        }
      });

      if (state == BroadcastObjectSelection.STATE_NORMAL_CLICK)
      {
        try
        {
          getModel().selectWithViewCursor();
        } catch (UnoHelperException ex)
        {
          LOGGER.debug("", ex);
        }
      }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {}

    @Override
    public void mouseEntered(MouseEvent e)
    {}

    @Override
    public void mouseExited(MouseEvent e)
    {}
  }
}
