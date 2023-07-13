/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.slv.events;

import org.libreoffice.lots.event.WollMuxEventListener;
import org.libreoffice.lots.event.handlers.OnSetVisibleState;
import org.libreoffice.lots.slv.ContentBasedDirectiveModel;

import com.google.common.eventbus.Subscribe;

/**
 * Event listener for content based directive events.
 *
 * There should be only one instance of this class (singleton-pattern).
 */
public class ContentBasedDirectiveEventListener implements WollMuxEventListener
{
  /**
   * Called when this service is instantiated.
   */
  public ContentBasedDirectiveEventListener()
  {
    // nothing to initialize
  }

  /**
   * Event handler for {@link OnChangeDirective} event.
   *
   * @param event
   *          A {@link OnChangeDirective} event.
   */
  @Subscribe
  public void onChangeDirective(OnChangeDirective event)
  {
    event.process();
  }

  /**
   * Event handler for {@link OnMarkBlock} event.
   *
   * @param event
   *          A {@link OnMarkBlock} event.
   */
  @Subscribe
  public void onMarkBlock(OnMarkBlock event)
  {
    event.process();
  }

  /**
   * Event handler for {@link OnChangeCopy} event.
   *
   * @param event
   *          A {@link OnChangeCopy} event.
   */
  @Subscribe
  public void onChangeCopy(OnChangeCopy event)
  {
    event.process();
  }

  /**
   * Event handler for {@link OnChangeRecipient} event.
   *
   * @param event
   *          A {@link OnChangeRecipient} event.
   */
  @Subscribe
  public void onChangeRecipient(OnChangeRecipient event)
  {
    event.process();
  }

  /**
   * Event handler for {@link OnSetPrintBlocksPropsViaPrintModel} event.
   *
   * @param event
   *          A {@link OnSetPrintBlocksPropsViaPrintModel} event.
   */
  @Subscribe
  public void onSetPrintBlocksPropsViaPrintModel(OnSetPrintBlocksPropsViaPrintModel event)
  {
    event.process();
  }

  /**
   * Event handler for {@link OnSetVisibleState} event.
   *
   * @param event
   *          A {@link OnSetVisibleState} event.
   */
  @Subscribe
  public void onSetVisibleState(OnSetVisibleState event)
  {
    ContentBasedDirectiveModel.createModel(event.getDocumentController()).adoptNumbers();
  }
}
