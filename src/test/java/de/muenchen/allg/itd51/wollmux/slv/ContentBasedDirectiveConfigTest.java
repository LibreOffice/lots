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
package de.muenchen.allg.itd51.wollmux.slv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;

public class ContentBasedDirectiveConfigTest
{

  @Test
  public void testConfig() throws IOException, SyntaxErrorException
  {
    ContentBasedDirectiveConfig.configure(new ConfigThingy("", null,
        new StringReader("SachleitendeVerfuegungen(NUMBERS \"roman\" ABDRUCK_NAME \"Abdruck\""
            + " ALL_VERSIONS_HIGHLIGHT_COLOR \"ffffc8\" NOT_IN_ORIGINAL_HIGHLIGHT_COLOR \"ffc8ff\""
            + " ORIGINAL_ONLY_HIGHLIGHT_COLOR \"b8b8ff\" DRAFT_ONLY_HIGHLIGHT_COLOR \"c8ffff\""
            + " COPY_ONLY_HIGHLIGHT_COLOR \"b8ffb8\")")));

    assertEquals("Abdruck", ContentBasedDirectiveConfig.getName(), "Wrong name for copies");

    assertEquals("ffffc8", ContentBasedDirectiveConfig.getHighlightColor(PrintBlockSignature.ALL_VERSIONS),
        "wrong color");
    assertEquals("ffc8ff", ContentBasedDirectiveConfig.getHighlightColor(PrintBlockSignature.NOT_IN_ORIGINAL),
        "wrong color");
    assertEquals("b8b8ff", ContentBasedDirectiveConfig.getHighlightColor(PrintBlockSignature.ORIGINAL_ONLY),
        "wrong color");
    assertEquals("c8ffff", ContentBasedDirectiveConfig.getHighlightColor(PrintBlockSignature.DRAFT_ONLY),
        "wrong color");
    assertEquals("b8ffb8", ContentBasedDirectiveConfig.getHighlightColor(PrintBlockSignature.COPY_ONLY), "wrong color");

    assertEquals("I.", ContentBasedDirectiveConfig.getNumber(1));
  }

  @Test
  public void testConfig2() throws IOException, SyntaxErrorException
  {
    ContentBasedDirectiveConfig.configure(new ConfigThingy("", null, new StringReader(
        "SachleitendeVerfuegungen(NUMBERS \"arabic\" ABDRUCK_NAME \"Copy\")")));

    assertEquals("Copy", ContentBasedDirectiveConfig.getName(), "Wrong name for copies");

    assertNull(ContentBasedDirectiveConfig.getHighlightColor(PrintBlockSignature.ALL_VERSIONS), "wrong color");
    assertNull(ContentBasedDirectiveConfig.getHighlightColor(PrintBlockSignature.NOT_IN_ORIGINAL), "wrong color");
    assertNull(ContentBasedDirectiveConfig.getHighlightColor(PrintBlockSignature.ORIGINAL_ONLY), "wrong color");
    assertNull(ContentBasedDirectiveConfig.getHighlightColor(PrintBlockSignature.DRAFT_ONLY), "wrong color");
    assertNull(ContentBasedDirectiveConfig.getHighlightColor(PrintBlockSignature.COPY_ONLY), "wrong color");

    assertEquals("1.", ContentBasedDirectiveConfig.getNumber(1));
  }
}
