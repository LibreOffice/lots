package de.muenchen.allg.itd51.wollmux.core.functions;

import java.math.BigDecimal;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;

public class ProductFunction extends NumberFunction
{
  private BigDecimal prod;

  public ProductFunction(ConfigThingy conf, FunctionLibrary funcLib,
      DialogLibrary dialogLib, Map<Object, Object> context)
  {
    super(conf, funcLib, dialogLib, context);
  }

  @Override
  protected String initComputation(Values parameters)
  {
    prod = BigDecimal.ONE;
    return null;
  }

  @Override
  protected String addToComputation(BigDecimal num)
  {
    prod = prod.multiply(num);
    return null;
  }

  @Override
  protected String computationResult()
  {
    return formatBigDecimal(prod);
  }
}