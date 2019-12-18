package de.muenchen.allg.itd51.wollmux.func.print;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung.VerfuegungspunktInfo;
import de.muenchen.allg.itd51.wollmux.XPrintModel;

/**
 * Print function for printing the directives specified in the property {link
 * {@link SachleitendeVerfuegung#PROP_SLV_SETTINGS}.
 */
public class SachleitendeVerfuegungOutput extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SachleitendeVerfuegungOutput.class);

  /**
   * A {@link PrintFunction} with name "SachleitendeVerfuegungOutput" and order 150.
   */
  public SachleitendeVerfuegungOutput()
  {
    super("SachleitendeVerfuegungOutput", 150);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void print(XPrintModel printModel)
  {
    List<VerfuegungspunktInfo> settings = new ArrayList<>();
    try
    {
      settings = (List<VerfuegungspunktInfo>) printModel
          .getPropertyValue(SachleitendeVerfuegung.PROP_SLV_SETTINGS);
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }

    short countMax = 0;
    for (VerfuegungspunktInfo v : settings)
      countMax += v.getCopyCount();
    printModel.setPrintProgressMaxValue(countMax);

    short count = 0;
    for (VerfuegungspunktInfo v : settings)
    {
      if (printModel.isCanceled())
        return;
      if (v.getCopyCount() > 0)
      {
        de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung.printVerfuegungspunkt(printModel,
            v.verfPunktNr, v.isDraft, v.isOriginal, v.getCopyCount());
      }
      count += v.getCopyCount();
      printModel.setPrintProgressValue(count);
    }
  }
}
