package de.muenchen.allg.itd51.wollmux.sidebar;

import java.util.LinkedHashSet;
import java.util.Set;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XWindow;
import com.sun.star.uno.UnoRuntime;

/**
 * Ein sehr einfacher LayoutManager für UNO-Steuerelemente in einem LibreOffice-
 * Fenster. Alle Steuerlemente werde vertikal untereinander ausgerichtet in der
 * Reihenfolge, in der sie dem LayoutManager hinzugefügt wurden. Die Breite der
 * Steuerelemente wird auf die Breite des Parent-Fensters gestreckt.
 *
 */
public class SimpleLayoutManager
{
  private XWindow owner;

  private Set<XControl> controls;

  private int height = 0;

  /**
   * Erzeugt einen LayoutManager für eine Fenster. Ein Fenster kann jedes UNO-Objekt
   * sein, dass das XWindow-Interface unterstützt.
   *
   * @param owner
   */
  public SimpleLayoutManager(XWindow owner)
  {
    this.owner = owner;
    controls = new LinkedHashSet<>();
  }

  public int getHeight()
  {
    return height;
  }

  /**
   * Steuerelemente müssen dem LayoutManager hinzugefügt werden, damit sie von diesem
   * erkannt werden. Es dürfen nur Steuerelemente hinzugefügt werden, die zu dem
   * Fenster des LayoutManagers gehören.
   *
   * @param control
   */
  public void add(XControl control)
  {
    controls.add(control);
    layout();
  }

  /**
   * Diese Funktion muss immer dann aufgerufen werden, wenn sich die Größe des vom
   * LayoutManager gesteuerten Fensters ändert.
   *
   */
  public void layout()
  {
    Rectangle r = owner.getPosSize();
    int yOffset = 0;

    for (XControl ctrl : controls)
    {
      XWindow wnd = UnoRuntime.queryInterface(XWindow.class, ctrl);
      wnd.setPosSize(0, yOffset, r.Width, 0, (short) (PosSize.Y | PosSize.WIDTH));

      Rectangle cr = wnd.getPosSize();
      yOffset += cr.Height;
    }

    height = yOffset;
  }
}
