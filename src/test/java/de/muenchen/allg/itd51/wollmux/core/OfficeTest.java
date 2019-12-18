package de.muenchen.allg.itd51.wollmux.core;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import de.muenchen.allg.afid.UNO;

@Tag("de.muenchen.allg.itd51.wollmux.core.OfficeTests")
public abstract class OfficeTest
{
  @BeforeAll
  public static void setUpBefore() throws Exception
  {
    ArrayList<String> options = new ArrayList<>();
    options.add("--headless");
    options.add("--norestore");
    options.add("--nocrashreport");
    options.add("--nolockcheck");
    UNO.init(options);
  }

  @AfterAll
  public static void tearDownAfter() throws Exception
  {
    UNO.desktop.terminate();
  }
}
