/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.mailmerge.ifthenelse;

/**
 * Comparator which can be used in If-Then-Else functions.
 */
public enum TestType
{
  STRCMP("genau =", "STRCMP"),
  NUMCMP("numerisch =", "NUMCMP"),
  LT("numerisch <", "LT"),
  LE("numerisch <=", "LE"),
  GT("numerisch >", "GT"),
  GE("numerisch >=", "GE"),
  MATCH("regulärer Ausdruck", "MATCH");

  private final String label;

  private final String func;

  private TestType(String label, String func)
  {
    this.label = label;
    this.func = func;
  }

  public String getLabel()
  {
    return label;
  }

  public String getFunc()
  {
    return func;
  }
}
