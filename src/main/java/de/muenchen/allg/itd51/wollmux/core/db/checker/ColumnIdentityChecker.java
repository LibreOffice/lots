/*
 * Copyright (c) 2008-2015 Landeshauptstadt München
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
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.core.db.checker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.db.Dataset;

/**
 * Ein DatasetChecker, der Datensätze darauf überprüft, ob sie einen exakten String
 * (allerdings CASE-INSENSITIVE) in einer Spalte haben.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class ColumnIdentityChecker implements DatasetChecker
{

  private static final Logger LOGGER = LoggerFactory.getLogger(ColumnIdentityChecker.class);

  private String columnName;

  private String compare;

  public ColumnIdentityChecker(String columnName, String compareValue)
  {
    this.columnName = columnName;
    this.compare = compareValue.toLowerCase();
  }

  @Override
  public boolean matches(Dataset ds)
  {
    try
    {
      return ds.get(columnName).equalsIgnoreCase(compare);
    }
    catch (Exception e)
    {
      LOGGER.trace("", e);
      return false;
    }
  }
}
