package de.muenchen.allg.itd51.wollmux.core.document;

/**
 * Bei einer Textersetzung (z.B. aus einer Variable oder beim insertFrag) kam es
 * zu einer Endlosschleife.
 * 
 * @author Christoph Lutz (D-III-ITD 5.1)
 */
public class EndlessLoopException extends Exception
{
  private static final long serialVersionUID = -3679814069994462633L;

  public EndlessLoopException(String msg)
  {
    super(msg);
  }
}
