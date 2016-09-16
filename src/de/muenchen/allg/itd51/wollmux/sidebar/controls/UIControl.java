package de.muenchen.allg.itd51.wollmux.sidebar.controls;

/**
 * Interface für Steuerelemente, die der WollMux-Konfiguration definiert sein können.  
 * 
 * @param <T>
 */
public interface UIControl<T>
{
  public String getId();
  
  public T getValue();
  
  /**
   * Aktion, die von dem Steuerelement ausgeführt werden soll.
   * 
   * @return
   */
  public UIElementAction getAction();
  
  public void setValue(T value);
  
  public boolean hasFocus();
  
  public void takeFocus();
}
