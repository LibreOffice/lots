package de.muenchen.allg.itd51.wollmux.core.dialog.controls;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

public class Listbox extends UIElementBase
{
  private JScrollPane scrollPane;

  private JList<Object> list;

  public Listbox(String id, JScrollPane scrollPane, JList<Object> list,
      Object layoutConstraints, UIElement.LabelPosition labelType, String label,
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

  @Override
  public Component getComponent()
  {
    return scrollPane;
  }

  @Override
  public String getString()
  {
    StringBuilder buffy = new StringBuilder();
    for (Object o : list.getSelectedValuesList())
    {
      if (buffy.length() > 0)
      {
        buffy.append('\n');
      }
      buffy.append(o.toString());
    }
    return buffy.toString();
  }

  @Override
  public boolean getBoolean()
  {
    return !getString().isEmpty();
  }

  @Override
  public void setString(String str)
  {
    Set<String> vals = new HashSet<>();
    String[] split = str.split("\n");
    for (int i = 0; i < split.length; ++i)
      vals.add(split[i]);

    List<Integer> indices = new ArrayList<>(split.length);
    DefaultListModel<?> model = (DefaultListModel<?>) list.getModel();
    Enumeration<?> enu = model.elements();
    int index = 0;
    while (enu.hasMoreElements())
    {
      if (vals.contains(enu.nextElement()))
      {
        indices.add(Integer.valueOf(index));
      }
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
   */
  public void select(Collection<? extends Number> indices)
  {
    int[] selected = new int[indices.size()];
    Iterator<? extends Number> iter = indices.iterator();
    int i = 0;
    while (iter.hasNext())
    {
      int index = iter.next().intValue();
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

  @Override
  public boolean isStatic()
  {
    return false;
  }
}