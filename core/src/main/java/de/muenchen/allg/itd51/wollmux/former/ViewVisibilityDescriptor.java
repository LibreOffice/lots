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
  {
  }

  /**
   * Copy Constructor.
   */
  public ViewVisibilityDescriptor(ViewVisibilityDescriptor orig)
  {
    this.formControlLineViewId = orig.formControlLineViewId;
    this.formControlLineViewLabel = orig.formControlLineViewLabel;
    this.formControlLineViewType = orig.formControlLineViewType;
    this.formControlLineViewAdditional = orig.formControlLineViewAdditional;
    this.formControlLineViewTooltip = orig.formControlLineViewTooltip;
    this.formControlLineViewReadonly = orig.formControlLineViewReadonly;
  }
}
