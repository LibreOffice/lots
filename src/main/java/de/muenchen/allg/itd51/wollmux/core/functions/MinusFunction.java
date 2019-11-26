package de.muenchen.allg.itd51.wollmux.core.functions;

import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class MinusFunction extends SumFunction
{
  public MinusFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  protected String computationResult()
  {
    return formatBigDecimal(numericComputationResult().negate());
  }
}