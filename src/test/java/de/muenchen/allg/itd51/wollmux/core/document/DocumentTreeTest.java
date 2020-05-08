package de.muenchen.allg.itd51.wollmux.core.document;


import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
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
