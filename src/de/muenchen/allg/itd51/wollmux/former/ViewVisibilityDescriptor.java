/*
* Dateiname: ViewVisibilityDescriptor.java
* Projekt  : WollMux
* Funktion : Speichert Informationen dazu, welche Teile von Views sichtbar sein sollen.
* 
* Copyright: Landeshauptstadt München
*
* Änderungshistorie:
* Datum      | Wer | Änderungsgrund
* -------------------------------------------------------------------
* 19.07.2007 | BNK | Erstellung
* -------------------------------------------------------------------
*
* @author Matthias Benkmann (D-III-ITD 5.1)
* @version 1.0
* 
*/
package de.muenchen.allg.itd51.wollmux.former;

/**
 * Speichert Informationen dazu, welche Teile von Views sichtbar sein sollen.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ViewVisibilityDescriptor
{
  /**
   * Anzeigen der ID in der 
   * {@link de.muenchen.allg.itd51.wollmux.former.control.OneFormControlLineView}.
   */
  public boolean formControlLineViewId = true;
  
  /**
   * Anzeigen des LABELs in der 
   * {@link de.muenchen.allg.itd51.wollmux.former.control.OneFormControlLineView}.
   */
  public boolean formControlLineViewLabel = true;
  
  /**
   * Anzeigen des TYPEs in der 
   * {@link de.muenchen.allg.itd51.wollmux.former.control.OneFormControlLineView}.
   */
  public boolean formControlLineViewType = true;

  /**
   * Anzeigen zusätzlicher Elemente in der  
   * {@link de.muenchen.allg.itd51.wollmux.former.control.OneFormControlLineView},
   * die von der Art des Controls abhängen.
   */
  public boolean formControlLineViewAdditional = true;
  
  public ViewVisibilityDescriptor(){};
  
  /**
   * Copy Constructor.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ViewVisibilityDescriptor(ViewVisibilityDescriptor orig)
  {
    this.formControlLineViewId = orig.formControlLineViewId;
    this.formControlLineViewLabel = orig.formControlLineViewLabel;
    this.formControlLineViewType = orig.formControlLineViewType;
    this.formControlLineViewAdditional = orig.formControlLineViewAdditional;
  };
}
