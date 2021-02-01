/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.document.commands.DocumentCommand.InvalidCommandException;
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
