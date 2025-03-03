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
package org.libreoffice.lots.config.scanner;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.libreoffice.lots.config.PathProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a file and included ones and and produces a list of tokens in order
 * with the appearance in the files.
 *
 * @author Daniel Sikeler daniel.sikeler@muenchen.de
 */
public class Scanner implements Iterator<Token>, Closeable
{

  private static final Logger LOGGER = LoggerFactory.getLogger(Scanner.class);

  /** Stack of running tokenizers. */
  private final Deque<Tokenizer> stack = new LinkedList<>();
  /** Is this the first time to read. */
  private boolean isStart;

  /**
   * Create a new ScannerReader and read the UTF-8 byte ordering mark if there
   * is one.
   *
   * @param filename
   *          The URL of the file to be read.
   * @throws ScannerException
   *           The ScannerReader can't be initialized, because there is no file
   *           or it can't be read.
   */
  public Scanner(final URL filename) throws ScannerException
  {
    isStart = true;
    stack.push(new Tokenizer(filename));
  }

  /**
   * Create a new ScannerReader and read the UTF-8 byte ordering mark if there
   * is one.
   *
   * @param stream
   *          The stream to read.
   * @throws ScannerException
   *           The ScannerReader can't be initialized, because there is no file
   *           or it can't be read.
   */
  public Scanner(final InputStream stream) throws ScannerException
  {
    isStart = true;
    stack.push(new Tokenizer(stream));
  }

  @Override
  public void close() throws ScannerException
  {
    try
    {
      for (final Tokenizer tokenizer : stack)
      {
        tokenizer.close();
      }
    } catch (final IOException e)
    {
      throw new ScannerException("The file reader can't be closed", e);
    }
  }

  @Override
  public boolean hasNext()
  {
    return stack.isEmpty() ? false : stack.peek().hasNext();
  }

  @Override
  public Token next()
  {
    if (isStart)
    {
      isStart = false;
      return new Token(stack.peek().getFilename().getFile(), TokenType.NEW_FILE);
    }
    final Token token = stack.peek().next();
    if (token.getType() == TokenType.NEW_FILE)
    {
      try
      {
        stack.push(new Tokenizer(new URL(stack.peek().getFilename(),
            PathProcessor.processInclude(token.getContent()))));
      } catch (final IOException e)
      {
        LOGGER.error("Could not open file for token {}.", token.getContent(), e);
        throw new NoSuchElementException("No more tokens avaiable");
      }
    } else if (token.getType() == TokenType.END_FILE)
    {
      stack.pop();
    }
    return token;
  }

  /** This method isn't supported. */
  @Override
  public void remove()
  {
    throw new UnsupportedOperationException("A token can't be removed");
  }

}
