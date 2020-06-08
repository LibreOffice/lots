package de.muenchen.allg.itd51.wollmux.slv.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import com.sun.star.lang.XComponent;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextViewCursor;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoDictionary;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InvalidCommandException;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.slv.ContentBasedDirectiveConfig;
import de.muenchen.allg.itd51.wollmux.slv.PrintBlockSignature;
import de.muenchen.allg.itd51.wollmux.test.WollMuxTest;
import de.muenchen.allg.util.UnoProperty;

public class MarkBlockDispatchTest extends WollMuxTest
{

  @Test
  public void testMarkBlock()
      throws InvalidCommandException, UnoHelperException, InterruptedException, ExecutionException, TimeoutException,
      IOException, SyntaxErrorException
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
