package de.muenchen.allg.itd51.wollmux.func.functions;

import java.math.BigDecimal;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;

public class DiffFunction extends NumberFunction
{
  private BigDecimal sum;

  private boolean first;

  public DiffFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
      throws ConfigurationErrorException
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  protected String initComputation(Values parameters)
  {
    sum = BigDecimal.ZERO;
    first = true;
    return null;
  }

  @Override
  protected String addToComputation(BigDecimal num)
  {
    if (first)
    {
      sum = sum.add(num);
      first = false;
    }
    else
    {
      sum = sum.subtract(num);
    }
    return null;
  }

  @Override
  protected String computationResult()
  {
    return formatBigDecimal(sum);
  }
}