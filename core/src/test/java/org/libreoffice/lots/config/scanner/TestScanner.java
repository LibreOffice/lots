/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.config.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.MalformedURLException;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.config.scanner.Scanner;
import org.libreoffice.lots.config.scanner.ScannerException;
import org.libreoffice.lots.config.scanner.Token;
import org.libreoffice.lots.config.scanner.TokenType;

/**
 * A Test class to verify that the scanner works correctly with includes.
 *
 * @author Daniel Sikeler
 */
public class TestScanner
{

  /**
   * Scan a file and test whether the correct tokens occur.
   *
   * @throws ScannerException
   *           Problems with scanner.
   * @throws MalformedURLException
   *           Couldn't read the configuration.
   */
  @Test
  public void scanWithoutInclude() throws ScannerException, MalformedURLException
  {
    Token[] tokens = { new Token(getClass().getResource("scannerTest.conf").getFile(), TokenType.NEW_FILE),
        new Token("A", TokenType.KEY), new Token("'X\"\"Y'", TokenType.VALUE), new Token("B", TokenType.KEY),
        new Token("'X\"Y'", TokenType.VALUE), new Token("C", TokenType.KEY), new Token("\"X''Y\"", TokenType.VALUE),
        new Token("D", TokenType.KEY), new Token("\"X'Y\"", TokenType.VALUE), new Token("GUI", TokenType.KEY),
        new Token("(", TokenType.OPENING_BRACKET), new Token("Dialoge", TokenType.KEY),
        new Token("(", TokenType.OPENING_BRACKET), new Token("Dialog1", TokenType.KEY),
        new Token("(", TokenType.OPENING_BRACKET), new Token("(", TokenType.OPENING_BRACKET),
        new Token("TYPE", TokenType.KEY), new Token("\"textbox\"", TokenType.VALUE), new Token("LABEL", TokenType.KEY),
        new Token("\"Name\"", TokenType.VALUE), new Token(")", TokenType.CLOSING_BRACKET),
        new Token(")", TokenType.CLOSING_BRACKET), new Token(")", TokenType.CLOSING_BRACKET),
        new Token(")", TokenType.CLOSING_BRACKET), new Token("Anredevarianten", TokenType.KEY),
        new Token("(", TokenType.OPENING_BRACKET), new Token("\"Herr\"", TokenType.VALUE),
        new Token("\"Frau\"", TokenType.VALUE), new Token("\"Pinguin\"", TokenType.VALUE),
        new Token(")", TokenType.CLOSING_BRACKET), new Token("(", TokenType.OPENING_BRACKET),
        new Token("\"Dies\"", TokenType.VALUE), new Token("\"ist\"", TokenType.VALUE),
        new Token("\"eine\"", TokenType.VALUE), new Token("\"unbenannte\"", TokenType.VALUE),
        new Token("\"Liste\"", TokenType.VALUE), new Token(")", TokenType.CLOSING_BRACKET),
        new Token("NAME", TokenType.KEY), new Token("\"WollMux%%%n\"", TokenType.VALUE),
        new Token("# FARBSCHEMA \"Ekelig\"", TokenType.COMMENT), new Token("", TokenType.END_FILE), };
    final Scanner scanner = new Scanner(getClass().getResource("scannerTest.conf"));
    int index = 0;
    while (scanner.hasNext())
    {
      final Token token = scanner.next();
      assertFalse(index >= tokens.length, "Tokenstream to long " + token);
      assertEquals(tokens[index++], token, "Token " + index + " is wrong");
    }
    assertFalse(index < tokens.length, "Tokenstream to short");
    scanner.close();
  }

  /**
   * Test if the scanner works properly.
   *
   * @throws ScannerException
   *           Scanner problems.
   * @throws MalformedURLException
   *           Couldn't find the file with the configuration.
   */
  @Test
  public void scanWithInclude() throws ScannerException, MalformedURLException
  {
    Token[] tokens = { new Token(getClass().getResource("includeTest.conf").getFile(), TokenType.NEW_FILE),
        new Token("file:inc/includeTest2.conf", TokenType.NEW_FILE), new Token("# includeTest2", TokenType.COMMENT),
        new Token("", TokenType.END_FILE), new Token("file:../scanner/inc/includeTest2.conf", TokenType.NEW_FILE),
        new Token("# includeTest2", TokenType.COMMENT), new Token("", TokenType.END_FILE),
        new Token("../scanner/inc/includeTest2.conf", TokenType.NEW_FILE),
        new Token("# includeTest2", TokenType.COMMENT), new Token("", TokenType.END_FILE),
        new Token("inc/includeTest3.conf", TokenType.NEW_FILE), new Token("includeTest2.conf", TokenType.NEW_FILE),
        new Token("# includeTest2", TokenType.COMMENT), new Token("", TokenType.END_FILE),
        new Token("", TokenType.END_FILE), new Token("", TokenType.END_FILE), };
    final Scanner scanner = new Scanner(getClass().getResource("includeTest.conf"));
    int index = 0;
    while (scanner.hasNext())
    {
      final Token token = scanner.next();
      assertFalse(index >= tokens.length, "Tokenstream to long " + token);
      assertEquals(tokens[index++], token, "Token " + index + " is wrong");
    }
    assertFalse(index < tokens.length, "Tokenstream to short");
    scanner.close();
  }

}
