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
  
  public UIElementAction getAction();
  
  public void setValue(T value);
  
  public boolean hasFocus();
  
  public void takeFocus();
}
