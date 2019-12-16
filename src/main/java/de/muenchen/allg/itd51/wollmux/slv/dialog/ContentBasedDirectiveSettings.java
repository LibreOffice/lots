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
