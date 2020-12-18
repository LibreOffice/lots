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
package de.muenchen.allg.itd51.wollmux.dialog.trafo;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;

/**
 * The model of an "ELSE" node in an "IF" function.
 */
public class ElseModel extends IfThenElseBaseModel
{

  public ElseModel()
  {
  }

  /**
   * Create the model from a configuration.
   *
   * @param conf
   *          The configuration.
   */
  public ElseModel(ConfigThingy conf)
  {
    if (conf.count() == 1)
    {
      try
      {
        ConfigThingy innerConf = conf.getFirstChild();
        if (innerConf.count() == 0)
        {
          setValue("");
        } else if (innerConf.getName().equals("CAT"))
        {
          setValue(parseCatFunction(innerConf));
        }
      } catch (NodeNotFoundException e)
      {
        // Can't happen as we tested count before
      }
    }
  }

  @Override
  public String toString()
  {
    return "SONST " + getValue();
  }
}
