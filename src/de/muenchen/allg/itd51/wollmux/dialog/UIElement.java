/*
* Dateiname: UIElement.java
* Projekt  : WollMux
* Funktion : Interface das von den konkreten UI Elementen (Combobox etc.) abstrahiert.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 11.01.2006 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Color;
import java.awt.Component;
import java.util.Collection;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;


/**
 * Interface das von den konkreten UI Elementen (Combobox etc.) abstrahiert. 
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface UIElement
{
  public static final Integer LABEL_NONE = new Integer(0);
  public static final Integer LABEL_LEFT = new Integer(-1);
  public static final Integer LABEL_RIGHT = new Integer(+1);

  /**
   * LABEL_LEFT, LABEL_RIGHT, LABEL_NONE 
   */
  public Integer getLabelType();
  /**
   * Label, nur definiert falls getLabelType() ungleich 0.
   */
  public Component getLabel();
  /**
   * Der funktionale Teil des UIElements. 
   */
  public Component getComponent();
  
  /**
   * Liefert das empfohlene zweite Argument für 
   * {@link java.awt.Container#add(java.awt.Component, java.lang.Object)}
   * für die funktionale Komponente dieses UI Elements.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getLayoutConstraints();
  
  /**
   * Liefert das empfohlene zweite Argument für 
   * {@link java.awt.Container#add(java.awt.Component, java.lang.Object)}
   * für das Label dieses UIElements. Nur definiert, 
   * falls getLabelType() ungleich 0.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getLabelLayoutConstraints();
  
  /**
   * Die Beschränkungen der möglichen Werte des UI Elements. null, falls keine
   * gesetzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Constraints getConstraints();
  
  /**
   * Der aktuelle Wert des UI Elements. Falls es sich um ein boolesches
   * Element (Checkbox, Radio Button) handelt, wird im angeschalteten Fall der
   * String "true", im ungesetzten Fall "false" zurückgeliefert.
   * Im Falle eines Buttons oder eines anderen Elements das keinen sinnvollen
   * Wert hat, wird immer "false" geliefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getString();
  
  /**
   * Falls es sich um ein boolesches Element (Checkbox, Radio Button) handelt,
   * wird true geliefert falls das Element angeschaltet ist, ansonsten false.
   * Im Falle von Text-Elementen wird true geliefert, falls der aktuelle
   * Wert nicht der leere String ist.
   * Im Falle eines Buttons oder eines anderen Elements das keinen sinnvollen
   * Wert hat, wird immer false geliefert.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isSet();
  
  /**
   * Liefert die ID dieses UIElements oder "" wenn nicht gesetzt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * TODO Testen
   */
  public String getId();
  
  /** Setzt eine neue Hintergrundfarbe für das UIElement. */
  public void setBackground(Color bg);
  
  public interface Constraints
  {
    /**
     * Liefert true, wenn die Constraints erfüllt sind.
     * @param mapIdToUIElement bildet die IDs der Eingabefelder auf
     * entsprechende UIElements ab.  
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public boolean checkValid(Map mapIdToUIElement);
    
    /**
     * Liefert eine Collection der IDs der Eingabefelder von denen diese
     * Constraints abhängen, d,h, die IDs die mindestens in der Map vorhanden sein
     * müssen, die an checkValid() übergeben wird. ACHTUNG! Die zurückgelieferte
     * Collection darf nicht verändert werden!
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public Collection dependencies();
  }

  public static abstract class UIElementBase implements UIElement
  {
    protected Integer labelType = LABEL_NONE;
    protected JLabel label = null;
    protected Object layoutConstraints = null;
    protected Object labelLayoutConstraints = null;
    protected Constraints constraints = null;
    protected String id = "";
  
    public void setBackground(Color bg)
    {
      this.getComponent().setBackground(bg);
    }
    
    public Integer getLabelType()
    {
      return labelType;
    }

    public Component getLabel()
    {
      return label;
    }

    public abstract Component getComponent();

    public Object getLayoutConstraints()
    {
      return layoutConstraints;
    }

    public Object getLabelLayoutConstraints()
    {
      return labelLayoutConstraints;
    }

    public Constraints getConstraints()
    {
      return constraints;
    }

    public abstract String getString();

    public abstract boolean isSet();
    
    public String getId() {return id;}
  }
  
  public static class Label extends UIElementBase
  {
    private JLabel component;

    public Label(String id, String label, Object layoutConstraints)
    {
      this.component = new JLabel(label);
      this.layoutConstraints = layoutConstraints;
      this.id = id;
    }

    public Component getComponent()
    {
      return component;
    }

    public String getString()
    {
      return "false";
    }

    public boolean isSet()
    {
      return false;
    }
  }
  
  
  public static class Button extends UIElementBase
  {
    private AbstractButton button;
      
    public Button(String id, AbstractButton button, Object layoutConstraints)
    {
      this.button = button;
      this.layoutConstraints = layoutConstraints;
      this.id = id;
    }

    public Component getComponent()
    {
      return button;
    }

    public String getString()
    {
      return "false";
    }

    public boolean isSet()
    {
      return false;
    }
  }
  
  public static class Textfield extends UIElementBase
  {
    private JTextField textfield;
    
    public Textfield(String id, JTextField tf, Object layoutConstraints, Integer labelType, String label, Object labelLayoutConstraints, Constraints constraints)
    {
      this.textfield = tf;
      this.layoutConstraints = layoutConstraints;
      this.labelLayoutConstraints = labelLayoutConstraints;
      this.constraints = constraints;
      this.label = new JLabel(label);
      this.labelType = labelType;
      this.id = id;
    }

    public Component getComponent()
    {
      return textfield;
    }

    public String getString()
    {
      return textfield.getText();
    }

    public boolean isSet()
    {
      return !getString().equals("");
    }
  }
  
  public static class Combobox extends UIElementBase
  {
    private JComboBox combo;
    
    public Combobox(String id, JComboBox combo, Object layoutConstraints, Integer labelType, String label, Object labelLayoutConstraints, Constraints constraints)
    {
      this.combo = combo;
      this.layoutConstraints = layoutConstraints;
      this.labelLayoutConstraints = labelLayoutConstraints;
      this.constraints = constraints;
      this.label = new JLabel(label);
      this.labelType = labelType;
      this.id = id;
    }

    public void setBackground(Color bg)
    {
      super.setBackground(bg);
      combo.getEditor().getEditorComponent().setBackground(bg);
    }
    
    public Component getComponent()
    {
      return combo;
    }

    public String getString()
    {
      return ((JTextComponent)combo.getEditor().getEditorComponent()).getText();
    }

    public boolean isSet()
    {
      return !getString().equals("");
    }
  }

  
  public static class Textarea extends UIElementBase
  {
    private JTextArea textarea;
    private Component textAreaComponent;
    
    public Textarea(String id, JTextArea textarea, Component textAreaComponent, Object layoutConstraints, Integer labelType, String label, Object labelLayoutConstraints, Constraints constraints)
    {
      this.textarea = textarea;
      this.textAreaComponent = textAreaComponent;
      this.layoutConstraints = layoutConstraints;
      this.labelLayoutConstraints = labelLayoutConstraints;
      this.constraints = constraints;
      this.label = new JLabel(label);
      this.labelType = labelType;
      this.id = id;
    }

    public Component getComponent()
    {
      return textAreaComponent;
    }

    public String getString()
    {
      return textarea.getText();
    }

    public boolean isSet()
    {
      return !getString().equals("");
    }
  }
  
  public static class Checkbox extends UIElementBase
  {
    private JCheckBox box;
    private Component kompost;
    
    public Checkbox(String id, JCheckBox boxBruceleitner, Component kompost, Object layoutConstraints)
    {
      this.kompost = kompost;
      this.box = boxBruceleitner;
      this.layoutConstraints = layoutConstraints;
      this.id = id;
    }

    public Component getComponent()
    {
      return kompost;
    }

    public String getString()
    {
      return isSet() ? "true" : "false";
    }

    public boolean isSet()
    {
      return box.isSelected();
    }
    
  }
  
  public static class Separator extends UIElementBase
  {
    private JSeparator seppl;
    
    public Separator(String id, JSeparator wurzelSepp, Object layoutConstraints)
    {
      this.layoutConstraints = layoutConstraints;
      this.seppl = wurzelSepp;
      this.id = id;
    }

    public Component getComponent()
    {
      return seppl;
    }

    public String getString()
    {
      return "false";
    }

    public boolean isSet()
    {
      return false;
    }
  }
  
  public static class Box extends UIElementBase
  {
    private javax.swing.Box jackInTheBox;
    
    public Box(String id, javax.swing.Box jackInTheBox, Object layoutConstraints)
    {
      this.jackInTheBox = jackInTheBox;
      this.layoutConstraints = layoutConstraints;
      this.id = id;
    }

    public Component getComponent()
    {
      return jackInTheBox;
    }

    public String getString()
    {
      return "false";
    }

    public boolean isSet()
    {
      return false;
    }
  }
  

}
