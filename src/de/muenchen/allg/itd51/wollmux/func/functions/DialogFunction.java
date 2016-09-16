package de.muenchen.allg.itd51.wollmux.func.functions;

import java.util.Collection;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

public class DialogFunction implements Function
{
  private Dialog dialog;

  private String dataName;

  private String dialogName;

  public DialogFunction(String dialogName, Dialog dialog, String dataName,
      Map<Object, Object> context) throws ConfigurationErrorException
  {
    this.dialog = dialog.instanceFor(context);
    this.dataName = dataName;
    this.dialogName = dialogName;
  }

  @Override
  public String[] parameters()
  {
    return noParams;
  }

  @Override
  public void getFunctionDialogReferences(Collection<String> set)
  {
    set.add(dialogName);
  }

  @Override
  public String getString(Values parameters)
  {
    Object data = dialog.getData(dataName);
    if (data == null) return Function.ERROR;
    return data.toString();
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getString(parameters).equalsIgnoreCase("true");
  }
}