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
package de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;

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
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Dialog for creating a gender field.
 */
public class GenderDialog
{
  private static final Logger LOGGER = LoggerFactory.getLogger(GenderDialog.class);

  private GenderDialog()
  {
    // nothing to do
  }

  /**
   * Start the dialog.
   *
   * @param fieldNames
   *          The fields of the document.
   * @param documentController
   *          The controller of the document.
   */
  public static void startDialog(List<String> fieldNames, TextDocumentController documentController)
  {
    HashSet<String> uniqueFieldNames = new HashSet<>(fieldNames);
    List<String> sortedNames = new ArrayList<>(uniqueFieldNames);
    Collections.sort(sortedNames);

    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UnoRuntime.queryInterface(XContainerWindowProvider.class,
          UNO.xMCF.createInstanceWithContext("com.sun.star.awt.ContainerWindowProvider", UNO.defaultContext));

      XWindow window = provider.createContainerWindow("vnd.sun.star.script:WollMux.gender_dialog?location=application",
          "", peer, null);
      XControlContainer controlContainer = UNO.XControlContainer(window);
      XDialog dialog = UNO.XDialog(window);

      XComboBox cbAnrede = UNO.XComboBox(controlContainer.getControl("cbSerienbrieffeld"));
      cbAnrede.addItems(fieldNames.toArray(new String[fieldNames.size()]), (short) 0);

      XButton btnAbort = UNO.XButton(controlContainer.getControl("btnAbort"));
      AbstractActionListener btnAbortActionListener = event -> dialog.endExecute();
      btnAbort.addActionListener(btnAbortActionListener);

      XButton btnOK = UNO.XButton(controlContainer.getControl("btnOK"));
      AbstractActionListener btnOKActionListener = event -> {
        ConfigThingy conf = generateGenderTrafoConf(
            UNO.XTextComponent(controlContainer.getControl("cbSerienbrieffeld")).getText(),
            UNO.XTextComponent(controlContainer.getControl("txtMale")).getText(),
            UNO.XTextComponent(controlContainer.getControl("txtFemale")).getText(),
            UNO.XTextComponent(controlContainer.getControl("txtOthers")).getText());
        documentController.replaceSelectionWithTrafoField(conf, "Gender");
        dialog.endExecute();
      };
      btnOK.addActionListener(btnOKActionListener);

      dialog.execute();
    } catch (com.sun.star.uno.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Generate the gender field describtion in the form
   * {@code BIND(FUNCTION "Gender" SET("Anrede", VALUE
   * "<anredeFieldId>") SET("Falls_Anrede_HerrN", "<textHerr>") SET("Falls_Anrede_Frau",
   * "<textFrau>") SET("Falls_sonstige_Anrede", "<textSonst>"))}
   *
   * @param anredeId
   *          ID of the field to determine the gender.
   * @param textHerr
   *          Male text.
   * @param textFrau
   *          Female text.
   * @param textSonst
   *          Text if neither male nor female.
   * @return A {@link ConfigThingy} describing the gender field.
   */
  private static ConfigThingy generateGenderTrafoConf(String anredeId, String textHerr, String textFrau,
      String textSonst)
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
