package de.muenchen.allg.itd51.wollmux.core.document.nodes;

import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XTextDocument;

public class TextFieldNode implements Node
{
  protected XDependentTextField textfield;

  protected XTextDocument doc;

  public TextFieldNode(XDependentTextField textField, XTextDocument doc)
  {
    super();
    this.textfield = textField;
    this.doc = doc;
  }
}