package de.muenchen.allg.itd51.wollmux.dialog.controls;

import java.awt.Component;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

public class Listbox extends UIElementBase
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