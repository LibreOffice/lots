package de.muenchen.allg.itd51.wollmux.dialog;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoinerFactory;

public class DialogFactory
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DialogFactory.class);

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
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
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
          LOGGER.error(L.m(
            "Funktionsdialog \"%1\" im selben Funktionsdialoge-Abschnitt mehrmals definiert",
            name));
        dialogsInBlock.add(name);
        try
        {
          funcDialogs.add(name,
            DatasourceSearchDialog.create(dialogConf, DatasourceJoinerFactory.getDatasourceJoiner()));
        }
        catch (ConfigurationErrorException e)
        {
          LOGGER.error(L.m("Fehler in Funktionsdialog %1", name), e);
        }
      }
    }

    return funcDialogs;
  }

}
