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
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.Condition;
import de.muenchen.allg.itd51.wollmux.ConditionFactory;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;


/**
 * Erzeugt zu ConfigThingys passende UI Elemente.
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class UIElementFactory
{
  /**
   * Standardbreite für Textfelder.
   * TODO Als Parameter in irgendeiner Form an die UIElementFactory übergeben.
   */
  private final static int TEXTFIELD_DEFAULT_WIDTH = 22;
  
  private static final String DEFAULT = "default";
  private Map mapTypeToLayoutConstraints;
  private Map mapTypeToLabelType;
  private Map mapTypeToLabelLayoutConstraints;
  private Set supportedActions;
  private UIElementEventHandler uiElementEventHandler;
  
  /**
   * Wenn kein Mapping gefunden wird, wird ein Mapping für "default" gesucht.
   * TODO null Werte in den maps sind erlaubt und werden zurückgeliefert (es wird nicht DEFAULT konsultiert) 
   */
  public UIElementFactory(Map mapTypeToLayoutConstraints, Map mapTypeToLabelType,
                          Map mapTypeToLabelLayoutConstraints, 
                          Set supportedActions, UIElementEventHandler handler)
  {
    this.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
    this.mapTypeToLabelType = mapTypeToLabelType;
    this.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
    this.supportedActions = supportedActions;
    uiElementEventHandler = handler;
  }
  
  /**
   * TODO Doku mit vollständiger Liste der unterstützten Typen (inkl. h-separator et al)
   * TODO Was passiert, wenn in einer map fuer einen Typ z.B. kein labelType oder keine LayoutConstraints gefunden werden (DEFAULT wird herangezogen)
   * @param context bildet (falls non-null) Typnamen auf andere ab. Auf diese Weise lassen sich
   *                unterschiedliche Interpretationen des selben Typs in verschiedenen
   *                Kontexten realisieren. Z.B. könnte in dieser Map der Typ "button" auf
   *                "menuitem" abgebildet werden.
   * @param conf
   * @return niemals null.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   * @throws ConfigurationErrorException falls irgendein Fehler in der Beschreibung
   *                                     des UI Elements gefunden wird.
   */
  public UIElement createUIElement(Map context, ConfigThingy conf) throws ConfigurationErrorException
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
    if (context != null && context.containsKey(type)) 
      type = (String)context.get(type);

    /*
     * ACHTUNG! Hier wird immer erst mit containsKey() getestet, anstatt
     * nur get() zu machen und auf null zu testen, weil null-Werte in
     * den Maps erlaubt sind und zurückgeliefert werden sollen.
     */
    
    Object layoutConstraints;
    if (mapTypeToLayoutConstraints.containsKey(type))
      layoutConstraints = mapTypeToLayoutConstraints.get(type);
    else
      layoutConstraints = mapTypeToLayoutConstraints.get(DEFAULT);
    
    Object labelLayoutConstraints;
    if (mapTypeToLabelLayoutConstraints.containsKey(type))
      labelLayoutConstraints = mapTypeToLabelLayoutConstraints.get(type);
    else
      labelLayoutConstraints = mapTypeToLabelLayoutConstraints.get(DEFAULT);
    /**
     * Falls nötig, erzeuge unabhängigen Klon der Layout Constraints.
     */
    if (layoutConstraints instanceof GridBagConstraints) 
      layoutConstraints = ((GridBagConstraints)layoutConstraints).clone();
    if (labelLayoutConstraints instanceof GridBagConstraints) 
      labelLayoutConstraints = ((GridBagConstraints)labelLayoutConstraints).clone();
        
    Integer labelType;
    if (mapTypeToLabelType.containsKey(type)) 
      labelType = (Integer)mapTypeToLabelType.get(type);
    else
      labelType = (Integer)mapTypeToLabelType.get(DEFAULT);
  
    UIElement uiElement;
    Condition constraints = ConditionFactory.getGrandchildCondition(conf.query("PLAUSI"));
    
    if (type.equals("button") ||
        type.equals("menuitem"))
    {
      AbstractButton button;
      if (type.equals("button"))
        button = new JButton(label);
      else
        button = new JMenuItem(label);
      
      button.setMnemonic(hotkey);
      if (!tip.equals("")) button.setToolTipText(tip);
      uiElement = new UIElement.Button(id, button, layoutConstraints);
      
      ActionListener actionL = getAction(uiElement, action, conf, uiElementEventHandler);
      if (actionL != null) button.addActionListener(actionL);
      return uiElement;
    }
    else if (type.equals("label"))
    {
      uiElement = new UIElement.Label(id, label, layoutConstraints);
      return uiElement;
    }
    else if (type.equals("textfield"))
    {
      JTextField tf = new JTextField(TEXTFIELD_DEFAULT_WIDTH);
      tf.setEditable(!readonly);
      if (!tip.equals("")) tf.setToolTipText(tip);
      uiElement = new UIElement.Textfield(id, tf, layoutConstraints, labelType, label, labelLayoutConstraints, constraints);
      tf.getDocument().addDocumentListener(new UIElementDocumentListener(uiElementEventHandler, uiElement, "valueChanged", new Object[]{}));
      return uiElement;
    }
    else if (type.equals("textarea"))
    {
      int lines = 3;
      try{ lines = Integer.parseInt(conf.get("LINES").toString()); } catch(Exception x){}
      JTextArea textarea = new JTextArea(lines,TEXTFIELD_DEFAULT_WIDTH);
      textarea.setEditable(!readonly);
      textarea.setFont(new JTextField().getFont());
      if (!tip.equals("")) textarea.setToolTipText(tip);
      
      JPanel panel = new JPanel(new GridLayout(1,1));
      JScrollPane scrollPane = new JScrollPane(textarea);//, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, JScrollPane.VERTICAL_SCROLLBAR_NEVER);
      scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
      panel.add(scrollPane);
      uiElement = new UIElement.Textarea(id, textarea, panel, layoutConstraints, labelType, label, labelLayoutConstraints, constraints);
      textarea.getDocument().addDocumentListener(new UIElementDocumentListener(uiElementEventHandler, uiElement, "valueChanged", new Object[]{}));
      return uiElement;
    }
    else if (type.equals("combobox"))
    {
      JComboBox combo = new JComboBox();
      combo.setEnabled(!readonly);
      combo.setEditable(editable);
      if (!tip.equals("")) combo.setToolTipText(tip);
      try
      {
        Iterator values = conf.get("VALUES").iterator();
        while (values.hasNext())
        {
          combo.addItem(values.next().toString());
        }
      } catch (Exception x) { Logger.error(x); }
      
      uiElement = new UIElement.Combobox(id, combo, layoutConstraints, labelType, label, labelLayoutConstraints, constraints);
      if (editable) 
        ((JTextComponent)combo.getEditor().getEditorComponent()).getDocument().addDocumentListener(new UIElementDocumentListener(uiElementEventHandler, uiElement, "valueChanged", new Object[]{}));
      else
        combo.addItemListener(new UIElementItemListener(uiElementEventHandler, uiElement, "valueChanged", new Object[]{}));
      return uiElement;
    }
    else if (type.equals("checkbox"))
    {
      final JCheckBox boxBruceleitner = new JCheckBox();
      JPanel agentinMitHerz = new JPanel(new FlowLayout(FlowLayout.LEADING, 5,0));
      agentinMitHerz.add(boxBruceleitner);
      JLabel herzileinMusstNichtTraurigSein = new JLabel(label);
      if (!tip.equals("")) herzileinMusstNichtTraurigSein.setToolTipText(tip);
      if (!tip.equals("")) boxBruceleitner.setToolTipText(tip);
      agentinMitHerz.add(herzileinMusstNichtTraurigSein);
      herzileinMusstNichtTraurigSein.addMouseListener(new MouseAdapter()
          {
            public void mouseClicked(MouseEvent e)
            {
              if (!e.isPopupTrigger())
                boxBruceleitner.doClick();
            }
          });
      uiElement = new UIElement.Checkbox(id, boxBruceleitner, agentinMitHerz, layoutConstraints);
      boxBruceleitner.addActionListener(new UIElementActionListener(uiElementEventHandler, uiElement, "valueChanged", new Object[]{}));
      
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
      Box box = Box.createHorizontalBox();
      try{
        int minsize = Integer.parseInt(conf.get("MINSIZE").toString());
        box.add(Box.createHorizontalStrut(minsize));
      }catch(Exception e){}
      box.add(Box.createHorizontalGlue());
      return new UIElement.Box(id, box, layoutConstraints);
    }
    else if (type.equals("v-glue"))
    {
      Box box = Box.createVerticalBox();
      try{
        int minsize = Integer.parseInt(conf.get("MINSIZE").toString());
        box.add(Box.createVerticalStrut(minsize));
      }catch(Exception e){}
      box.add(Box.createVerticalGlue());
      return new UIElement.Box(id, box, layoutConstraints);
    }
    else
      throw new ConfigurationErrorException("Ununterstützter TYPE für GUI Element: \""+type+"\"");
      
    //Sollte nie erreicht werden. Entweder wir liefern ein UIElement oder wir
    //werfen eine Exception mit problemspezifischer Meldung.
//    throw new ConfigurationErrorException("Interner Fehler #637: Gesuchte Metapher nicht terminiert.");
  }

  private static class UIElementActionListener implements ActionListener
  {
    private UIElementEventHandler handler;
    private UIElement uiElement;
    private String eventType;
    private Object[] args;
    
    public UIElementActionListener(UIElementEventHandler handler, UIElement uiElement, String eventType, Object[] args)
    {
      this.handler = handler;
      this.uiElement = uiElement;
      this.eventType = eventType;
      this.args = args;
    }
    public void actionPerformed(ActionEvent e)
    {
      handler.processUiElementEvent(uiElement, eventType, args);
    }
  }
  
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
  
  private ActionListener getAction(UIElement uiElement, String action, ConfigThingy conf, UIElementEventHandler handler)
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
        return new UIElementActionListener(handler, uiElement, "action", new Object[]{action, window});
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
        return new UIElementActionListener(handler, uiElement, "action", new Object[]{action, fragId});
      }
      else
      {
        Logger.error("ACTION \""+action+"\" erfordert mindestens ein Attribut FRAG_ID");
      }
    }
    else 
    {
      return new UIElementActionListener(handler, uiElement, "action", new Object[]{action});
    }
    
    return null;
  }



}
