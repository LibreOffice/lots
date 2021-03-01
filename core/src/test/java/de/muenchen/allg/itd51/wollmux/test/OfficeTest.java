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
package de.muenchen.allg.itd51.wollmux.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Properties;

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
    try
    {
      ArrayList<String> options = new ArrayList<>();
      options.add("--headless");
      options.add("--norestore");
      options.add("--nocrashreport");
      options.add("--nolockcheck");
      //Properties prop = new Properties();
      //prop.load(OfficeTest.class.getClassLoader().getResourceAsStream("libreoffice.properties"));
      //options.add("-env:UserInstallation=file://" + prop.getProperty("office.profile"));
      UNO.init(options);
    } catch (Exception e)
    {
      fail("Can't start office", e);
    }
  }

  @AfterAll
  public static void terminateDesktop() throws Exception
  {
    if (!UNO.desktop.terminate())
    {
      fail();
    }
  }

  /**
   * Load a component as hidden document without macros.
   *
   * @param filename
   *          The name of the component to load.
   * @param template
   *          If true, create a new document form the file.
   * @param hidden
   *          If true, don't create windows and frames.
   * @return The component.
   * @throws UnoHelperException
   *           Component can't be loaded.
   */
  public static XComponent loadComponent(String filename, boolean template, boolean hidden) throws UnoHelperException
  {
    return UNO.loadComponentFromURL(filename, template, false, hidden);
  }
}
