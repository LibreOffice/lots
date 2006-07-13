/*
 * Dateiname: FormModel.java
 * Projekt  : WollMux
 * Funktion : Erlaubt Zugriff auf die Formularbestandteile eines Dokuments abstrahiert von den dahinterstehenden OOo-Objekten.
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 27.12.2005 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

/**
 * Erlaubt Zugriff auf die Formularbestandteile eines Dokuments abstrahiert von
 * den dahinterstehenden OOo-Objekten. ACHTUNG! Der FormController ruft die
 * Methoden dieser Klasse aus dem Event Dispatching Thread auf. Dort dürfen sie
 * aber meist nicht laufen. Deshalb müssen alle entsprechenden Methoden über den
 * WollMuxEventHandler ein Event Objekt erzeugen und in die WollMux-Queue zur
 * späteren Ausführung schieben. Es muss dafür gesorgt werden, dass das
 * FormModel Objekt auch funktioniert, wenn das zugrundeliegende Office-Dokument
 * disposed wurde, da der FormController evtl. im Moment des disposens darauf
 * zugreifen möchte. Hoffentlich löst obiges Umsetzen der Aufrufe in
 * Event-Objekte dieses Problem schon weitgehend.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface FormModel
{

  /**
   * @param docX
   * @param docY
   * @param docWidth
   * @param docHeight
   */
  public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight);

  public void setWindowVisible(boolean vis);

  public void close();

  public void setVisibleState(String groupId, boolean visible);

  /**
   * Es ist nicht garantiert, dass sich der Wert tatsächlich geändert hat. Die
   * fieldId kann leer sein (aber nie null).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void valueChanged(String fieldId, String newValue);

  public void focusGained(String fieldId);

  public void focusLost(String fieldId);

  /**
   * ACHTUNG: dispose darf nur indirekt über
   * WollMuxEventHandler.handleDisposeFormModel angesprochen werden!
   */
  public void dispose();
}
