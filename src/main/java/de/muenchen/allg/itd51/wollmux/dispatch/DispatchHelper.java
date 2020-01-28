package de.muenchen.allg.itd51.wollmux.dispatch;

/**
 * Perform operations on a dispatch.
 */
public interface DispatchHelper
{
  /**
   * Call original dispatch with original URL and arguments.
   */
  void dispatchOriginal();

  /**
   * Send a result to the listener of this dispatch.
   * 
   * @param success
   *          Was the operation successful?
   */
  void dispatchFinished(final boolean success);
}
