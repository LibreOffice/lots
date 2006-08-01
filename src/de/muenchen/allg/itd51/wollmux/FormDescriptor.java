/*
 * Dateiname: FormDescriptor.java
 * Projekt  : WollMux
 * Funktion : Repräsentiert die Formularbeschreibung eines Formulars in Form
 *            von ein bis mehreren WM(CMD'Form')-Kommandos mit den zugehörigen Notizen.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 24.07.2006 | LUT | Erstellung als FormDescriptor
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */

package de.muenchen.allg.itd51.wollmux;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.sun.star.container.XEnumeration;
import com.sun.star.lang.DisposedException;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;

/**
 * Diese Klasse repräsentiert eine Formularbeschreibung eines Formulardokuments
 * in Form eines oder mehrerer WM(CMD'Form')-Kommandos mit den zugehörigen
 * Notizfeldern, die die Beschreibungstexte in ConfigThingy-Syntax enthalten.
 * Die Klasse startet zunächst als ein leerer Container für
 * DocumentCommand.Form-Objekte, in den über die add()-Methode einzelne
 * DocumentCommand.Form-Objekte hinzugefügt werden können. Logisch betrachtet
 * werden alle Beschreibungstexte zu einer großen ConfigThingy-Struktur
 * zusammengefügt und über die Methode toConfigThingy() bereitgestellt.
 * 
 * Die Klasse bietet darüber hinaus Methoden zum Abspeichern und Auslesen der
 * original-Feldwerte im Notizfeld des ersten DocumentCommand.Form-Objekts an,
 * das einen Formularwerte-Abschnitt enthält.
 * 
 * Im Zusammenhang mit der EntwicklerGUI könnten auch alle Operationen der
 * EntwicklerGUI an der Formularbeschreibung (Hinzufügen/Löschen/Verschieben von
 * Eingabeelementen) über diese Klasse abstrahiert werden.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class FormDescriptor
{
  /**
   * Das Dokument, das als Fabrik für neue Annotations benötigt wird.
   */
  private XTextDocument doc;

  /**
   * Enthält alle Formular-Abschnitte, die in den mit add hinzugefügten
   * Form-Kommandos gefunden wurden.
   */
  private ConfigThingy formularConf;

  /**
   * Enthält das erste hinzugefügte WM(CMD'Form')-Kommando, hinter dem eine
   * Notiz für die Aufbewahrung der Formularwerte erzeugt wird, wenn nicht
   * bereits eine andere im Dokument vorhanden ist.
   */
  private DocumentCommand.Form firstFormCmd;

  /**
   * Enthält das Notizfeld das sich unter dem zuletzt mit add hinzugefügten
   * Form-Kommandos befindet, das einen "Formularwerte"-Abschnitt besitzt.
   */
  private XTextContent werteField;

  /**
   * Enthält die aktuellen Werte der Formularfelder als Zuordnung id -> Wert.
   */
  private HashMap formFieldValues;

  /**
   * Erzeugt einen neuen leeren FormDescriptor, dem über add()
   * WM(CMD'Form')-Kommandos mit Formularbeschreibungsnotizen hinzugefügt werden
   * können.
   */
  public FormDescriptor(XTextDocument doc)
  {
    this.doc = doc;
    this.formularConf = new ConfigThingy("WM");
    this.firstFormCmd = null;
    this.werteField = null;
    this.formFieldValues = new HashMap();
  }

  /**
   * Die Methode fügt die Formularbeschreibung, die unterhalb der Notiz des
   * WM(CMD'Form')-Kommandos gefunden wird zur Gesamtformularbeschreibung hinzu.
   * 
   * @param formCmd
   *          Das formCmd, das die Notzi mit der hinzuzufügenden
   *          Formularbeschreibung enthält.
   * @throws ConfigurationErrorException
   *           Die Notiz der Formularbeschreibung ist nicht vorhanden, die
   *           Formularbeschreibung ist nicht vollständig oder kann nicht
   *           geparst werden.
   */
  public void add(DocumentCommand.Form formCmd)
      throws ConfigurationErrorException
  {
    XTextRange range = formCmd.getTextRange();

    Object annotationField = findAnnotationFieldRecursive(range);
    if (annotationField == null)
      throw new ConfigurationErrorException(
          "Die Notiz mit der Formularbeschreibung fehlt.");

    Object content = UNO.getProperty(annotationField, "Content");
    if (content == null)
      throw new ConfigurationErrorException(
          "Die Notiz mit der Formularbeschreibung kann nicht gelesen werden.");

    ConfigThingy conf;
    try
    {
      conf = new ConfigThingy("", null, new StringReader(content.toString()));
    }
    catch (java.lang.Exception e)
    {
      throw new ConfigurationErrorException(
          "Die Formularbeschreibung innerhalb der Notiz ist fehlerhaft:\n"
              + e.getMessage());
    }

    // Formular-Abschnitt auswerten:
    ConfigThingy formular = null;
    try
    {
      formular = conf.get("WM").get("Formular");
      formularConf.addChild(formular);
      if (firstFormCmd == null) firstFormCmd = formCmd;
    }
    catch (NodeNotFoundException e)
    {
    }

    // Formularwerte-Abschnitt lesen:
    ConfigThingy werte = null;
    try
    {
      werte = conf.get("WM").get("Formularwerte");
    }
    catch (NodeNotFoundException e)
    {
    }

    // "Formularwerte"-Abschnitt auswerten.
    if (werte != null)
    {
      werteField = UNO.XTextContent(annotationField);

      formFieldValues = new HashMap();
      Iterator iter = werte.iterator();
      while (iter.hasNext())
      {
        ConfigThingy element = (ConfigThingy) iter.next();
        try
        {
          String id = element.get("ID").toString();
          String value = element.get("VALUE").toString();
          formFieldValues.put(id, value);
        }
        catch (NodeNotFoundException e)
        {
          Logger.error(e);
        }
      }
    }

    if (formular == null && werte == null)
      throw new ConfigurationErrorException(
          "Die Formularbeschreibung innerhalb der Notiz enthält keine Abschnitte 'Formular' oder 'Formularwerte'.");
  }

  /**
   * Liefert eine ConfigThingy-Repräsentation, die unterhalb des Wurzelknotens
   * "WM" der Reihe nach die Vereinigung der "Formular"-Abschnitte aller
   * Formularbeschreibungen der enthaltenen WM(CMD'Form')-Kommandos enthält.
   * 
   * @return ConfigThingy-Repräsentation mit dem Wurzelknoten "WM", die alle
   *         "Formular"-Abschnitte der Formularbeschreibungen enthält.
   */
  public ConfigThingy toConfigThingy()
  {
    return formularConf;
  }

  /**
   * Informiert den FormDescriptor über den neuen Wert value der Formularfelder
   * mit der ID id; die Änderung wird erst nach einem Aufruf von
   * updateDocument() im "Formularwerte"-Abschnitt persistent gespeichert.
   * 
   * @param id
   *          die id der Formularfelder, deren Wert neu gesetzt wurde.
   * @param value
   *          der neu zu setzende Wert.
   */
  public void setFormFieldValue(String id, String value)
  {
    formFieldValues.put(id, value);
  }

  /**
   * Liefert den zuletzt gesetzten Wert des Formularfeldes mit der ID id zurück.
   * 
   * @param id
   *          Die id des Formularfeldes, dessen Wert zurück geliefert werden
   *          soll.
   * @return der zuletzt gesetzte Wert des Formularfeldes mit der ID id.
   */
  public String getFormFieldValue(String id)
  {
    return (String) formFieldValues.get(id);
  }

  /**
   * Liefert ein Set zurück, das alle dem FormDescriptor bekannten IDs für
   * Formularfelder enthält.
   * 
   * @return ein Set das alle dem FormDescriptor bekannten IDs für
   *         Formularfelder enthält.
   */
  public Set getFormFieldIDs()
  {
    return formFieldValues.keySet();
  }

  /**
   * Diese Methode legt den aktuellen Werte aller Fomularfelder in einem
   * Abschnitt "Formularwerte" in der Notiz des ersten mit add() hinzugefügten
   * WM(CMD'Form')-Kommandos ab, das einen Formularwerte-Abschnitt besitzt. Ist
   * kein entsprechendes Kommando vorhanden, so wird es erzeugt.
   */
  public void updateDocument()
  {
    Logger.debug2(this.getClass().getSimpleName() + ".updateDocument()");

    // Neues ConfigThingy für "Formularwerte"-Abschnitt erzeugen:
    ConfigThingy werte = new ConfigThingy("WM");
    ConfigThingy formwerte = new ConfigThingy("Formularwerte");
    werte.addChild(formwerte);
    Iterator iter = formFieldValues.keySet().iterator();
    while (iter.hasNext())
    {
      String key = (String) iter.next();
      String value = (String) formFieldValues.get(key);
      if (key != null && value != null)
      {
        ConfigThingy entry = new ConfigThingy("");
        ConfigThingy cfID = new ConfigThingy("ID");
        cfID.add(key);
        ConfigThingy cfVALUE = new ConfigThingy("VALUE");
        cfVALUE.add(value);
        entry.addChild(cfID);
        entry.addChild(cfVALUE);
        formwerte.addChild(entry);
      }
    }

    // neues WM(CMD'Form') Kommando zur Ablage der Formularwerte erzeugen, wenn
    // nicht bereits eines vorhanden ist.
    if (werteField == null && firstFormCmd != null)
    {
      try
      {
        werteField = UNO.XTextContent(UNO.XMultiServiceFactory(doc)
            .createInstance("com.sun.star.text.TextField.Annotation"));
      }
      catch (java.lang.Exception e)
      {
      }

      XTextRange range = firstFormCmd.getTextRange();
      try
      {
        range.getText().insertTextContent(range.getEnd(), werteField, false);
      }
      catch (java.lang.Exception e)
      {
      }

      if (doc != null && werteField != null)
        new Bookmark("WM(CMD 'Form')", doc, werteField.getAnchor());
    }

    // neuen "Formularwerte"-Abschnitt setzen
    try
    {
      UNO.setProperty(werteField, "Content", werte.stringRepresentation());
    }
    catch (DisposedException e)
    {
    }
  }

  // Helper-Methoden:

  /**
   * Diese Methode durchsucht das Element element bzw. dessen XEnumerationAccess
   * Interface rekursiv nach TextField.Annotation-Objekten und liefert das erste
   * gefundene TextField.Annotation-Objekt zurück, oder null, falls kein
   * entsprechendes Element gefunden wurde.
   * 
   * @param element
   *          Das erste gefundene InputField.
   */
  private static XTextField findAnnotationFieldRecursive(Object element)
  {
    // zuerst die Kinder durchsuchen (falls vorhanden):
    if (UNO.XEnumerationAccess(element) != null)
    {
      XEnumeration xEnum = UNO.XEnumerationAccess(element).createEnumeration();

      while (xEnum.hasMoreElements())
      {
        try
        {
          Object child = xEnum.nextElement();
          XTextField found = findAnnotationFieldRecursive(child);
          // das erste gefundene Element zurückliefern.
          if (found != null) return found;
        }
        catch (java.lang.Exception e)
        {
          Logger.error(e);
        }
      }
    }

    // jetzt noch schauen, ob es sich bei dem Element um eine Annotation
    // handelt:
    if (UNO.XTextField(element) != null)
    {
      Object textField = UNO.getProperty(element, "TextField");
      if (UNO.supportsService(
          textField,
          "com.sun.star.text.TextField.Annotation"))
      {
        return UNO.XTextField(textField);
      }
    }

    return null;
  }

}
