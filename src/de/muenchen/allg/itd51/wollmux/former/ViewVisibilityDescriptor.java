/*
 * Dateiname: ViewVisibilityDescriptor.java
 * Projekt  : WollMux
 * Funktion : Speichert Informationen dazu, welche Teile von Views sichtbar sein sollen.
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
  
  /**
   * Anzeigen des TOOLTIPs in der
   * {@link de.muenchen.allg.itd51.wollmux.former.control.OneFormControlLineView}.
   */
  public boolean formControlLineViewTooltip = false;

  /**
   * Anzeigen des READONLYs in der
   * {@link de.muenchen.allg.itd51.wollmux.former.control.OneFormControlLineView}.
   */
  public boolean formControlLineViewReadonly = false;


  public ViewVisibilityDescriptor()
  {};

  /**
   * Copy Constructor.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public ViewVisibilityDescriptor(ViewVisibilityDescriptor orig)
  {
    this.formControlLineViewId = orig.formControlLineViewId;
    this.formControlLineViewLabel = orig.formControlLineViewLabel;    
    this.formControlLineViewType = orig.formControlLineViewType;
    this.formControlLineViewAdditional = orig.formControlLineViewAdditional;
    this.formControlLineViewTooltip = orig.formControlLineViewTooltip;
    this.formControlLineViewReadonly = orig.formControlLineViewReadonly;
  };
}
