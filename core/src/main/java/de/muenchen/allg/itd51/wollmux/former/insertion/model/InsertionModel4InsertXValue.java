/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.former.insertion.model;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UnoHelperException;
import de.muenchen.allg.document.text.Bookmark;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.config.SyntaxErrorException;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelectionProvider;
import de.muenchen.allg.itd51.wollmux.former.insertion.UnknownIDException;
import de.muenchen.allg.itd51.wollmux.former.model.IdModel;

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
  
  private static final String TRAFO_FUNCTION = "TRAFO";

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
  private IdModel dataId;

  /**
   * Liste von {@link InsertionModel.AutosepInfo} Objekten.
   */
  private List<AutosepInfo> autosep = new ArrayList<>();

  /**
   * Das Bookmark, das diese Einfügestelle umschließt.
   */
  private Bookmark bookmark;

  private IdModel.IDChangeListener myIDChangeListener = new MyIDChangeListener();

  /**
   * Erzeugt ein neues InsertionModel für das Bookmark mit Namen bookmarkName, das bereits im
   * Dokument vorhanden sein muss.
   *
   * @param doc
   *          das Dokument in dem sich das Bookmark befindet
   * @param funcSelections
   *          ein FunctionSelectionProvider, der für das TRAFO Attribut eine passende
   *          FunctionSelection liefern kann.
   * @param formularMax4000
   *          Der FormularMax4000 zu dem dieses InsertionModel gehört.
   * @throws SyntaxErrorException
   *           wenn bookmarkName nicht korrekte ConfigThingy-Syntax hat oder kein korrektes
   *           Einfügekommando ist.
   * @throws NoSuchElementException
   *           wenn ein Bookmark dieses Namens in doc nicht existiert.
   * @throws UnoHelperException
   *           Can't find the book mark.
   */
  public InsertionModel4InsertXValue(String bookmarkName, XBookmarksSupplier doc,
      FunctionSelectionProvider funcSelections, FormularMax4kController formularMax4000)
      throws SyntaxErrorException, NoSuchElementException, UnoHelperException
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

    ConfigThingy trafoConf = conf.query(TRAFO_FUNCTION);
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
  public String updateDocument(
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
        conf.add(TRAFO_FUNCTION).add(trafo.getFunctionName());
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

        conf.add(TRAFO_FUNCTION).add(funcName);
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
    
    if (!bookmark.rename(newBookmarkName).equals(Bookmark.BROKEN))    
    {
      return "";
    } else
    {
      return Bookmark.BROKEN;
    }
  }

  /**
   * Liefert je nach Typ der Einfügung das DB_SPALTE oder ID Attribut.
   */
  public IdModel getDataID()
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
    
    if (newId.length() == 0)
    {
        LOGGER.trace("Leere ID");
        throw new UnknownIDException();
    }
    
    if (sourceType == FORM_TYPE)
    {
      IdModel newDataId =
        formularMax4000.getIDManager().getExistingID(
          FormularMax4kController.NAMESPACE_FORMCONTROLMODEL, newId);
      
      if (newDataId == null)
      {
        LOGGER.trace("newDataID is NULL.");
        throw new UnknownIDException();
      }
    
      dataId.removeIDChangeListener(myIDChangeListener);
      dataId = newDataId;
      dataId.addIDChangeListener(myIDChangeListener);
    }
    else
    {
      dataId.removeIDChangeListener(myIDChangeListener);
      dataId =
        formularMax4000.getIDManager().getID(FormularMax4kController.NAMESPACE_DB_SPALTE,
          newId);
      dataId.addIDChangeListener(myIDChangeListener);
    }
    notifyListeners(ID_ATTR, dataId);
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
  public void selectWithViewCursor() throws UnoHelperException
  {
    bookmark.select();
  }

  @Override
  public void removeFromDocument() throws UnoHelperException
  {
    XTextRange range = bookmark.getAnchor();
    if (range != null) range.setString("");
    bookmark.remove();
  }

  /**
   * Entfernt das zur Einfügestelle gehörende WollMux-Bookmark, nicht jedoch den zugehörigen
   * Feldbefehl.
   *
   * @throws UnoHelperException
   *           Can't delete the book mark.
   */
  public void removeBookmark() throws UnoHelperException
  {
    bookmark.remove();
  }

  private static class AutosepInfo
  {
    private int autosep = AUTOSEP_LEFT;

    /**
     * Defaultwert Leerzeichen, wenn nicht definiert (siehe WollMux-Doku).
     */
    private String separator = " ";
  }

  private class MyIDChangeListener implements IdModel.IDChangeListener
  {
    @Override
    public void idHasChanged(IdModel id)
    {
      if (id != dataId)
      {
        LOGGER.error("Event für eine ID erhalten, die ich nicht kenne: {}", id);
        return;
      }
      notifyListeners(ID_ATTR, dataId);
    }
  }

}
