package de.muenchen.uno;

public class UnoReflectionException extends RuntimeException
{
  private static final long serialVersionUID = 1L;

  public UnoReflectionException()
  {
    super();
  }

  public UnoReflectionException(String msg)
  {
    super(msg);
  }

  public UnoReflectionException(Throwable e)
  {
    super(e);
  }

  public UnoReflectionException(String msg, Throwable e)
  {
    super(msg, e);
  }

  public UnoReflectionException(String msg, Throwable e, boolean enableSuppression,
      boolean writableStackTrace)
  {
    super(msg, e, enableSuppression, writableStackTrace);
  }

}
