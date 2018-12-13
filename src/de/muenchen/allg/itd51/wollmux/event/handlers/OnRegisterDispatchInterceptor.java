package de.muenchen.allg.itd51.wollmux.event.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.frame.XFrame;

import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.document.FrameController;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.event.DispatchProviderAndInterceptor;

/**
 * Erzeugt ein neues WollMuxEvent zum Registrieren eines (frischen)
 * {@link DispatchProviderAndInterceptor} auf frame.
 *
 * @param frame
 *          der {@link XFrame} auf den der {@link DispatchProviderAndInterceptor}
 *          registriert werden soll.
 */
public class OnRegisterDispatchInterceptor extends BasicEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnRegisterDispatchInterceptor.class);

  private TextDocumentController documentController;

  public OnRegisterDispatchInterceptor(
      TextDocumentController documentController)
  {
    this.documentController = documentController;
  }

  @Override
  protected void doit()
  {
    FrameController fc = documentController.getFrameController();
    if (fc.getFrame() == null)
    {
      LOGGER.debug(L.m("Ignoriere handleRegisterDispatchInterceptor(null)"));
      return;
    }
    try
    {
      DispatchProviderAndInterceptor
          .registerDocumentDispatchInterceptor(fc.getFrame());
    } catch (java.lang.Exception e)
    {
      LOGGER.error(L.m("Kann DispatchInterceptor nicht registrieren:"), e);
    }

    // Sicherstellen, dass die Schaltflächen der Symbolleisten aktiviert werden:
    try
    {
      fc.getFrame().contextChanged();
    } catch (java.lang.Exception e)
    {
      LOGGER.error("", e);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#"
        + documentController.getFrameController().getFrame().hashCode() + ")";
  }
}
