/*
 * Dateiname: MailMergeParams.java
 * Projekt  : WollMux
 * Funktion : Dialoge zur Bestimmung der Parameter für den wirklichen Merge (z.B. ob in Gesamtdokument oder auf Drucker geschrieben werden soll.)
 * 
 * Copyright (c) 2008-2019 Landeshauptstadt München
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
 * 25.05.2010 | ERT | GUI für PDF-Gesamtdruck
 * 20.12.2010 | ERT | Defaultwerte für Druckdialog von ... bis
 * 08.05.2012 | jub | vorgeschlagener name für den anhang eines serienbrief/emailversands
 *                    kommt ohne endung, da für den nutzer auswahl zwischen pdf/odt
 *                    möglich ist
 * 23.01.2014 | loi | Für den Seriendruck einen Wollmux Druckerauswahl Dialog eingefugt,
 *                    da der LO Dialog Druckeroptionen zur Auswahl bietet, die im Druck
 *                    nicht umgesetz werden.
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.dialog.TextComponentTags;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeController.ACTION;
import de.muenchen.allg.itd51.wollmux.sidebar.controls.UIElementAction;

/**
 * Dialoge zur Bestimmung der Parameter für den wirklichen Merge (z.B. ob in
 * Gesamtdokument oder auf Drucker geschrieben werden soll.)
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class MailMergeParams
{

  private static final Logger LOGGER = LoggerFactory.getLogger(MailMergeParams.class);

  /**
   * Tag für {@link TextComponentTags}, das als Platzhalter für die Serienbriefnummer
   * steht.
   */
  public static final String TAG_SERIENBRIEFNUMMER = "#SB";

  /**
   * Tag für {@link TextComponentTags}, das als Platzhalter für die Datensatznummer
   * steht.
   */
  public static final String TAG_DATENSATZNUMMER = "#DS";
  
  /**
   * Enthält den String der im Attribut VALUE zur zuletzt ausgeführten
   * {@link UIElementAction#setActionType}-Action angegeben war. Beispiel:
   *
   * Wird in der GUI das Formularelement '(LABEL "Gesamtdokument erstellen" TYPE
   * "radio" ACTION "setActionType" VALUE "gesamtdok")' ausgewählt, dann enthält
   * diese Variable den Wert "gesamtdok".
   */
  private ACTION currentActionType = ACTION.NOTHING;

  /**
   * Enthält den String der im Attribut VALUE zur zuletzt ausgeführten
   * {@link UIElementAction#setOutput}-Action angegeben war. Beispiel:
   *
   * Wird in der GUI das Formularelement '(LABEL "ODT-Datei" TYPE "radio" GROUP "odt"
   * ACTION "setOutput" VALUE "odt")' ausgewählt, dann enthält diese Variable den
   * Wert "odt".
   */
  private String currentOutput = "";

  /**
   * Enthält die Namen der über das zuletzt ausgeführte
   * {@link RuleStatement#USE_PRINTFUNCTIONS} -Statement gesetzten PrintFunctions.
   */
  private List<String> usePrintFunctions = new ArrayList<>();

  /**
   * Enthält den Wert des zuletzt ausgeführten
   * {@link RuleStatement#IGNORE_DOC_PRINTFUNCTIONS}-Statements
   */
  private Boolean ignoreDocPrintFuncs;

  /**
   * Enthält den String, der als Vorbelegung im Formularfeld für das
   * Absender-Email-Feld gesetzt wird.
   */
  private String defaultEmailFrom = "";
}
