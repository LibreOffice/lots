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
 * aber meist nicht laufen. Deshalb müssen alle entsprechenden Methoden ein
 * Event Objekt erzeugen und in die WollMux-Queue zur späteren Ausführung
 * schieben. Es muss dafür gesorgt werden, dass das FormModel Objekt auch
 * funktioniert, wenn das zugrundeliegende Office-Dokument disposed wurde, da
 * der FormController evtl. im Moment des disposens darauf zugreifen möchte.
 * Hoffentlich löst obiges Umsetzen der Aufrufe in Event-Objekte dieses Problem
 * schon weitgehend.
 * 
 * Rückgabewerte an den FormController, der die Methoden aufruft sind so
 * natürlich nicht möglich. Deshalb muss bei entsprechenden Änderungen jeweils
 * ein Callback erfolgen, typischerweise ein Aufruf der Funktion
 * FormController.updateUI().
 * 
 * FormModel und/oder FormController müssen auf die Situation vorbereitet sein,
 * dass FormController eine Änderung anstossen möchte, die aufgrund einer
 * vorhergehenden Änderung gar nicht mehr möglich ist. Beispiel:
 * 
 * 1. FormController ersetzt Textfragment F1 durch F2.
 * 
 * 2. Fragment F11 war in F1 enthalten und ist deshalb jetzt nicht mehr
 * vorhanden.
 * 
 * 3. Bevor der updateUI() Aufruf abgearbeitet wurde, der dem Benutzer das UI
 * zum Ändern von F11 genommen hätte setzt der Benutzer noch eine Änderung
 * von F11 ab.
 * 
 * Eventuell wird der Zugriff auf die FormModel-Funktionen, die die Struktur
 * des Models auslesen synchronisiert erfolgen müssen,
 * damit der WollMux-Main-Thread und der Event-Dispatching Thread sich nicht
 * in die Quere kommen können.
 * Andere Lösung wäre, auch diese lesenden Zugriffe über Callbacks zu 
 * realisieren, d.h. FormController ruft Methode auf "requestModelStructure()"
 * und erhält dann nach einer Weile den Callback mit der entsprechenden
 * Struktur. Mal sehen, ob sich das im FormController mit vertretbarem
 * Aufwand realisieren lässt. Eventuell kann aber sogar ganz auf
 * strukturauslesende Methoden verzichtet werden, wenn alle nötigen Infos
 * in FormUIDescription drinstecken.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface FormModel
{

  
}
