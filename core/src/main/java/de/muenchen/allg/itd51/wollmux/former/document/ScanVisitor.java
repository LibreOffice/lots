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
package de.muenchen.allg.itd51.wollmux.former.document;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.config.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.document.DocumentTreeVisitor;
import de.muenchen.allg.itd51.wollmux.document.InsertionBookmark;
import de.muenchen.allg.itd51.wollmux.document.TextRange;
import de.muenchen.allg.itd51.wollmux.document.nodes.Container;
import de.muenchen.allg.itd51.wollmux.document.nodes.DropdownFormControl;
import de.muenchen.allg.itd51.wollmux.document.nodes.FormControl;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.control.FormControlModel;
import de.muenchen.allg.itd51.wollmux.former.function.FunctionSelection;
import de.muenchen.allg.itd51.wollmux.former.function.ParamValue;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel4InsertXValue;
import de.muenchen.allg.itd51.wollmux.util.L;

public class ScanVisitor implements DocumentTreeVisitor
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ScanVisitor.class);

  private final FormularMax4kController formularMax4000;

  /**
   * Maximale Anzahl Zeichen für ein automatisch generiertes Label.
   */
  private static final int GENERATED_LABEL_MAXLENGTH = 30;

  /**
   * Beim Import neuer Formularfelder oder Checkboxen schaut der FormularMax4000 nach
   * speziellen Hinweisen/Namen/Einträgen, die diesem Muster entsprechen. Diese
   * Zusatzinformationen werden herangezogen um Labels, IDs und andere Informationen
   * zu bestimmen.
   * 
   * >>>>Eingabefeld<<<<: Als "Hinweis" kann "Label<<ID>>" angegeben werden und wird
   * beim Import entsprechend berücksichtigt. Wird nur "<<ID>>" angegeben, so
   * markiert das Eingabefeld eine reine Einfügestelle (insertValue oder
   * insertContent) und beim Import wird dafür kein Formularsteuerelement erzeugt.
   * Wird ID ein "glob:" vorangestellt, so wird gleich ein insertValue-Bookmark
   * erstellt.
   * 
   * >>>>>Eingabeliste/Dropdown<<<<<: Als "Name" kann "Label<<ID>>" angegeben werden
   * und wird beim Import berücksichtigt. Als Spezialeintrag in der Liste kann
   * "<<Freitext>>" eingetragen werden und signalisiert dem FM4000, dass die ComboBox
   * im Formular auch die Freitexteingabe erlauben soll. Wie bei Eingabefeldern auch
   * ist die Angabe "<<ID>>" ohne Label möglich und signalisiert, dass es sich um
   * eine reine Einfügestelle handelt, die kein Formularelement erzeugen soll. Wird
   * als "Name" die Spezialsyntax "<<gender:ID>>" verwendet, so wird eine reine
   * Einfügestelle erzeugt, die mit einer Gender-TRAFO versehen wird, die abhängig
   * vom Formularfeld ID einen der Werte des Dropdowns auswählt, und zwar bei "Herr"
   * oder "Herrn" den ersten Eintrag, bei "Frau" den zweiten Eintrag und bei allem
   * sonstigen den dritten Eintrag. Hat das Dropdown nur 2 Einträge, so wird im
   * sonstigen Fall das Feld ID untransformiert übernommen. Falls vorhanden werden
   * bis zu N-1 Leerzeichen am Ende eines Eintrages der Dropdown-Liste entfernt,
   * wobei N die Anzahl der Einträge ist, die bis auf folgende Leerzeichen identisch
   * zu diesem Eintrag sind. Dies ermöglicht es, das selbe Wort mehrfach in die Liste
   * aufzunehmen.
   * 
   * >>>>>Checkbox<<<<<: Bei Checkboxen kann als "Hilfetext" "Label<<ID>>" angegeben
   * werden und wird beim Import entsprechend berücksichtigt.
   * 
   * Technischer Hinweis: Auf dieses Pattern getestet wird grundsätzlich der String,
   * der von {@link FormControl#getDescriptor()} geliefert wird.
   * 
   */
  private static final Pattern MAGIC_DESCRIPTOR_PATTERN =
    Pattern.compile("\\A(.*)<<(.*)>>\\z");

  /**
   * Präfix zur Markierung von IDs der magischen Deskriptor-Syntax um anzuzeigen,
   * dass ein insertValue anstatt eines insertFormValue erzeugt werden soll.
   */
  private static final String GLOBAL_PREFIX = "glob:";

  /**
   * Präfix zur Markierung von IDs der magischen Deskriptor-Syntax um anzuzeigen,
   * dass ein insertFormValue mit Gender-TRAFO erzeugt werden soll.
   */
  private static final String GENDER_PREFIX = "gender:";

  /**
   * Regex für Test ob String mit Buchstabe oder Underscore beginnt. ACHTUNG! Das .*
   * am Ende ist notwendig, da String.matches() immer den ganzen String testet.
   */
  private static final String STARTS_WITH_LETTER_RE = "^[a-zA-Z_].*";

  /**
   * Die Namen der Parameter, die die Gender-Trafo erwartet. ACHTUNG! Diese müssen
   * exakt mit den Parametern der Gender()-Funktion aus der WollMux-Konfig
   * übereinstimmen. Insbesondere dürfen sie nicht übersetzt werden, ohne dass die
   * Gender()-Funktion angepasst wird. Und falls die Gender()-Funktion geändert wird,
   * dann funktionieren existierende Formulare nicht mehr.
   */
  private static final String[] GENDER_TRAFO_PARAMS = new String[] {
    "Falls_Anrede_HerrN", "Falls_Anrede_Frau", "Falls_sonstige_Anrede", "Anrede" };

  /**
   * @param formularMax4000
   */
  public ScanVisitor(FormularMax4kController formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
  }

  private Map<String, InsertionBookmark> insertions =
    new HashMap<>();

  private StringBuilder text = new StringBuilder();

  private StringBuilder fixupText = new StringBuilder();

  private FormControlModel fixupCheckbox = null;

  private void fixup()
  {
    if (fixupCheckbox != null && fixupCheckbox.getLabel().length() == 0)
    {
      fixupCheckbox.setLabel(makeLabelFromStartOf(fixupText,
        2 * GENERATED_LABEL_MAXLENGTH));
      fixupCheckbox = null;
    }
    fixupText.setLength(0);
  }

  @Override
  public boolean container(Container container, int count)
  {
    fixup();

    if (container.getType() != Container.Type.PARAGRAPH)
    {
      text.setLength(0);
    }

    return true;
  }

  @Override
  public boolean textRange(TextRange textRange)
  {
    String str = textRange.getString();
    text.append(str);
    fixupText.append(str);
    return true;
  }

  @Override
  public boolean insertionBookmark(InsertionBookmark bookmark)
  {
    if (bookmark.isStart())
      insertions.put(bookmark.getName(), bookmark);
    else
      insertions.remove(bookmark.getName());

    return true;
  }

  @Override
  public boolean formControl(FormControl control)
  {
    fixup();

    if (insertions.isEmpty())
    {
      FormControlModel model = registerFormControl(control, text);
      if (model != null && model.getType() == FormControlModel.CHECKBOX_TYPE)
        fixupCheckbox = model;
    }

    return true;
  }
  
  /**
   * Fügt der {@link #formControlModelList} ein neues {@link FormControlModel} hinzu
   * für das {@link de.muenchen.allg.itd51.wollmux.document.nodes.FormControl}
   * control, wobei text der Text sein sollte, der im Dokument vor control steht.
   * Dieser Text wird zur Generierung des Labels herangezogen. Es wird ebenfalls der
   * {@link #insertionModelList} ein entsprechendes {@link InsertionModel}
   * hinzugefügt. Zusätzlich wird immer ein entsprechendes Bookmark um das Control
   * herumgelegt, das die Einfügestelle markiert.
   * 
   * @return null, falls es sich bei dem Control nur um eine reine Einfügestelle
   *         handelt. In diesem Fall wird nur der {@link #insertionModelList} ein
   *         Element hinzugefügt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  FormControlModel registerFormControl(FormControl control,
      StringBuilder text)
  {
    boolean insertionOnlyNoLabel = false;
    String label = "";
    String id;
    String descriptor = control.getDescriptor();
    Matcher m = MAGIC_DESCRIPTOR_PATTERN.matcher(descriptor);
    if (m.matches())
    {
      label = m.group(1).trim();
      if (label.length() == 0)
      {
        insertionOnlyNoLabel = true;
      }
      id = m.group(2).trim();
    }
    else
    {
      if (control.getType() == FormControl.FormControlType.CHECKBOX_CONTROL)
        label = ""; // immer fixUp-Text von hinter der Checkbox benutzen, weil
      // meist bessere Ergebnisse als Text von vorne
      else
        label = makeLabelFromEndOf(text, GENERATED_LABEL_MAXLENGTH);
      id = descriptor;
    }

    id = makeControlId(id, insertionOnlyNoLabel);

    FormControlModel model = null;

    if (!insertionOnlyNoLabel)
    {
      switch (control.getType())
      {
      case CHECKBOX_CONTROL:
          model = registerCheckbox(control, label, id);
          break;
        case DROPDOWN_CONTROL:
          model = registerDropdown((DropdownFormControl) control, label, id);
          break;
        case INPUT_CONTROL:
          model = registerInput(control, label, id);
          break;
        default:
          LOGGER.error(L.m("Unbekannter Typ Formular-Steuerelement"));
          return null;
      }
    }

    boolean doGenderTrafo = false;

    String bookmarkName = insertFormValue(id);
    if (insertionOnlyNoLabel)
    {
      if (id.startsWith(GLOBAL_PREFIX))
      {
        id = id.substring(GLOBAL_PREFIX.length());
        bookmarkName = insertValue(id);
      }
      else if (id.startsWith(GENDER_PREFIX))
      {
        id = id.substring(GENDER_PREFIX.length());
        bookmarkName = insertFormValue(id);
        if (control.getType() == FormControl.FormControlType.DROPDOWN_CONTROL)
          doGenderTrafo = true;
      }
    }

    try
    {
      bookmarkName = control.surroundWithBookmark(bookmarkName);
      InsertionModel imodel = new InsertionModel4InsertXValue(bookmarkName,
          UNO.XBookmarksSupplier(formularMax4000.getDocumentController().getModel().doc),
          formularMax4000.getFunctionSelectionProvider(), formularMax4000);
      if (doGenderTrafo)
      {
        addGenderTrafo(imodel, (DropdownFormControl) control);
      }
      formularMax4000.getInsertionModelList().add(imodel);
    }
    catch (Exception x)
    {
      LOGGER.error(
        L.m("Es wurde ein fehlerhaftes Bookmark generiert: \"%1\"", bookmarkName), x);
    }

    return model;
  }

  /**
   * Fügt {@link #formControlModelList} ein neues {@link FormControlModel} für eine
   * Checkbox hinzu und liefert es zurück.
   * 
   * @param control
   *          das entsprechende
   *          {@link de.muenchen.allg.itd51.wollmux.document.nodes.FormControl}
   * @param label
   *          das Label
   * @param id
   *          die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerCheckbox(FormControl control, String label,
      String id)
  {
    FormControlModel model = null;
    model = FormControlModel.createCheckbox(label, id, formularMax4000);
    if (control.getString().equalsIgnoreCase("true"))
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add("true");
      model.setAutofill(formularMax4000.getFunctionSelectionProvider().getFunctionSelection(autofill));
    }
    formularMax4000.getFormControlModelList().add(model);
    return model;
  }

  /**
   * Fügt {@link #formControlModelList} ein neues {@link FormControlModel} für eine
   * Auswahlliste hinzu und liefert es zurück.
   * 
   * @param control
   *          das entsprechende
   *          {@link de.muenchen.allg.itd51.wollmux.document.nodes.FormControl}
   * @param label
   *          das Label
   * @param id
   *          die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerDropdown(DropdownFormControl control,
      String label, String id)
  {
    FormControlModel model = null;
    String[] items = control.getItems();
    boolean editable = false;
    for (int i = 0; i < items.length; ++i)
    {
      if (items[i].equalsIgnoreCase("<<Freitext>>"))
      {
        String[] newItems = new String[items.length - 1];
        System.arraycopy(items, 0, newItems, 0, i);
        System.arraycopy(items, i + 1, newItems, i, items.length - i - 1);
        items = newItems;
        editable = true;
        break;
      }
    }
    model = FormControlModel.createComboBox(label, id, items, formularMax4000);
    model.setEditable(editable);
    String preset = unicodeTrim(control.getString());
    if (preset.length() > 0)
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add(preset);
      model.setAutofill(formularMax4000.getFunctionSelectionProvider().getFunctionSelection(autofill));
    }
    formularMax4000.getFormControlModelList().add(model);
    return model;
  }

  /**
   * Fügt {@link #formControlModelList} ein neues {@link FormControlModel} für ein
   * Eingabefeld hinzu und liefert es zurück.
   * 
   * @param control
   *          das entsprechende
   *          {@link de.muenchen.allg.itd51.wollmux.document.nodes.FormControl}
   * @param label
   *          das Label
   * @param id
   *          die ID
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private FormControlModel registerInput(FormControl control, String label, String id)
  {
    FormControlModel model = null;
    model = FormControlModel.createTextfield(label, id, formularMax4000);
    String preset = unicodeTrim(control.getString());
    if (preset.length() > 0)
    {
      ConfigThingy autofill = new ConfigThingy("AUTOFILL");
      autofill.add(preset);
      model.setAutofill(formularMax4000.getFunctionSelectionProvider().getFunctionSelection(autofill));
    }
    formularMax4000.getFormControlModelList().add(model);
    return model;
  }

  /**
   * Liefert str zurück minus führende und folgende Whitespace (wobei
   * Unicode-Leerzeichen) korrekt berücksichtigt werden.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private String unicodeTrim(String str)
  {
    if (str.length() == 0)
    {
      return str;
    }

    if (Character.isWhitespace(str.charAt(0))
      || Character.isWhitespace(str.charAt(str.length() - 1)))
    {
      int i = 0;
      while (i < str.length() && Character.isWhitespace(str.charAt(i)))
      {
        ++i;
      }
      
      int j = str.length() - 1;
      while (j >= 0 && Character.isWhitespace(str.charAt(j)))
      {
        --j;
      }
        
      if (i > j) 
      {
        return "";
      }
      return str.substring(i, j + 1);
    }
    else
      return str;
  }

  /**
   * Bastelt aus dem Start des Textes text ein Label, das maximal maxlen Zeichen lang
   * ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeLabelFromStartOf(StringBuilder text, int maxlen)
  {
    String label;
    String str = text.toString().trim();
    int len = str.length();
    if (len > maxlen) 
    {
      len = maxlen;
    }
    label = str.substring(0, len);
    if (label.length() < 2)
    {
      label = "";
    }
    return label;
  }
  
  /**
   * Bastelt aus dem Ende des Textes text ein Label das maximal maxlen Zeichen lang
   * ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeLabelFromEndOf(StringBuilder text, int maxlen)
  {
    String label;
    String str = text.toString().trim();
    int len = str.length();
    if (len > maxlen)
    {
      len = maxlen;
    }
    label = str.substring(str.length() - len);
    if (label.length() < 2)
    {
      label = "";
    }
    return label;
  }

  /**
   * Macht aus str einen passenden Bezeichner für ein Steuerelement. Falls
   * insertionOnlyNoLabel == true, so muss der Bezeichner nicht eindeutig sein (dies
   * ist der Marker für eine reine Einfügestelle, für die kein Steuerelement erzeugt
   * werden muss).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String makeControlId(String str, boolean insertionOnlyNoLabel)
  {
    String s = str;
    if (insertionOnlyNoLabel)
    {
      String prefix = "";
      if (s.startsWith(GLOBAL_PREFIX))
      {
        prefix = GLOBAL_PREFIX;
        s = s.substring(GLOBAL_PREFIX.length());
      }
      else if (s.startsWith(GENDER_PREFIX))
      {
        prefix = GENDER_PREFIX;
        s = s.substring(GENDER_PREFIX.length());
      }
      s = s.replaceAll("[^a-zA-Z_0-9]", "");
      if (s.length() == 0) 
      {
        s = "Einfuegung";
      }
      if (!s.matches(STARTS_WITH_LETTER_RE))
      {
        s = "_" + s;
      }
      return prefix + s;
    }
    else
    {
      s = s.replaceAll("[^a-zA-Z_0-9]", "");
      if (s.length() == 0)
      {
        s = "Steuerelement";
      }
      if (!s.matches(STARTS_WITH_LETTER_RE))
      {
        s = "_" + s;
      }
      return formularMax4000.getFormControlModelList().makeUniqueId(s);
    }
  }
  
  /**
   * Liefert "WM(CMD'insertValue' DB_SPALTE '&lt;id>').
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertValue(String id)
  {
    return "WM(CMD 'insertValue' DB_SPALTE '" + id + "')";
  }

  /**
   * Liefert "WM(CMD'insertFormValue' ID '&lt;id>').
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private String insertFormValue(String id)
  {
    return "WM(CMD 'insertFormValue' ID '" + id + "')";
  }
  
  /**
   * Verpasst model eine Gender-TRAFO, die ihre Herr/Frau/Anders-Texte aus den Items
   * von control bezieht.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addGenderTrafo(InsertionModel model, DropdownFormControl control)
  {
    String[] items = control.getItems();
    FunctionSelection genderTrafo =
      formularMax4000.getFunctionSelectionProvider().getFunctionSelection("Gender");

    for (int i = 0; i < 3 && i < items.length; ++i)
    {
      String item = items[i];

      /*
       * Bestimme die maximal am Ende des Eintrags zu entfernende Anzahl Leerzeichen.
       * Dies ist die Anzahl an Einträgen, die bis auf folgende Leerzeichen identisch
       * sind MINUS 1.
       */
      String item1 = item;
      while (item1.endsWith(" "))
      {
        item1 = item1.substring(0, item1.length() - 1);
      }
      int n = 0;
      for (int j = 0; j < items.length; ++j)
      {
        String item2 = items[j];
        while (item2.endsWith(" "))
        {
          item2 = item2.substring(0, item2.length() - 1);
        }
        if (item1.equals(item2)) 
        {
          ++n;
        }
      }

      // bis zu N-1 Leerzeichen am Ende löschen, um mehrere gleiche Einträge zu
      // erlauben.
      for (; n > 1 && item.endsWith(" "); --n)
      {
        item = item.substring(0, item.length() - 1);
      }
      genderTrafo.setParameterValue(GENDER_TRAFO_PARAMS[i], ParamValue.literal(item));
    }

    model.setTrafo(genderTrafo);
  }
}