package de.muenchen.allg.itd51.wollmux.core.db;

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
          return Pattern.matches(part.getSearchString().replaceAll("\\*", ""),
              ds.get(part.getColumnName()));
        } catch (ColumnNotFoundException ex)
        {
          return false;
        }
      };

      return pred;
    }).reduce(matchAll, Predicate::and);
  }
}
