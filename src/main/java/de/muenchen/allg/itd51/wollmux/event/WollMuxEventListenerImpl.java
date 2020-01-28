package de.muenchen.allg.itd51.wollmux.event;

import com.google.common.eventbus.Subscribe;

import de.muenchen.allg.itd51.wollmux.event.handlers.OnAbout;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAddDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnAddPALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCloseAndOpenExt;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCloseTextDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnCollectNonWollMuxFormFieldsViaPrintModel;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnDumpInfo;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnExecutePrintFunction;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFocusFormField;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFormControllerInitCompleted;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFormValueChanged;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFormularMax4000Returned;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFormularMax4000Show;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnFunctionDialog;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnJumpToMark;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnJumpToPlaceholder;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnKill;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnManagePrintFunction;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnNotifyDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnOpenDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPALChangedNotify;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPrint;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnPrintPage;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnProcessTextDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRegisterDispatchInterceptor;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRemoveDocumentEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnRemovePALChangeEventListener;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnReprocessTextDocument;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSaveAs;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSaveTempAndOpenExt;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetFormValue;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetFormValueFinished;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetInsertValues;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetSender;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetVisibleState;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnSetWindowVisible;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnShowDialogAbsenderAuswaehlen;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnShowDialogPersoenlicheAbsenderlisteVerwalten;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextDocumentClosed;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnTextbausteinEinfuegen;
import de.muenchen.allg.itd51.wollmux.event.handlers.OnUpdateInputFields;

public class WollMuxEventListenerImpl implements WollMuxEventListener
{

  public WollMuxEventListenerImpl()
  {
  }

  @Subscribe
  public void onTextbausteinEinfuegen(OnTextbausteinEinfuegen event)
  {
    event.process();
  }

  @Subscribe
  public void onShowDialogPersoenlicheAbsenderlisteVerwalten(
      OnShowDialogPersoenlicheAbsenderlisteVerwalten event)
  {
    event.process();
  }

  @Subscribe
  public void onShowDialogAbsenderAuswaehlen(
      OnShowDialogAbsenderAuswaehlen event)
  {
    event.process();
  }

  @Subscribe
  public void onSetWindowVisible(OnSetWindowVisible event)
  {
    event.process();
  }

  @Subscribe
  public void onSetSender(OnSetSender event)
  {
    event.process();
  }

  @Subscribe
  public void onSetInsertValues(OnSetInsertValues event)
  {
    event.process();
  }

  @Subscribe
  public void onSetFormValueFinished(OnSetFormValueFinished event)
  {
    event.process();
  }

  @Subscribe
  public void onSaveTempAndOpenExt(OnSaveTempAndOpenExt event)
  {
    event.process();
  }

  @Subscribe
  public void onSaveAs(OnSaveAs event)
  {
    event.process();
  }

  @Subscribe
  public void onReprocessTextDocument(OnReprocessTextDocument event)
  {
    event.process();
  }

  @Subscribe
  public void onRemovePALChangeEventListener(
      OnRemovePALChangeEventListener event)
  {
    event.process();
  }

  @Subscribe
  public void onProcessTextDocument(OnProcessTextDocument event)
  {
    event.process();
  }

  @Subscribe
  public void onPrintPage(OnPrintPage event)
  {
    event.process();
  }

  @Subscribe
  public void onPALChangedNotify(OnPALChangedNotify event)
  {
    event.process();
  }

  @Subscribe
  public void onOpenDocument(OnOpenDocument event)
  {
    event.process();
  }

  @Subscribe
  public void onManagePrintFunction(OnManagePrintFunction event)
  {
    event.process();
  }

  @Subscribe
  public void onKill(OnKill event)
  {
    event.process();
  }

  @Subscribe
  public void onJumpToPlaceholder(OnJumpToPlaceholder event)
  {
    event.process();
  }

  @Subscribe
  public void onJumpToMark(OnJumpToMark event)
  {
    event.process();
  }

  @Subscribe
  public void onFunctionDialog(OnFunctionDialog event)
  {
    event.process();
  }

  @Subscribe
  public void onFormValueChanged(OnFormValueChanged event)
  {
    event.process();
  }

  @Subscribe
  public void onFormularMax4000Show(OnFormularMax4000Show event)
  {
    event.process();
  }

  @Subscribe
  public void onFormularMax4000Returned(OnFormularMax4000Returned event)
  {
    event.process();
  }

  @Subscribe
  public void onFormControllerInitCompleted(OnFormControllerInitCompleted event)
  {
    event.process();
  }

  @Subscribe
  public void onFocusFormField(OnFocusFormField event)
  {
    event.process();
  }

  @Subscribe
  public void onDumpInfo(OnDumpInfo event)
  {
    event.process();
  }

  @Subscribe
  public void onAddPALChangeEventListener(OnAddPALChangeEventListener event)
  {
    event.process();
  }

  @Subscribe
  public void onCloseAndOpenExt(OnCloseAndOpenExt event)
  {
    event.process();
  }

  @Subscribe
  public void onAddDocumentEventListener(OnAddDocumentEventListener event)
  {
    event.process();
  }

  @Subscribe
  public void onCloseTextDocument(OnCloseTextDocument event)
  {
    event.process();
  }

  @Subscribe
  public void onCollectNonWollMuxFormFieldsViaPrintModel(
      OnCollectNonWollMuxFormFieldsViaPrintModel event)
  {
    event.process();
  }

  @Subscribe
  public void onExecutePrintFunction(OnExecutePrintFunction event)
  {
    event.process();
  }

  @Subscribe
  public void onNotifyDocumentEventListener(OnNotifyDocumentEventListener event)
  {
    event.process();
  }

  @Subscribe
  public void onPrint(OnPrint event)
  {
    event.process();
  }

  @Subscribe
  public void onRegisterDispatchInterceptor(OnRegisterDispatchInterceptor event)
  {
    event.process();
  }

  @Subscribe
  public void onRemoveDocumentEventListener(OnRemoveDocumentEventListener event)
  {
    event.process();
  }

  @Subscribe
  public void onSetFormValue(OnSetFormValue event)
  {
    event.process();
  }

  @Subscribe
  public void onSetVisibleState(OnSetVisibleState event)
  {
    event.process();
  }

  @Subscribe
  public void onTextDocumentClosed(OnTextDocumentClosed event)
  {
    event.process();
  }

  @Subscribe
  public void onAbout(OnAbout event)
  {
    event.process();
  }

  @Subscribe
  public void onUpdateInputFields(OnUpdateInputFields event)
  {
    event.process();
  }

}
