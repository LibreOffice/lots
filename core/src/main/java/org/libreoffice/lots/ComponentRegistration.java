/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots;

import java.util.ServiceLoader;

import org.libreoffice.lots.comp.WollMux;

import com.sun.star.comp.loader.FactoryHelper;
import com.sun.star.comp.loader.JavaLoader;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.registry.XRegistryKey;

/**
 * A service for registering and creating UNO Service components.
 *
 * This class name has be passed as 'RegistrationClassName' in the manifest to
 * be called by LibreOffice.
 */
public interface ComponentRegistration
{
  /**
   * get the class to register.
   *
   * @return A class implementing a component.
   */
  Class<?> getComponent();

  /**
   * get the name of the class.
   *
   * @return The class name.
   */
  String getName();

  /**
   * Get a list of all services supported by this component.
   *
   * @return A list of services.
   */
  String[] getServiceNames();

  /**
   * Provides a component factory and is needed by
   * {@link JavaLoader#activate(String, String, String, XRegistryKey)}.
   *
   * @param sImplName
   *          The implementation name.
   * @return The factory for the component.
   */
  @SuppressWarnings("java:S100")
  static XSingleComponentFactory __getComponentFactory(String sImplName)
  {
    com.sun.star.lang.XSingleComponentFactory xFactory = null;
    for (ComponentRegistration component : ServiceLoader
        .load(ComponentRegistration.class, WollMux.class.getClassLoader()))
    {
      if (sImplName.equals(component.getName()))
      {
	xFactory = Factory.createComponentFactory(component.getComponent(),
	    component.getServiceNames());
	break;
      }
    }

    return xFactory;
  }

  /**
   * Registers a component in a registry under a given root key. It is called
   * during extension installation.
   *
   * This method is called by
   * {@link JavaLoader#writeRegistryInfo(XRegistryKey, String, String)}.
   *
   * @param xRegKey
   *          The root key.
   * @return {@link Factory#writeRegistryServiceInfo(String, String[], XRegistryKey)}
   */
  @SuppressWarnings({ "java:S100", "java:S1148" })
  static boolean __writeRegistryServiceInfo(XRegistryKey xRegKey)
  {
    try
    {
      for (ComponentRegistration component : ServiceLoader
          .load(ComponentRegistration.class, ComponentRegistration.class.getClassLoader()))
      {
	FactoryHelper.writeRegistryServiceInfo(component.getName(),
	    component.getServiceNames(), xRegKey);
      }
      return true;
    } catch (Exception t)
    {

      // print exception on stderr to provide information during extension
      // installation.
      t.printStackTrace();
      return false;
    }
  }
}
