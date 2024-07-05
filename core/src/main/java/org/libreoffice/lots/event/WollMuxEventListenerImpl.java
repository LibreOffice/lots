/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
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
package org.libreoffice.lots.event;

import org.libreoffice.lots.event.handlers.OnAbout;
import org.libreoffice.lots.event.handlers.OnActivateSidebar;
import org.libreoffice.lots.event.handlers.OnAddDocumentEventListener;
import org.libreoffice.lots.event.handlers.OnCloseAndOpenExt;
import org.libreoffice.lots.event.handlers.OnCloseTextDocument;
import org.libreoffice.lots.event.handlers.OnCollectNonWollMuxFormFieldsViaPrintModel;
import org.libreoffice.lots.event.handlers.OnDumpInfo;
import org.libreoffice.lots.event.handlers.OnExecutePrintFunction;
import org.libreoffice.lots.event.handlers.OnFormValueChanged;
import org.libreoffice.lots.event.handlers.OnFormularMax4000Show;
import org.libreoffice.lots.event.handlers.OnFunctionDialog;
import org.libreoffice.lots.event.handlers.OnJumpToMark;
import org.libreoffice.lots.event.handlers.OnJumpToPlaceholder;
import org.libreoffice.lots.event.handlers.OnKill;
import org.libreoffice.lots.event.handlers.OnManagePrintFunction;
import org.libreoffice.lots.event.handlers.OnNotifyDocumentEventListener;
import org.libreoffice.lots.event.handlers.OnOpenDocument;
import org.libreoffice.lots.event.handlers.OnPrint;
import org.libreoffice.lots.event.handlers.OnPrintPage;
import org.libreoffice.lots.event.handlers.OnRemoveDocumentEventListener;
import org.libreoffice.lots.event.handlers.OnRemoveFormularMax;
import org.libreoffice.lots.event.handlers.OnReprocessTextDocument;
import org.libreoffice.lots.event.handlers.OnResetDocumentState;
import org.libreoffice.lots.event.handlers.OnSaveAs;
import org.libreoffice.lots.event.handlers.OnSaveTempAndOpenExt;
import org.libreoffice.lots.event.handlers.OnSetFormValue;
import org.libreoffice.lots.event.handlers.OnSetFormValueFinished;
import org.libreoffice.lots.event.handlers.OnSetInsertValues;
import org.libreoffice.lots.event.handlers.OnSetVisibleState;
import org.libreoffice.lots.event.handlers.OnSetWindowVisible;
import org.libreoffice.lots.event.handlers.OnTextDocumentClosed;
import org.libreoffice.lots.event.handlers.OnTextbausteinEinfuegen;
import org.libreoffice.lots.event.handlers.OnUpdateInputFields;

import com.google.common.eventbus.Subscribe;

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
