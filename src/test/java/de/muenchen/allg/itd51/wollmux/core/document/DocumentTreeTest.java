package de.muenchen.allg.itd51.wollmux.core.document;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.OfficeTest;

public class DocumentTreeTest extends OfficeTest
{
  private XTextDocument xDoc;

  @Before
  public void setUp() throws Exception
  {
    URL file = getClass().getResource("ExternerBriefkopf.odt");
    UNO.loadComponentFromURL(file.toString(), false, false);
    xDoc = UNO.XTextDocument(UNO.desktop.getCurrentComponent());
  }

  @After
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
    assertTrue("Parsing document took to much time: " + time, time < 1500);
    // TODO: make test instead of console output
    // System.out.println(DocumentTree.treeDump(tree.getRoot(), ""));
  }

}
