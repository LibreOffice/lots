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
package de.muenchen.allg.itd51.wollmux.slv.dialog;

import de.muenchen.allg.itd51.wollmux.slv.PrintBlockSignature;

/**
 * Print settings of a directive.
 */
public class ContentBasedDirectiveSettings
{
  /**
   * The id of the directive, to which these settings belong.
   */
  public final int directiveId;

  /**
   * Should print blocks of type {@link PrintBlockSignature#DRAFT_ONLY} be printed?
   */
  public final boolean isDraft;

  /**
   * Should print blocks of type {@link PrintBlockSignature#ORIGINAL_ONLY} and
   * {@link PrintBlockSignature#NOT_IN_ORIGINAL} be printed?
   */
  public final boolean isOriginal;

  private short copyCount;

  /**
   * Create new print settings of a directive.
   *
   * @param directiveId
   *          The id of the directive to which these settings belong.
   * @param copyCount
   *          The number of prints for this directive.
   * @param isDraft
   *          {@link #isDraft}
   * @param isOriginal
   *          {@link #isOriginal}
   */
  public ContentBasedDirectiveSettings(int directiveId, short copyCount, boolean isDraft,
      boolean isOriginal)
  {
    this.directiveId = directiveId;
    this.copyCount = copyCount;
    this.isDraft = isDraft;
    this.isOriginal = isOriginal;
  }

  public short getCopyCount()
  {
    return copyCount;
  }

  public void setCopyCount(short copyCount)
  {
    this.copyCount = copyCount;
  }

  @Override
  public String toString()
  {
    return "DirectiveSettings(directiveId=" + directiveId + ", copyCount=" + copyCount
        + ", isDraft=" + isDraft + ", isOriginal=" + isOriginal + ")";
  }
}
