package de.muenchen.allg.itd51.wollmux.core.dialog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XControl;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.itd51.wollmux.core.constants.XLabelProperties;
import de.muenchen.allg.itd51.wollmux.core.constants.XNumericFieldProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;

public class ControlProperties
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ControlProperties.class);
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
    XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, this.xControl.getModel());
    
    try
    {
      propertySet.setPropertyValue(XLabelProperties.LABEL, label );
    } catch (UnknownPropertyException | IllegalArgumentException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }
  
  public void setEnabled(boolean isEnabled)
  {
    XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, this.xControl.getModel());
    
    try
    {
      propertySet.setPropertyValue("Enabled", isEnabled);
    } catch (UnknownPropertyException | IllegalArgumentException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }
  
  public void setBorder(short value) {
    XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, this.xControl.getModel());
    try
    {
      propertySet.setPropertyValue(XNumericFieldProperties.BORDER, value);
    } catch (UnknownPropertyException | IllegalArgumentException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }
  
  public void setBorderColor(int borderColor) {
    XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, this.xControl.getModel());
    try
    {
      propertySet.setPropertyValue(XNumericFieldProperties.BORDER_COLOR, borderColor);
    } catch (UnknownPropertyException | IllegalArgumentException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }
  
  public void setSpinEnabled(boolean enabled) {
    XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, this.xControl.getModel());
    try
    {
      propertySet.setPropertyValue(XNumericFieldProperties.SPIN, enabled);
    } catch (UnknownPropertyException | IllegalArgumentException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }
  
  public void setValue(int value) {
    XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, this.xControl.getModel());
    try
    {
      propertySet.setPropertyValue(XNumericFieldProperties.VALUE, value);
    } catch (UnknownPropertyException | IllegalArgumentException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }
 
  public void setDecimalAccuracy(short value) {
    XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, this.xControl.getModel());
    try
    {
      propertySet.setPropertyValue(XNumericFieldProperties.DECIMAL_ACCURACY, value);
    } catch (UnknownPropertyException | IllegalArgumentException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }
  
  public void setComboBoxDropDown(boolean value) {
    XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, this.xControl.getModel());
    try
    {
      propertySet.setPropertyValue("Dropdown", value);
    } catch (UnknownPropertyException | IllegalArgumentException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }
  
  public void enableToggleButton(boolean value) {
    XPropertySet propertySet = UnoRuntime.queryInterface(XPropertySet.class, this.xControl.getModel());
    try
    {
      propertySet.setPropertyValue("Toggle", value);
    } catch (UnknownPropertyException | IllegalArgumentException | PropertyVetoException
        | WrappedTargetException e)
    {
      LOGGER.error("", e);
    }
  }
  
}
