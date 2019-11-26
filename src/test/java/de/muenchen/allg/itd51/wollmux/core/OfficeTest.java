package de.muenchen.allg.itd51.wollmux.core;

import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

import de.muenchen.allg.afid.UNO;

@Category(OfficeTests.class)
public abstract class OfficeTest
{
  @BeforeClass
  public static void setUpBefore() throws Exception
  {
    UNO.init();
  }
}
