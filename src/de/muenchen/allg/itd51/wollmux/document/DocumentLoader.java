package de.muenchen.allg.itd51.wollmux.document;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;

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
import com.sun.star.lib.uno.Proxy;
import com.sun.star.uno.Exception;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.util.L;

public class DocumentLoader
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(DocumentLoader.class);

  private LoadingCache<URL, ByteBuffer> cache;

  public static DocumentLoader getInstance()
  {
    return new DocumentLoader();
  }

  private DocumentLoader()
  {
    cache = CacheBuilder.newBuilder()
        .build(new CacheLoader<URL, ByteBuffer>()
        {
          @Override
          public ByteBuffer load(URL url) throws Exception
          {
            return loadDocument(url);
          }
        });
  }

  private ByteBuffer loadDocument(URL url)
  {
    byte[] buf = null;
    try (InputStream in = url.openStream())
    {
      buf = IOUtils.toByteArray(in);
    }
    catch (IOException e)
    {
      LOGGER.error(L.m(
          "Die Vorlage mit der URL '%1' kann nicht geöffnet werden.", url), e);
    }

    return ByteBuffer.wrap(buf);
  }

  public void insertDocument(Proxy target, String path)
  {
    try
    {
      ByteBuffer buf = cache.getUnchecked(new URL(path));
      XInputStream in = new ByteBufferInputStream(buf);
      UNO.XDocumentInsertable(target).insertDocumentFromURL(path,
          new PropertyValue[] { new PropertyValue("InputStream", -1, in,
              PropertyState.DIRECT_VALUE) });

    }
    catch (MalformedURLException | IllegalArgumentException
        | com.sun.star.io.IOException e)
    {
      LOGGER.error("", e);
    }
  }
}
