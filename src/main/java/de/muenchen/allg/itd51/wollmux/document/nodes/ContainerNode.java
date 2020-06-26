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
package de.muenchen.allg.itd51.wollmux.document.nodes;

import java.util.Collection;
import java.util.Iterator;

import de.muenchen.allg.itd51.wollmux.document.DocumentTreeVisitor;

/**
 * Oberklasse für Knoten, die Nachfahren haben können (z,B, Absätze).
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ContainerNode implements Container, Node
{
  private Collection<Node> children;

  public ContainerNode(Collection<Node> children)
  {
    super();
    this.children = children;
  }

  @Override
  public Iterator<Node> iterator()
  {
    return children.iterator();
  }

  @Override
  public String toString()
  {
    return "CONTAINER";
  }

  @Override
  public Container.Type getType()
  {
    return Container.Type.CONTAINER;
  }

  @Override
  public boolean visit(DocumentTreeVisitor visit)
  {
    if (!visit.container(this, 0)) {
      return false;
    }

    Iterator<Node> iter = iterator();
    while (iter.hasNext())
    {
      if (!iter.next().visit(visit)) {
        return false;
      }
    }
    if (!visit.container(this, 1)) {
      return false;
    }
    return true;
  }
}