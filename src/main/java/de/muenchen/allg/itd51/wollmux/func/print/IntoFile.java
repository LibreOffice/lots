package de.muenchen.allg.itd51.wollmux.func.print;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.print.PrintIntoFile;

/**
 * Add {@link XTextDocument} of the {@link XPrintModel} to the {@link XTextDocument} specified in
 * the property {@link #OUTPUT_DOCUMENT_PROPERTY}. If there is no such property a new
 * {@link XTextDocument} is created.
 */
public class IntoFile extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(IntoFile.class);

  /**
   * Property of type {@link XTextDocument} for storing the output document.
   */
  public static final String OUTPUT_DOCUMENT_PROPERTY = "PrintIntoFile_OutputDocument";

  /**
   * Property of type {@link XTextDocument} for storing the previous output document.
   */
  public static final String PREVIOUS_DOCUMENT_PROPERTY = "PrintIntoFile_PreviousOutputDocument";

  /**
   * A {@link PrintFunction} with name "Gesamtdokument" and order 200.
   */
  public IntoFile()
  {
    super("Gesamtdokument", 200);
  }

  @Override
  public void print(XPrintModel printModel) throws PrintException
  {
    XTextDocument outputDoc = null;
    try
    {
      outputDoc = UNO.XTextDocument(printModel.getPropertyValue(OUTPUT_DOCUMENT_PROPERTY));
    } catch (UnknownPropertyException | WrappedTargetException e)
    {
      outputDoc = createNewTargetDocument(printModel);
    }

    boolean firstAppend = true;
    try
    {
      XTextDocument previousDoc = UNO
          .XTextDocument(printModel.getPropertyValue(PREVIOUS_DOCUMENT_PROPERTY));

      /*
       * It is important to do the firstAppend check via this comparison as opposed to just storing
       * a boolean property. This is because in the case of a mail merge with multiple output
       * documents this print function will be called several times with the same PrintModel but
       * different documents in the PrintIntoFile_OutputDocument property.
       */
      firstAppend = !(UnoRuntime.areSame(outputDoc, previousDoc));
    } catch (UnknownPropertyException | WrappedTargetException e)
    {
      LOGGER.trace("", e);
    }

    PrintIntoFile.appendToFile(outputDoc, printModel.getTextDocument(), firstAppend);

    try
    {
      if (firstAppend)
        printModel.setPropertyValue(PREVIOUS_DOCUMENT_PROPERTY, outputDoc);
    } catch (UnknownPropertyException | WrappedTargetException | IllegalArgumentException
        | PropertyVetoException e)
    {
      throw new PrintException("Could not set property PrintIntoFile_PreviousOutputDocument", e);
    }
  }

  /**
   * Create a new visible {@link XTextDocument} used by the {@link XPrintModel} printModel
   *
   * @param printModel
   *          A {@link XPrintModel} which gets the property {@link #OUTPUT_DOCUMENT_PROPERTY} set to
   *          the newly created {@link XTextDocument}.
   *
   * @return The created {@link XTextDocument}.
   */
  private XTextDocument createNewTargetDocument(final XPrintModel printModel)
      throws PrintException
  {
    try
    {
      XTextDocument outputDoc = UNO
          .XTextDocument(UNO.loadComponentFromURL("private:factory/swriter", true, true, false));
      printModel.setPropertyValue(OUTPUT_DOCUMENT_PROPERTY, outputDoc);

      return outputDoc;
    } catch (UnknownPropertyException | WrappedTargetException | IllegalArgumentException
        | PropertyVetoException | UnoHelperException e)
    {
      throw new PrintException("Could not create target document", e);
    }
  }

}
