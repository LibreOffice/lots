package de.muenchen.allg.itd51.wollmux.test;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import com.sun.star.lang.XComponent;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoHelperException;

@Tag("de.muenchen.allg.itd51.wollmux.test.OfficeTest")
public abstract class OfficeTest
{
  @BeforeAll
  public static void initUNO() throws Exception
  {
    ArrayList<String> options = new ArrayList<>();
    options.add("--headless");
    options.add("--norestore");
    options.add("--nocrashreport");
    options.add("--nolockcheck");
    UNO.init(options);
  }

  @AfterAll
  public static void terminateDesktop() throws Exception
  {
    UNO.desktop.terminate();
  }

  /**
   * Load a component as hidden document without macros.
   *
   * @param filename
   *          The name of the component to load.
   * @return The component.
   * @throws UnoHelperException
   *           Component can't be loaded.
   */
  public static XComponent loadComponent(String filename) throws UnoHelperException
  {
    return UNO.loadComponentFromURL(filename, false, false, true);
  }
}
