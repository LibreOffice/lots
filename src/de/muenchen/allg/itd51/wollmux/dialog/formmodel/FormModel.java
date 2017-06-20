/*
 * Dateiname: FormModel.java
 * Projekt  : WollMux
 * Funktion : Erlaubt Zugriff auf die Formularbestandteile eines Dokuments abstrahiert von den dahinterstehenden OOo-Objekten.
 * 
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
 * 27.12.2005 | BNK | Erstellung
 * 28.04.2008 | BNK | +save(), +saveAs()
 * 02.06.2010 | BED | +saveTempAndOpenExt
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.formmodel;

import java.awt.event.ActionListener;
import java.util.List;

/**
 * Erlaubt Zugriff auf die Formularbestandteile eines Dokuments abstrahiert von den
 * dahinterstehenden OOo-Objekten. ACHTUNG! Der FormController ruft die Methoden
 * dieser Klasse aus dem Event Dispatching Thread auf. Dort dürfen sie aber meist
 * nicht laufen. Deshalb müssen alle entsprechenden Methoden über den
 * WollMuxEventHandler ein Event Objekt erzeugen und in die WollMux-Queue zur
 * späteren Ausführung schieben. Es muss dafür gesorgt werden, dass das FormModel
 * Objekt auch funktioniert, wenn das zugrundeliegende Office-Dokument disposed
 * wurde, da der FormController evtl. im Moment des disposens darauf zugreifen
 * möchte. Hoffentlich löst obiges Umsetzen der Aufrufe in Event-Objekte dieses
 * Problem schon weitgehend.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface FormModel
{

  /**
   * Setzt die Position und Größe des Fensters des zugehörigen Dokuments auf die
   * vorgegebenen Werte setzt. ACHTUNG: Die Maßangaben beziehen sich auf die linke
   * obere Ecke des Fensterinhalts OHNE die Titelzeile und die Fensterdekoration des
   * Rahmens. Um die linke obere Ecke des gesamten Fensters richtig zu setzen, müssen
   * die Größenangaben des Randes der Fensterdekoration und die Höhe der Titelzeile
   * VOR dem Aufruf der Methode entsprechend eingerechnet werden.
   * 
   * @param docX
   *          Die linke obere Ecke des Fensterinhalts X-Koordinate der Position in
   *          Pixel, gezählt von links oben.
   * @param docY
   *          Die Y-Koordinate der Position in Pixel, gezählt von links oben.
   * @param docWidth
   *          Die Größe des Dokuments auf der X-Achse in Pixel
   * @param docHeight
   *          Die Größe des Dokuments auf der Y-Achse in Pixel. Auch hier wird die
   *          Titelzeile des Rahmens nicht beachtet und muss vorher entsprechend
   *          eingerechnet werden.
   * 
   * @author christoph.lutz
   */
  public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight);

  /**
   * Setzt den Sichtbarkeitsstatus des Fensters des zugehörigen Dokuments auf vis
   * (true=sichtbar, false=unsichtbar).
   * 
   * @param vis
   *          true=sichtbar, false=unsichtbar
   * 
   * @author christoph.lutz
   */
  public void setWindowVisible(boolean vis);

  /**
   * Versucht das Dokument zu schließen. Wurde das Dokument verändert
   * (Modified-Status des Dokuments==true), so erscheint der Dialog
   * "Speichern"/"Verwerfen"/"Abbrechen" (über den ein sofortiges Schließen des
   * Dokuments durch den Benutzer verhindert werden kann)
   * 
   * @author christoph.lutz
   */
  public void close();

  /**
   * Setzt den Sichtbarkeitsstatus der Sichtbarkeitsgruppe mit der ID groupID auf
   * visible.
   * 
   * @param groupId
   *          Die ID der Gruppe, die Sichtbar/unsichtbar geschalten werden soll.
   * @param visible
   *          true==sichtbar, false==unsichtbar
   * 
   * @author christoph.lutz
   */
  public void setVisibleState(String groupId, boolean visible);

  /**
   * Setzt den Wert aller Formularfelder im Dokument, die von fieldId abhängen auf
   * den neuen Wert newValue (bzw. auf das Ergebnis der zu diesem Formularelement
   * hinterlegten Trafo-Funktion).
   * 
   * Es ist nicht garantiert, dass sich der Wert tatsächlich geändert hat. Die
   * fieldId kann leer sein (aber nie null).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void valueChanged(String fieldId, String newValue);

  /**
   * Das Formularfeld im Dokument mit der ID fieldId erhält den Fokus. Gibt es im
   * Dokument mehrere Formularfelder, die von der ID abhängen, so erhält immer das
   * erste Formularfeld den Fokus - bevorzugt werden dabei auch die nicht
   * transformierten Formularfelder.
   * 
   * @param fieldId
   *          id des Formularfeldes, das den Fokus bekommen soll.
   * 
   * @author christoph.lutz
   */
  public void focusGained(String fieldId);

  /**
   * Not Yet Implemented: Nimmt dem Formularfeld mit der ID fieldId den Fokus wieder
   * weg - ergibt aber bisher keinen Sinn.
   * 
   * @param fieldId
   * 
   * @author christoph.lutz
   */
  public void focusLost(String fieldId);

  /**
   * Informiert das FormModel, dass das zugrundeliegende Dokument source geschlossen
   * wird und das FormModel entsprechend handeln soll um sicherzustellen, dass das
   * Dokument in Zukunft nicht mehr angesprochen wird.
   * 
   * Abhängig von der Implementierung des FormModels werden unterschiedliche Aktionen
   * erledigt. Dazu gehören z.B. das Beenden einer bereits gestarteten FormGUI oder
   * das Wiederherstellen der Fensterattribute des Dokumentfensters auf die Werte,
   * die das Fenster vor dem Starten der FormGUI hatte.
   * 
   * @param source
   *          Das Dokument das geschlossen wurde.
   * 
   * @author christoph.lutz
   */
  public void closing(Object sender);

  /**
   * Teilt der FormGUI die zu diesem FormModel gehört mit, dass der Wert des
   * Formularfeldes mit der id fieldId auf den neuen Wert value gesetzt werden soll
   * und ruft nach erfolgreicher aktion die Methode actionPerformed(ActionEvent arg0)
   * des Listeners listener.
   * 
   * @param fieldId
   *          die Id des Feldes das in der FormGUI auf den neuen Wert value gesetzt
   *          werden soll.
   * @param value
   *          der neue Wert value.
   * @param listener
   *          der Listener der informiert wird, nachdem der Wert erfolgreich gesetzt
   *          wurde.
   * 
   * @author christoph.lutz
   */
  public void setValue(String fieldId, String value, ActionListener listener);

  /**
   * Erzeugt eine FormGUI zu diesem FormModel und startet diese.
   * 
   * @author christoph.lutz
   */
  public void startFormGUI();

  /**
   * Startet den Ausdruck unter Verwendung eventuell vorhandener
   * Komfortdruckfunktionen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void print();

  /**
   * Exportiert das Dokument als PDF. Bei Multi-Form wird die Aktion für alle
   * Formulare der Reihe nach aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void pdf();

  /**
   * Speichert das Dokument (Datei/Speichern). Bei Multi-Form wird die Aktion für
   * alle Formulare der Reihe nach aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void save();

  /**
   * Speichert das Dokument (Datei/Speichern unter...). Bei Multi-Form wird die
   * Aktion für alle Formulare der Reihe nach aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void saveAs();

  /**
   * Speichert dieses Formular in eine temporäre Datei unter Verwendung des in in
   * ExterneAnwendungen für ext festgelegten FILTERs, startet dann die zugehörige
   * externe Anwendung mit dieser Datei und schließt das Formular.
   */
  public void closeAndOpenExt(String ext);

  /**
   * Speichert dieses Formular in eine temporäre Datei unter Verwendung des in in
   * ExterneAnwendungen für ext festgelegten FILTERs und startet dann die zugehörige
   * externe Anwendung mit dieser Datei.
   */
  public void saveTempAndOpenExt(String ext);
  
  /**
   * Öffnet durch ACTION-Event ein neues Dokument oder Template.
   * Durch Angabe der FragID wird die entsprechende Vorlage zugeordnet.
   */
  public void openTemplateOrDocument(List<String> fragIds);

  /**
   * Über diese Methode kann der FormController das FormModel informieren, dass er
   * vollständig initialisiert wurde und notwendige Aktionen wie z.B. das
   * zurücksetzen des modified-Status des Dokuments durchgeführt werden sollen.
   */
  public void formControllerInitCompleted();

  /**
   * Liefert den Titel des zum FormModel gehörenden Fensters oder null, falls kein
   * Titel bekannt oder nicht anwendbar (z.B. Multi-Form).
   */
  public String getWindowTitle();
}
