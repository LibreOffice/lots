package de.muenchen.allg.itd51.wollmux.slv.events;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.event.WollMuxEventListener;

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
}
