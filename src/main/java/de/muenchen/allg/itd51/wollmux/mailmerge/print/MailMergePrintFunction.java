/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.mailmerge.print;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XStorable;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;

/**
 * An abstract print function for LibreOffice mailmerge.
 */
public abstract class MailMergePrintFunction extends PrintFunction
{

  private static final Logger LOGGER = LoggerFactory.getLogger(MailMergePrintFunction.class);

  /**
   * Key for saving the name of the target directory as a property of a {@link XPrintModel}.
   *
   * The property type is a {@link String}.
   */
  public static final String PROP_TARGETDIR = "MailMergeNew_TargetDir";

  /**
   * Key for saving the filename pattern as a property of a {@link XPrintModel}.
   *
   * The property type is a {@link String}.
   */
  public static final String PROP_FILEPATTERN = "MailMergeNew_FilePattern";

  /**
   * Create a new print function with name and order.
   *
   * @param functionName
   *          The name of the print function.
   * @param order
   *          The order of the print function.
   */
  public MailMergePrintFunction(String functionName, int order)
  {
    super(functionName, order);
  }

  /**
   * Replaces all occurrences of <code>{{<tag>}}</code> with the value specified in the data set.
   *
   * @param text
   *          Text, which contains tags to be replaced.
   * @param dataset
   *          Map of key-value pairs, where the key is <code><tag></code>.
   * @return Text where all occurrences are replaced, if there exists a record in data set.
   */
  public String replaceTextByMergeFieldValue(String text, Map<String, String> dataset)
  {
    for (Entry<String, String> entry : dataset.entrySet())
    {
      String mergeFieldWithTags = createMergeFieldTag(entry.getKey());

      if (text.contains(mergeFieldWithTags))
      {
        text = text.replace(mergeFieldWithTags, entry.getValue());
      }
    }

    return text;
  }

  /**
   * Creates a <code><tag></code>, which can be replaced by
   * {@link #replaceTextByMergeFieldValue(String, Map)}.
   *
   * @param mergeField
   *          The name of the tag.
   * @return The name of the tag surrounded by {{ and }}.
   */
  public static String createMergeFieldTag(String mergeField)
  {
    return "{{" + mergeField + "}}";
  }

  /**
   * Creates a temporary file with the pattern specified by the property {@link #PROP_FILEPATTERN}
   * in the directory specified by the property {@link #PROP_TARGETDIR}.
   *
   * @param pmod
   *          The {@link XPrintModel}.
   * @param isODT
   *          If true creates an odt file, otherwise a pdf file.
   * @return The temporary file.
   */
  public File createTempDocument(XPrintModel pmod, boolean isODT)
  {
    File outputDir = new File(
        pmod.getProp(PROP_TARGETDIR, System.getProperty("user.home") + "/Seriendruck").toString());

    @SuppressWarnings("unchecked")
    HashMap<String, String> dataset = new HashMap<>((HashMap<String, String>) pmod
        .getProp(SetFormValue.PROP_DATASET_EXPORT, new HashMap<String, String>()));

    String filename = replaceTextByMergeFieldValue((String) pmod.getProp(PROP_FILEPATTERN, null),
        dataset);

    if (!filename.toLowerCase().endsWith(".odt") && !filename.toLowerCase().endsWith(".pdf"))
    {
      if (isODT)
        filename = filename + ".odt";
      else
        filename = filename + ".pdf";
    }

    return new File(outputDir, filename);
  }

  /**
   * document with the name of outFile and closes it.
   *
   * @param outFile
   *          The filename use.
   * @param doc
   *          The document to save.
   * @return The really used file to save the document.
   */
  public File saveOutputFile(File outFile, XTextDocument doc)
  {
    try
    {
      String outFilePath = outFile.getPath();
      XStorable store = UNO.XStorable(doc);
      PropertyValue[] options;

      // fyi: http://wiki.services.openoffice.org/wiki/API/Tutorials/PDF_export
      if (outFilePath.endsWith(".pdf"))
      {
        options = new PropertyValue[1];

        options[0] = new PropertyValue();
        options[0].Name = "FilterName";
        options[0].Value = "writer_pdf_Export";
      } else if (outFilePath.endsWith(".doc"))
      {
        options = new PropertyValue[1];

        options[0] = new PropertyValue();
        options[0].Name = "FilterName";
        options[0].Value = "MS Word 97";
      } else
      {
        if (!outFilePath.endsWith(".odt"))
        {
          outFilePath = outFilePath + ".odt";
        }

        options = new PropertyValue[0];
      }

      if (System.getProperty("os.name").toLowerCase().contains("windows"))
      {
        outFilePath = outFilePath.replace("\\", "/");
      }

      com.sun.star.util.URL url = UNO.getParsedUNOUrl(outFilePath);

      // storeTOurl() has to be used instead of storeASurl() for PDF export
      store.storeToURL(url.Complete, options);
    } catch (Exception x)
    {
      LOGGER.error("", x);
    }

    return outFile;
  }

}
