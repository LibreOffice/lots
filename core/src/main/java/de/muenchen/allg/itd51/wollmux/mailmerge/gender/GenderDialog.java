/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.mailmerge.gender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XButton;
import com.sun.star.awt.XContainerWindowProvider;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialog2;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.ui.dialogs.ExecutableDialogResults;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperRuntimeException;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractActionListener;
import org.libreoffice.ext.unohelper.dialog.adapter.AbstractTextListener;
import org.libreoffice.ext.unohelper.util.UnoComponent;

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
   * @param currentValues
   *          The current TRAFO for the mail merge field.
   * @return {@link XDialog#execute()}.
   */
  public static short startDialog(List<String> fieldNames, GenderTrafoModel currentValues)
  {
    HashSet<String> uniqueFieldNames = new HashSet<>(fieldNames);
    List<String> sortedNames = new ArrayList<>(uniqueFieldNames);
    Collections.sort(sortedNames);

    try
    {
      XWindowPeer peer = UNO.XWindowPeer(UNO.desktop.getCurrentFrame().getContainerWindow());
      XContainerWindowProvider provider = UNO.XContainerWindowProvider(
          UnoComponent.createComponentWithContext(UnoComponent.CSS_AWT_CONTAINER_WINDOW_PROVIDER));

      XWindow window = provider.createContainerWindow("vnd.sun.star.script:WollMux.gender_dialog?location=application",
          "", peer, null);
      XControlContainer controlContainer = UNO.XControlContainer(window);
      XDialog2 dialog = UNO.XDialog2(window);

      XTextComponent field = UNO.XTextComponent(controlContainer.getControl("cbSerienbrieffeld"));
      UNO.XComboBox(field).addItems(fieldNames.toArray(new String[sortedNames.size()]), (short) 0);
      field.setText(currentValues.getField());
      AbstractTextListener anredeListener = e -> currentValues.setField(UNO.XTextComponent(e.Source).getText());
      field.addTextListener(anredeListener);

      XTextComponent male = UNO.XTextComponent(controlContainer.getControl("txtMale"));
      male.setText(currentValues.getMale());
      AbstractTextListener maleListener = e -> currentValues.setMale(UNO.XTextComponent(e.Source).getText());
      male.addTextListener(maleListener);

      XTextComponent female = UNO.XTextComponent(controlContainer.getControl("txtFemale"));
      female.setText(currentValues.getFemale());
      AbstractTextListener femaleListener = e -> currentValues.setFemale(UNO.XTextComponent(e.Source).getText());
      female.addTextListener(femaleListener);

      XTextComponent other = UNO.XTextComponent(controlContainer.getControl("txtOthers"));
      other.setText(currentValues.getOther());
      AbstractTextListener otherListener = e -> currentValues.setOther(UNO.XTextComponent(e.Source).getText());
      other.addTextListener(otherListener);

      XButton btnAbort = UNO.XButton(controlContainer.getControl("btnAbort"));
      AbstractActionListener btnAbortActionListener = event -> dialog.endDialog(ExecutableDialogResults.CANCEL);
      btnAbort.addActionListener(btnAbortActionListener);

      XButton btnOK = UNO.XButton(controlContainer.getControl("btnOK"));
      AbstractActionListener btnOKActionListener = event -> dialog.endDialog(ExecutableDialogResults.OK);
      btnOK.addActionListener(btnOKActionListener);

      return dialog.execute();
    } catch (UnoHelperRuntimeException e)
    {
      LOGGER.error("", e);
      return ExecutableDialogResults.CANCEL;
    }
  }
}
