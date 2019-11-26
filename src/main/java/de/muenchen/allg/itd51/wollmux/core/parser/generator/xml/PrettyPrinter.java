package de.muenchen.allg.itd51.wollmux.core.parser.generator.xml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Prints some data to a writer line by line.
 * 
 * @author daniel.sikeler
 */
public class PrettyPrinter
{

  /** The writer to write the data. */
  private final BufferedWriter writer;
  /** The line to be written. */
  private StringBuilder line;
  /** The indention. */
  private static final String INDENTION = "  ";
  /** The current indention. */
  private final StringBuilder indent = new StringBuilder();

  /**
   * Create a new PrettyPrinter.
   * 
   * @param writer
   *          The writer to use for writing the data.
   */
  public PrettyPrinter(final Writer writer)
  {
    this.writer = new BufferedWriter(writer);
    this.line = new StringBuilder();
  }

  /** Increase the indention. */
  public void indent()
  {
    indent.append(INDENTION);
  }

  /** Decrease the indention. */
  public void removeIndent()
  {
    indent.delete(0, INDENTION.length());
  }

  /**
   * Add some data to the line.
   * 
   * @param data
   *          The data.
   */
  public void add2Line(final String data)
  {
    line.append(data);
  }

  /**
   * Print the line with indention.
   * 
   * @throws IOException
   *           Line couldn't be written.
   */
  public void print() throws IOException
  {
    writer.write(indent.toString() + line.toString());
    writer.newLine();
    line = new StringBuilder();
  }

  /**
   * Causes data to be written immediately.
   * 
   * @throws IOException
   *           Data couldn't be written.
   */
  public void flush() throws IOException
  {
    writer.flush();
  }

  /**
   * Close the printer and underlying streams.
   * 
   * @throws IOException
   *           Printer couldn't be closed.
   */
  public void close() throws IOException
  {
    writer.close();
  }

}
