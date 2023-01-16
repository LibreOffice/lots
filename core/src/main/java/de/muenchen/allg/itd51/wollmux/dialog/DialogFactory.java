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
package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.db.Datasources;
import de.muenchen.allg.itd51.wollmux.util.L;

public class DialogFactory
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DialogFactory.class);

  private DialogFactory()
  {
    // hide public constructor
  }

  /**
   * Parst die "Funktionsdialoge" Abschnitte aus conf und liefert als Ergebnis eine
   * DialogLibrary zurück.
   *
   * @param baselib
   *          falls nicht-null wird diese als Fallback verlinkt, um Dialoge zu
   *          liefern, die anderweitig nicht gefunden werden.
   * @param context
   *          der Kontext in dem in Dialogen enthaltene Funktionsdefinitionen
   *          ausgewertet werden sollen (insbesondere DIALOG-Funktionen). ACHTUNG!
   *          Hier werden Werte gespeichert, es ist nicht nur ein Schlüssel.
   */
  public static DialogLibrary parseFunctionDialogs(ConfigThingy conf,
      DialogLibrary baselib, Map<Object, Object> context)
  {
    DialogLibrary funcDialogs = new DialogLibrary(baselib);

    Set<String> dialogsInBlock = new HashSet<>();

    conf = conf.query("Funktionsdialoge");
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
              + "in the same 'Funktionsdialoge' section", name);
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
