package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.util.Arrays;

import com.sun.star.awt.XComboBox;

import de.muenchen.allg.afid.UNO;

public class MailMergeField
{
  private final XComboBox comboBox;

  public MailMergeField(XComboBox comboBox)
  {
    this.comboBox = comboBox;
    comboBox.addItem("Bitte wählen..", (short) 0);
  }
  
  public void setMailMergeDatasource(MailMergeDatasource mailMergeDatasource)
  {
    comboBox.removeItems((short) 1, comboBox.getItemCount());
    if (mailMergeDatasource.hasDatasource())
    {
      String[] mailMergeColumnNames = mailMergeDatasource.getColumnNames().toArray(new String[] {});
      Arrays.sort(mailMergeColumnNames);
      comboBox.addItems(mailMergeColumnNames, (short) 1);
    }
    UNO.XTextComponent(comboBox).setText(comboBox.getItem((short) 0));
  }
}
