package de.muenchen.allg.itd51.wollmux.core.parser;


/**
 * Handles the different name formats for files, which are included.
 * 
 * @author Daniel Sikeler
 */
public final class PathProcessor
{

  private PathProcessor()
  {
  }

  /**
   * Process include instruction. Extracts the file path and the protocol.
   * 
   * @param url
   *          The URL of the file.
   * @return A string representation of the file.
   */
  public static String processInclude(final String url)
  {
    String path = url;
    final String filePrefix = "file:";
    final String[] prefix = { "///", "//localhost/", "/", "//" };
    if (path.startsWith(filePrefix))
    {
      path = path.substring(filePrefix.length());
    }
    for (int index = 0; index < prefix.length; index++)
    {
      if (path.startsWith(prefix[index]))
      {
        return trim(path, prefix[index].length());
      }
    }
    return path;
  }

  /**
   * Remove a offset from a String. If it is a Linux-System removes one char
   * less than offset.
   * 
   * @param string
   *          The string to trim.
   * @param offset
   *          The number of chars to remove.
   * @return A new String starting at offset in string.
   */
  private static String trim(final String string, final int offset)
  {
    if (System.getProperty("os.name").toLowerCase().contains("win"))
    {
      return string.substring(offset);
    } else
    {
      return string.substring(offset - 1);
    }
  }
}
