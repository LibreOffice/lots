package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class SignFunction extends SumFunction
{
  public SignFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  protected String computationResult()
  {
    // signum() liefert int, deswegen hier nur ""+ und nicht formatBigDecimal()
    return "" + numericComputationResult().signum();
  }
}