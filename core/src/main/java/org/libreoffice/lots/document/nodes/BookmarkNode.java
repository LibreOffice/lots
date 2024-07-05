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
package org.libreoffice.lots.document.nodes;

import org.libreoffice.ext.unohelper.document.text.Bookmark;

public class BookmarkNode implements Node
{
  private boolean isStart;

  private Bookmark bookmark;

  public BookmarkNode(Bookmark bookmark, boolean isStart)
  {
    super();
    this.bookmark = bookmark;
    this.isStart = isStart;
  }

  public String getName()
  {
    return bookmark.getName();
  }

  public boolean isStart()
  {
    return isStart;
  }

  @Override
  public String toString()
  {
    return "Bookmark '" + bookmark.getName() + (isStart ? "' Start" : "' End");
  }
}
