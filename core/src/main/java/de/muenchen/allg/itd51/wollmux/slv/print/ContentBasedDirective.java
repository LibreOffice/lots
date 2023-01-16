/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.slv.print;

import java.util.ArrayList;
import java.util.List;

/**
 * The data (number, heading, content, print settings) of a content based directive.
 */
public class ContentBasedDirective
{
  /**
   * Complete text of first row including the number.
   */
  private String heading;

  /**
   * List of all receiver lines, add with {@link #addReceiverLine(String)}.
   */
  private List<String> receiverLines;

  /**
   * Minimum number of copies.
   */
  private int minNumberOfCopies;

  /**
   * Creates a new content based directive.
   *
   * @param heading
   *          Text of the first line of the content based directive including number. Tabs are
   *          replaced by single spaces.
   */
  public ContentBasedDirective(String heading)
  {
    this.heading = heading.replaceAll("\\s+", " ");
    this.receiverLines = new ArrayList<>();
    this.minNumberOfCopies = 0;
  }

  public List<String> getReceiverLines()
  {
    return receiverLines;
  }

  public String getHeading()
  {
    return heading;
  }

  /**
   * Adds a new receiver.
   *
   * @param receiverLine
   *          Text of the receiver line.
   */
  public void addReceiverLine(String receiverLine)
  {
    receiverLines.add(receiverLine);
  }

  /**
   * Number of copies for this content based directive. It's increased with every receiver line. The
   * minimum can be set with {@link #setMinNumberOfCopies(int)}.
   *
   * @return Number of copies.
   */
  public int getNumberOfCopies()
  {
    if (receiverLines.size() > minNumberOfCopies)
      return receiverLines.size();
    else
      return minNumberOfCopies;
  }

  /**
   * Set the minimal number of copies to print although there are less receiver lines.
   *
   * @param minNumberOfCopies
   *          Minimal number of copies.
   */
  public void setMinNumberOfCopies(int minNumberOfCopies)
  {
    this.minNumberOfCopies = minNumberOfCopies;
  }
}
