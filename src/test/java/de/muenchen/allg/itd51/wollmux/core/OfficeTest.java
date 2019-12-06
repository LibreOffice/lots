package de.muenchen.allg.itd51.wollmux.core;

import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

import de.muenchen.allg.afid.UNO;

@Category(OfficeTests.class)
public abstract class OfficeTest
{
  @BeforeClass
  public static void setUpBefore() throws Exception
  {
    ArrayList<String> options = new ArrayList<>();
    options.add("--headless");
    options.add("--norestore");
    options.add("--nocrashreport");
    options.add("--nolockcheck");
    UNO.init(options);
  }

  @AfterClass
  public static void tearDownAfter() throws Exception
  {
    UNO.desktop.terminate();
  }
}
