/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.libreoffice.lots.db.Dataset;
import org.libreoffice.lots.db.DatasetPredicate;
import org.libreoffice.lots.db.QueryPart;
import org.libreoffice.lots.db.SimpleDataset;

public class DatasetPredicateTest
{
  @Test
  public void makePredicateValidEntry() throws Exception
  {
    List<QueryPart> query = List.of(
        new QueryPart("Vorname", "*Sheldon"), new QueryPart("Vorname", "Sheldon"), // Valid
        new QueryPart("Vorname", "Sheldon*"));
    Predicate<Dataset> pred = DatasetPredicate.makePredicate(query);
    Dataset ds = new SimpleDataset("Test", Map.of("Vorname", "Sheldon"));
    assertTrue(pred.test(ds), "Given dataset does not match with a given QueryPart:");
  }

  @Test
  public void makePredicateInValidEntry() throws Exception
  {
    List<QueryPart> query = List.of(new QueryPart("Vorname", "*Sheldon"), new QueryPart("Vorname", "Sheldon*"));
    Predicate<Dataset> pred = DatasetPredicate.makePredicate(query);
    Dataset ds = new SimpleDataset("Test", Map.of("Vorname", "Sheldon"));
    assertTrue(pred.test(ds), "Given dataset does not match with a given QueryPart:");
  }

  @Test
  public void makePredicateIgnoreCase() throws Exception
  {
    List<QueryPart> query = List.of(new QueryPart("Vorname", "sheldon"));
    Predicate<Dataset> pred = DatasetPredicate.makePredicate(query);
    Dataset ds = new SimpleDataset("Test", Map.of("Vorname", "Sheldon"));
    assertTrue(pred.test(ds));

    query = List.of(new QueryPart("Vorname", "Sheldon"));
    pred = DatasetPredicate.makePredicate(query);
    ds = new SimpleDataset("Test", Map.of("Vorname", "sheldon"));
    assertTrue(pred.test(ds));
  }

  @Test
  public void makePredicateCheckCount() throws Exception
  {
    List<QueryPart> query = List.of(new QueryPart("Vorname", "*Sheldon"), new QueryPart("Vorname", "Sheldon*"),
        new QueryPart("Vorname", "*Sheldon*"), new QueryPart("Vorname", "*Sheldon**"),
        new QueryPart("Vorname", "**Sheldon*"), new QueryPart("Vorname", "**Sheldon**"));
    Predicate<Dataset> pred = DatasetPredicate.makePredicate(query);

    List<Dataset> datasetList = List.of(new SimpleDataset("Test", Map.of("Vorname", "Sheldon")),
        new SimpleDataset("Test", Map.of("Vorname", "Lennard")));

    assertEquals(1, datasetList.stream().filter(pred::test).count(),
        "Given datasets have a wrong match count with given QueryParts:");
  }

  @Test
  public void unknwonColumn()
  {
    List<QueryPart> query = List.of(new QueryPart("Vorname", "*Sheldon"), new QueryPart("Vorname", "Sheldon*"));
    Predicate<Dataset> pred = DatasetPredicate.makePredicate(query);
    Dataset ds = new SimpleDataset("Test", Map.of("Nachname", "Couper"));
    assertFalse(pred.test(ds), "Given dataset does not match with a given QueryPart:");
  }

  @Test
  public void matchAll()
  {
    Predicate<Dataset> pred = DatasetPredicate.matchAll;
    List<Dataset> datasetList = List.of(new SimpleDataset("Test", Map.of("Vorname", "Sheldon")),
        new SimpleDataset("Test", Map.of("Vorname", "Sheldon*")),
        new SimpleDataset("Test", Map.of("Vorname", "**Sheldon**")));
    assertEquals(3, datasetList.stream().filter(pred::test).count(),
        "Given datasets have a wrong match count with given QueryParts:");
  }

}
