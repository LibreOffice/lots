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
package de.muenchen.allg.itd51.wollmux.form.sidebar;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.awt.ItemEvent;
import com.sun.star.awt.XCheckBox;
import com.sun.star.awt.XControlContainer;
import com.sun.star.awt.XControlModel;
import com.sun.star.awt.XItemListener;
import com.sun.star.awt.XListBox;
import com.sun.star.awt.XTextComponent;
import com.sun.star.awt.tab.XTabPage;
import com.sun.star.awt.tab.XTabPageContainer;
import com.sun.star.drawing.XControlShape;
import com.sun.star.drawing.XDrawPage;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.ui.XDeck;
import com.sun.star.ui.XPanel;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.itd51.wollmux.test.WollMuxTest;
import de.muenchen.allg.util.UnoProperty;

public class FormTest extends WollMuxTest
{
  private XTextDocument xDoc;
  private XDeck deck;
  private XControlContainer controlContainer;

  @BeforeEach
  public void setUp() throws Exception
  {
    URL file = getClass().getResource("formtest.ott");
    xDoc = UNO.XTextDocument(loadAsyncComponent(file.toString(), true, false).get(5, TimeUnit.SECONDS));
    deck = UNO.XDeck(UNO.XController2(xDoc.getCurrentController()).getSidebar().getDecks()
        .getByName(FormSidebarController.WM_FORM_GUI));
    controlContainer = UNO
        .XControlContainer(UnoRuntime.queryInterface(XPanel.class, deck.getPanels().getByIndex(0)).getDialog());
  }

  @AfterEach
  public void tearDown() throws Exception
  {
    UNO.XCloseable(xDoc).close(false);
  }

  @Test
  public void testFormSidebar() throws Exception
  {
    XTabPageContainer tabPageContainer = UNO.XTabPageContainer(controlContainer.getControl("tabControl"));
    assertEquals(2, tabPageContainer.getTabPageCount(), "should have two tabs");
    assertEquals(1, tabPageContainer.getActiveTabPageID(), "wrong tab is active");
    XTabPage tabPage = tabPageContainer.getTabPageByID((short) 1);
    XControlContainer tabControlContainer = UNO.XControlContainer(tabPage);
    assertEquals(11, tabControlContainer.getControls().length, "tab should have 10 controls");

    String name = "WM(CMD 'insertFormValue' ID '%s')\n";
    XBookmarksSupplier supplier = UNO.XBookmarksSupplier(xDoc);
    UnoDictionary<XTextContent> bookmarks = UnoDictionary.create(supplier.getBookmarks(), XTextContent.class);

    XTextComponent textField = UNO.XTextComponent(tabControlContainer.getControl("text"));
    textField.setText("test");
    XTextContent contentTextField = bookmarks.get(String.format(name, "text"));
    assertEquals("test", contentTextField.getAnchor().getString(), "wrong text");

    XTextComponent textArea = UNO.XTextComponent(tabControlContainer.getControl("area"));
    textArea.setText("test\narea");
    XTextContent contentTextArea = bookmarks.get(String.format(name, "area"));
    assertEquals("test\narea", contentTextArea.getAnchor().getString(), "wrong text");

    XListBox listBox = UNO.XListBox(tabControlContainer.getControl("combo"));
    listBox.selectItemPos((short) 0, true);
    XTextContent contentListBox = bookmarks.get(String.format(name, "combo"));
    assertEquals("A", contentListBox.getAnchor().getString(), "wrong element selected");
    listBox.selectItemPos((short) 1, true);
    assertEquals("B", contentListBox.getAnchor().getString(), "wrong element selected");

    XCheckBox checkBox = UNO.XCheckBox(tabControlContainer.getControl("check"));
    checkBox.setState((short) 1);
    ItemEvent event = new ItemEvent(checkBox, 1, 1, 1);
    UnoRuntime.queryInterface(XItemListener.class, checkBox).itemStateChanged(event);
    XDrawPage page = UNO.XDrawPageSupplier(xDoc).getDrawPage();
    XControlShape shape = UNO.XControlShape(page.getByIndex(0));
    XControlModel checkBoxModel = shape.getControl();
    assertEquals((short) 1, UnoProperty.getProperty(checkBoxModel, UnoProperty.STATE), "check box should be selected");
  }

}
