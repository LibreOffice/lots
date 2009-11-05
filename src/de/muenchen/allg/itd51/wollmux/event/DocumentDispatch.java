/*
 * Dateiname: DocumentDispatch.java
 * Projekt  : WollMux
 * Funktion : Implementiert XDispatch und kann alle Dispatch-URLs behandeln, die ein DocumentModel erfordern.
 * 
 * Copyright (c) 2009 Landeshauptstadt München
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL), 
 * version 1.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see 
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 05.11.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 * 
 */
package de.muenchen.allg.itd51.wollmux.event;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;

import de.muenchen.allg.itd51.wollmux.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;

/**
 * Implementiert XDispatch und kann alle Dispatch-URLs behandeln, die ein
 * DocumentModel erfordern. Nähere Infos zur Funktionsweise siehe {@link Dispatch}.
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class DocumentDispatch extends Dispatch
{
  /**
   * Ein XDispatch-Objekt, das die ursprüngliche Standard-Aktion der URL url
   * 
   */
  private XDispatch origDisp;

  /**
   * Zu origDisp passender URL um das Standardverhalten auszulösen.
   */
  private com.sun.star.util.URL origUrl;

  /**
   * Das Textdokument auf den sich alle Dispatches beziehen.
   */
  private TextDocumentModel model;

  /**
   * Erzeugt einen neuen DocumentDispatch.
   * 
   * @param origDisp
   *          Ein XDispatch-Objekt, das die ursprüngliche Standard-Aktion der URL url
   *          auslösen kann.
   * @param origUrl
   *          Zu origDisp passender URL um das Standardverhalten auszulösen.
   * @param model
   *          das {@link TextDocumentModel} in dessen Kontext der Dispatch ausgeführt
   *          werden soll.
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   */
  public DocumentDispatch(XDispatch origDisp, com.sun.star.util.URL origUrl,
      TextDocumentModel model)
  {
    this.origDisp = origDisp;
    this.origUrl = origUrl;
    this.model = model;
  }

  public void dispatch__uno_print(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handlePrint(model, origDisp, origUrl, props);
  }

  public void dispatch__uno_printdefault(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handlePrint(model, origDisp, origUrl, props);
  }

  public void dispatch_wollmux_functiondialog(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleFunctionDialog(model, arg);
  }

  public void dispatch_wollmux_formularmax4000(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleFormularMax4000Show(model);
  }

  public void dispatch_wollmux_ziffereinfuegen(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleButtonZifferEinfuegenPressed(model);
  }

  public void dispatch_wollmux_abdruck(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleButtonAbdruckPressed(model);
  }

  public void dispatch_wollmux_zuleitungszeile(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleButtonZuleitungszeilePressed(model);
  }

  public void dispatch_wollmux_markblock(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleMarkBlock(model, arg);
  }

  public void dispatch_wollmux_textbausteineinfuegen(String arg,
      PropertyValue[] props)
  {
    WollMuxEventHandler.handleTextbausteinEinfuegen(model, true);
  }

  public void dispatch_wollmux_platzhalteranspringen(String arg,
      PropertyValue[] props)
  {
    WollMuxEventHandler.handleJumpToPlaceholder(model);
  }

  public void dispatch_wollmux_textbausteinverweiseinfuegen(String arg,
      PropertyValue[] props)
  {
    WollMuxEventHandler.handleTextbausteinEinfuegen(model, false);
  }

  public void dispatch_wollmux_seriendruck(String arg, PropertyValue[] props)
  {
    WollMuxEventHandler.handleSeriendruck(model, false);
  }

  public void dispatch_wollmux_test(String arg, PropertyValue[] props)
  {
    if (WollMuxFiles.installQATestHandler()) TestHandler.doTest(model, arg);
  }

}
