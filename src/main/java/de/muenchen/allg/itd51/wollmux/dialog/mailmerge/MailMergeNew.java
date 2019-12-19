/*
 * Dateiname: MailMergeNew.java
 * Projekt  : WollMux
 * Funktion : Die neuen erweiterten Serienbrief-Funktionalitäten
 *
 * Copyright (c) 2010-2019 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
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
 * 11.10.2007 | BNK | Erstellung
 * 25.05.2010 | ERT | Aufruf von PDFGesamtdruck-Druckfunktion
 * 20.12.2010 | ERT | Bei ungültigem indexSelection.rangeEnd wird der
 *                    Wert auf den letzten Datensatz gesetzt
 * 08.05.2012 | jub | um beim serienbrief/emailversand die auswahl zwischen odt und pdf
 *                    anhängen anbieten zu können, sendAsEmail() und saveToFile() mit
 *                    einer flage versehen, die zwischen den beiden formaten
 *                    unterscheidet.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Die neuen erweiterten Serienbrief-Funktionalitäten.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MailMergeNew
{

  /**
   * Falls nicht null wird dieser Listener aufgerufen nachdem der MailMergeNew
   * geschlossen wurde.
   */
  private ActionListener abortListener = null;

  /**
   * Stellt die Felder und Datensätze für die Serienbriefverarbeitung bereit.
   */
  private MailMergeDatasource ds;

  public MailMergeDatasource getDs()
  {
    return ds;
  }

  /**
   * Die zentrale Klasse, die die Serienbrieffunktionalität bereitstellt.
   *
   * @param documentController
   *          das {@link TextDocumentModel} an dem die Toolbar hängt.
   */
  public MailMergeNew(TextDocumentController documentController, ActionListener abortListener)
  {
    this.ds = new MailMergeDatasource(documentController);
    this.abortListener = abortListener;
  }

  /**
   * Schliesst den MailMergeNew und alle zugehörigen Fenster.
   */
  public void dispose()
  {
    if (abortListener != null)
      abortListener.actionPerformed(new ActionEvent(this, 0, ""));
  }
}
