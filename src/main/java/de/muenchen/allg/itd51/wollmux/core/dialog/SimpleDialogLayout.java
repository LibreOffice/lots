package de.muenchen.allg.itd51.wollmux.core.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.PosSize;
import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowEvent;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Align;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Dock;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Orientation;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;

public class SimpleDialogLayout extends AbstractWindowListener
{
  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDialogLayout.class);
  private XControlContainer controlContainer;
  private XWindow containerWindow;
  private int marginBetweenControls = 5;
  private List<ControlModel> controlList = new ArrayList<>();
  private int yOffset = 0;
  private int xOffset = 0;
  private int marginTop = 0;
  private int marginRight = 0;
  private int marginLeft = 0;
  private int windowBottomMargin = 0;
  private List<ControlModel> cachedBindings = new ArrayList<>();
  private int yOffsetBinding = 0;

  public SimpleDialogLayout(XWindow dialogWindow)
  {
    this.containerWindow = dialogWindow;
    this.containerWindow.addWindowListener(this);
    this.controlContainer = UnoRuntime.queryInterface(XControlContainer.class, dialogWindow);
  }

  public void draw()
  {
    Rectangle windowRect = this.getContainerWindow().getPosSize();
    this.cachedBindings.clear();

    this.yOffset = this.getMarginTop() > 0 ? this.getMarginTop() : 0;
    this.xOffset = this.getMarginLeft() > 0 ? this.getMarginLeft() : 0;
    this.xOffsetRight = windowRect.Width / 2;
    this.xOffsetBinding = 0;

    for (ControlModel entry : this.getControlList())
    {
      entry.getDock().ifPresent(dock -> this.setDock(windowRect, entry, dock));

      if (entry.getOrientation() == Orientation.HORIZONTAL)
      {
        if (entry.getBindToControlControlWidthAndXOffset() == null)
        {
          for (ControlProperties control : entry.getControls())
          {
            XControl controlByControlContainer = this.controlContainer
                .getControl(control.getControlName());
            if (controlByControlContainer != null)
            {
              modifyHorizontalControl(windowRect, controlByControlContainer, control,
                  entry.getAlignment(), entry.getControls().size());
            } else
            {
              this.modifyHorizontalControl(windowRect, control.getXControl(), control,
                  entry.getAlignment(), entry.getControls().size());

              this.addControlToControlContainer(control.getControlName(), control.getXControl());
            }
          }
        } else
        {
          this.yOffsetBinding = this.yOffset; 
          cachedBindings.add(entry);
        }
      } else if (entry.getOrientation() == Orientation.VERTICAL)
      {
        for (ControlProperties control : entry.getControls())
        {
          XControl controlByControlContainer = this.controlContainer
              .getControl(control.getControlName());
          if (controlByControlContainer != null)
          {
            this.modifyVerticalControl(windowRect, control.getXControl(), control,
                entry.getAlignment());
          } else
          {
            this.modifyVerticalControl(windowRect, control.getXControl(), control,
                entry.getAlignment());

            this.addControlToControlContainer(control.getControlName(), control.getXControl());
          }
        }
      }

      this.newLine(entry);
    }

    for (ControlModel controlModel : cachedBindings)
    {
      applyControlBinding(controlModel);
    }
  }

  private Optional<ControlProperties> getMaxControlHeightByControlList(ControlModel controlModel)
  {
    return controlModel.getControls().stream().max((v1, v2) -> Integer
        .compare(v1.getControlSize().getHeight(), v2.getControlSize().getHeight()));
  }

  private void setDock(Rectangle windowRect, ControlModel controlModel, Dock dock)
  {
    if (dock == Dock.BOTTOM)
    {
      Optional<ControlProperties> maxControlHeight = this
          .getMaxControlHeightByControlList(controlModel);
      this.yOffset = this.getBottomYOffset(windowRect,
          maxControlHeight.isPresent() ? maxControlHeight.get().getControlPercentSize().getHeight()
              : 0);
    }
  }

  private int xOffsetRight = 0;
  private int xOffsetBinding = 0;

  private void modifyHorizontalControl(Rectangle windowRect, XControl control,
      ControlProperties controlProperties, Align alignment, int controlCount)
  {
    XWindow wnd = UnoRuntime.queryInterface(XWindow.class, control);

    if (alignment == Align.RIGHT)
    {
      int calculatedControlWidthRight = controlProperties.getControlPercentSize().getWidth() > 0
          ? ((windowRect.Width / 100 / 2) * controlProperties.getControlPercentSize().getWidth())
          : (windowRect.Width - (controlCount * 10)) / controlCount;

      wnd.setPosSize(xOffsetRight, yOffset, calculatedControlWidthRight,
          controlProperties.getControlPercentSize().getHeight(), (PosSize.POSSIZE));

      xOffsetRight += calculatedControlWidthRight + controlProperties.getMarginBetweenControls();
    } else if (alignment == Align.LEFT)
    {
      int calculatedControlWidthLeft = (windowRect.Width / 2 - (controlCount * 5)) / controlCount;
      wnd.setPosSize(xOffset, yOffset, calculatedControlWidthLeft,
          controlProperties.getControlPercentSize().getHeight(), (PosSize.POSSIZE));
    } else
    {
      xOffset = controlProperties.getMarginLeft() > 0 ? controlProperties.getMarginLeft() : xOffset;

      int calculatedControlWidth = controlProperties.getControlPercentSize().getWidth() > 0
          ? ((windowRect.Width / 100) * controlProperties.getControlPercentSize().getWidth())
              - (this.getMarginLeft() / controlCount) - (this.getMarginRight() / controlCount)
          : (windowRect.Width - (controlCount * 10)) / controlCount;

      // full width
      wnd.setPosSize(xOffset, yOffset, calculatedControlWidth,
          controlProperties.getControlPercentSize().getHeight(), (PosSize.POSSIZE));

      xOffset += calculatedControlWidth + this.getMarginBetweenControls();
    }
  }

  private void applyControlBinding(ControlModel controlModel)
  {
    XControl sourceControl = this.getControlContainer()
        .getControl(controlModel.getBindToControlControlWidthAndXOffset());

    if (sourceControl == null)
      return;

    XWindow wndXControl = UNO.XWindow(sourceControl);
    Rectangle rect = wndXControl.getPosSize();

    for (ControlProperties controlProperties : controlModel.getControls())
    {
      int calculatedControlWidthBinding = ((rect.Width / 100)
          * controlProperties.getControlPercentSize().getWidth());

      XControl targetControl = this.getControlContainer()
          .getControl(controlProperties.getControlName());

      if (targetControl == null)
      {
        this.addControlToControlContainer(controlProperties.getControlName(),
            controlProperties.getXControl());
        targetControl = controlProperties.getXControl();
      }

      XWindow xWindow = UNO.XWindow(targetControl);
      xWindow.setPosSize(rect.X + xOffsetBinding, yOffsetBinding, calculatedControlWidthBinding,
          controlProperties.getControlPercentSize().getHeight(), (PosSize.POSSIZE));

      xOffsetBinding += calculatedControlWidthBinding + this.getMarginBetweenControls();
    }
  }

  private void modifyVerticalControl(Rectangle windowRect, XControl control,
      ControlProperties controlProperties, Align alignment)
  {
    XWindow wnd = UnoRuntime.queryInterface(XWindow.class, control);

    Rectangle cr = wnd.getPosSize();

    if (alignment == Align.RIGHT)
    {
      wnd.setPosSize(windowRect.Width / 2, yOffset, windowRect.Width / 2,
          controlProperties.getControlPercentSize().getHeight(), (PosSize.POSSIZE));
    } else if (alignment == Align.LEFT)
    {
      wnd.setPosSize(0, yOffset, windowRect.Width / 2,
          controlProperties.getControlPercentSize().getHeight(), (PosSize.POSSIZE));
    } else
    {
      int xOffsetTemp = 0;

      xOffsetTemp = this.getMarginLeft() > 0 ? this.getMarginLeft() : xOffsetTemp;
      xOffsetTemp = controlProperties.getMarginLeft() > 0 ? controlProperties.getMarginLeft()
          : xOffsetTemp;

      // full width
      // TODO: -10 wieder entfernen, dirty hack wegen überlappung sidebar controls zum Fenster.
      wnd.setPosSize(xOffsetTemp, yOffset, windowRect.Width - this.getMarginLeft() - 10,
          controlProperties.getControlPercentSize().getHeight(), (PosSize.POSSIZE));
    }

    yOffset += cr.Height + (controlProperties.getMarginBetweenControls() > 0
        ? controlProperties.getMarginBetweenControls()
        : this.getMarginBetweenControls());
  }

  private void newLine(ControlModel controlModel)
  {
    yOffset += controlModel.getLinebreakHeight();
    xOffset = this.getMarginLeft() > 0 ? this.getMarginLeft() : 0;
  }

  int groupBoxHeight;

  public int calcGroupBoxHeightByControlProperties(List<ControlModel> controlModels)
  {
    if (controlModels.isEmpty())
    {
      return 0;
    }

    groupBoxHeight = 0;

    controlModels.parallelStream().forEach(
        controlModel -> controlModel.getControls().parallelStream().forEach(controlProperty -> {
          groupBoxHeight += controlProperty.getControlSize().getHeight()
              + controlProperty.getMarginBetweenControls();
        }));

    return groupBoxHeight + 10;
  }

  private int getBottomYOffset(Rectangle windowRect, int controlHeight)
  {
    return windowRect.Height - controlHeight - this.getWindowBottomMargin();
  }

  private void addControlToControlContainer(String name, XControl control)
  {
    this.getControlContainer().addControl(name, control);
  }

  public void addControlsToList(ControlModel control)
  {
    this.controlList.add(control);
  }

  public List<ControlModel> getControlList()
  {
    return this.controlList;
  }

  private int getMarginBetweenControls()
  {
    return this.marginBetweenControls;
  }

  public void setMarginBetweenControls(int margin)
  {
    this.marginBetweenControls = margin;
  }

  public void setMarginTop(int marginTop)
  {
    this.marginTop = marginTop;
  }

  private int getMarginTop()
  {
    return this.marginTop;
  }

  public void setMarginRight(int marginRight)
  {
    this.marginRight = marginRight;
  }

  private int getMarginRight()
  {
    return this.marginRight;
  }

  public void setMarginLeft(int marginLeft)
  {
    this.marginLeft = marginLeft;
  }

  private int getWindowBottomMargin()
  {
    return this.windowBottomMargin;
  }

  public void setWindowBottomMargin(int marginBottom)
  {
    this.windowBottomMargin = marginBottom;
  }

  private int getMarginLeft()
  {
    return this.marginLeft;
  }

  public XControl getControl(String name)
  {
    return this.getControlContainer().getControl(name);
  }

  public XControl[] getControls()
  {
    return this.getControlContainer().getControls();
  }

  public XControlContainer getControlContainer()
  {
    if (this.controlContainer == null)
    {
      LOGGER.error("BaseLayout: getControlContainer(): controlContainer is NULL.");
    }

    return this.controlContainer;
  }

  public XWindow getContainerWindow()
  {
    if (this.containerWindow == null)
    {
      LOGGER.error("BaseLayout: getContainerWindow(): containerWindow is NULL.");
    }

    return this.containerWindow;
  }

  @Override
  public void windowResized(WindowEvent arg0)
  {
    this.draw();
  }

  @Override
  public void windowShown(EventObject arg0)
  {
    this.draw();
  }
}