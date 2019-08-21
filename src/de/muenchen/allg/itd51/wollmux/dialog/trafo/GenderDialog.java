/*
 * Dateiname: GenderDialog.java
 * Projekt  : WollMux
 * Funktion : Erlaubt die Bearbeitung der Funktion eines Gender-Feldes.
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
 * 21.02.2008 | LUT | Erstellung als GenderDialog
 * -------------------------------------------------------------------
 *
 * Christoph lutz (D-III-ITD 5.1)
 * @version 1.0
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XComboBox;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.dialog.adapter.AbstractActionListener;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Erlaubt die Bearbeitung der Funktion eines Gender-Feldes.
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class GenderDialog
{
  private static final Logger LOGGER = LoggerFactory.getLogger(GenderDialog.class);

  private XControlContainer controlContainer;

  private XDialog dialog;

  private TextDocumentController documentController;

  public GenderDialog(List<String> fieldNames, TextDocumentController documentController)
  {
    this.documentController = documentController;

    HashSet<String> uniqueFieldNames = new HashSet<>(fieldNames);
    List<String> sortedNames = new ArrayList<>(uniqueFieldNames);
    Collections.sort(sortedNames);

    buildGUI(sortedNames);
  }

  /**
   * Baut das genderPanel auf.
   */
  private void buildGUI(final List<String> fieldNames)
  {
    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider",
              UNO.defaultContext));

      XWindow window = provider.createContainerWindow(
          "vnd.sun.star.script:WollMux.gender_dialog?location=application", "", peer, null);
      controlContainer = UnoRuntime.queryInterface(XControlContainer.class, window);

      XComboBox cbAnrede = UNO.XComboBox(controlContainer.getControl("cbSerienbrieffeld"));
      cbAnrede.addItems(fieldNames.toArray(new String[fieldNames.size()]), (short) 0);

      XButton btnAbort = UNO.XButton(controlContainer.getControl("btnAbort"));
      btnAbort.addActionListener(btnAbortActionListener);

      XButton btnOK = UNO.XButton(controlContainer.getControl("btnOK"));
      btnOK.addActionListener(btnOKActionListener);

      dialog = UnoRuntime.queryInterface(XDialog.class, window);
      dialog.execute();
    } catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  private AbstractActionListener btnAbortActionListener = event -> dialog.endExecute();

  private AbstractActionListener btnOKActionListener = event -> {
    ConfigThingy conf = generateGenderTrafoConf(
        UNO.XTextComponent(controlContainer.getControl("cbSerienbrieffeld")).getText(),
        UNO.XTextComponent(controlContainer.getControl("txtMale")).getText(),
        UNO.XTextComponent(controlContainer.getControl("txtFemale")).getText(),
        UNO.XTextComponent(controlContainer.getControl("txtOthers")).getText());
    documentController.replaceSelectionWithTrafoField(conf, "Gender");
    dialog.endExecute();
  };

  /**
   * Erzeugt ein ConfigThingy mit dem Aufbau BIND(FUNCTION "Gender" SET("Anrede", VALUE
   * "<anredeFieldId>") SET("Falls_Anrede_HerrN", "<textHerr>") SET("Falls_Anrede_Frau",
   * "<textFrau>") SET("Falls_sonstige_Anrede", "<textSonst>"))
   *
   * @param anredeId
   *          Id des geschlechtsbestimmenden Feldes
   * @param textHerr
   *          Text für Herr
   * @param textFrau
   *          Text für Frau
   * @param textSonst
   *          Text für sonstige Anreden
   */
  public static ConfigThingy generateGenderTrafoConf(String anredeId, String textHerr,
      String textFrau, String textSonst)
  {
    ConfigThingy conf = new ConfigThingy("Func");
    ConfigThingy bind = new ConfigThingy("BIND");
    conf.addChild(bind);
    bind.add("FUNCTION").add("Gender");

    ConfigThingy setAnrede = bind.add("SET");
    setAnrede.add("Anrede");
    setAnrede.add("VALUE").add(anredeId);

    ConfigThingy setHerr = bind.add("SET");
    setHerr.add("Falls_Anrede_HerrN");
    setHerr.add(textHerr);

    ConfigThingy setFrau = bind.add("SET");
    setFrau.add("Falls_Anrede_Frau");
    setFrau.add(textFrau);

    ConfigThingy setSonst = bind.add("SET");
    setSonst.add("Falls_sonstige_Anrede");
    setSonst.add(textSonst);

    return conf;
  }
}
