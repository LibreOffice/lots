package de.muenchen.allg.itd51.wollmux.func.print;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.NoSuchMethodException;

import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung.VerfuegungspunktInfo;
import de.muenchen.allg.itd51.wollmux.XPrintModel;

/**
 * Print function for configuration of the content based directive print. It creates a GUI and
 * passes the settings as a property called {@link #PROP_SLV_SETTINGS} of the {@link XPrintModel} to
 * the next {@link PrintFunction}.
 */
public class SachleitendeVerfuegung extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(SachleitendeVerfuegung.class);

  /**
   * Key for saving the content based directive settings as a property of a {@link XPrintModel}.
   * This property is read by {@link SachleitendeVerfuegungOutput}.
   *
   * The property type is a {@link List} of {@link VerfuegungspunktInfo}.
   */
  public static final String PROP_SLV_SETTINGS = "SLV_Settings";

  /**
   * A {@link PrintFunction} with name "SachleitendeVerfuegung" and order 50.
   */
  public SachleitendeVerfuegung()
  {
    super("SachleitendeVerfuegung", 50);
  }

  @Override
  public void print(XPrintModel printModel)
  {
    // add print function SachleitendeVerfuegungOutput
    try
    {
      printModel.usePrintFunction("SachleitendeVerfuegungOutput");
    } catch (NoSuchMethodException e)
    {
      LOGGER.error("", e);
      printModel.cancel();
      return;
    }

    List<VerfuegungspunktInfo> settings = de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung
        .callPrintDialog(printModel.getTextDocument());
    if (settings != null)
    {
      try
      {
        printModel.setPropertyValue(PROP_SLV_SETTINGS, settings);
      } catch (java.lang.Exception e)
      {
        LOGGER.error("", e);
        printModel.cancel();
        return;
      }
      printModel.printWithProps();
    }
  }

}
