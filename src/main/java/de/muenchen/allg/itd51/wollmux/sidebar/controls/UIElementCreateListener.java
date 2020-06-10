package de.muenchen.allg.itd51.wollmux.sidebar.controls;

import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementConfig;

public interface UIElementCreateListener
{
  void createControl(UIElementConfig element, boolean isMenu, String parentEntry);
}
