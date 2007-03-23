/*
* Dateiname: UIElementFactory.java
* Projekt  : WollMux
* Funktion : Erzeugt zu ConfigThingys passende UI Elemente.
* 
* Copyright: Landeshauptstadt München
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
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.Box;
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

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;


/**
 * Erzeugt zu ConfigThingys passende UI Elemente.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class UIElementFactory
{
  /**
   * Standardbreite für Textfelder und Textareas. Wird verwendet, wenn nicht
   * mit setTextfieldWidth() ein anderer Wert gesetzt wurde.
   */
  private final static int TEXTFIELD_DEFAULT_WIDTH = 22;
  
  /**
   * Wird in einer der übergebenen Maps ein TYPE nicht gefunden, so wird
   * stattdessen nach einem Eintrag für diesen Namen gesucht.
   */
  private static final String DEFAULT = "default";
  
  /**
   * Die Breite (in Zeichen) für Textfields und Textareas. Kann mit
   * {@link #setTextfieldWidth(int)} gesetzt werden.
   */
  private int textfieldWidth = TEXTFIELD_DEFAULT_WIDTH;
  
  /**
   * Erzeugt eine Factory, die aus {@link ConfigThingy}s Objekte des
   * Typs {@link UIElement} erzeugt. Die zu übergebenen Maps dürfen alle
   * null-Werte enthalten, die den entsprechenden Eigenschaften der erzeugten
   * UIElemente zugewiesen werden. 
   * 
   * @param mapTypeToLayoutConstraints bildet einen TYPE auf die dazugehörigen 
   *        layout constraints (d,i, der optionale zweite Parameter von 
   * {@link java.awt.Container#add(java.awt.Component, java.lang.Object) java.awt.Container.add()}) 
   * ab. Wird von {@link UIElement#getLayoutConstraints()} zurückgeliefert.
   * 
   * @param mapTypeToLabelType bildet einen TYPE auf einen Integer ab, der angibt, 
   * ob das UI Element ein zusätzliches Label links oder rechts bekommen soll. 
   * Mögliche Werte sind
   * {@link UIElement#LABEL_LEFT}, {@link UIElement#LABEL_RIGHT} und
   * {@link UIElement#LABEL_NONE}. Wird von {@link UIElement#getLabelType()}
   * zurückgeliefert.
   * 
   * @param mapTypeToLabelLayoutConstraints Für UI Elemente, die ein zusätzliches 
   * Label links oder rechts bekommen sollen
   * (siehe Parameter mapTypeToLabelType) liefert diese Map die layout constraints
   * für das Label. Achtung! UI Elemente mit TYPE "label" beziehen ihre
   * layout constraints nicht aus dieser Map, sondern wie alle anderen UI Elemente
   * auch aus mapTypeToLayoutConstraints.
   * 
   * @param supportedActions Die Menge (von Strings) der ACTIONs, die akzeptiert 
   * werden sollen. Alle anderen produzieren eine Fehlermeldung.
   * 
   * @param handler ist der {@link UIElementEventHandler}, an den die erzeugten 
   * UI Elemente ihre Ereignisse melden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) 
   */
  public UIElementFactory() {}
  
  /**
   * Setzt die Breite für erzeugte Textfields und Textareas auf anzahlZeichen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
    */
  public void setTextfieldWidth(int anzahlZeichen)
  {
    textfieldWidth = anzahlZeichen;
  }
  
  /**
   * Erzeugt aus der Spezifikation in conf (muss als Kind einen TYPE-Knoten haben)
   * ein passendes {@link UIElement}. Die folgenden TYPES werden unterstützt
   * <dl>
   *   <dt>button</dt>
   *   <dd>Ein normaler Button.</dd>
   *   
   *   <dt>menuitem</dt>
   *   <dd>Erzeugt ein JMenuItem, ist aber ansonsten genau wie ein "button".</dd>
   *   
   *   <dt>textfield</dt>
   *   <dd>Ein einzeiliges Textfeld.</dd>
   *   
   *   <dt>textarea</dt>
   *   <dd>Ein mehrzeiliger Texteingabebereich.</dd>
   *   
   *   <dt>combobox</dt>
   *   <dd>Eine Combobox.</dd>
   *   
   *   <dt>checkbox</dt>
   *   <dd>Eine Checkbox mit integriertem Label das immer rechts ist.</dd>
   *   
   *   <dt>listbox</dt>
   *   <dd>Eine Liste von Einträgen.</dd>
   *   
   *   <dt>v-separator</dt>
   *   <dd>Ein separator mit <b>vertikaler</b> Ausdehnung (z.B. ein senkrechter Strich).
   *       Diese Art Separator wir benutzt, um horizontal angeordnete Elemente zu
   *       trennen.</dd>
   *   
   *   <dt>h-separator</dt>
   *   <dd>Ein separator mit <b>horizontaler</b> Ausdehnung (z.B. ein horizontaler Strich).
   *       Diese Art Separator wir benutzt, um vertikal angeordnete Elemente zu
   *       trennen (z.B. in einem Pull-Down-Menü).</dd>
   *   
   *   <dt>h-glue</dt>
   *   <dd>Leerraum mit <b>horizontaler</b> Ausdehnung. Wird verwendet, um Abstand
   *   zwischen horizontal angeordneten Elementen zu schaffen.</dd>
   *   
   *   <dt>v-glue</dt>
   *   <dd>Leerraum mit <b>vertikaler</b> Ausdehnung. Wird verwendet, um Abstand
   *   zwischen vertikal angeordneten Elementen zu schaffen (z.B. in Pull-Down-Menüs).</dd>
   *   
   *   <dt>default</dt>
   *   <dd>Dieser TYPE wird als Fallback verwendet, wenn in einer der an
   *   den Konstruktor übergebenen Maps ein TYPE nicht gefunden wird.</dd>
   * </dl>
   * 
   * @param context Liefert Informationen für die Erstellung der UI Elemente.
   *  Ist für einen TYPE in einer Map kein Mapping
   * angegeben (auch kein null-Wert), so wird erst geschaut, ob ein Mapping
   * für "default" vorhanden ist. Falls ja, so wird dieses der entsprechenden
   * Eigenschaft des erzeugten UIElements zugewiesen, ansonsten null.
   * 
   * @return niemals null.
   * @throws ConfigurationErrorException falls irgendein Fehler in der Beschreibung
   *                des UI Elements gefunden wird.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public UIElement createUIElement(Context context, ConfigThingy conf) throws ConfigurationErrorException
  {
    String label = "";
    String tip = "";
    String id = "";
    String type = "";
    char hotkey = 0;
    String action = "";
    boolean readonly = false;
    boolean editable = false;
    Iterator iter = conf.iterator();
    while (iter.hasNext())
    {
      ConfigThingy node = (ConfigThingy)iter.next();
      String name = node.getName();
      String str = node.toString();

      if (name.equals("LABEL"))    label  = str; else 
      if (name.equals("TIP"))      tip    = str; else
      if (name.equals("ID"))       id     = str; else
      if (name.equals("TYPE"))     type   = str; else
      if (name.equals("HOTKEY"))   hotkey = str.length() > 0 ? str.charAt(0) : 0; else
      if (name.equals("ACTION"))   action = str; else
      if (name.equals("READONLY")) readonly = str.equals("true"); else
      if (name.equals("EDIT"))     editable = str.equals("true");
    }
    
    if (type.length() == 0) throw new ConfigurationErrorException("TYPE-Angabe fehlt bei Element mit Label \""+label+"\"");
    
    /*
     * Den richtigen type aus dem context bestimmen.
     */
    if (context.mapTypeToType != null && context.mapTypeToType.containsKey(type)) 
      type = (String)context.mapTypeToType.get(type);

    /*
     * ACHTUNG! Hier wird immer erst mit containsKey() getestet, anstatt
     * nur get() zu machen und auf null zu testen, weil null-Werte in
     * den Maps erlaubt sind und zurückgeliefert werden sollen.
     */
    
    Object layoutConstraints;
    if (context.mapTypeToLayoutConstraints.containsKey(type))
      layoutConstraints = context.mapTypeToLayoutConstraints.get(type);
    else
      layoutConstraints = context.mapTypeToLayoutConstraints.get(DEFAULT);
    
    Object labelLayoutConstraints;
    if (context.mapTypeToLabelLayoutConstraints.containsKey(type))
      labelLayoutConstraints = context.mapTypeToLabelLayoutConstraints.get(type);
    else
      labelLayoutConstraints = context.mapTypeToLabelLayoutConstraints.get(DEFAULT);
    /**
     * Falls nötig, erzeuge unabhängigen Klon der Layout Constraints.
     */
    if (layoutConstraints instanceof GridBagConstraints) 
      layoutConstraints = ((GridBagConstraints)layoutConstraints).clone();
    if (labelLayoutConstraints instanceof GridBagConstraints) 
      labelLayoutConstraints = ((GridBagConstraints)labelLayoutConstraints).clone();
        
    Integer labelType;
    if (context.mapTypeToLabelType.containsKey(type)) 
      labelType = (Integer)context.mapTypeToLabelType.get(type);
    else
      labelType = (Integer)context.mapTypeToLabelType.get(DEFAULT);
  
    UIElement uiElement;
    
    if (type.equals("button") ||
        type.equals("menuitem"))
    {
      AbstractButton button;
      if (type.equals("button"))
      {
        button = new JButton(label);
        copySpaceBindingToEnter(button);
      }
      else
        button = new JMenuItem(label);
      
      button.setMnemonic(hotkey);
      if (!tip.equals("")) button.setToolTipText(tip);
      uiElement = new UIElement.Button(id, button, layoutConstraints);
      
      ActionListener actionL = getAction(uiElement, action, conf, context.uiElementEventHandler, context.supportedActions);
      if (actionL != null) button.addActionListener(actionL);
      button.addFocusListener(new UIElementFocusListener(context.uiElementEventHandler, uiElement));
      return uiElement;
    }
    else if (type.equals("label"))
    {
      uiElement = new UIElement.Label(id, label, layoutConstraints);
      return uiElement;
    }
    else if (type.equals("textfield"))
    {
      JTextField tf = new JTextField(textfieldWidth);
      tf.setEditable(!readonly);
      tf.setFocusable(!readonly);
      if (!tip.equals("")) tf.setToolTipText(tip);
      uiElement = new UIElement.Textfield(id, tf, layoutConstraints, labelType, label, labelLayoutConstraints);
      tf.getDocument().addDocumentListener(new UIElementDocumentListener(context.uiElementEventHandler, uiElement, "valueChanged", new Object[]{}));
      tf.addFocusListener(new UIElementFocusListener(context.uiElementEventHandler, uiElement));
      if (action.length() > 0)
      {
        ActionListener actionL = getAction(uiElement, action, conf, context.uiElementEventHandler, context.supportedActions);
        if (actionL != null) tf.addActionListener(actionL);
      }
      return uiElement;
    }
    else if (type.equals("textarea"))
    {
      int lines = 3;
      boolean wrap = true;
      try{ lines = Integer.parseInt(conf.get("LINES").toString()); } catch(Exception x){}
      try{ wrap = conf.get("WRAP").toString().equalsIgnoreCase("true"); } catch(Exception x){}
      JTextArea textarea = new JTextArea(lines,textfieldWidth);
      textarea.setEditable(!readonly);
      textarea.setFocusable(!readonly);
      if (wrap)
      {
        textarea.setLineWrap(true);
        textarea.setWrapStyleWord(true);
      }
      textarea.setFont(new JTextField().getFont());
      if (!tip.equals("")) textarea.setToolTipText(tip);
      
      /*
       * Tab auch zum Weiterschalten und Shift-Tab zum Zurückschalten erlauben
       */
      Set focusKeys = textarea.getFocusTraversalKeys(
          KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
      focusKeys = new HashSet(focusKeys);
      focusKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
      textarea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,focusKeys);
      focusKeys = textarea.getFocusTraversalKeys(
          KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
      focusKeys = new HashSet(focusKeys);
      focusKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK ));
      textarea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,focusKeys);
      
      JPanel panel = new JPanel(new GridLayout(1,1));
      JScrollPane scrollPane = new JScrollPane(textarea);//, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, JScrollPane.VERTICAL_SCROLLBAR_NEVER);
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
      panel.add(scrollPane);
      uiElement = new UIElement.Textarea(id, textarea, panel, layoutConstraints, labelType, label, labelLayoutConstraints);
      textarea.getDocument().addDocumentListener(new UIElementDocumentListener(context.uiElementEventHandler, uiElement, "valueChanged", new Object[]{}));
      textarea.addFocusListener(new UIElementFocusListener(context.uiElementEventHandler, uiElement));
      return uiElement;
    }
    else if (type.equals("combobox"))
    {
      JComboBox combo = new JComboBox();
      combo.setEnabled(!readonly);
      combo.setFocusable(!readonly);
      combo.setEditable(editable);
      if (!tip.equals("")) combo.setToolTipText(tip);
      try
      {
        Iterator values = conf.get("VALUES").iterator();
        while (values.hasNext())
        {
          combo.addItem(values.next().toString());
        }
      } catch (Exception x) { Logger.error("Fehlerhaftes Element des Typs \"combobox\"",x); }
      
      uiElement = new UIElement.Combobox(id, combo, layoutConstraints, labelType, label, labelLayoutConstraints);
      
      if (editable) 
      {
        JTextComponent tc = ((JTextComponent)combo.getEditor().getEditorComponent());
        tc.addFocusListener(new UIElementFocusListener(context.uiElementEventHandler, uiElement));
        tc.getDocument().addDocumentListener(new UIElementDocumentListener(context.uiElementEventHandler, uiElement, "valueChanged", new Object[]{}));
      }
      else
      {
        combo.addItemListener(new UIElementItemListener(context.uiElementEventHandler, uiElement, "valueChanged", new Object[]{}));
        combo.addFocusListener(new UIElementFocusListener(context.uiElementEventHandler, uiElement));
      }
      return uiElement;
    }
    else if (type.equals("checkbox"))
    {
      /*
       * ACHTUNG! Diese checkbox hat ihr Label fest integriert auf der
       * rechten Seite und liefert als Zusatzlabel immer LABEL_NONE.
       */
      final JCheckBox boxBruceleitner = new JCheckBox(label);
      copySpaceBindingToEnter(boxBruceleitner);
      boxBruceleitner.setEnabled(!readonly);
      boxBruceleitner.setFocusable(!readonly);
      if (!tip.equals("")) boxBruceleitner.setToolTipText(tip);
      uiElement = new UIElement.Checkbox(id, boxBruceleitner, layoutConstraints);
      boxBruceleitner.addActionListener(new UIElementActionListener(context.uiElementEventHandler, uiElement, true, "valueChanged", new Object[]{}));
      boxBruceleitner.addFocusListener(new UIElementFocusListener(context.uiElementEventHandler, uiElement));
      return uiElement;
    }
    else if (type.equals("listbox"))
    {
      int lines = 10;
      try{ lines = Integer.parseInt(conf.get("LINES").toString()); } catch(Exception e){}
      
      JList list = new JList(new DefaultListModel());
      
      list.setVisibleRowCount(lines);
      list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      list.setLayoutOrientation(JList.VERTICAL);
      list.setPrototypeCellValue("Al-chman hemnal ulhillim el-WollMux(W-OLL-MUX-5.1)");
      
      JScrollPane scrollPane = new JScrollPane(list);
      scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
      
      uiElement = new UIElement.Listbox(id, scrollPane, list, layoutConstraints, labelType, label, labelLayoutConstraints); 
      
      list.addListSelectionListener(new UIElementListSelectionListener(context.uiElementEventHandler, uiElement, "listSelectionChanged", new Object[]{}));
      
      ActionListener actionL = getAction(uiElement, action, conf, context.uiElementEventHandler, context.supportedActions);
      if (actionL != null) list.addMouseListener(new MyActionMouseListener(list, actionL));
      return uiElement;
    }
    else if (type.equals("h-separator"))
    {
      JSeparator wurzelSepp = new JSeparator(SwingConstants.HORIZONTAL);
      return new UIElement.Separator(id, wurzelSepp, layoutConstraints);
    }
    else if (type.equals("v-separator"))
    {
      JSeparator wurzelSepp = new JSeparator(SwingConstants.VERTICAL);
      return new UIElement.Separator(id, wurzelSepp, layoutConstraints);
    }
    else if (type.equals("h-glue"))
    {
      int minsize = 0;
      int prefsize = 0;
      int maxsize = Integer.MAX_VALUE;
      try{ minsize = Integer.parseInt(conf.get("MINSIZE").toString()); }catch(Exception x){};
      try{ maxsize = Integer.parseInt(conf.get("MAXSIZE").toString()); }catch(Exception x){};
      try{ prefsize = Integer.parseInt(conf.get("PREFSIZE").toString()); }catch(Exception x){};
      
      return new UIElement.Box(id, new Box.Filler(new Dimension(minsize, 0), new Dimension(prefsize, 0), new Dimension(maxsize, Integer.MAX_VALUE)), layoutConstraints);
    }
    else if (type.equals("v-glue"))
    {
      int minsize = 0;
      int prefsize = 0;
      int maxsize = Integer.MAX_VALUE;
      try{ minsize = Integer.parseInt(conf.get("MINSIZE").toString()); }catch(Exception x){};
      try{ maxsize = Integer.parseInt(conf.get("MAXSIZE").toString()); }catch(Exception x){};
      try{ prefsize = Integer.parseInt(conf.get("PREFSIZE").toString()); }catch(Exception x){};
      
      return new UIElement.Box(id, new Box.Filler(new Dimension(0, minsize), new Dimension(0, prefsize), new Dimension(Integer.MAX_VALUE, maxsize)), layoutConstraints);
    }
    else
      throw new ConfigurationErrorException("Ununterstützter TYPE für GUI Element: \""+type+"\"");
  }

  private void copySpaceBindingToEnter(AbstractButton button)
  {
    InputMap imap = button.getInputMap(JComponent.WHEN_FOCUSED);
    
    Object binding = imap.get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true));
    if (binding != null) 
      imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), binding);
    
    binding = imap.get(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false));
    if (binding != null) 
      imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), binding);
  }

  /**
   * Wird als FocusListener auf UI Elemente registriert, um die auftretenden
   * Events an einen {@link UIElementEventHandler} weiterzureichen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class UIElementFocusListener implements FocusListener
  {
    private UIElementEventHandler handler;
    private UIElement uiElement;
    private static final String[] lost = new String[]{"lost"};
    private static final String[] gained = new String[]{"gained"};
    
    public UIElementFocusListener(UIElementEventHandler handler, UIElement uiElement)
    {
      this.handler = handler;
      this.uiElement = uiElement;
    }
    public void focusGained(FocusEvent e)
    {
      handler.processUiElementEvent(uiElement, "focus", gained);     
    }
    public void focusLost(FocusEvent e)
    {
      handler.processUiElementEvent(uiElement, "focus", lost);
    }
  }
  
  /**
   * Wird als ActionListener auf UI Elemente registriert, um die auftretenden
   * Events an einen {@link UIElementEventHandler} weiterzureichen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class UIElementActionListener implements ActionListener
  {
    private UIElementEventHandler handler;
    private UIElement uiElement;
    private String eventType;
    private Object[] args;
    private boolean takeFocus;
    
    public UIElementActionListener(UIElementEventHandler handler, UIElement uiElement, boolean takeFocus, String eventType, Object[] args)
    {
      this.handler = handler;
      this.uiElement = uiElement;
      this.takeFocus = takeFocus;
      this.eventType = eventType;
      this.args = args;
    }
    public void actionPerformed(ActionEvent e)
    {
      if (takeFocus && !uiElement.hasFocus()) uiElement.takeFocus();
      handler.processUiElementEvent(uiElement, eventType, args);
    }
  }
  
  /**
   * Wird als DocumentListener auf UI Elemente registriert, um die auftretenden
   * Events an einen {@link UIElementEventHandler} weiterzureichen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class UIElementDocumentListener implements DocumentListener
  {
    private UIElementEventHandler handler;
    private UIElement uiElement;
    private String eventType;
    private Object[] args;
    
    public UIElementDocumentListener(UIElementEventHandler handler, UIElement uiElement, String eventType, Object[] args)
    {
      this.handler = handler;
      this.uiElement = uiElement;
      this.eventType = eventType;
      this.args = args;
    }
    public void insertUpdate(DocumentEvent e)
    {
      handler.processUiElementEvent(uiElement, eventType, args);
    }
    public void removeUpdate(DocumentEvent e)
    {
      handler.processUiElementEvent(uiElement, eventType, args);
    }
    public void changedUpdate(DocumentEvent e)
    {
      handler.processUiElementEvent(uiElement, eventType, args);
    }
  }
  
  /**
   * Wird als ItemListener auf UI Elemente registriert, um die auftretenden
   * Events an einen {@link UIElementEventHandler} weiterzureichen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class UIElementItemListener implements ItemListener
  {
    private UIElementEventHandler handler;
    private UIElement uiElement;
    private String eventType;
    private Object[] args;
    
    public UIElementItemListener(UIElementEventHandler handler, UIElement uiElement, String eventType, Object[] args)
    {
      this.handler = handler;
      this.uiElement = uiElement;
      this.eventType = eventType;
      this.args = args;
    }
    public void itemStateChanged(ItemEvent e)
    {
      if (e.getStateChange() == ItemEvent.SELECTED)
        handler.processUiElementEvent(uiElement, eventType, args);
    }
  }
  
  /**
   * Wird als ListSelectionListener auf UI Elemente registriert, um die auftretenden
   * Events an einen {@link UIElementEventHandler} weiterzureichen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class UIElementListSelectionListener implements ListSelectionListener
  {
    private UIElementEventHandler handler;
    private UIElement uiElement;
    private String eventType;
    private Object[] args;
    
    public UIElementListSelectionListener(UIElementEventHandler handler, UIElement uiElement, String eventType, Object[] args)
    {
      this.handler = handler;
      this.uiElement = uiElement;
      this.eventType = eventType;
      this.args = args;
    }
    public void valueChanged(ListSelectionEvent e)
    {
      handler.processUiElementEvent(uiElement, eventType, args);
    }
  }
  
  /**
   * Wartet auf Doppelklick in eine JList und führt dann die 
   * actionPerformed() Methode eines ActionListeners aus.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyActionMouseListener extends MouseAdapter
  {
    private JList list;
    private ActionListener action;
    
    public MyActionMouseListener(JList list, ActionListener action)
    {
      this.list = list;
      this.action = action;
    }
    
    public void mouseClicked(MouseEvent e) 
    {
      if (e.getClickCount() == 2) 
      {
        Point location = e.getPoint();
        int index = list.locationToIndex(location);
        if (index < 0) return;
        Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(location)) return;
        action.actionPerformed(null);
      }
    }
  }
  
  /**
   * Liefert einen {@link UIElementActionListener} zurück, der ActionEvents von
   * uiElement an handler weitergibt, wobei der eventType "action" ist.
   * @param uiElement das uiElement zu dem der ActionListener gehört. Achtung!
   *        der ActionListener wird durch diese Methode nicht auf uiElement
   *        registriert!
   * @param action wird als erstes Element des args Arrays an die
   * Funktion {@link UIElementEventHandler#processUiElementEvent(UIElement, String, Object[])}
   * übergeben.
   * @param conf Manche ACTIONs erfordern zusätzliche Angaben (z.B. WINDOW Attribut
   * für die ACTION "switchWindow"). Damit diese ausgewertet und an handler
   * übergeben werden können muss hier das ConfigThingy des UI Elements übergeben
   * werden (also der Knoten, der TYPE als Kind hat).
   * @param handler der {@link UIElementEventHandler} an den die Events
   * weitergereicht werden sollen.
   * @return einen ActionListener, den man auf uiElement registrieren kann,
   * damit er dessen Actions an handler weiterreicht. Im Falle eines Fehlers 
   * (z.B. fehlende Zusatzangaben für ACTION die dieses erfordert) wird null
   * geliefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private ActionListener getAction(UIElement uiElement, String action, ConfigThingy conf, UIElementEventHandler handler, Set supportedActions)
  {
    if (!supportedActions.contains(action))
    {
      Logger.error("Ununterstützte ACTION \""+action+"\"");
      return null;
    }
    
    if (action.equals("switchWindow"))
    {
      try{
        String window = conf.get("WINDOW").toString();
        return new UIElementActionListener(handler, uiElement, false, "action", new Object[]{action, window});
      }catch(NodeNotFoundException x)
      {
        Logger.error("ACTION \"switchWindow\" erfordert WINDOW-Attribut");
      }
    }
    else if (action.equals("openTemplate") ||
             action.equals("openDocument"))
    {
      ConfigThingy fids = conf.query("FRAG_ID");
      if(fids.count() > 0) 
      {
        Iterator i = fids.iterator();
        String fragId = i.next().toString();
        while (i.hasNext())
        {
          fragId += "&" + i.next().toString();
        }
        return new UIElementActionListener(handler, uiElement, false, "action", new Object[]{action, fragId});
      }
      else
      {
        Logger.error("ACTION \""+action+"\" erfordert mindestens ein Attribut FRAG_ID");
      }
    }
    else if (action.equals("openExt"))
    {
      ConfigThingy ext = conf.query("EXT");
      if (ext.count() != 1)
      {
        Logger.error("ACTION \""+action+"\" erfordert genau ein Attribut EXT");
      }
      else
      {
        ConfigThingy url = conf.query("URL");
        if (url.count() != 1)
        {
          Logger.error("ACTION \""+action+"\" erfordert genau ein Attribut URL");
        }
        else
        {
          return new UIElementActionListener(handler, uiElement, false, "action", new Object[]{action, ext.toString(), url.toString()});
        }
      }
    }
    else if (action.equals("open"))
    {
      try
      {
        ConfigThingy openConf = conf.get("OPEN");
        return new UIElementActionListener(handler, uiElement, false, "action", new Object[]{action, openConf});
      }
      catch (NodeNotFoundException e)
      {
        Logger.error("ACTION \"open\" erfordert die Angabe OPEN \"...\"");
      }
    }
    else if (action.equals("funcDialog"))
    {
      try{
        String dialogName = conf.get("DIALOG").toString();
        return new UIElementActionListener(handler, uiElement, false, "action", new Object[]{action, dialogName});
      }catch(NodeNotFoundException x)
      {
        Logger.error("ACTION \"funcDialog\" erfordert DIALOG-Attribut");
      }
    }
    else 
    {
      return new UIElementActionListener(handler, uiElement, false, "action", new Object[]{action});
    }
    
    return null;
  }

  public static class Context
  {
    /**
     * Bildet einen TYPE auf die dazugehörigen layout constraints (d,i, der optionale
     * zweite Parameter von 
     * {@link java.awt.Container#add(java.awt.Component, java.lang.Object) java.awt.Container.add()}) 
     * ab. Darf null-Werte enthalten.  Ist für einen TYPE kein Mapping
   * angegeben (auch kein null-Wert), so wird erst geschaut, ob ein Mapping
   * für "default" vorhanden ist. Falls ja, so wird dieses der entsprechenden
   * Eigenschaft des erzeugten UIElements zugewiesen, ansonsten null.
     */
    public Map mapTypeToLayoutConstraints;
    
    /**
     * Bildet einen TYPE auf einen Integer ab, der angibt, ob das UI Element ein
     * zusätzliches Label links oder rechts bekommen soll. Mögliche Werte sind
     * {@link UIElement#LABEL_LEFT}, {@link UIElement#LABEL_RIGHT} und
     * {@link UIElement#LABEL_NONE}. Darf null-Werte enthalten. Ist für einen TYPE kein Mapping
   * angegeben (auch kein null-Wert), so wird erst geschaut, ob ein Mapping
   * für "default" vorhanden ist. Falls ja, so wird dieses der entsprechenden
   * Eigenschaft des erzeugten UIElements zugewiesen, ansonsten null.
     */
    public Map mapTypeToLabelType;
    
    /**
     * Für UI Elemente, die ein zusätzliches Label links oder rechts bekommen sollen
     * (siehe {@link #mapTypeToLabelType}) liefert diese Map die layout constraints
     * für das Label. Achtung! UI Elemente mit TYPE "label" beziehen ihre
     * layout constraints nicht aus dieser Map, sondern wie alle anderen UI Elemente
     * auch aus {@link #mapTypeToLayoutConstraints}. Darf null-Werte enthalten.
     * Ist für einen TYPE kein Mapping
   * angegeben (auch kein null-Wert), so wird erst geschaut, ob ein Mapping
   * für "default" vorhanden ist. Falls ja, so wird dieses der entsprechenden
   * Eigenschaft des erzeugten UIElements zugewiesen, ansonsten null.  
     */
    public Map mapTypeToLabelLayoutConstraints;
    
    /**
     * Die Menge (von Strings) der ACTIONs, die akzeptiert werden sollen. Alle 
     * anderen produzieren eine Fehlermeldung.
     */
    public Set supportedActions;
    
    /**
     * Der {@link UIElementEventHandler}, an den die erzeugten UI Elemente ihre
     * Ereignisse melden.
     */
    public UIElementEventHandler uiElementEventHandler;

    /**
     * Enthält diese Map für einen TYPE ein Mapping auf einen anderen TYPE, so
     * wird der andere TYPE verwendet. Dies ist nützlich, um abhängig vom Kontext
     * den TYPE "separator" entweder auf "h-separator" oder "v-separator" abzubilden.
     */
    public Map mapTypeToType;
  }

}
