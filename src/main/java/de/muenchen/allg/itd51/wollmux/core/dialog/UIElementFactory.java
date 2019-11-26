/*
 * Dateiname: UIElementFactory.java
 * Projekt  : WollMux
 * Funktion : Erzeugt zu ConfigThingys passende UI Elemente.
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
 * 05.01.2006 | BNK | Erstellung
 * 21.04.2006 | BNK | +Set supportedActions zum Angeben welche ACTIONs akzeptiert werden
 *                  | +TYPE "menuitem"
 *                  | +ACTION "openTemplate" und "openDocument"
 *                  | null-Werte in den Maps unterstützt
 * 24.04.2006 | BNK | Qualitätssicherung
 * 29.05.2006 | BNK | ordentliche Context-Klasse
 * 31.05.2006 | BNK | +funcDialog
 * 16.06.2006 | BNK | Beim Ändern eines Checkbox-Werts holt sich die Checkbox jetzt den Fokus
 * 13.09.2006 | BNK | Bei glues werden jetzt MINSIZE, MAXSIZE und PREFSIZE unterstützt.
 * 08.01.2007 | BNK | [R4698]WRAP-Attribut bei textareas
 *                  | [R4296]Wenn READONLY, dann nicht fokussierbar
 * 09.01.2007 | BNK | ENTER kann jetzt auch Checkboxen und Buttons aktivieren
 * 07.02.2007 | BNK | +ACTION "open"
 * 23.03.2007 | BNK | openExt implementiert
 * 02.06.2010 | BED | Unterstützung von ACTION "saveTempAndOpenExt"
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.dialog;

import java.awt.AWTKeyStroke;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.dialog.controls.Box;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.Button;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.Checkbox;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.Combobox;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.Label;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.Listbox;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.Separator;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.Textarea;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.Textfield;
import de.muenchen.allg.itd51.wollmux.core.dialog.controls.UIElement;
import de.muenchen.allg.itd51.wollmux.core.form.model.Control;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormType;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Erzeugt zu ConfigThingys passende UI Elemente.
 */
public class UIElementFactory
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(UIElementFactory.class);

  /**
   * Standardbreite für Textfelder und Textareas. Wird verwendet, wenn nicht mit
   * setTextfieldWidth() ein anderer Wert gesetzt wurde.
   */
  private static final int TEXTFIELD_DEFAULT_WIDTH = 22;

  /**
   * Die Breite (in Zeichen) für Textfields und Textareas. Kann mit
   * {@link #setTextfieldWidth(int)} gesetzt werden.
   */
  private int textfieldWidth = TEXTFIELD_DEFAULT_WIDTH;

  /**
   * Erzeugt eine Factory.
   */
  public UIElementFactory()
  {
    // nothing to initialize
  }

  /**
   * Setzt die Breite für erzeugte Textfields und Textareas auf anzahlZeichen.
   */
  public void setTextfieldWidth(int anzahlZeichen)
  {
    textfieldWidth = anzahlZeichen;
  }

  /**
   * Erzeugt aus der Spezifikation in conf (muss als Kind einen TYPE-Knoten haben) ein passendes
   * {@link UIElement}. Die folgenden TYPES werden unterstützt
   * <dl>
   * <dt>button</dt>
   * <dd>Ein normaler Button.</dd>
   * 
   * <dt>menuitem</dt>
   * <dd>Erzeugt ein JMenuItem, ist aber ansonsten genau wie ein "button".</dd>
   * 
   * <dt>textfield</dt>
   * <dd>Ein einzeiliges Textfeld.</dd>
   * 
   * <dt>textarea</dt>
   * <dd>Ein mehrzeiliger Texteingabebereich.</dd>
   * 
   * <dt>combobox</dt>
   * <dd>Eine Combobox.</dd>
   * 
   * <dt>checkbox</dt>
   * <dd>Eine Checkbox mit integriertem Label das immer rechts ist.</dd>
   * 
   * <dt>listbox</dt>
   * <dd>Eine Liste von Einträgen.</dd>
   * 
   * <dt>v-separator</dt>
   * <dd>Ein separator mit <b>vertikaler</b> Ausdehnung (z.B. ein senkrechter Strich). Diese Art
   * Separator wir benutzt, um horizontal angeordnete Elemente zu trennen.</dd>
   * 
   * <dt>h-separator</dt>
   * <dd>Ein separator mit <b>horizontaler</b> Ausdehnung (z.B. ein horizontaler Strich). Diese Art
   * Separator wir benutzt, um vertikal angeordnete Elemente zu trennen (z.B. in einem
   * Pull-Down-Menü).</dd>
   * 
   * <dt>h-glue</dt>
   * <dd>Leerraum mit <b>horizontaler</b> Ausdehnung. Wird verwendet, um Abstand zwischen horizontal
   * angeordneten Elementen zu schaffen.</dd>
   * 
   * <dt>v-glue</dt>
   * <dd>Leerraum mit <b>vertikaler</b> Ausdehnung. Wird verwendet, um Abstand zwischen vertikal
   * angeordneten Elementen zu schaffen (z.B. in Pull-Down-Menüs).</dd>
   * 
   * <dt>default</dt>
   * <dd>Dieser TYPE wird als Fallback verwendet, wenn in einer der an den Konstruktor übergebenen
   * Maps ein TYPE nicht gefunden wird.</dd>
   * </dl>
   * 
   * @param context
   *          Liefert Informationen für die Erstellung der UI Elemente. Ist für einen TYPE in einer
   *          Map kein Mapping angegeben (auch kein null-Wert), so wird erst geschaut, ob ein
   *          Mapping für "default" vorhanden ist. Falls ja, so wird dieses der entsprechenden
   *          Eigenschaft des erzeugten UIElements zugewiesen, ansonsten null.
   * 
   * @return niemals null.
   * @throws ConfigurationErrorException
   *           falls irgendein Fehler in der Beschreibung des UI Elements gefunden wird.
   */
  public UIElement createUIElement(UIElementContext context, Control control)
  {
    /*
     * Den richtigen type aus dem context bestimmen.
     */
    FormType type = context.getMappedType(control.getType());

    Object layoutConstraints = context.getLayoutConstraints(type);
    Object labelLayoutConstraints = context.getLabelLayoutConstraints(type);
    UIElement.LabelPosition labelType = context.getLabelType(type);

    switch (type)
    {
    case BUTTON:
    case MENUITEM:
      return createButton(context, control, layoutConstraints);
    case LABEL:
      return createLabel(control, layoutConstraints);
    case TEXTFIELD:
      return createTextfield(context, control, layoutConstraints, labelLayoutConstraints,
          labelType);
    case TEXTAREA:
      return createTextarea(context, control, layoutConstraints, labelLayoutConstraints, labelType);
    case COMBOBOX:
      return createCombobox(context, control, layoutConstraints, labelLayoutConstraints, labelType);
    case CHECKBOX:
      return createCheckbox(context, control, layoutConstraints);
    case H_SEPARATOR:
      JSeparator wurzelSepp = new JSeparator(SwingConstants.HORIZONTAL);
      return new Separator(control.getId(), wurzelSepp, layoutConstraints);
    case V_SEPARATOR:
      JSeparator sepp = new JSeparator(SwingConstants.VERTICAL);
      return new Separator(control.getId(), sepp, layoutConstraints);
    case H_GLUE:
      return new Box(control.getId(),
          new javax.swing.Box.Filler(new Dimension(control.getMinsize(), 0),
              new Dimension(control.getPrefsize(), 0),
              new Dimension(control.getMaxsize(), Integer.MAX_VALUE)),
          layoutConstraints);
    case V_GLUE:
      return new Box(control.getId(),
          new javax.swing.Box.Filler(new Dimension(0, control.getMinsize()),
              new Dimension(0, control.getPrefsize()),
              new Dimension(Integer.MAX_VALUE, control.getMaxsize())),
          layoutConstraints);
    case LISTBOX:
      return createListbox(context, control, layoutConstraints, labelLayoutConstraints, labelType);
    default:
      throw new ConfigurationErrorException(
          L.m("Ununterstützter TYPE für GUI Element: \"%1\"", type));
    }
  }

  private UIElement createListbox(UIElementContext context, Control control,
      Object layoutConstraints, Object labelLayoutConstraints,
      UIElement.LabelPosition labelType)
  {
    UIElement uiElement;

    JList<Object> list = new JList<>(new DefaultListModel<>());

    list.setVisibleRowCount(control.getLines());
    list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.setLayoutOrientation(JList.VERTICAL);
    list.setPrototypeCellValue(
        "Al-chman hemnal ulhillim el-WollMux(W-OLL-MUX-5.1)");

    JScrollPane scrollPane = new JScrollPane(list);
    scrollPane.setHorizontalScrollBarPolicy(
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

    uiElement = new Listbox(control.getId(), scrollPane, list, layoutConstraints,
        labelType,
        control.getLabel(), labelLayoutConstraints);

    list.addListSelectionListener(new UIElementListSelectionListener(
        context.getUiElementEventHandler(), uiElement, "listSelectionChanged",
        new Object[] {}));

    ActionListener actionL = getAction(uiElement, control,
        context.getUiElementEventHandler(), context.getSupportedActions());
    if (actionL != null)
      list.addMouseListener(new MyActionMouseListener(list, actionL));

    return uiElement;
  }

  private UIElement createCheckbox(UIElementContext context, Control control,
      Object layoutConstraints)
  {
    UIElement uiElement;
    /*
     * ACHTUNG! Diese checkbox hat ihr Label fest integriert auf der rechten Seite
     * und liefert als Zusatzlabel immer LABEL_NONE.
     */
    final JCheckBox boxBruceleitner = new JCheckBox(control.getLabel());
    copySpaceBindingToEnter(boxBruceleitner);
    boxBruceleitner.setEnabled(!control.isReadonly());
    boxBruceleitner.setFocusable(!control.isReadonly());
    if (!control.getTip().isEmpty())
    {
      boxBruceleitner.setToolTipText(control.getTip());
    }
    uiElement = new Checkbox(control.getId(), boxBruceleitner, layoutConstraints);
    boxBruceleitner.addActionListener(new UIElementActionListener(
        context.getUiElementEventHandler(), uiElement, true, "valueChanged",
        new Object[] {}));
    boxBruceleitner.addFocusListener(new UIElementFocusListener(
        context.getUiElementEventHandler(), uiElement));
    return uiElement;
  }

  private UIElement createCombobox(UIElementContext context, Control control,
      Object layoutConstraints, Object labelLayoutConstraints, UIElement.LabelPosition labelType)
  {
    UIElement uiElement;
    JComboBox<Object> combo = new JComboBox<>();
    combo.setEnabled(!control.isReadonly());
    combo.setFocusable(!control.isReadonly());
    combo.setEditable(control.isEditable());
    if (!control.getTip().isEmpty())
    {
      combo.setToolTipText(control.getTip());
    }
    control.getOptions().forEach(combo::addItem);

    uiElement = new Combobox(control.getId(), combo, layoutConstraints, labelType,
        control.getLabel(), labelLayoutConstraints);

    if (control.isEditable())
    {
      JTextComponent tc = ((JTextComponent) combo.getEditor()
          .getEditorComponent());
      tc.addFocusListener(new UIElementFocusListener(
          context.getUiElementEventHandler(), uiElement));
      tc.getDocument().addDocumentListener(
          new UIElementDocumentListener(context.getUiElementEventHandler(),
              uiElement,
              "valueChanged", new Object[] {}));
    }
    else
    {
      combo.addItemListener(new UIElementItemListener(
          context.getUiElementEventHandler(), uiElement, "valueChanged",
          new Object[] {}));
      combo.addFocusListener(new UIElementFocusListener(
          context.getUiElementEventHandler(), uiElement));
    }
    return uiElement;
  }

  private UIElement createTextarea(UIElementContext context, Control control,
      Object layoutConstraints, Object labelLayoutConstraints, UIElement.LabelPosition labelType)
  {
    UIElement uiElement;
    JTextArea textarea = new JTextArea(control.getLines(), textfieldWidth);
    textarea.setEditable(!control.isReadonly());
    textarea.setFocusable(!control.isReadonly());
    if (control.isWrap())
    {
      textarea.setLineWrap(true);
      textarea.setWrapStyleWord(true);
    }
    textarea.setFont(new JTextField().getFont());
    if (!control.getTip().isEmpty())
    {
      textarea.setToolTipText(control.getTip());
    }

    // Tab auch zum Weiterschalten und Shift-Tab zum Zurückschalten erlauben
    Set<AWTKeyStroke> focusKeys = textarea
        .getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
    focusKeys = new HashSet<>(focusKeys);
    focusKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
    textarea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, focusKeys);
    focusKeys = textarea.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
    focusKeys = new HashSet<>(focusKeys);
    focusKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK));
    textarea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, focusKeys);

    JPanel panel = new JPanel(new GridLayout(1, 1));
    JScrollPane scrollPane = new JScrollPane(textarea);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    panel.add(scrollPane);
    uiElement = new Textarea(control.getId(), textarea, panel, layoutConstraints, labelType,
        control.getLabel(), labelLayoutConstraints);
    textarea.getDocument().addDocumentListener(new UIElementDocumentListener(
        context.getUiElementEventHandler(), uiElement, "valueChanged", new Object[] {}));
    textarea.addFocusListener(
        new UIElementFocusListener(context.getUiElementEventHandler(), uiElement));
    return uiElement;
  }

  private UIElement createTextfield(UIElementContext context, Control control, 
      Object layoutConstraints, Object labelLayoutConstraints, UIElement.LabelPosition labelType)
  {
    UIElement uiElement;
    JTextField tf = new JTextField(textfieldWidth);
    tf.setEditable(!control.isReadonly());
    tf.setFocusable(!control.isReadonly());
    if (!control.getTip().isEmpty())
    {
      tf.setToolTipText(control.getTip());
    }
    uiElement = new Textfield(control.getId(), tf, layoutConstraints, labelType, control.getLabel(),
        labelLayoutConstraints);
    tf.getDocument().addDocumentListener(new UIElementDocumentListener(
        context.getUiElementEventHandler(), uiElement, "valueChanged", new Object[] {}));
    tf.addFocusListener(new UIElementFocusListener(context.getUiElementEventHandler(), uiElement));
    if (control.getAction() != null && control.getAction().length() > 0)
    {
      ActionListener actionL = getAction(uiElement, control, context.getUiElementEventHandler(),
          context.getSupportedActions());
      if (actionL != null)
      {
        ((JTextField) uiElement.getComponent()).addActionListener(actionL);
      }
    }
    return uiElement;
  }

  private UIElement createLabel(Control control, Object layoutConstraints)
  {
    UIElement uiElement;
    uiElement = new Label(control.getId(), control.getLabel(), layoutConstraints);
    return uiElement;
  }

  private UIElement createButton(UIElementContext context, Control control,
      Object layoutConstraints)
  {
    UIElement uiElement;
    AbstractButton button;
    if (FormType.BUTTON == context.getMappedType(control.getType()))
    {
      button = new JButton(control.getLabel());
      copySpaceBindingToEnter(button);
    } else
    {
      button = new JMenuItem(control.getLabel());
    }

    button.setMnemonic(control.getHotkey());
    if (!control.getTip().isEmpty())
    {
      button.setToolTipText(control.getTip());
    }
    uiElement = new Button(control.getId(), button, layoutConstraints);

    button.addFocusListener(
        new UIElementFocusListener(context.getUiElementEventHandler(), uiElement));
    ActionListener actionL = getAction(uiElement, control, context.getUiElementEventHandler(),
        context.getSupportedActions());
    if (actionL != null)
    {
      ((AbstractButton) uiElement.getComponent()).addActionListener(actionL);
    }
    return uiElement;
  }

  private void copySpaceBindingToEnter(AbstractButton button)
  {
    InputMap imap = button.getInputMap(JComponent.WHEN_FOCUSED);

    Object binding = imap
        .get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true));
    if (binding != null)
      imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), binding);

    binding = imap.get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false));
    if (binding != null)
      imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), binding);
  }

  /**
   * Liefert einen {@link UIElementActionListener} zurück, der ActionEvents von uiElement an handler
   * weitergibt, wobei der eventType "action" ist.
   * 
   * @param uiElement
   *          das uiElement zu dem der ActionListener gehört. Achtung! der ActionListener wird durch
   *          diese Methode nicht auf uiElement registriert!
   * @param action
   *          wird als erstes Element des args Arrays an die Funktion
   *          {@link UIElementEventHandler#processUiElementEvent(UIElement, String, Object[])}
   *          übergeben.
   * @param handler
   *          der {@link UIElementEventHandler} an den die Events weitergereicht werden sollen.
   * @param supportedActions
   *          Ist action nicht in dieser Menge enthalten gibt es einen Fehler.
   * @return einen ActionListener, den man auf uiElement registrieren kann, damit er dessen Actions
   *         an handler weiterreicht. Im Falle eines Fehlers (z.B. fehlende Zusatzangaben für ACTION
   *         die dieses erfordert) wird null geliefert.
   */
  private ActionListener getAction(UIElement uiElement, Control control,
      UIElementEventHandler handler, Set<String> supportedActions)
  {
    if (!supportedActions.contains(control.getAction()))
    {
      LOGGER.error(L.m("Ununterstützte ACTION \"%1\"", control.getAction()));
      return null;
    }

    if ("switchWindow".equals(control.getAction()))
    {
      if (control.getWindow() != null)
      {
        return new UIElementActionListener(handler, uiElement, false, "action",
            new Object[] { control.getAction(), control.getWindow() });
      } else
      {
        LOGGER.error(L.m("ACTION \"switchWindow\" erfordert WINDOW-Attribut"));
      }
    } else if ("openTemplate".equals(control.getAction())
        || "openDocument".equals(control.getAction()))
    {
      if (control.getFragId() != null && !control.getFragId().isEmpty())
      {
        return new UIElementActionListener(handler, uiElement, false, "action",
            new Object[] { control.getAction(), control.getFragId() });
      } else
      {
        LOGGER.error(
            L.m("ACTION \"%1\" erfordert mindestens ein Attribut FRAG_ID", control.getAction()));
      }
    } else if ("openExt".equals(control.getAction()))
    {
      if (control.getExt() == null)
      {
        LOGGER.error(L.m("ACTION \"%1\" erfordert genau ein Attribut EXT", control.getAction()));
      } else
      {
        if (control.getUrl() == null)
        {
          LOGGER.error(L.m("ACTION \"%1\" erfordert genau ein Attribut URL", control.getAction()));
        } else
        {
          return new UIElementActionListener(handler, uiElement, false, "action",
              new Object[] { control.getAction(), control.getExt(), control.getUrl() });
        }
      }
    } else if ("closeAndOpenExt".equals(control.getAction())
        || "saveTempAndOpenExt".equals(control.getAction()))
    {
      if (control.getExt() != null)
      {
        LOGGER.error(L.m("ACTION \"%1\" erfordert genau ein Attribut EXT", control.getAction()));
      } else
      {
        return new UIElementActionListener(handler, uiElement, false, "action",
            new Object[] { control.getAction(), control.getExt() });
      }
    } else if ("funcDialog".equals(control.getAction()))
    {
      if (control.getDialog() != null)
      {
        return new UIElementActionListener(handler, uiElement, false, "action",
            new Object[] { control.getAction(), control.getDialog() });
      } else
      {
        LOGGER.error(L.m("ACTION \"funcDialog\" erfordert DIALOG-Attribut"));
      }
    } else
    {
      return new UIElementActionListener(handler, uiElement, false, "action",
          new Object[] { control.getAction() });
    }

    return null;
  }

  /**
   * Wird als FocusListener auf UI Elemente registriert, um die auftretenden Events
   * an einen {@link UIElementEventHandler} weiterzureichen.
   */
  private static class UIElementFocusListener implements FocusListener
  {
    private UIElementEventHandler handler;

    private UIElement uiElement;

    private static final String[] lost = new String[] { "lost" };

    private static final String[] gained = new String[] { "gained" };

    public UIElementFocusListener(UIElementEventHandler handler,
        UIElement uiElement)
    {
      this.handler = handler;
      this.uiElement = uiElement;
    }

    @Override
    public void focusGained(FocusEvent e)
    {
      handler.processUiElementEvent(uiElement, "focus", gained);
    }

    @Override
    public void focusLost(FocusEvent e)
    {
      handler.processUiElementEvent(uiElement, "focus", lost);
    }
  }

  /**
   * Wird als ActionListener auf UI Elemente registriert, um die auftretenden Events
   * an einen {@link UIElementEventHandler} weiterzureichen.
   */
  private static class UIElementActionListener implements ActionListener
  {
    private UIElementEventHandler handler;

    private UIElement uiElement;

    private String eventType;

    private Object[] args;

    private boolean takeFocus;

    public UIElementActionListener(UIElementEventHandler handler,
        UIElement uiElement, boolean takeFocus, String eventType, Object[] args)
    {
      this.handler = handler;
      this.uiElement = uiElement;
      this.takeFocus = takeFocus;
      this.eventType = eventType;
      this.args = args;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if (takeFocus && !uiElement.hasFocus())
      {
        uiElement.takeFocus();
      }
      handler.processUiElementEvent(uiElement, eventType, args);
    }
  }

  /**
   * Wird als DocumentListener auf UI Elemente registriert, um die auftretenden
   * Events an einen {@link UIElementEventHandler} weiterzureichen.
   */
  private static class UIElementDocumentListener implements DocumentListener
  {
    private UIElementEventHandler handler;

    private UIElement uiElement;

    private String eventType;

    private Object[] args;

    public UIElementDocumentListener(UIElementEventHandler handler,
        UIElement uiElement, String eventType, Object[] args)
    {
      this.handler = handler;
      this.uiElement = uiElement;
      this.eventType = eventType;
      this.args = args;
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
      handler.processUiElementEvent(uiElement, eventType, args);
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
      handler.processUiElementEvent(uiElement, eventType, args);
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
      handler.processUiElementEvent(uiElement, eventType, args);
    }
  }

  /**
   * Wird als ItemListener auf UI Elemente registriert, um die auftretenden Events an
   * einen {@link UIElementEventHandler} weiterzureichen.
   */
  private static class UIElementItemListener implements ItemListener
  {
    private UIElementEventHandler handler;

    private UIElement uiElement;

    private String eventType;

    private Object[] args;

    public UIElementItemListener(UIElementEventHandler handler,
        UIElement uiElement,
        String eventType, Object[] args)
    {
      this.handler = handler;
      this.uiElement = uiElement;
      this.eventType = eventType;
      this.args = args;
    }

    @Override
    public void itemStateChanged(ItemEvent e)
    {
      if (e.getStateChange() == ItemEvent.SELECTED)
        handler.processUiElementEvent(uiElement, eventType, args);
    }
  }

  /**
   * Wird als ListSelectionListener auf UI Elemente registriert, um die auftretenden
   * Events an einen {@link UIElementEventHandler} weiterzureichen.
   */
  private static class UIElementListSelectionListener implements
      ListSelectionListener
  {
    private UIElementEventHandler handler;

    private UIElement uiElement;

    private String eventType;

    private Object[] args;

    public UIElementListSelectionListener(UIElementEventHandler handler,
        UIElement uiElement, String eventType, Object[] args)
    {
      this.handler = handler;
      this.uiElement = uiElement;
      this.eventType = eventType;
      this.args = args;
    }

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
      handler.processUiElementEvent(uiElement, eventType, args);
    }
  }

  /**
   * Wartet auf Doppelklick in eine JList und führt dann die actionPerformed()
   * Methode eines ActionListeners aus.
   */
  private static class MyActionMouseListener extends MouseAdapter
  {
    private JList<Object> list;

    private ActionListener action;

    public MyActionMouseListener(JList<Object> list, ActionListener action)
    {
      this.list = list;
      this.action = action;
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
      if (e.getClickCount() == 2)
      {
        Point location = e.getPoint();
        int index = list.locationToIndex(location);
        if (index < 0)
        {
          return;
        }
        Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(location))
        {
          return;
        }
        action.actionPerformed(null);
      }
    }
  }
}
