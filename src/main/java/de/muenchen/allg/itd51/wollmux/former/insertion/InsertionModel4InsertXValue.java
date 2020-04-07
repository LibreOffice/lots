/*
 * Dateiname: InsertionModel.java
 * Projekt  : WollMux
 * Funktion : Stellt eine Einfügestelle im Dokument (insertValue oder insertFormValue) dar.
 *
 * Copyright (c) 2008-2019 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 06.09.2006 | BNK | Erstellung
 * 08.02.2007 | BNK | Beim Generieren von Trafo-Funktionsnamen Timestamp in den Namen codieren.
 * 16.03.2007 | BNK | +getFormularMax4000()
 *                  | +MyTrafoAccess.updateFieldReferences()
 * 29.06.2007 | BNK | [R7343]AUTOSEP unterstützt
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 *
 */
package de.muenchen.allg.itd51.wollmux.former.insertion;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.itd51.wollmux.UnknownIDException;
import de.muenchen.allg.itd51.wollmux.core.document.Bookmark;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.IDManager.ID;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionProvider;

/**
 * Stellt eine Einfügestelle im Dokument (insertValue oder insertFormValue) dar.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class InsertionModel4InsertXValue extends InsertionModel
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(InsertionModel4InsertXValue.class);

  /**
   * Siehe {@link #autosep}.
   */
  private static final int AUTOSEP_BOTH = 1;

  /**
   * Siehe {@link #autosep}.
   */
  private static final int AUTOSEP_LEFT = 2;

  /**
   * Siehe {@link #autosep}.
   */
  private static final int AUTOSEP_RIGHT = 3;

  /**
   * Attribut-ID-Konstante für
   * {@link InsertionModel.ModelChangeListener#attributeChanged(InsertionModel, int, Object)}.
   */
  public static final int ID_ATTR = 0;

  /**
   * Konstante für {@link #sourceType}, die angibt, dass die Daten für die Einfügung
   * aus einer externen Datenquelle kommen (insertValue).
   */
  private static final int DATABASE_TYPE = 0;

  /**
   * Konstante für {@link #sourceType}, die angibt, dass die Daten für die Einfügung
   * aus dem Formular kommen (insertFormValue).
   */
  private static final int FORM_TYPE = 1;

  /**
   * Gibt an, um woher die Einfügung ihre Daten bezieht.
   *
   * @see #FORM_TYPE
   * @see #DATABASE_TYPE
   */
  private int sourceType = FORM_TYPE;

  /**
   * DB_SPALTE oder ID je nach {@link #sourceType}.
   */
  private IDManager.ID dataId;

  /**
   * Liste von {@link InsertionModel.AutosepInfo} Objekten.
   */
  private List<AutosepInfo> autosep = new Vector<>();

  /**
   * Das Bookmark, das diese Einfügestelle umschließt.
   */
  private Bookmark bookmark;

  private IDManager.IDChangeListener myIDChangeListener = new MyIDChangeListener();

  /**
   * Erzeugt ein neues InsertionModel für das Bookmark mit Namen bookmarkName, das
   * bereits im Dokument vorhanden sein muss.
   *
   * @param doc
   *          das Dokument in dem sich das Bookmark befindet
   * @param funcSelections
   *          ein FunctionSelectionProvider, der für das TRAFO Attribut eine passende
   *          FunctionSelection liefern kann.
   * @param formularMax4000
   *          Der FormularMax4000 zu dem dieses InsertionModel gehört.
   * @throws SyntaxErrorException
   *           wenn bookmarkName nicht korrekte ConfigThingy-Syntax hat oder kein
   *           korrektes Einfügekommando ist.
   * @throws NoSuchElementException
   *           wenn ein Bookmark dieses Namens in doc nicht existiert.
   */
  public InsertionModel4InsertXValue(String bookmarkName, XBookmarksSupplier doc,
      FunctionSelectionProvider funcSelections, FormularMax4kController formularMax4000)
      throws SyntaxErrorException, NoSuchElementException
  {
    this.formularMax4000 = formularMax4000;
    bookmark = new Bookmark(bookmarkName, doc);
    // eventuell vorhandene Ziffern am Ende löschen
    String confStr = bookmarkName.replaceAll("\\d*\\z", "");
    URL url = null;
    try
    {
      url = new URL("file:///");
    }
    catch (MalformedURLException x)
    {
      LOGGER.trace("", x);
    }

    ConfigThingy conf;
    try
    {
      conf = new ConfigThingy("INSERT", url, new StringReader(confStr));
    }
    catch (IOException x)
    {
      throw new SyntaxErrorException(x);
    }

    String cmd = conf.query("CMD").toString();
    if (cmd.equals("insertValue"))
    {
      ConfigThingy dbSpalteConf = conf.query("DB_SPALTE");
      if (dbSpalteConf.count() == 0) throw new SyntaxErrorException();
      dataId =
        formularMax4000.getIDManager().getID(FormularMax4kController.NAMESPACE_DB_SPALTE,
          dbSpalteConf.toString());
      dataId.addIDChangeListener(myIDChangeListener);
      sourceType = DATABASE_TYPE;
    }
    else if (cmd.equals("insertFormValue"))
    {
      ConfigThingy idConf = conf.query("ID");
      if (idConf.count() == 0) throw new SyntaxErrorException();
      dataId =
        formularMax4000.getIDManager().getID(
          FormularMax4kController.NAMESPACE_FORMCONTROLMODEL, idConf.toString());
      dataId.addIDChangeListener(myIDChangeListener);
      sourceType = FORM_TYPE;
    }
    else
      throw new SyntaxErrorException();

    ConfigThingy trafoConf = conf.query("TRAFO");
    if (trafoConf.count() == 0)
      this.trafo = new FunctionSelection();
    else
    {
      String functionName = trafoConf.toString();
      this.trafo = funcSelections.getFunctionSelection(functionName);
    }

    // INSERT(WM(<zu iterierender Teil>))
    Iterator<ConfigThingy> iter = (conf.iterator().next()).iterator();
    AutosepInfo autosepInfo = null;
    while (iter.hasNext())
    {
      ConfigThingy subConf = iter.next();
      String name = subConf.getName();
      String value = subConf.toString();

      if (name.equals("AUTOSEP"))
      {
        if (autosepInfo != null) autosep.add(autosepInfo);

        autosepInfo = new AutosepInfo();

        if (value.equalsIgnoreCase("both"))
          autosepInfo.autosep = AUTOSEP_BOTH;
        else if (value.equalsIgnoreCase("left"))
          autosepInfo.autosep = AUTOSEP_LEFT;
        else if (value.equalsIgnoreCase("right"))
          autosepInfo.autosep = AUTOSEP_RIGHT;

      }
      else if (name.equals("SEPARATOR"))
      {
        if (autosepInfo != null)
        {
          autosepInfo.separator = value;
          autosep.add(autosepInfo);
        }

        autosepInfo = null;
      }
    }
    if (autosepInfo != null) autosep.add(autosepInfo);

  }

  @Override
  public boolean updateDocument(
      Map<String, ConfigThingy> mapFunctionNameToConfigThingy)
  {
    ConfigThingy conf = new ConfigThingy("WM");
    String cmd = "insertValue";
    String idType = "DB_SPALTE";
    if (sourceType == FORM_TYPE)
    {
      cmd = "insertFormValue";
      idType = "ID";
    }

    conf.add("CMD").add(cmd);
    conf.add(idType).add(getDataID().toString());

    if (!trafo.isNone())
    {
      // Falls eine externe Funktion referenziert wird, ohne dass irgendwelche
      // ihrer Parameter gebunden wurden, dann nehmen wir direkt den
      // Original-Funktionsnamen für das TRAFO-Attribut ...
      if (trafo.isReference() && !trafo.hasSpecifiedParameters())
      {
        conf.add("TRAFO").add(trafo.getFunctionName());
      }
      else
      // ... ansonsten müssen wir eine neue Funktion machen.
      {
        int count = 1;
        String funcName;
        do
        {
          funcName =
            FM4000AUTO_GENERATED_TRAFO + (count++) + "_"
              + System.currentTimeMillis();
        } while (mapFunctionNameToConfigThingy.containsKey(funcName));

        conf.add("TRAFO").add(funcName);
        mapFunctionNameToConfigThingy.put(funcName, trafo.export(funcName));
      }
    }

    Iterator<AutosepInfo> iter = autosep.iterator();
    while (iter.hasNext())
    {
      AutosepInfo autosepInfo = iter.next();
      String autosepStr = "both";
      if (autosepInfo.autosep == AUTOSEP_LEFT)
        autosepStr = "left";
      else if (autosepInfo.autosep == AUTOSEP_RIGHT) autosepStr = "right";

      conf.add("AUTOSEP").add(autosepStr);
      conf.add("SEPARATOR").add(autosepInfo.separator);
    }

    String newBookmarkName = conf.stringRepresentation(false, '\'', true);
    return bookmark.rename(newBookmarkName) != Bookmark.BROKEN;
  }

  /**
   * Liefert je nach Typ der Einfügung das DB_SPALTE oder ID Attribut.
   */
  public IDManager.ID getDataID()
  {
    return dataId;
  }

  /**
   * Ändert je nach Type der Einfügung DB_SPALTE oder ID Attribut auf den Wert newId (falls newId
   * gleich der alten ID ist, wird nichts getan). ACHTUNG! Die Änderung betrifft nur die Einfügung
   * und wird nicht auf die Formularelemente übertragen (wogegen umgekehrt Änderungen an den
   * Formularelemente-IDs zu Änderungen der Einfügungen führen). Hintergrund dieser Implementierung
   * ist, dass man einerseits normalerweise nicht in der Einfügungen-Sicht IDs von Steuerelementen
   * ändern möchte und andererseits nur so ermöglicht wird, die Quelle einer Einfügung auf ein
   * anderes Steuerelement zu ändern.
   *
   * @throws UnknownIDException
   *           falls diese Einfügung eine Formularwert-Einfügung ist (d.h. das ID-Attribut betroffen
   *           wäre) und newID dem IDManager im Namensraum
   *           {@link FormularMax4kController#NAMESPACE_FORMCONTROLMODEL} unbekannt ist, oder falls
   *           newId der leere String ist. Im Falle des DB_SPALTE Attributs wird nur geworfen, wenn
   *           newId der leere String ist.
   */
  public void setDataID(String newId) throws UnknownIDException
  {
    if (newId.equals(dataId.toString())) return;
    if (newId.length() == 0) throw new UnknownIDException(L.m("Leere ID"));
    if (sourceType == FORM_TYPE)
    {
      IDManager.ID newDataId =
        formularMax4000.getIDManager().getExistingID(
          FormularMax4kController.NAMESPACE_FORMCONTROLMODEL, newId);
      if (newDataId == null) throw new UnknownIDException(newId);
      dataId.removeIDChangeListener(myIDChangeListener);
      dataId = newDataId;
      dataId.addIDChangeListener(myIDChangeListener);
    }
    else
    // if (sourceType == DATABASE_TYPE)
    {
      dataId.removeIDChangeListener(myIDChangeListener);
      dataId =
        formularMax4000.getIDManager().getID(FormularMax4kController.NAMESPACE_DB_SPALTE,
          newId);
      dataId.addIDChangeListener(myIDChangeListener);
    }
    notifyListeners(ID_ATTR, dataId);
    // formularMax4000.documentNeedsUpdating(); ist bereits in notifyListeners
  }

  @Override
  public String getName()
  {
    return bookmark.getName();
  }

  public int getSourceType(){
    return sourceType;
  }

  @Override
  public void selectWithViewCursor()
  {
    bookmark.select();
  }

  @Override
  public void removeFromDocument()
  {
    XTextRange range = bookmark.getAnchor();
    if (range != null) range.setString("");
    bookmark.remove();
  }

  /**
   * Entfernt das zur Einfügestelle gehörende WollMux-Bookmark, nicht jedoch den
   * zugehörigen Feldbefehl.
   */
  public void removeBookmark()
  {
    bookmark.remove();
  }

  private static class AutosepInfo
  {
    public int autosep = AUTOSEP_LEFT;

    /**
     * Defaultwert Leerzeichen, wenn nicht definiert (siehe WollMux-Doku).
     */
    public String separator = " ";
  }

  private class MyIDChangeListener implements IDManager.IDChangeListener
  {
    @Override
    public void idHasChanged(ID id)
    {
      if (id != dataId)
      {
        LOGGER.error(L.m("Event für eine ID erhalten, die ich nicht kenne: %1",
          id.toString()));
        return;
      }
      notifyListeners(ID_ATTR, dataId);
      // formularMax4000.documentNeedsUpdating(); ist bereits in notifyListeners
    }
  }

}
