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
package org.libreoffice.lots.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sun.star.awt.FontDescriptor;

import org.libreoffice.ext.unohelper.ui.HTMLElement;

public class HTMLElementTest
{

  @Test
  public void parseNonHtml()
  {
    HTMLElement element = new HTMLElement("test");
    assertEquals("test", element.getText());
  }

  @Test
  public void parseWithSpecialCharaters()
  {
    HTMLElement element = new HTMLElement("<&>\"\'");
    assertEquals("<&>\"\'", element.getText());
  }

  @Test
  public void parseSimpleHtml()
  {
    HTMLElement element = new HTMLElement("<html>test");
    assertEquals("test", element.getText());
    element = new HTMLElement("<html>test</html>");
    assertEquals("test", element.getText());
  }

  @Test
  public void parseEmbeddedHtml()
  {
    HTMLElement element = new HTMLElement("before <html>test");
    assertEquals("before test", element.getText());
    element = new HTMLElement("<html>test</html> after");
    assertEquals("test after", element.getText());
    element = new HTMLElement("before <html>test</html> middle <html>test2</html> after");
    assertEquals("before test middle test2 after", element.getText());
  }

  @Test
  public void parseWithBr()
  {
    HTMLElement element = new HTMLElement("<html>t<br>e<br>s<br>t</html>");
    assertEquals("t\ne\ns\nt", element.getText());
  }

  @Test
  public void parseWithP()
  {
    HTMLElement element = new HTMLElement("<p>p1</p><p>p2</p>");
    assertEquals("p1\np2", element.getText());
  }

  @Test
  public void parseWithA()
  {
    HTMLElement element = new HTMLElement("<html>test <a href=\"file://test.txt\">link</a> test</html>");
    assertEquals("test link test", element.getText());
    assertEquals("file://test.txt", element.getHref());
    element = new HTMLElement("<html>test <a href=\"http://www.google.com\">link</a> test</html>");
    assertEquals("http://www.google.com", element.getHref());
    element = new HTMLElement("<html>test <a href=\"www.google.com\">link</a> test</html>");
    assertEquals("http://www.google.com", element.getHref());
  }

  @Test
  public void parseWithFontColor()
  {
    HTMLElement element = new HTMLElement(
        "<html><font color =#000099>Örtlicher Ausbildungsplan für den <br>Studiengang</font></html>");
    assertEquals(153, element.getRGBColor());
    assertEquals("Örtlicher Ausbildungsplan für den \nStudiengang", element.getText());
    element = new HTMLElement(
        "<html><p style=\"color:navy\">Örtlicher Ausbildungsplan für den <br>Studiengang</html>");
    assertEquals(128, element.getRGBColor());
    element = new HTMLElement("<html>Örtlicher Ausbildungsplan für den <br>Studiengang</html>");
    assertEquals(0, element.getRGBColor());
    element = new HTMLElement(
        "<html><p style=\\\"font-size: 24pt;\\\">Örtlicher Ausbildungsplan für den <br>Studiengang</p></html>");
    assertEquals(0, element.getRGBColor());
  }

  @Test
  public void parseWithFontStyle()
  {
    HTMLElement element = new HTMLElement(
        "<html><font style=\"font-size: 24pt;\">Örtlicher Ausbildungsplan für den <br>Studiengang</font></html>");
    FontDescriptor fontDesc = element.getFontDescriptor();
    assertEquals(24 * 2/3, fontDesc.Height);
    assertEquals("Örtlicher Ausbildungsplan für den \nStudiengang", element.getText());

    element = new HTMLElement(
        "<html><p style=\"font-size: 24pt;\">Örtlicher Ausbildungsplan für den <br>Studiengang</font></html>");
    fontDesc = element.getFontDescriptor();
    assertEquals(24 * 2/3, fontDesc.Height);
  }
}
