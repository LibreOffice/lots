/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2020 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.event;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnAbout;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnActivateSidebar;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAddDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCloseAndOpenExt;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCloseTextDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCollectNonWollMuxFormFieldsViaPrintModel;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnDumpInfo;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnExecutePrintFunction;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFormValueChanged;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFormularMax4000Show;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFunctionDialog;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnJumpToMark;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnJumpToPlaceholder;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnKill;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnManagePrintFunction;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnNotifyDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnOpenDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPrint;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPrintPage;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRemoveDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRemoveFormularMax;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnReprocessTextDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnResetDocumentState;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSaveAs;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSaveTempAndOpenExt;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetFormValue;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetFormValueFinished;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetInsertValues;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetVisibleState;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetWindowVisible;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextDocumentClosed;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextbausteinEinfuegen;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnUpdateInputFields;

/**
 * An event listener for all unspecified events.
 */
public class WollMuxEventListenerImpl implements WollMuxEventListener
{
  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onTextbausteinEinfuegen(OnTextbausteinEinfuegen event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onSetWindowVisible(OnSetWindowVisible event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onSetInsertValues(OnSetInsertValues event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onSetFormValueFinished(OnSetFormValueFinished event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onSaveTempAndOpenExt(OnSaveTempAndOpenExt event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onSaveAs(OnSaveAs event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onReprocessTextDocument(OnReprocessTextDocument event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onPrintPage(OnPrintPage event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onOpenDocument(OnOpenDocument event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onManagePrintFunction(OnManagePrintFunction event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onKill(OnKill event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onJumpToPlaceholder(OnJumpToPlaceholder event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onJumpToMark(OnJumpToMark event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onFunctionDialog(OnFunctionDialog event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onFormValueChanged(OnFormValueChanged event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onFormularMax4000Show(OnFormularMax4000Show event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onFormularMax4000Returned(OnRemoveFormularMax event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onFormControllerInitCompleted(OnResetDocumentState event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onDumpInfo(OnDumpInfo event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onCloseAndOpenExt(OnCloseAndOpenExt event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onAddDocumentEventListener(OnAddDocumentEventListener event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onCloseTextDocument(OnCloseTextDocument event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onCollectNonWollMuxFormFieldsViaPrintModel(
      OnCollectNonWollMuxFormFieldsViaPrintModel event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onExecutePrintFunction(OnExecutePrintFunction event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onNotifyDocumentEventListener(OnNotifyDocumentEventListener event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onPrint(OnPrint event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onRemoveDocumentEventListener(OnRemoveDocumentEventListener event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onSetFormValue(OnSetFormValue event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onSetVisibleState(OnSetVisibleState event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onTextDocumentClosed(OnTextDocumentClosed event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onAbout(OnAbout event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onUpdateInputFields(OnUpdateInputFields event)
  {
    event.process();
  }

  /**
   * Execute the event
   *
   * @param event
   *          The event.
   */
  @Subscribe
  public void onActivateSidebar(OnActivateSidebar event)
  {
    event.process();
  }

}
