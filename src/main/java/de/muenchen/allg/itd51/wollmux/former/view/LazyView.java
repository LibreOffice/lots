package de.muenchen.allg.itd51.wollmux.former.view;

/**
 * Eine View die erst wenn sie das erste mal angezeigt wird initialisiert wird.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public interface LazyView extends View
{
  /**
   * Muss aufgerufen werden, wenn die View angezeigt wird. Dies veranlasst die View,
   * sich komplett zu initialisieren. Vor dem Aufruf dieser Funktion ist nicht
   * garantiert, dass in der View tatsächlich Inhalt angezeigt wird.
   */
  public void viewIsVisible();

  /**
   * Sollte (muss aber nicht) aufgerufen werden, wenn die View nicht mehr angezeigt
   * wird. Sie kann dann falls möglich Ressourcen freigeben.
   */
  public void viewIsNotVisible();
}
