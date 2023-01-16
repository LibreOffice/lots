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
package de.muenchen.allg.itd51.wollmux.db;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 *
 * Delivers Predicates for Datasets.
 *
 */
public class DatasetPredicate
{
  public static final Predicate<Dataset> matchAll = (Dataset ds) -> true;

  private DatasetPredicate()
  {
  }

  /**
   * Matches a List of {@link QueryPart} against a given {@link Dataset}. Invalid * character in
   * QueryPart's search string are filtered out.
   *
   * @param query
   *          List of {@link QueryPart}
   * @return Predicate for a dataset.
   */
  public static Predicate<Dataset> makePredicate(List<QueryPart> query)
  {
    return query.stream().map(part -> {
      @SuppressWarnings("squid:S1488")
      Predicate<Dataset> pred = (Dataset ds) -> {
        try
        {
          return Pattern.compile(part.getSearchString().replace("*", "").toLowerCase())
              .matcher(ds.get(part.getColumnName()).toLowerCase())
              .find();
        } catch (ColumnNotFoundException ex)
        {
          return false;
        }
      };

      return pred;
    }).reduce(matchAll, Predicate::and);
  }
}
