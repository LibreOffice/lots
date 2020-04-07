package de.muenchen.allg.itd51.wollmux.core.dialog;

import com.sun.star.awt.XControl;

import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;
import de.muenchen.allg.itd51.wollmux.core.util.Utils;
import de.muenchen.allg.util.UnoProperty;

public class ControlProperties
{
  private ControlType controlType;
  private int marginBetweenControls;
  private int marginLeft;
  private String controlName;
  private XControl xControl;
  private PercentSize percentSize = new PercentSize(0, 0);
  private Size size = new Size(0, 0);

  public class PercentSize
  {
    private int controlPercentWidth;
    private int controlPercentHeight;

    public PercentSize(int controlPercentWidth, int controlPercentHeight)
    {
      this.controlPercentWidth = controlPercentWidth;
      this.controlPercentHeight = controlPercentHeight;
    }

    public int getWidth()
    {
      return this.controlPercentWidth;
    }

    public int getHeight()
    {
      return this.controlPercentHeight;
    }
  }

  public class Size
  {
    private int controlWidth;
    private int controlHeight;

    public Size(int controlWidth, int controlHeight)
    {
      this.controlWidth = controlWidth;
      this.controlHeight = controlHeight;
    }
    
    public int getWidth()
    {
      return this.controlWidth;
    }

    public int getHeight()
    {
      return this.controlHeight;
    }
  }

  public ControlProperties(ControlType controlType, String controlName)
  {
    this.controlType = controlType;
    this.controlName = controlName;
    this.xControl = UNODialogFactory.convertToXControl(this);
  }

  public XControl getXControl()
  {
    return this.xControl;
  }
  
  public void setXControl(XControl xControl) {
    this.xControl = xControl;
  }

  public void setMarginLeft(int marginLeft)
  {
    this.marginLeft = marginLeft;
  }

  public int getMarginLeft()
  {
    return this.marginLeft;
  }

  public void setMarginBetweenControls(int margin)
  {
    this.marginBetweenControls = margin;
  }

  public int getMarginBetweenControls()
  {
    return this.marginBetweenControls;
  }

  public String getControlName()
  {
    return this.controlName;
  }

  public ControlType getControlType()
  {
    return this.controlType;
  }

  public void setControlPercentSize(int percentWidth, int percentHeight)
  {
    this.percentSize = new PercentSize(percentWidth, percentHeight);
  }

  public PercentSize getControlPercentSize()
  {
    return this.percentSize;
  }

  public void setControlSize(int width, int height)
  {
    this.size = new Size(width, height);
  }

  public Size getControlSize()
  {
    return this.size;
  }

  public void setLabel(String label)
  {
    Utils.setProperty(this.xControl.getModel(), UnoProperty.LABEL, label);
  }
  
  public void setEnabled(boolean isEnabled)
  {
    Utils.setProperty(this.xControl.getModel(), UnoProperty.ENABLED, isEnabled);
  }
  
  public void setBorder(short value)
  {
    Utils.setProperty(this.xControl.getModel(), UnoProperty.BORDER, value);
  }
  
  public void setBorderColor(int borderColor)
  {
    Utils.setProperty(this.xControl.getModel(), UnoProperty.BORDER_COLOR, borderColor);
  }
  
  public void setSpinEnabled(boolean enabled)
  {
    Utils.setProperty(this.xControl.getModel(), UnoProperty.SPIN, enabled);
  }
  
  public void setValue(int value)
  {
    Utils.setProperty(this.xControl.getModel(), UnoProperty.VALUE, value);
  }
 
  public void setDecimalAccuracy(short value)
  {
    Utils.setProperty(this.xControl.getModel(), UnoProperty.DECIMAL_ACCURACY, value);
  }
  
  public void setComboBoxDropDown(boolean value)
  {
    Utils.setProperty(this.xControl.getModel(), UnoProperty.DROPDOWN, value);
  }
  
  /**
   * Makes a button a toggle button.
   *
   * @param value
   *          True if it should be a toggle button. False otherwise.
   */
  public void enableToggleButton(boolean value)
  {
    Utils.setProperty(this.xControl.getModel(), UnoProperty.TOGGLE, value);
  }
  
}
