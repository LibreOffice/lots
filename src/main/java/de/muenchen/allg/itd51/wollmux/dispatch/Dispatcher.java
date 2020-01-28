package de.muenchen.allg.itd51.wollmux.dispatch;

import java.util.Arrays;
import java.util.List;

import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.util.URL;

/**
 * A service for creating libreoffice dispatches.
 */
public abstract class Dispatcher
{
  private List<String> commands;

  /**
   * Create a new dispatcher for the given commands.
   *
   * @param commands
   *          List of command strings.
   */
  public Dispatcher(String... commands)
  {
    this.commands = Arrays.asList(commands);
  }

  /**
   * The command defined by the URL.
   *
   * @param url
   *          A command URL.
   * @return The command.
   */
  protected String getDispatchMethodName(URL url)
  {
    return url.Complete.split("#")[0];
  }

  /**
   * Can this dispatcher create dispatches for the given URL.
   *
   * @param url
   *          A command URL.
   * @return True if this dispatcher is capable of creating dispatches.
   */
  public boolean supports(URL url)
  {
    return commands.contains(getDispatchMethodName(url));
  }

  /**
   * Creates a new dispatch for the URL.
   *
   * @param origDisp
   *          The original dispatch.
   * @param url
   *          The command URL.
   * @param frame
   *          The corresponding frame.
   * @return A new dispatch.
   */
  public abstract XDispatch create(XDispatch origDisp, URL url, XFrame frame);
}
