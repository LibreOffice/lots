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
package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.lang.XComponent;

import org.libreoffice.ext.unohelper.common.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.config.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.config.InvalidIdentifierException;
import de.muenchen.allg.itd51.wollmux.document.DocumentLoader;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.document.VisibleTextFragmentList;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Event for loading and opening a file.
 */
public class OnOpenDocument extends WollMuxEvent
{
  private static final Logger LOGGER = LoggerFactory.getLogger(OnOpenDocument.class);

  private boolean asTemplate;

  private List<String> fragIDs;

  /**
   * Create this event.
   *
   * @param fragIDs
   *          List of fragment IDs. The first ID is the document to open. All other IDs are passed
   *          as argument to the command "insertContent".
   * @param asTemplate
   *          If true opens the template for modification, otherwise a document is created from the
   *          template.
   */
  public OnOpenDocument(List<String> fragIDs, boolean asTemplate)
  {
    this.fragIDs = fragIDs;
    this.asTemplate = asTemplate;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    if (!fragIDs.isEmpty())
    {
      openTextDocument(fragIDs, asTemplate);
    }
  }

  /**
  *
  * @param fragIDs
  * @param asTemplate
  * @throws WollMuxFehlerException
  */
  private void openTextDocument(List<String> fragIDs,
      boolean asTemplate) throws WollMuxFehlerException
  {
    // das erste Argument ist das unmittelbar zu landende Textfragment und
    // wird nach urlStr aufgelöst. Alle weiteren Argumente (falls vorhanden)
    // werden nach argsUrlStr aufgelöst.
    String loadUrlStr = "";
    String[] fragUrls = new String[fragIDs.size() - 1];
    String urlStr = "";

    Iterator<String> iter = fragIDs.iterator();
    for (int i = 0; iter.hasNext(); ++i)
    {
      String frag_id = iter.next();

      // Fragment-URL holen und aufbereiten:
      List<String> urls = new ArrayList<>();

      java.lang.Exception error = new ConfigurationErrorException(L.m(
          "The textfragment with the FRAG_ID \"{0}\" is not defined!",
          frag_id));
      try
      {
        urls = VisibleTextFragmentList
            .getURLsByID(WollMuxFiles.getWollmuxConf(), frag_id);
      } catch (InvalidIdentifierException e)
      {
        error = e;
      }
      if (urls.isEmpty())
      {
        throw new WollMuxFehlerException(
            L.m("The URL for the text fragment with the FRAG_ID \"{0}\" cannot be determined:",
                frag_id),
            error);
      }

      // Nur die erste funktionierende URL verwenden. Dazu werden alle URL zu
      // dieser FRAG_ID geprüft und in die Variablen loadUrlStr und fragUrls
      // übernommen.
      StringBuilder  errors = new StringBuilder();
      boolean found = false;
      Iterator<String> iterUrls = urls.iterator();
      while (iterUrls.hasNext() && !found)
      {
        urlStr = iterUrls.next();

        // URL erzeugen und prüfen, ob sie aufgelöst werden kann
        try
        {
          URL url = WollMuxFiles.makeURL(urlStr);
          urlStr = UNO.getParsedUNOUrl(url.toExternalForm()).Complete;
          url = WollMuxFiles.makeURL(urlStr);
          found = DocumentLoader.getInstance().hasDocument(urlStr);
          if (!found)
          {
            WollMuxSingleton.checkURL(url);
            found = true;
          }
        } catch (MalformedURLException e)
        {
          LOGGER.info("", e);
          errors.append(L.m("The URL \"{0}\" is invalid:", urlStr)).append("\n").append(e.getLocalizedMessage())
              .append("\n\n");
          continue;
        } catch (IOException e)
        {
          LOGGER.info("", e);
          errors.append(e.getLocalizedMessage()).append("\n\n");
          continue;
        }
      }

      if (!found)
      {
        throw new WollMuxFehlerException(L.m(
            "The text fragment with the FRAG_ID \"{0}\" cannot be resolved:",
            frag_id)
            + "\n\n" + errors);
      }

      // URL in die in loadUrlStr (zum sofort öffnen) und in argsUrlStr (zum
      // später öffnen) aufnehmen
      if (i == 0)
      {
        loadUrlStr = urlStr;
      } else
      {
        fragUrls[i - 1] = urlStr;
      }
    }

    // open document as Template (or as document):
    TextDocumentController documentController = null;
    try
    {
      XComponent doc = DocumentLoader.getInstance().loadDocument(loadUrlStr, asTemplate, true);

      if (UNO.XTextDocument(doc) != null)
      {
        documentController = DocumentManager
            .getTextDocumentController(UNO.XTextDocument(doc));
        documentController.getModel().setFragUrls(fragUrls);
      }
    } catch (java.lang.Exception x)
    {
      // sollte eigentlich nicht auftreten, da bereits oben geprüft.
      throw new WollMuxFehlerException(L.m(
          "The template with the URL \"{0}\" could not be opened.",
          loadUrlStr), x);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "("
        + ((asTemplate) ? "asTemplate" : "asDocument") + ", " + fragIDs + ")";
  }
}
