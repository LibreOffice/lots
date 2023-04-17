/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package de.muenchen.allg.itd51.wollmux.mailmerge.printsettings;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * The settings made by the user for his mail merge.
 */
public class PrintSettings
{
  /**
   * Defines how the document is printed.
   */
  private ACTION action = ACTION.SINGLE_DOCUMENT_ODT;

  /**
   * If {@link #selection} is {@link DatasetSelectionType#RANGE}, this is the first record to be
   * printed. It's co-domain is [1, {@link #rangeEnd}]. If there're less records, this must be
   * handled by the printing functionality.
   */
  private int rangeStart = 1;

  /**
   * If {@link #selection} is {@link DatasetSelectionType#RANGE}, this is the last record to be
   * printed. It's co-domain is [{@link #rangeStart}, {@link Integer#MAX_VALUE}]. If there're less
   * records, this must be handled by the printing functionality.
   */
  private int rangeEnd = Integer.MAX_VALUE;

  /**
   * The number of records in the data source.
   */
  private int numberOfRecords;

  /**
   * If {@link #selection} is {@link DatasetSelectionType#INDIVIDUAL}, the records contained in this
   * set are printed. All integers are greater or equal to 1.
   */
  private Set<Integer> records = new TreeSet<>();

  /**
   * Defines how the user has selected the records.
   */
  private DatasetSelectionType selection = DatasetSelectionType.ALL;

  /**
   * The format selected by the user.
   */
  private FORMAT format = FORMAT.ODT;

  /**
   * The directory to store the files.
   */
  private Optional<String> targetDirectory = Optional.empty();

  /**
   * A template for creating filenames.
   */
  private Optional<String> filenameTemplate = Optional.empty();

  /**
   * The sender of the mails.
   */
  private Optional<String> emailFrom = Optional.empty();

  /**
   * The field containing the mail address.
   */
  private Optional<String> emailToFieldName = Optional.empty();

  /**
   * The message of the mails.
   */
  private Optional<String> emailText = Optional.empty();

  /**
   * The subject of the mails.
   */
  private Optional<String> emailSubject = Optional.empty();

  /**
   * Create some print settings.
   *
   * @param numberOfRecords
   *          The number of records in the data source.
   */
  public PrintSettings(int numberOfRecords)
  {
    this.numberOfRecords = numberOfRecords;
    setRangeEnd(numberOfRecords);
  }

  public FORMAT getFormat()
  {
    return format;
  }

  public void setFormat(FORMAT format)
  {
    this.format = format;
  }

  public ACTION getAction()
  {
    return action;
  }

  public void setAction(ACTION action)
  {
    this.action = action;
  }

  public DatasetSelectionType getSelection()
  {
    return selection;
  }

  public void setSelection(DatasetSelectionType selection)
  {
    this.selection = selection;
  }

  public int getRangeStart()
  {
    return rangeStart;
  }

  public int getRangeEnd()
  {
    return rangeEnd;
  }

  public Set<Integer> getRecords()
  {
    return records;
  }

  public Optional<String> getTargetDirectory()
  {
    return targetDirectory;
  }

  public void setTargetDirectory(String targetDirectory)
  {
    this.targetDirectory = Optional.ofNullable(targetDirectory);
  }

  public Optional<String> getFilenameTemplate()
  {
    return filenameTemplate;
  }

  public void setFilenameTemplate(String filenameTemplate)
  {
    this.filenameTemplate = Optional.ofNullable(filenameTemplate);
  }

  public Optional<String> getEmailFrom()
  {
    return emailFrom;
  }

  public void setEmailFrom(String emailFrom)
  {
    this.emailFrom = Optional.ofNullable(emailFrom);
  }

  public Optional<String> getEmailToFieldName()
  {
    return emailToFieldName;
  }

  public void setEmailToFieldName(String emailToFieldName)
  {
    this.emailToFieldName = Optional.ofNullable(emailToFieldName);
  }

  public Optional<String> getEmailText()
  {
    return emailText;
  }

  public void setEmailText(String emailText)
  {
    this.emailText = Optional.ofNullable(emailText);
  }

  public Optional<String> getEmailSubject()
  {
    return emailSubject;
  }

  public void setEmailSubject(String emailSubject)
  {
    this.emailSubject = Optional.ofNullable(emailSubject);
  }

  /**
   * Sets a new range start. If it's bigger than {@link #rangeEnd}, they are swapped. Values smaller
   * than 1 are permitted.
   *
   * @param rangeStart
   *          The new start of the range.
   */
  public void setRangeStart(int rangeStart)
  {
    if (rangeStart > rangeEnd)
    {
      setRangeStart(rangeEnd);
      setRangeEnd(rangeStart);
    } else
    {
      this.rangeStart = Math.min(Math.max(1, rangeStart), numberOfRecords);
    }
    assert this.rangeStart >= 1 : "start smaller than 1";
    assert this.rangeStart <= rangeEnd : "start bigger than end";
  }

  /**
   * Sets a new range end. If it's smaller than {@link #rangeStart}, they are swapped. Values
   * smaller than 1 are permitted.
   *
   * @param rangeEnd
   *          The end start of the range.
   */
  public void setRangeEnd(int rangeEnd)
  {
    if (rangeEnd < rangeStart)
    {
      setRangeEnd(rangeStart);
      setRangeStart(rangeEnd);
    } else
    {
      this.rangeEnd = Math.min(Math.max(1, rangeEnd), numberOfRecords);
    }
    assert this.rangeEnd >= rangeStart : "end smaller than start";
  }

  /**
   * Adds a new record to the list of printed records.
   *
   * @param record
   *          A new record. It's only added if it's bigger than 1.
   */
  public void addRecord(int record)
  {
    if (record > 0)
    {
      records.add(record);
    }
  }

  /**
   * Describes how the document should be printed.
   */
  public enum ACTION
  {
    /**
     * Create one single ODT file with all records.
     */
    SINGLE_DOCUMENT_ODT,
    /**
     * Create one single PDF file with all records.
     */
    SINGLE_DOCUMENT_PDF,
    /**
     * Print the files on a printer.
     */
    DIRECT,
    /**
     * Send the files via mail.
     */
    MAIL,
    /**
     * Create single files for each record.
     */
    MULTIPLE_DOCUMENTS,
    /**
     * Nothing is done.
     */
    NOTHING;
  }

  /**
   * How were the records of the data selected.
   */
  public enum DatasetSelectionType
  {
    /**
     * Print all records.
     */
    ALL,

    /**
     * Print all records between {@link PrintSettings#rangeStart} and
     * {@link PrintSettings#rangeEnd}.
     */
    RANGE,

    /**
     * Print nothing.
     */
    NOTHING;
  }

  /**
   * The format of the resulting files.
   */
  public enum FORMAT
  {
    /**
     * Print as ODT files.
     */
    ODT,
    /**
     * Print as PDF files.
     */
    PDF,
    /**
     * Shouldn't happen.
     */
    NOTHING;
  }
}
