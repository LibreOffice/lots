package de.muenchen.allg.itd51.wollmux.core.functions;

import java.math.BigDecimal;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class SumFunction extends NumberFunction
{
  private BigDecimal sum;

  public SumFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  protected String initComputation(Values parameters)
  {
    sum = BigDecimal.ZERO;
    return null;
  }

  @Override
  protected String addToComputation(BigDecimal num)
  {
    sum = sum.add(num);
    return null;
  }

  @Override
  protected String computationResult()
  {
    return formatBigDecimal(numericComputationResult());
  }

  protected BigDecimal numericComputationResult()
  {
    return sum;
  }
}