package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

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
    comboBox.addItems(new String[] { "Bitte wählen..", "Gender", "Wenn...Dann...Sonst",
        "Datensatznummer", "Serienbriefnummer", "Feld bearbeiten..." }, (short) 0);
    UNO.XTextComponent(comboBox).setText(comboBox.getItem((short) 0));
  }
}
