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

import de.muenchen.allg.itd51.wollmux.core.db.Dataset;

/**
 * Ein DatasetChecker, der 2 andere Checker auswertet und die oder-Verknüpfung ihrer
 * matches() Ergebnisse liefert.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class OrDatasetChecker implements DatasetChecker
{
  private DatasetChecker check1;

  private DatasetChecker check2;

  public OrDatasetChecker(DatasetChecker check1, DatasetChecker check2)
  {
    this.check1 = check1;
    this.check2 = check2;
  }

  @Override
  public boolean matches(Dataset ds)
  {
    return check1.matches(ds) || check2.matches(ds);
  }
}
