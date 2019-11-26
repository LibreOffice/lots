package de.muenchen.allg.itd51.wollmux.core.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ControlModel
{
  private Orientation orientation;
  private Align alignment;
  private Optional<Dock> dock;
  private int lineBreakHeight = 40; //default
  private String bindingControlName;
  private List<ControlProperties> controls = new ArrayList<>();

  public ControlModel(Orientation horizontal, Align right,
      List<ControlProperties> controls,
      Optional<Dock> dock)
  {
    this.orientation = horizontal;
    this.alignment = right;
    this.dock = dock;
    this.controls = controls;
  }
  
  public void setBindToControlWidthAndXOffset(String controlName) {
    this.bindingControlName = controlName;
  }
  
  public String getBindToControlControlWidthAndXOffset() {
    return this.bindingControlName;
  }
  
  public int getLinebreakHeight() {
    return this.lineBreakHeight;
  }
  
  public void setLinebreakHeight(int linebreakHeight) {
    this.lineBreakHeight = linebreakHeight;
  }

  public Orientation getOrientation()
  {
    return this.orientation;
  }

  public Align getAlignment()
  {
    return this.alignment;
  }

  public Optional<Dock> getDock()
  {
    return this.dock;
  }

  public List<ControlProperties> getControls()
  {
    return this.controls;
  }
  
  public void addControlToControlList(ControlProperties control) {
    this.controls.add(control);
  }

  public enum Align
  {
    NONE("NONE"),
    RIGHT("RIGHT"),
    LEFT("LEFT");

    private final String alignment;

    Align(final String alignment)
    {
      this.alignment = alignment;
    }

    @Override
    public String toString()
    {
      return this.alignment;
    }
  }

  public enum Dock
  {
    TOP("TOP"),
    BOTTOM("BOTTOM");

    private final String dock;

    Dock(final String dock)
    {
      this.dock = dock;
    }

    @Override
    public String toString()
    {
      return this.dock;
    }
  }

  public enum Orientation
  {
    HORIZONTAL("HORIZONTAL"),
    VERTICAL("VERTICAL");

    private final String orientation;

    Orientation(final String orientation)
    {
      this.orientation = orientation;
    }

    @Override
    public String toString()
    {
      return this.orientation;
    }
  }

  public enum ControlType
  {
    EDIT("com.sun.star.awt.UnoControlEdit"),
    BUTTON("com.sun.star.awt.UnoControlButton"),
    PROGRESSBAR("com.sun.star.awt.UnoControlProgressBar"),
    CHECKBOX("com.sun.star.awt.UnoControlCheckBox"),
    RADIO("com.sun.star.awt.UnoControlRadioButton"),
    DATE("com.sun.star.awt.UnoControlDateField"),
    LINE("com.sun.star.awt.UnoControlFixedLine"),
    LABEL("com.sun.star.awt.UnoControlFixedText"),
    SCROLLBAR("com.sun.star.awt.UnoControlScrollBar"),
    COMBOBOX("com.sun.star.awt.UnoControlComboBox"),
    SPINBUTTON("com.sun.star.awt.UnoControlSpinButton"),
    NUMERIC_FIELD("com.sun.star.awt.UnoControlNumericField"),
    FIXEDLINE("com.sun.star.awt.UnoControlFixedLine"),
    GROUPBOX("com.sun.star.awt.UnoControlGroupBox"),
    LIST_BOX("com.sun.star.awt.UnoControlListBox"),
    IMAGE_CONTROL("com.sun.star.awt.UnoControlImageControl");

    private String controlType;

    ControlType(String controlType)
    {
      this.controlType = controlType;
    }

    @Override
    public String toString()
    {
      return this.controlType;
    }
  }
}
