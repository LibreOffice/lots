package de.muenchen.allg.itd51.wollmux.core.parser.scanner;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a file and and produces a list of tokens in order with the appearance
 * in the file.
 *
 * @author Daniel Sikeler daniel.sikeler@muenchen.de
 */
public class Tokenizer implements Iterator<Token>, Closeable
{

  private static final Logger LOGGER = LoggerFactory.getLogger(Tokenizer.class);

  /** Number of bytes of UTF-8 ordering mark. */
  private static final int BYTE_ORDERING_MARK_LENGTH = 4;
  /** The file reader. */
  private final BufferedReader reader;
  /** The current line to tokenize. */
  private String line;
  /** The file from which this scanner reads. */
  private final URL filename;

  /**
   * Create a new Tokenizer and read the UTF-8 byte ordering mark if there is
   * one.
   *
   * @param filename
   *          The file to scan.
   * @throws ScannerException
   *           The Tokenizer can't be initialized, because there is no file or
   *           it can't be read.
   */
  Tokenizer(final URL filename) throws ScannerException
  {
    try
    {
      this.filename = filename;
      reader = new BufferedReader(new InputStreamReader(filename.openStream(),
          Charset.forName("UTF-8")));
      reader.mark(BYTE_ORDERING_MARK_LENGTH);
      if ('\ufeff' != reader.read())
      {
        reader.reset();
      }
      line = reader.readLine();
    } catch (final FileNotFoundException e)
    {
      throw new ScannerException(e);
    } catch (final IOException e)
    {
      throw new ScannerException("The BOM can't be read", e);
    }
  }

  /**
   * Create a new Tokenizer and read the UTF-8 byte ordering mark if there is
   * one.
   *
   * @param stream
   *          The stream to scan.
   * @throws ScannerException
   *           The Tokenizer can't be initialized, because there is no stream or
   *           it can't be read.
   */
  Tokenizer(final InputStream stream) throws ScannerException
  {
    try
    {
      reader = new BufferedReader(new InputStreamReader(stream,
          Charset.forName("UTF-8")));
      reader.mark(BYTE_ORDERING_MARK_LENGTH);
      if ('\ufeff' != reader.read())
      {
        reader.reset();
      }
      line = reader.readLine();
      this.filename = new URL("file:.");
    } catch (final FileNotFoundException e)
    {
      throw new ScannerException(e);
    } catch (final IOException e)
    {
      throw new ScannerException("The BOM can't be read", e);
    }
  }

  public URL getFilename()
  {
    return filename;
  }

  @Override
  public void close() throws ScannerException
  {
    try
    {
      reader.close();
    } catch (final IOException e)
    {
      throw new ScannerException("The file reader can't be closed", e);
    }
  }

  @Override
  public boolean hasNext()
  {
    return line != null;
  }

  @Override
  public Token next()
  {
    // Read the next line if this was the last token in the line.
    if (line.isEmpty())
    {
      try
      {
        line = reader.readLine();
        if (line == null)
        {
          return new Token("", TokenType.END_FILE);
        } else
        {
          return next();
        }
      } catch (final IOException e)
      {
        LOGGER.error("Die Konfigurationsdatei konnte nicht gelesen werden.", e);
        throw new NoSuchElementException("File can't be read");
      }
    }

    Token token = parseLine();
    if (token != null)
    {
      return token;
    }

    throw new NoSuchElementException("No more tokens");
  }

  /**
   * Extracts the next token from the current line.
   *
   * @return The next token.
   */
  private Token parseLine()
  {
    for (final TokenType tokenType : TokenType.values())
    {
      if (!tokenType.hasRegex())
      {
        continue;
      }
      final Matcher matcher = tokenType.getRegex().matcher(line);
      if (matcher.find())
      {
        final int end = matcher.end();
        final String content = line.substring(0, end);
        line = line.substring(end, line.length());
        if (tokenType == TokenType.NEW_FILE)
        {
          return new Token(content.split("\"")[1], tokenType);
        } else if (tokenType == TokenType.WHITESPACE)
        {
          return next();
        } else
        {
          return new Token(content, tokenType);
        }
      }
    }
    return null;
  }

  /** This method isn't supported. */
  @Override
  public void remove()
  {
    throw new UnsupportedOperationException("Tokens can't be deleted");
  }

}
