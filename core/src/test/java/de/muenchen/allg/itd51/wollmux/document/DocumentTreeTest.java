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
package de.muenchen.allg.itd51.wollmux.document;


import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.text.XTextDocument;

import org.libreoffice.ext.unohelper.common.UNO;
import de.muenchen.allg.itd51.wollmux.test.OfficeTest;

public class DocumentTreeTest extends OfficeTest
{
  private XTextDocument xDoc;

  @BeforeEach
  public void setUp() throws Exception
  {
    URL file = getClass().getResource("ExternerBriefkopf.odt");
    xDoc = UNO.XTextDocument(loadComponent(file.toString(), false, true));
  }

  @AfterEach
  public void tearDown() throws Exception
  {
    UNO.XCloseable(xDoc).close(false);
  }

  /**
   * Parsen dauert ca. 1/10 s pro Seite
   *
   * @throws Exception
   */
  @Test
  public void parseOneDocumenttest() throws Exception
  {
    long start = System.currentTimeMillis();
    DocumentTree tree = new DocumentTree(xDoc);
    long time = System.currentTimeMillis() - start;
    assertTrue(time < 1500, "Parsing document took to much time: " + time);
    // TODO: make test instead of console output
    // System.out.println(DocumentTree.treeDump(tree.getRoot(), ""));
  }

}
