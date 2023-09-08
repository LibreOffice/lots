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
package org.libreoffice.lots.dialog;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.db.Datasources;
import org.libreoffice.lots.util.L;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogFactory
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DialogFactory.class);

  private DialogFactory()
  {
    // hide public constructor
  }

  /**
   * Paste the "Function Dialogs" sections from conf and return a DialogLibrary as a result.
   *
   * @param baselib
   *          if non-null this is linked as a fallback to open dialogs
   *          provide information that cannot be found elsewhere.
   * @param context
   *          the context in which function definitions contained in dialogs
   *          should be evaluated (especially DIALOG functions). DANGER!
   *          This is where values ​​are stored, it's not just a key.
   */
  public static DialogLibrary parseFunctionDialogs(ConfigThingy conf,
      DialogLibrary baselib, Map<Object, Object> context)
  {
    DialogLibrary funcDialogs = new DialogLibrary(baselib);

    Set<String> dialogsInBlock = new HashSet<>();

    conf = conf.query("FunctionDialogs");
    Iterator<ConfigThingy> parentIter = conf.iterator();
    while (parentIter.hasNext())
    {
      dialogsInBlock.clear();
      Iterator<ConfigThingy> iter = parentIter.next().iterator();
      while (iter.hasNext())
      {
        ConfigThingy dialogConf = iter.next();
        String name = dialogConf.getName();
        if (dialogsInBlock.contains(name))
          LOGGER.error("Function dialog '{}' was defined more than once "
              + "in the same 'FunctionDialogs' section", name);
        dialogsInBlock.add(name);
        try
        {
          funcDialogs.add(name, new DatasourceSearchDialog(dialogConf, Datasources.getDatasources()));
        }
        catch (ConfigurationErrorException e)
        {
          LOGGER.error("Error in Function dialog {}", name, e);
        }
      }
    }

    return funcDialogs;
  }

}
