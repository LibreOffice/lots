/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package de.muenchen.allg.itd51.wollmux.slv.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.sun.star.lang.XComponent;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextViewCursor;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoDictionary;

import de.muenchen.allg.itd51.wollmux.test.WollMuxTest;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.slv.ContentBasedDirectiveConfig;
import org.libreoffice.lots.slv.PrintBlockSignature;
import org.libreoffice.lots.slv.dispatch.MarkBlockDispatch;

public class MarkBlockDispatchTest extends WollMuxTest
{

  @Test
  public void testMarkBlock() throws Exception
  {
    XComponent comp = loadAsyncComponent("private:factory/swriter", false, true).get(5, TimeUnit.SECONDS);
    wollmux.getWollMuxDocument(UNO.XComponent(comp));
    XTextDocument xDoc = UNO.XTextDocument(comp);
    XTextViewCursor viewCursor = UNO.XTextViewCursorSupplier(xDoc.getCurrentController()).getViewCursor();
    XTextCursor cursor = xDoc.getText().createTextCursor();
    cursor.setString("ABC");
    cursor.gotoStart(true);
    viewCursor.gotoRange(cursor, true);

    UNO.dispatch(xDoc, MarkBlockDispatch.COMMAND + "#allVersions");
    XBookmarksSupplier supplier = UNO.XBookmarksSupplier(xDoc);
    UnoDictionary<XTextContent> bookmarks = UnoDictionary.create(supplier.getBookmarks(), XTextContent.class);
    assertEquals(1, bookmarks.size(), "Print block wasn't created");
    assertEquals(Integer.parseInt(ContentBasedDirectiveConfig.getHighlightColor(PrintBlockSignature.ALL_VERSIONS),
        16), UnoProperty.getProperty(cursor, UnoProperty.CHAR_BACK_COLOR), "Wrong background color");

    viewCursor.gotoRange(cursor, true);
    UNO.dispatch(xDoc, MarkBlockDispatch.COMMAND + "#allVersions");
    bookmarks = UnoDictionary.create(supplier.getBookmarks(), XTextContent.class);
    assertEquals(0, bookmarks.size(), "Print block wasn't removed");
    assertEquals(-1, UnoProperty.getProperty(cursor, UnoProperty.CHAR_BACK_COLOR), "Wrong background color");
  }
}
