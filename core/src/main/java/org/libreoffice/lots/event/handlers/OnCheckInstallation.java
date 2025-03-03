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
package org.libreoffice.lots.event.handlers;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.util.XStringSubstitution;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.util.UnoComponent;
import org.libreoffice.lots.WollMuxFehlerException;
import org.libreoffice.lots.dialog.InfoDialog;
import org.libreoffice.lots.util.L;

/**
 * Event for checking if WollMux is correctly installed. There should be only one installation.
 */
public class OnCheckInstallation extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnCheckInstallation.class);

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    // default values
    String title = L.m("Multiple WollMux installations");
    String msg = L.m("A system wide and a local WollMux installation were found "
        + "(or the remains of an incomplete deinstallation).\n"
        + "This constellation can cause obscure errors.\n\nRemove one of the two installations.\n\n"
        + "More information regarding the affected paths can be found in the wollmux.log.");
    String logMsg = msg;

    // get all installations
    List<Triple<String, Date, Boolean>> wmInsts = getInstallations();

    // get newest installation
    Optional<Triple<String, Date, Boolean>> recentInst = wmInsts.stream()
        .sorted((t1, t2) -> t1.getMiddle().compareTo(t2.getMiddle())).findFirst();

    // get older installations
    String otherInstsList = wmInsts.stream()
        .sorted((t1, t2) -> t1.getMiddle().compareTo(t2.getMiddle())).skip(1)
        .map(t -> "- " + t.getLeft()).collect(Collectors.joining("\n"));

    if (recentInst.isPresent() && wmInsts.size() > 1)
    {
      logMsg += "\n"
          + "The newest WollMux installation is located under:"
          + "\n "
          + recentInst.get().getLeft() + "\n"
          + "Furthermore the following WollMux installations were found:"
          + "\n" + otherInstsList;
      LOGGER.error(logMsg);
      InfoDialog.showInfoModal(title, msg);
    }
  }

  /**
   * Find all installations of WollMux.
   *
   * @return List of installations (name, date, shared).
   */
  private List<Triple<String, Date, Boolean>> getInstallations()
  {
    List<Triple<String, Date, Boolean>> wmInstallations = new ArrayList<>();

    try
    {
      XStringSubstitution xSS = UNO.XStringSubstitution(
          UnoComponent.createComponentWithContext(UnoComponent.CSS_UTIL_PATH_SUBSTITUTION));
      // user installations
      String myPath = xSS.substituteVariables("$(user)/uno_packages/cache/uno_packages/", true);
      // shared installations
      String oooPath = xSS.substituteVariables("$(inst)/share/uno_packages/cache/uno_packages/",
          true);
      String oooPathNew = xSS.substituteVariables(
          "$(brandbaseurl)/share/uno_packages/cache/uno_packages/",
          true);

      if (myPath == null || oooPath == null)
      {
        LOGGER.error("Determination of the installation path for the WollMux package failed.");
        return wmInstallations;
      }

      findWollMuxInstallations(wmInstallations, myPath, false);
      findWollMuxInstallations(wmInstallations, oooPath, true);
      if (oooPathNew != null)
      {
        findWollMuxInstallations(wmInstallations, oooPathNew, true);
      }
    } catch (NoSuchElementException e)
    {
      LOGGER.error("", e);
    }

    return wmInstallations;
  }

  /**
   * Find directories which contain WollMux.oxt.
   *
   * @param wmInstallations
   *          Information about the directory are added to this list.
   * @param path
   *          The path to search for directories.
   * @param isShared
   *          Contains the path shared installations.
   */
  private static void findWollMuxInstallations(List<Triple<String, Date, Boolean>> wmInstallations,
      String path, boolean isShared)
  {
    try
    {
      URI uriPath = new URI(path);

      File[] installedPackages = new File(uriPath).listFiles();
      if (installedPackages != null)
      {
        Arrays.stream(installedPackages).filter(File::isDirectory)
            .forEach(dir -> Arrays.stream(dir.listFiles()).filter(File::isDirectory)
                .filter(f -> f.getName().startsWith("WollMux.")).forEach(file -> {
                  // name of directory containing WollMux.oxt
                  String directoryName = file.getAbsolutePath();
                  // last modified date of directory containing WollMux.oxt
                  Date directoryDate = new Date(file.lastModified());
                  wmInstallations
                      .add(new ImmutableTriple<>(directoryName, directoryDate, isShared));
                }));
      }
    } catch (URISyntaxException e)
    {
      LOGGER.error("", e);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "()";
  }
}
