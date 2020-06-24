/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
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
import de.muenchen.allg.dialog.adapter.AbstractActionListener;
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
      controlContainer = UNO.XControlContainer(window);

      XComboBox cbAnrede = UNO.XComboBox(controlContainer.getControl("cbSerienbrieffeld"));
      cbAnrede.addItems(fieldNames.toArray(new String[fieldNames.size()]), (short) 0);

      XButton btnAbort = UNO.XButton(controlContainer.getControl("btnAbort"));
      btnAbort.addActionListener(btnAbortActionListener);

      XButton btnOK = UNO.XButton(controlContainer.getControl("btnOK"));
      btnOK.addActionListener(btnOKActionListener);

      dialog = UNO.XDialog(window);
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
