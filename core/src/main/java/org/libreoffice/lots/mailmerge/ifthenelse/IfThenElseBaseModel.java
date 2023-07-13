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
package org.libreoffice.lots.mailmerge.ifthenelse;

import java.util.UUID;

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.NodeNotFoundException;

/**
 * Base model for "IF" function nodes.
 */
public abstract class IfThenElseBaseModel
{
  protected static final String VALUE_FUNCTION = "VALUE";
  private final String id;
  private String value;
  private IfThenElseBaseModel parent;

  /**
   * Initialize the basic fields.
   */
  protected IfThenElseBaseModel()
  {
    id = UUID.randomUUID().toString();
    value = "";
    parent = null;
  }

  public IfThenElseBaseModel getParent()
  {
    return parent;
  }

  public void setParent(IfThenElseBaseModel parent)
  {
    this.parent = parent;
  }

  public String getId()
  {
    return id;
  }

  public String getValue()
  {
    return value;
  }

  public void setValue(String value)
  {
    this.value = value;
  }

  /**
   * Create a ConfigThingy from this model. This basic implementation creates a "CAT" function from
   * the {@link #getValue()} field.
   *
   * @return The configuration.
   */
  public ConfigThingy create()
  {
    ConfigThingy catConf = new ConfigThingy("CAT");
    int index = 0;
    do
    {
      int startIndex = value.indexOf("{{", index);
      int endIndex = value.indexOf("}}", index);
      if (startIndex > -1 && endIndex > -1)
      {
        String prefix = value.substring(index, startIndex);
        if (!prefix.isEmpty())
        {
          catConf.add(prefix);
        }
        ConfigThingy valueConf = new ConfigThingy(VALUE_FUNCTION);
        valueConf.add(value.substring(startIndex + 2, endIndex));
        catConf.addChild(valueConf);
        index = endIndex + 2;
      } else
      {
        catConf.add(value.substring(index));
        index = value.length();
      }
    } while (index < value.length());

    return catConf;
  }

  /**
   * Get the node with the given ID.
   *
   * @param id
   *          The ID.
   * @return The node or null if there's no such node.
   */
  public IfThenElseBaseModel getById(String id)
  {
    if (this.id.equals(id))
    {
      return this;
    }
    return null;
  }

  /**
   * Parse a "CAT" function and return the string. Fields of "VALUE" functions are surrounded by
   * "{{" and "{{".
   *
   * @param innerConf
   *          The configuration of the "CAT" function.
   * @return The string.
   * @throws NodeNotFoundException
   *           Invalid VALUE function.
   */
  protected String parseCatFunction(ConfigThingy innerConf) throws NodeNotFoundException
  {
    StringBuilder valueStr = new StringBuilder();
    for (ConfigThingy entry : innerConf)
    {
      if (entry.getName().equals(VALUE_FUNCTION))
      {
        valueStr.append("{{" + entry.getFirstChild().toString() + "}}");
      } else
      {
        valueStr.append(entry.toString());
      }
    }
    return valueStr.toString();
  }
}
