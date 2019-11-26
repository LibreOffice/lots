package de.muenchen.allg.itd51.wollmux.core.document;

/**
 * Wird von Nodes unterstützt, die ein Bookmark repräsentieren, das eine Einfügung
 * (insertFormValue oder insertValue) darstellt.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface InsertionBookmark
{
  public String getName();

  public boolean isStart();
}