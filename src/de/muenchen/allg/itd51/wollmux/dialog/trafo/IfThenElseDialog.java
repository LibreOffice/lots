/*
 * Dateiname: IfThenElseDialog.java
 * Projekt  : WollMux
 * Funktion : Erlaubt die Bearbeitung der Funktion eines Wenn-Dann-Sonst-Feldes.
 * 
 * Copyright (c) 2008-2019 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 01.02.2008 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractItemListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractTextListener;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractWindowListener;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * Erlaubt die Bearbeitung der Funktion eines Wenn-Dann-Sonst-Feldes.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class IfThenElseDialog
{
  private static final Logger LOGGER = LoggerFactory.getLogger(IfThenElseDialog.class);

  private XControlContainer controlContainer;

  private XDialog dialog;

  private XWindow window;

  /**
   * Das Objekt, das den Startinhalt des Dialogs spezifiziert (und am Ende verwendet wird, um den
   * Rückgabewert zu speichern).
   */
  private TrafoDialogParameters params;

  public IfThenElseDialog(TrafoDialogParameters params)
  {
    this.params = params;
    if (!params.isValid || params.conf == null || params.fieldNames == null
        || params.fieldNames.size() == 0)
      throw new IllegalArgumentException();

    params.isValid = false; // erst bei Beendigung mit Okay werden sie wieder valid

    buildGUI(params);
  }

  private void buildGUI(TrafoDialogParameters params)
  {

    XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
    XContainerWindowProvider provider = null;

    try
    {
      provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));
    } catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }

    window = provider.createContainerWindow(
        "vnd.sun.star.script:WollMux.if_then_else2?location=application", "", peer, null);
    window.addWindowListener(windowListener);

    controlContainer = UnoRuntime.queryInterface(XControlContainer.class, window);

    currentResultConfig = createRootFunc();

    XComboBox cbWennSerienbrieffeld = UNO
        .XComboBox(controlContainer.getControl("cbWennSerienbrieffeld"));
    XComboBox cbDannSerienbrieffeld = UNO
        .XComboBox(controlContainer.getControl("cbDannSerienbrieffeld"));
    XComboBox cbSonstSerienbrieffeld = UNO
        .XComboBox(controlContainer.getControl("cbSonstSerienbrieffeld"));
    UNO.XTextComponent(cbWennSerienbrieffeld).setText("Serienbrieffeld");
    UNO.XTextComponent(cbDannSerienbrieffeld).setText("Serienbrieffeld");
    UNO.XTextComponent(cbSonstSerienbrieffeld).setText("Serienbrieffeld");

    params.fieldNames.forEach(fieldName -> {
      cbWennSerienbrieffeld.addItem(fieldName, (short) (cbWennSerienbrieffeld.getItemCount() + 1));
      cbDannSerienbrieffeld.addItem(fieldName, (short) (cbWennSerienbrieffeld.getItemCount() + 1));
      cbSonstSerienbrieffeld.addItem(fieldName, (short) (cbWennSerienbrieffeld.getItemCount() + 1));
    });

    XComboBox cbWennComperator = UNO.XComboBox(controlContainer.getControl("cbWennComporator"));
    testTypes.forEach(item -> cbWennComperator.addItem(item.label,
        (short) (cbWennComperator.getItemCount() + 1)));
    cbWennComperator.addItemListener(comparatorItemListener);

    XComboBox cbNot = UNO.XComboBox(controlContainer.getControl("cbNot"));
    cbNot.addItem("", (short) 0);
    cbNot.addItem("NOT", (short) 1);
    cbNot.addItemListener(notItemListener);

    XButton wennButton = UNO.XButton(controlContainer.getControl("tbnWenn"));
    wennButton.addActionListener(wennActionListener);

    XButton dannButton = UNO.XButton(controlContainer.getControl("btnDann"));
    dannButton.addActionListener(dannActionListener);

    XButton sonstButton = UNO.XButton(controlContainer.getControl("btnSonst"));
    sonstButton.addActionListener(sonstActionListener);

    XTextComponent txtValueWenn = UNO.XTextComponent(controlContainer.getControl("txtValueWenn"));
    txtValueWenn.addTextListener(txtValueWennListener);

    dialog = UnoRuntime.queryInterface(XDialog.class, window);
    short result = dialog.execute();

    if (result == ExecutableDialogResults.OK)
    {
      updateTrafoConf();
    } else
    {
      dialog.endExecute();
    }
  }

  private ConfigThingy currentResultConfig;

  private AbstractTextListener txtValueWennListener = event -> {
  };

  private AbstractActionListener sonstActionListener = event -> {
    XTextComponent txtField = UNO.XTextComponent(controlContainer.getControl("resultTextField"));
    addElse(currentResultConfig);
    txtField.setText(currentResultConfig.stringRepresentation());
  };

  private AbstractActionListener wennActionListener = event -> {
    XTextComponent txtField = UNO.XTextComponent(controlContainer.getControl("resultTextField"));
    addIf(currentResultConfig);
    txtField.setText(currentResultConfig.stringRepresentation());
  };

  private AbstractActionListener dannActionListener = event -> {
    XTextComponent txtField = UNO.XTextComponent(controlContainer.getControl("resultTextField"));
    addThen(currentResultConfig);
    txtField.setText(currentResultConfig.stringRepresentation());
  };

  private AbstractItemListener notItemListener = event -> {
    XTextComponent not = UNO.XTextComponent(controlContainer.getControl("cbNot"));

    if (not.getText() == null || not.getText().isEmpty())
      return;

    XTextComponent txtField = UNO.XTextComponent(controlContainer.getControl("resultTextField"));

    addNot(currentResultConfig);
    txtField.setText(currentResultConfig.stringRepresentation());
  };

  private AbstractItemListener comparatorItemListener = event -> {
    XComboBox cbComparator = UNO.XComboBox(controlContainer.getControl("cbWennComporator"));
    String func = "";
    for (TestType testType : testTypes)
    {
      if (testType.label.equals(UNO.XTextComponent(cbComparator).getText()))
      {
        func = testType.func;
        break;
      }
    }

    XComboBox cbSerienbrieffeld = UNO
        .XComboBox(controlContainer.getControl("cbWennSerienbrieffeld"));
    XTextComponent txtField = UNO.XTextComponent(controlContainer.getControl("resultTextField"));
    XTextComponent txtValue = UNO.XTextComponent(controlContainer.getControl("txtValueWenn"));

    addSTRCMPBlock(currentResultConfig, func, cbSerienbrieffeld.getItem((short) event.Selected),
        txtValue.getText());
    
    txtField.setText(currentResultConfig.stringRepresentation());
  };

  private ConfigThingy addSTRCMPBlock(ConfigThingy conf, String comparator, String value1,
      String value2)
  {
    ConfigThingy result = null;

    try
    {
      result = conf.get("NOT").add(comparator);

      ConfigThingy innerChild = result.add("VALUE").add(value1);
      innerChild.add(value2);
    } catch (NodeNotFoundException e)
    {
      LOGGER.error("", e);
    }

    return result;
  }

  private ConfigThingy createRootFunc()
  {
    return new ConfigThingy("FUNC");
  }

  private ConfigThingy addNot(ConfigThingy config)
  {
    try
    {
      config.getLastChild().add("NOT");
    } catch (NodeNotFoundException e)
    {
      LOGGER.error("", e);
    }

    return config;
  }

  private ConfigThingy addIf(ConfigThingy config)
  {
    try
    {
      config = config.getLastChild();
    } catch (NodeNotFoundException e)
    {
      LOGGER.error("", e);
    }
    return config.add("IF");
  }

  private ConfigThingy addThen(ConfigThingy config)
  {
    return config.add("THEN");
  }

  private ConfigThingy addElse(ConfigThingy config)
  {
    return config.add("ELSE");
  }

  private ConfigThingy addCAT(ConfigThingy config)
  {
    return config.add("CAT");
  }

  /**
   * Aktualisiert {@link #params},conf anhand des aktuellen Dialogzustandes und setzt params,isValid
   * auf true.
   * 
   */
  private void updateTrafoConf()
  {
    params.conf = new ConfigThingy(params.conf.getName());
    params.conf.addChild(currentResultConfig);
    params.isValid = true;
  }

  private static class TestType
  {
    public String label;

    public String func;

    public TestType(String label, String func)
    {
      this.label = label;
      this.func = func;
    }

    @Override
    public String toString()
    {
      return label;
    }
  }

  private static final List<TestType> testTypes = Arrays.asList(
      new TestType(L.m("genau ="), "STRCMP"), new TestType(L.m("numerisch ="), "NUMCMP"),
      new TestType(L.m("numerisch <"), "LT"), new TestType(L.m("numerisch <="), "LE"),
      new TestType(L.m("numerisch >"), "GT"), new TestType(L.m("numerisch >="), "GE"),
      new TestType(L.m("regulärer A."), "MATCH"));

  private AbstractWindowListener windowListener = new AbstractWindowListener()
  {
    @Override
    public void disposing(EventObject event)
    {
      updateTrafoConf();
    }
  };

}
