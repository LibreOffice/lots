/*
 * Dateiname: UIElement.java
 * Projekt  : WollMux
 * Funktion : Interface das von den konkreten UI Elementen (Combobox etc.) abstrahiert.
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
 * 11.01.2006 | BNK | Erstellung
 * 24.04.2006 | BNK | kommentiert.
 * 05.05.2006 | BNK | Condition -> Function
 * 15.05.2006 | BNK | +setString()
 * 18.05.2006 | BNK | +isStatic()
 * 30.05.2006 | BNK | UIElement.Listbox unterstützt jetzt Zusatzfunktionen
 * 16.06.2006 | BNK | +hasFocus(), +takeFocus()
 * 29.09.2006 | BNK | Verbessertes Auslesen von ComboBox-Daten 
 * 25.01.2006 | BNK | [R5038]Hintergrundfarbe von Textareas korrekt setzen
 * 05.07.2007 | BNK | [R7464]revalidate() auf Parent nach setVisible()
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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.func.Function;
import de.muenchen.allg.itd51.wollmux.func.Value;

/**
 * Interface das von den konkreten UI Elementen (Combobox etc.) abstrahiert.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface UIElement extends Value
{

  /**
   * Gibt an, dass das UI Element kein zusätzliches Label erhalten soll.
   */
  public static final Integer LABEL_NONE = Integer.valueOf(0);

  /**
   * Gibt an, dass links neben dem UI Element ein zusätzliches Label stehen soll.
   */
  public static final Integer LABEL_LEFT = Integer.valueOf(-1);

  /**
   * Gibt an, dass rechts neben dem UI Element ein zusätzliches Label stehen soll.
   */
  public static final Integer LABEL_RIGHT = Integer.valueOf(+1);

  /**
   * Liefert die Information, ob das UI Element ein zusätzliches Label links oder
   * rechts bekommen soll. Mögliche Werte sind {@link #LABEL_LEFT},
   * {@link #LABEL_RIGHT} und {@link #LABEL_NONE}.
   */
  public Integer getLabelType();

  /**
   * Ist nur definiert, wenn getLabelType() nicht LABEL_NONE ist und liefert das
   * anzuzeigende Zusatzlabel. Achtung! Bei UI Elementen, die selbst Labels sind
   * (TYPE "label") liefert diese Methode <b>nicht</b> das UI Element selbst.
   */
  public Component getLabel();

  /**
   * Der funktionale Teil des UIElements. Achtung! Bei UI Elementen vom TYPE "label"
   * liefert diese Methode (nicht getLabel()) das Label.
   */
  public Component getComponent();

  /**
   * Liefert das empfohlene zweite Argument für
   * {@link java.awt.Container#add(java.awt.Component, java.lang.Object)} für die
   * funktionale Komponente dieses UI Elements.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getLayoutConstraints();

  /**
   * Liefert das empfohlene zweite Argument für
   * {@link java.awt.Container#add(java.awt.Component, java.lang.Object)} für das
   * Zusatz-Label dieses UIElements. Nur definiert, falls getLabelType() nicht
   * LABEL_NONE ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getLabelLayoutConstraints();

  /**
   * Liefert das mit setAdditionalData() gesetzte Objekt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getAdditionalData();

  /**
   * Setzt das Objekt, das von getAdditionalData() zurückgeliefert werden soll.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setAdditionalData(Object o);

  /**
   * Der aktuelle Wert des UI Elements. Falls es sich um ein boolesches Element
   * (Checkbox, Radio Button) handelt, wird im angeschalteten Fall der String "true",
   * im ungesetzten Fall "false" zurückgeliefert. Im Falle eines Buttons oder eines
   * anderen Elements das keinen sinnvollen Wert hat, wird immer "false" geliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getString();

  /**
   * Falls es sich um ein boolesches Element (Checkbox, Radio Button) handelt, wird
   * true geliefert falls das Element angeschaltet ist, ansonsten false. Im Falle von
   * Text-Elementen wird true geliefert, falls der aktuelle Wert nicht der leere
   * String ist. Im Falle eines Buttons oder eines anderen Elements das keinen
   * sinnvollen Wert hat, wird immer false geliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean getBoolean();

  /**
   * Setzt den aktuellen Wert dieses UI Elements (falls möglich) auf str. Falls es
   * sich um ein boolesches Element (Checkbox etc) handelt, so wird der String "true"
   * (ohne Berücksichtigung von Gross-/Kleinschreibung) als true und jeder andere
   * String als false betrachtet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setString(String str);

  /**
   * Liefert die ID dieses UIElements oder "" wenn nicht gesetzt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getId();

  /**
   * Setzt eine neue Hintergrundfarbe für das UIElement.
   */
  public void setBackground(Color bg);
  
  public void setEnabled(boolean enabled);

  /**
   * Setzt die Sichtbarkeit der Komponente und ihres Zusatzlabels (falls vorhanden).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setVisible(boolean vis);

  /**
   * Liefert true, wenn das Element keine Änderungen erlaubt (z,B, ein Separator oder
   * ein Label).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isStatic();

  /**
   * Liefert true, wenn dieses UIElement im Moment den Eingabefokus hat.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasFocus();

  /**
   * Sagt dem UIElement, es soll versuchen, den Eingabefokus zu übernehmen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void takeFocus();

  /**
   * Abstrakte Basis-Klasse für UIElemente.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static abstract class UIElementBase implements UIElement
  {
    protected Integer labelType = LABEL_NONE;

    protected JLabel label = null;

    protected Object layoutConstraints = null;

    protected Object labelLayoutConstraints = null;

    protected Function constraints = null;

    protected String id = "";

    protected Object addData = null;

    public void setBackground(Color bg)
    {
      this.getComponent().setBackground(bg);
    }
    
    public void setEnabled(boolean enabled)
    {
      this.getComponent().setEnabled(enabled);
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

    public Object getAdditionalData()
    {
      return addData;
    }

    public void setAdditionalData(Object o)
    {
      addData = o;
    }

    public void setVisible(boolean vis)
    {
      if (getLabel() != null) getLabel().setVisible(vis);
      getComponent().setVisible(vis);
      /*
       * einige Komponenten (z.B. JTextField) tun dies nicht richtig siehe
       * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4403550
       */
      ((JComponent) getComponent().getParent()).revalidate();
    }

    public abstract String getString();

    public abstract boolean getBoolean();

    public String getId()
    {
      return id;
    }

    public void setString(String str)
    {};

    public boolean hasFocus()
    {
      return getComponent().isFocusOwner();
    }

    public void takeFocus()
    {
      getComponent().requestFocusInWindow();
    }
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

    public boolean getBoolean()
    {
      return false;
    }

    public boolean isStatic()
    {
      return true;
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

    public boolean getBoolean()
    {
      return false;
    }

    public boolean isStatic()
    {
      return true;
    }
  }

  public static class Textfield extends UIElementBase
  {
    private JTextField textfield;

    public Textfield(String id, JTextField tf, Object layoutConstraints,
        Integer labelType, String label, Object labelLayoutConstraints)
    {
      this.textfield = tf;
      this.layoutConstraints = layoutConstraints;
      this.labelLayoutConstraints = labelLayoutConstraints;
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

    public boolean getBoolean()
    {
      return !getString().equals("");
    }

    public void setString(String str)
    {
      textfield.setText(str);
    }

    public boolean isStatic()
    {
      return false;
    }
  }

  public static class Combobox extends UIElementBase
  {
    private JComboBox<?> combo;

    public Combobox(String id, JComboBox<?> combo, Object layoutConstraints,
        Integer labelType, String label, Object labelLayoutConstraints)
    {
      this.combo = combo;
      this.layoutConstraints = layoutConstraints;
      this.labelLayoutConstraints = labelLayoutConstraints;
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
      if (combo.isEditable())
      {
        Document comboDoc =
          ((JTextComponent) combo.getEditor().getEditorComponent()).getDocument();
        try
        {
          return comboDoc.getText(0, comboDoc.getLength());
        }
        catch (BadLocationException x)
        {
          Logger.error(x);
          return "";
        }
      }
      else
      {
        Object selected = combo.getSelectedItem();
        return selected == null ? "" : selected.toString();
      }
    }

    public boolean getBoolean()
    {
      return !getString().equals("");
    }

    public void setString(String str)
    {
      boolean edit = combo.isEditable();
      combo.setEditable(true);
      combo.setSelectedItem(str);
      combo.setEditable(edit);
    }

    public boolean isStatic()
    {
      return false;
    }
  }

  public static class Listbox extends UIElementBase
  {
    private JScrollPane scrollPane;

    private JList<Object> list;

    public Listbox(String id, JScrollPane scrollPane, JList<Object> list,
        Object layoutConstraints, Integer labelType, String label,
        Object labelLayoutConstraints)
    {
      this.scrollPane = scrollPane;
      this.list = list;
      this.layoutConstraints = layoutConstraints;
      this.labelLayoutConstraints = labelLayoutConstraints;
      this.label = new JLabel(label);
      this.labelType = labelType;
      this.id = id;
    }

    public Component getComponent()
    {
      return scrollPane;
    }

    public String getString()
    {
      StringBuffer buffy = new StringBuffer();
      for (Object o : list.getSelectedValuesList())
      {
        if (buffy.length() > 0) buffy.append('\n');
        buffy.append(o.toString());
      }
      return buffy.toString();
    }

    public boolean getBoolean()
    {
      return !getString().equals("");
    }

    public void setString(String str)
    {
      Set<String> vals = new HashSet<String>();
      String[] split = str.split("\n");
      for (int i = 0; i < split.length; ++i)
        vals.add(split[i]);

      Vector<Integer> indices = new Vector<Integer>(split.length);
      DefaultListModel<?> model = (DefaultListModel<?>) list.getModel();
      Enumeration<?> enu = model.elements();
      int index = 0;
      while (enu.hasMoreElements())
      {
        if (vals.contains(enu.nextElement())) indices.add(Integer.valueOf(index));
        ++index;
      }

      if (!indices.isEmpty())
      {
        int[] selIndices = new int[indices.size()];
        for (int i = 0; i < selIndices.length; ++i)
          selIndices[i] = indices.get(i).intValue();

        list.setSelectedIndices(selIndices);
      }
    }

    /**
     * Löscht alle alten Einträge dieser ListBox und ersetzt sie durch die Einträge
     * von newEntries (beliebige Objects).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    public void setList(Collection<?> newEntries)
    {
      DefaultListModel<Object> listModel = (DefaultListModel<Object>) list.getModel();
      listModel.clear();
      for (Object o : newEntries)
      {
        listModel.addElement(o);
      }
    }

    /**
     * Liefert alle selektierten Objekte der Liste.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    public List<Object> getSelected()
    {
      return list.getSelectedValuesList();
    }

    /**
     * Falls Mehrfachauswahl möglich ist werden alle gültigen Indizes (Numbers,
     * gezählt ab 0) aus indices selektiert, falls nur Einfachauswahl möglich wird
     * nur der erste gültige Index selektiert.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     * 
     */
    public void select(Collection<? extends Number> indices)
    {
      int[] selected = new int[indices.size()];
      Iterator<? extends Number> iter = indices.iterator();
      int i = 0;
      while (iter.hasNext())
      {
        int index = ((Number) iter.next()).intValue();
        selected[i++] = index;
      }

      if (i < selected.length)
      {
        int[] newSelected = new int[i];
        System.arraycopy(selected, 0, newSelected, 0, i);
        selected = newSelected;
      }

      list.setSelectedIndices(selected);
    }

    public boolean isStatic()
    {
      return false;
    }
  }

  public static class Textarea extends UIElementBase
  {
    private JTextArea textarea;

    private Component textAreaComponent;

    public Textarea(String id, JTextArea textarea, Component textAreaComponent,
        Object layoutConstraints, Integer labelType, String label,
        Object labelLayoutConstraints)
    {
      this.textarea = textarea;
      this.textAreaComponent = textAreaComponent;
      this.layoutConstraints = layoutConstraints;
      this.labelLayoutConstraints = labelLayoutConstraints;
      this.label = new JLabel(label);
      this.labelType = labelType;
      this.id = id;
    }

    public Component getComponent()
    {
      return textAreaComponent;
    }

    /**
     * Da getComponent das Panel zurückliefert, in dem sich die Textarea befindet,
     * gibt diese Funktion das eigentliche JTextArea-Objekt zurück.
     * 
     * @return
     * @author Andor Ertsey (D-III-ITD-D101)
     */
    public JTextArea getTextArea()
    {
      return textarea;
    }

    public String getString()
    {
      return textarea.getText();
    }

    public boolean getBoolean()
    {
      return !getString().equals("");
    }

    public void setString(String str)
    {
      textarea.setText(str);
    }

    public void setBackground(Color bg)
    {
      textarea.setBackground(bg);
    }

    public boolean isStatic()
    {
      return false;
    }
  }

  public static class Checkbox extends UIElementBase
  {
    private JCheckBox box;

    public Checkbox(String id, JCheckBox boxBruceleitner, Object layoutConstraints)
    {
      /*
       * labelType wird geerbt als LABEL_NONE, da diese Checkbox ihr Label im UI
       * Element fest integriert hat.
       */
      this.box = boxBruceleitner;
      this.layoutConstraints = layoutConstraints;
      this.id = id;
    }

    public Component getComponent()
    {
      return box;
    }

    public String getString()
    {
      return getBoolean() ? "true" : "false";
    }

    public boolean getBoolean()
    {
      return box.isSelected();
    }

    public void setString(String str)
    {
      box.setSelected(str.equalsIgnoreCase("true"));
    }

    public boolean isStatic()
    {
      return false;
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

    public boolean getBoolean()
    {
      return false;
    }

    public boolean isStatic()
    {
      return true;
    }
  }

  public static class Box extends UIElementBase
  {
    private Component jackInTheBox;

    public Box(String id, Component jackInTheBox, Object layoutConstraints)
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

    public boolean getBoolean()
    {
      return false;
    }

    public boolean isStatic()
    {
      return true;
    }
  }

}
