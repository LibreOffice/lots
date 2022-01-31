/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.print;

/**
 * Definition of a page range.
 */
public class PageRange
{
  /**
   * The type of the given page range.
   */
  private PageRangeType pageRangeType;

  /**
   * The value
   */
  private String pageRangeValue;

  /**
   * Initialize a page range.
   *
   * @param pageRangeType
   *          The type.
   * @param pageRangeValue
   *          The value.
   */
  public PageRange(PageRangeType pageRangeType, String pageRangeValue)
  {
    this.pageRangeType = pageRangeType;
    this.pageRangeValue = pageRangeValue;
  }

  public PageRangeType getPageRangeType()
  {
    return pageRangeType;
  }

  public void setPageRangeType(PageRangeType pageRangeType)
  {
    this.pageRangeType = pageRangeType;
  }

  public String getPageRangeValue()
  {
    return pageRangeValue;
  }

  public void setPageRangeValue(String pageRangeValue)
  {
    this.pageRangeValue = pageRangeValue;
  }

  @Override
  public String toString()
  {
    return "PageRange(" + pageRangeType + ", '" + pageRangeValue + "')";
  }

  /**
   * Definition of different page range types.
   */
  public enum PageRangeType
  {
    /**
     * Print all pages.
     */
    ALL,
    /**
     * Print selected pages.
     */
    USER_DEFINED,
    /**
     * Print the current page.
     */
    CURRENT_PAGE,
    /**
     * Print the current and following pages.
     */
    CURRENT_AND_FOLLOWING;
  }
}
