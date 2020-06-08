package de.muenchen.allg.itd51.wollmux.slv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.TextRangeRelation;
import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand.InvalidCommandException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.test.OfficeTest;
import de.muenchen.allg.util.UnoProperty;

public class PrintBlockCommandTest extends OfficeTest
{
  @Test
  public void testPrintBlockCommand()
      throws UnoHelperException, InvalidCommandException, IOException, SyntaxErrorException
  {
    XTextDocument doc = UNO.XTextDocument(loadComponent("private:factory/swriter", false, true));
    XTextCursor cursor = doc.getText().createTextCursor();
    cursor.setString("ABC");
    String conf = "WM(CMD 'allVersions' HIGHLIGHT_COLOR 'ffffc8')";
    Bookmark bm = new Bookmark(conf, doc, cursor);
    PrintBlockCommand cmd = new PrintBlockCommand(new ConfigThingy("", null, new StringReader(conf)), bm);
    assertEquals(TextRangeRelation.A_MATCH_B, TextRangeRelation.compareTextRanges(cursor, cmd.getAnchor()),
        "wrong cursor");
    assertEquals("ffffc8", cmd.getHighlightColor(), "wrong color");
    assertEquals(PrintBlockSignature.ALL_VERSIONS, cmd.getName());

    assertEquals(0, cmd.execute(new PrintBlockProcessor()), "Command executed with errors");
    assertEquals(Integer.parseInt(cmd.getHighlightColor(), 16),
        UnoProperty.getProperty(cursor, UnoProperty.CHAR_BACK_COLOR), "Wrong background not set");
    assertTrue(cmd.isDone(), "Command should be done");
    assertFalse(cmd.hasError(), "Command shouldn't have errors");

    cmd.showHighlightColor(false);
    assertEquals(-1, UnoProperty.getProperty(cursor, UnoProperty.CHAR_BACK_COLOR), "Wrong background not reset");

    assertThrows(InvalidCommandException.class,
        () -> new PrintBlockCommand(
            new ConfigThingy("", null, new StringReader("WM(CMD 'foo' HIGHLIGHT_COLOR 'ffffc8')")), bm));
  }
}
