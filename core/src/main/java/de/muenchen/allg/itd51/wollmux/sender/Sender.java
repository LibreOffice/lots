/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.sender;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import de.muenchen.allg.itd51.wollmux.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.db.Dataset;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * A sender, which provides information to fill documents.
 */
public class Sender
{

  public static final String NACHNAME = "Nachname";

  public static final String VORNAME = "Vorname";

  public static final String ROLLE = "Rolle";

  protected String key;

  /**
   * Bildet Spaltennamen auf (String-)Werte ab. Die Daten in myLOS repräsentieren den lokalen
   * Override, die in myBS die (gecachten) Daten aus der Hintergrunddatenbank. myLOS kann null sein,
   * dann wird der Datensatz als nicht aus dem LOS kommend betrachtet.
   */
  protected Map<String, String> overridenValues;

  protected Dataset dataset;

  protected boolean selected;

  /**
   * New sender without overridden values.
   *
   * @param dataset
   *          The data set from the database.
   */
  public Sender(Dataset dataset)
  {
    this(null, dataset, new HashMap<>());
  }

  /**
   * New sender without database entry.
   *
   * @param dsoverride
   *          The values of the sender.
   */
  public Sender(Map<String, String> dsoverride)
  {
    this(null, null, dsoverride);
  }

  /**
   * New sender.
   *
   * @param key
   *          The key of sender. If {@code null} the key is equal to the key of the data set or a
   *          random UUID.
   * @param dataset
   *          The data set from the database.
   * @param dsoverride
   *          The overridden values.
   */
  public Sender(String key, Dataset dataset, Map<String, String> dsoverride)
  {
    this.dataset = dataset;
    this.overridenValues = dsoverride;
    this.selected = false;
    this.key = key;
    if (key == null)
    {
      if (dataset == null)
      {
        this.key = UUID.randomUUID().toString();
      } else
      {
        this.key = dataset.getKey();
      }
    }
  }

  /**
   * Copy of a sender.
   *
   * @param copy
   *          The original.
   */
  public Sender(Sender copy)
  {
    this(copy.key, copy.dataset, new HashMap<>(copy.overridenValues));
    this.selected = copy.selected;
  }

  public String getKey()
  {
    return key;
  }

  public Dataset getDataset()
  {
    return dataset;
  }

  public Map<String, String> getOverridenValues()
  {
    return overridenValues;
  }

  public boolean isSelected()
  {
    return selected;
  }

  public void setSelected(boolean selected)
  {
    this.selected = selected;
  }

  /**
   * Get the value for a column.
   *
   * @param columnName
   *          The name of the column.
   * @return If the column is overridden the overridden value is returned. Otherwise the data set is
   *         asked for the value. If there is no data set or the data set has no such column,
   *         {@code null} is returned.
   */
  public String get(String columnName)
  {
    String res = overridenValues.get(columnName);
    if (res != null)
    {
      return res;
    }
    return getFromDatabase(columnName);
  }

  /**
   * Get the value for a column of the data source not matter if the values is overridden.
   *
   * @param columnName
   *          The name of the column.
   * @return The value of the data set. If there is no data set or the data set has no such column,
   *         {@code null} is returned.
   */
  public String getFromDatabase(String columnName)
  {
    if (dataset != null)
    {
      try
      {
        return dataset.get(columnName);
      } catch (ColumnNotFoundException e)
      {
        return null;
      }
    }
    return null;
  }

  /**
   * Override a column of sender.
   *
   * @param columnName
   *          The column to set.
   * @param newValue
   *          The new value of the column. Not null.
   * @throws SenderException
   *           If the newValue is {@code null}. Use {@link #drop(String)} to remove columns.
   */
  public void overrideValue(String columnName, String newValue) throws SenderException
  {
    if (newValue == null)
    {
      throw new SenderException(L.m("Override kann nicht null sein"));
    }
    overridenValues.put(columnName, newValue);
  }

  /**
   * Has this sender an entry in the database?
   *
   * @return True if it has an entry, false otherwise.
   */
  public boolean isFromDatabase()
  {
    return dataset != null;
  }

  /**
   * Provide a human readable representation of the sender. It uses the role, first and last name.
   *
   * @return A human readable representation.
   */
  public String getDisplayString()
  {
    StringBuilder stringBuilder = new StringBuilder();

    String rolle = get(ROLLE);
    String nachname = get(NACHNAME);
    String vorname = get(VORNAME);

    stringBuilder.append(rolle == null || rolle.isEmpty() ? "" : "(" + rolle + ") ");
    stringBuilder.append(nachname == null || nachname.isEmpty() ? "" : nachname);
    stringBuilder.append(", ");
    stringBuilder.append(vorname == null || vorname.isEmpty() ? "" : vorname);

    return stringBuilder.toString();
  }

  /**
   * Remove the column from the overridden values. If the sender has an entry in the database, the
   * value from database is returned by {@link #get(String)} afterwards.
   *
   * @param columnName
   *          Name of the column to remove.
   */
  public void drop(String columnName)
  {
    overridenValues.remove(columnName);
  }

  /**
   * Check if any of the overridden columns has a different value than the database entry.
   *
   * @return True if there are overridden columns which differ from the database entry, false
   *         otherwise.
   */
  public boolean isOverriden()
  {
    if (overridenValues == null)
      return false;

    return overridenValues.keySet().stream().anyMatch(this::isColumnOverriden);
  }

  private boolean isColumnOverriden(String column)
  {
    String losValue = overridenValues.get(column);
    if (losValue == null || losValue.isEmpty())
    {
      return false;
    }
    String bsValue;

    if (!isFromDatabase())
    {
      return false;
    }
    try
    {
      bsValue = dataset.get(column);
    } catch (ColumnNotFoundException e)
    {
      return true;
    }

    return !losValue.equals(bsValue);
  }

  /**
   * Comparator for senders. The senders are compared according to the provided column. If a sender
   * returns a {@code null} value for the column it comes at the end.
   *
   * @param column
   *          The column to use for comparison.
   * @return The comparator.
   */
  public static Comparator<Sender> comparatorByColumn(String column)
  {
    Comparator<String> nullLast = Comparator.nullsLast(String::compareTo);
    return Comparator.comparing(s -> s.get(column), nullLast);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(dataset, key, overridenValues, selected);
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    Sender other = (Sender) obj;
    return Objects.equals(dataset, other.dataset) && Objects.equals(key, other.key)
        && Objects.equals(overridenValues, other.overridenValues) && selected == other.selected;
  }
}
