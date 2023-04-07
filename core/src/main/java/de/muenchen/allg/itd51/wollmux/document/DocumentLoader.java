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
package de.muenchen.allg.itd51.wollmux.document;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.io.XInputStream;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XComponent;
import com.sun.star.uno.Exception;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoHelperException;
import de.muenchen.allg.itd51.wollmux.util.L;

/**
 * Funktionen zum Laden und Einfügen von Dokumenten. Geladene Dokumente werden
 * gecacht.
 */
public class DocumentLoader
{
  private static final Logger LOGGER = LoggerFactory
    .getLogger(DocumentLoader.class);

  private static DocumentLoader instance;
  private LoadingCache<String, ByteBuffer> cache;

  /**
   * Zugriff auf den DocumentLoader als Singleton.
   *
   * @return Singleton-Instanz des DocumentLoaders
   */
  public static DocumentLoader getInstance()
  {
    if (instance == null)
    {
      instance = new DocumentLoader();
    }
    return instance;
  }

  private DocumentLoader()
  {
    cache = CacheBuilder.newBuilder()
      .maximumSize(50)
      .expireAfterAccess(8, TimeUnit.HOURS)
        .build(new CacheLoader<String, ByteBuffer>()
      {
        @Override
          public ByteBuffer load(String url) throws Exception
        {
          return downloadDocument(url);
        }
      });
  }

  private ByteBuffer downloadDocument(String url)
  {
    byte[] buf = null;
    try (InputStream in = new URL(url).openStream())
    {
      buf = IOUtils.toByteArray(in);
    } catch (IOException e)
    {
      LOGGER.error(
        L.m("The template with the URL \"{0}\" could not be opened.", url),
        e);
    }

    return ByteBuffer.wrap(buf);
  }

  /**
   * Lädt ein Dokument und fügt es an der Stelle von target ein. target muss den
   * Service XDocumentInsertable unterstützen.
   *
   * @param target
   * @param path   URL des Dokuments
   */
  public void insertDocument(Object target, String path)
  {
    try
    {
      XInputStream in = getDocumentStream(path);
      UNO.XDocumentInsertable(target).insertDocumentFromURL(path,
        new PropertyValue[] {
          new PropertyValue("InputStream", -1, in, PropertyState.DIRECT_VALUE),
          new PropertyValue("FilterName", -1, "StarOffice XML (Writer)",
            PropertyState.DIRECT_VALUE)
        });
    } catch (IllegalArgumentException | com.sun.star.io.IOException | ExecutionException e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Lädt ein Dokument und öffnet es.
   *
   * @param path
   *          URL des Dokuments
   * @param asTemplate
   *          behandelt das Dokument als Template
   * @param allowMacros
   *          erlaubt die Ausführung von Makros
   *
   * @return Das geladene Dokument.
   */
  public XComponent loadDocument(String path, boolean asTemplate,
    boolean allowMacros)
  {
    try
    {
      XInputStream in = getDocumentStream(path);
      return UNO.loadComponentFromURL(path, asTemplate, allowMacros,
          new PropertyValue("InputStream", -1, in, PropertyState.DIRECT_VALUE));
    } catch (UnoHelperException | ExecutionException e)
    {
      LOGGER.error("", e);
    }

    return null;
  }

  public boolean hasDocument(String path)
  {
    return cache.getIfPresent(path) != null;
  }

  public XInputStream getDocumentStream(String path) throws ExecutionException
  {
    ByteBuffer buf = cache.get(path);
    return new ByteBufferInputStream(buf);
  }
}
