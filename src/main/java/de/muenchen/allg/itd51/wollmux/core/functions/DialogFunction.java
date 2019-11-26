package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import de.muenchen.allg.itd51.wollmux.core.dialog.Dialog;

public class DialogFunction implements Function
{
  private Dialog dialog;

  private String dataName;

  private String dialogName;

  public DialogFunction(String dialogName, Dialog dialog, String dataName,
      Map<Object, Object> context)
  {
    this.dialog = dialog.instanceFor(context);
    this.dataName = dataName;
    this.dialogName = dialogName;
  }

  @Override
  public String[] parameters()
  {
    return ArrayUtils.EMPTY_STRING_ARRAY;
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
    if (data == null) return FunctionLibrary.ERROR;
    return data.toString();
  }

  @Override
  public boolean getBoolean(Values parameters)
  {
    return getString(parameters).equalsIgnoreCase("true");
  }
}