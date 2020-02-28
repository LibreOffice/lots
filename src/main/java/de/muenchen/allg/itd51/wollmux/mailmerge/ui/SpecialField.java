package de.muenchen.allg.itd51.wollmux.mailmerge.ui;

import com.sun.star.awt.XComboBox;

import de.muenchen.allg.afid.UNO;

public class SpecialField
{
  private SpecialField()
  {
    // nothing to do
  }

  public static void addItems(XComboBox comboBox)
  {
    String[] items = new String[] { "Bitte wählen..", "Gender", "Wenn...Dann...Sonst",
        "Datensatznummer", "Serienbriefnummer", "Nächster Datensatz" };
    addItems(comboBox, items);
  }

  public static void addItems(XComboBox comboBox, String[] items)
  {
    comboBox.addItems(items, (short) 0);
    UNO.XTextComponent(comboBox).setText(comboBox.getItem((short) 0));
  }
}
