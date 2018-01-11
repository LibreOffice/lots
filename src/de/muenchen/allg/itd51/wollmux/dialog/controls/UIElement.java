/*
 * Dateiname: UIElement.java
 * Projekt  : WollMux
 * Funktion : Interface das von den konkreten UI Elementen (Combobox etc.) abstrahiert.
 * 
 * Copyright (c) 2008-2018 Landeshauptstadt München
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
 * 11.01.2006 | BNK | Erstellung
 * 24.04.2006 | BNK | kommentiert.
 * 05.05.2006 | BNK | Condition -> Function
 * 15.05.2006 | BNK | +setString()
 * 18.05.2006 | BNK | +isStatic()
 * 30.05.2006 | BNK | UIElement.Listbox unterstützt jetzt Zusatzfunktionen
 * 16.06.2006 | BNK | +hasFocus(), +takeFocus()
 * 29.09.2006 | BNK | Verbessertes Auslesen von ComboBox-Daten 
 * 25.01.2006 | BNK | [R5038]Hintergrundfarbe von Textareas korrekt setzen
 * 05.07.2007 | BNK | [R7464]revalidate() auf Parent nach setVisible()
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.controls;

import java.awt.Color;
import java.awt.Component;

import de.muenchen.allg.itd51.wollmux.core.functions.Value;

/**
 * Interface das von den konkreten UI Elementen (Combobox etc.) abstrahiert.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public interface UIElement extends Value
{

  /**
   * Gibt an, dass das UI Element kein zusätzliches Label erhalten soll.
   */
  public static final Integer LABEL_NONE = Integer.valueOf(0);

  /**
   * Gibt an, dass links neben dem UI Element ein zusätzliches Label stehen soll.
   */
  public static final Integer LABEL_LEFT = Integer.valueOf(-1);

  /**
   * Gibt an, dass rechts neben dem UI Element ein zusätzliches Label stehen soll.
   */
  public static final Integer LABEL_RIGHT = Integer.valueOf(+1);

  /**
   * Liefert die Information, ob das UI Element ein zusätzliches Label links oder
   * rechts bekommen soll. Mögliche Werte sind {@link #LABEL_LEFT},
   * {@link #LABEL_RIGHT} und {@link #LABEL_NONE}.
   */
  public Integer getLabelType();

  /**
   * Ist nur definiert, wenn getLabelType() nicht LABEL_NONE ist und liefert das
   * anzuzeigende Zusatzlabel. Achtung! Bei UI Elementen, die selbst Labels sind
   * (TYPE "label") liefert diese Methode <b>nicht</b> das UI Element selbst.
   */
  public Component getLabel();

  /**
   * Der funktionale Teil des UIElements. Achtung! Bei UI Elementen vom TYPE "label"
   * liefert diese Methode (nicht getLabel()) das Label.
   */
  public Component getComponent();

  /**
   * Liefert das empfohlene zweite Argument für
   * {@link java.awt.Container#add(java.awt.Component, java.lang.Object)} für die
   * funktionale Komponente dieses UI Elements.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getLayoutConstraints();

  /**
   * Liefert das empfohlene zweite Argument für
   * {@link java.awt.Container#add(java.awt.Component, java.lang.Object)} für das
   * Zusatz-Label dieses UIElements. Nur definiert, falls getLabelType() nicht
   * LABEL_NONE ist.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getLabelLayoutConstraints();

  /**
   * Liefert das mit setAdditionalData() gesetzte Objekt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public Object getAdditionalData();

  /**
   * Setzt das Objekt, das von getAdditionalData() zurückgeliefert werden soll.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setAdditionalData(Object o);

  /**
   * Der aktuelle Wert des UI Elements. Falls es sich um ein boolesches Element
   * (Checkbox, Radio Button) handelt, wird im angeschalteten Fall der String "true",
   * im ungesetzten Fall "false" zurückgeliefert. Im Falle eines Buttons oder eines
   * anderen Elements das keinen sinnvollen Wert hat, wird immer "false" geliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getString();

  /**
   * Falls es sich um ein boolesches Element (Checkbox, Radio Button) handelt, wird
   * true geliefert falls das Element angeschaltet ist, ansonsten false. Im Falle von
   * Text-Elementen wird true geliefert, falls der aktuelle Wert nicht der leere
   * String ist. Im Falle eines Buttons oder eines anderen Elements das keinen
   * sinnvollen Wert hat, wird immer false geliefert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean getBoolean();

  /**
   * Setzt den aktuellen Wert dieses UI Elements (falls möglich) auf str. Falls es
   * sich um ein boolesches Element (Checkbox etc) handelt, so wird der String "true"
   * (ohne Berücksichtigung von Gross-/Kleinschreibung) als true und jeder andere
   * String als false betrachtet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setString(String str);

  /**
   * Liefert die ID dieses UIElements oder "" wenn nicht gesetzt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public String getId();

  /**
   * Setzt eine neue Hintergrundfarbe für das UIElement.
   */
  public void setBackground(Color bg);
  
  public void setEnabled(boolean enabled);

  /**
   * Setzt die Sichtbarkeit der Komponente und ihres Zusatzlabels (falls vorhanden).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setVisible(boolean vis);

  /**
   * Liefert true, wenn das Element keine Änderungen erlaubt (z,B, ein Separator oder
   * ein Label).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean isStatic();

  /**
   * Liefert true, wenn dieses UIElement im Moment den Eingabefokus hat.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public boolean hasFocus();

  /**
   * Sagt dem UIElement, es soll versuchen, den Eingabefokus zu übernehmen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void takeFocus();

}
