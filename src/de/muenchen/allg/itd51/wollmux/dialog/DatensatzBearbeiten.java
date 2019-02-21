/*
 * Dateiname: DatensatzBearbeiten.java
 * Projekt  : WollMux
 * Funktion : Dynamisches Erzeugen eines Swing-GUIs für das Bearbeiten eines Datensatzes anhand von ConfigThingy
 *
 * Copyright (c) 2010-2018 Landeshauptstadt München
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
 * 11.10.2005 | BNK | Erstellung
 * 14.10.2005 | BNK | Interaktion mit DJDataset
 * 14.10.2005 | BNK | Kommentiert
 * 17.10.2005 | BNK | Unterstützung für immer ausgegraute Buttons.
 * 17.10.2005 | BNK | Unterstützung für READONLY
 * 18.10.2005 | BNK | Zusätzliche Exceptions loggen
 * 24.10.2005 | BNK | dialogEndListener wird am Ende aufgerufen
 *                  | show() entfernt zur Vermeidung von Thread-Problemen
 * 24.10.2005 | BNK | restoreStandard() Buttons nicht mehr ausgegraut, wenn
 *                  | Werte nicht geändert wurden, aber bereits aus dem LOS sind.
 * 27.10.2005 | BNK | back + CLOSEACTION
 * 02.11.2005 | BNK | +saveAndBack()
 * 15.11.2005 | BNK | Endlosschleife beseitigt durch vertauschen der || Operanden
 * 22.11.2005 | BNK | Common.setLookAndFeel() verwenden
 * 11.01.2006 | BNK | EDIT "true" bei comboboxen unterstützt
 * 25.01.2006 | BNK | Auch editierbare Comboboxen ändern nun den Hintergrund korrekt.
 * 19.04.2006 | BNK | [R1337]Fehlermeldung, bei unbekanntem TYPE
 * 15.05.2006 | BNK | nicht-editierbare Comboboxen funktionieren jetzt hoffentlich
 *                  | richtig mit Vorgabewerten, die nicht in der Liste sind.
 * 29.09.2006 | BNK | Verbessertes Auslesen von ComboBox-Daten
 * 23.07.2006 | BNK | [R2551][23097]Scrollbar machen
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XDialog;
import com.sun.star.awt.XWindow;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.Wizard;
import com.sun.star.ui.dialogs.WizardButton;
import com.sun.star.ui.dialogs.XWizard;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.NoBackingStoreException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventHandler;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen Dialogbeschreibung
 * einen (mehrseitigen) Dialog zur Bearbeitung eines
 * {@link de.muenchen.allg.itd51.wollmux.db.DJDataset}s. <b>ACHTUNG:</b> Die
 * private-Funktionen dürfen NUR aus dem Event-Dispatching Thread heraus aufgerufen
 * werden.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class DatensatzBearbeiten
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DatensatzBearbeiten.class);

  /**
   * Wie {@link #DatensatzBearbeiten(ConfigThingy, DJDataset, ActionListener)} mit
   * null als dialogEndListener.
   *
   * @param conf
   * @param datensatz
   * @param edit
   * @throws ConfigurationErrorException
   */
  public DatensatzBearbeiten(DJDataset datensatz)
  {
      XWizard wizard = Wizard.createSinglePathWizard(UNO.defaultContext, DatensatzBearbeitenWizardController.PATHS, new DatensatzBearbeitenWizardController(datensatz));
      // disable help button does not work currently
      wizard.enableButton(WizardButton.HELP, false);
      wizard.setTitle("Datensatz bearbeiten");
      short result = wizard.execute();
      
      if (result == ExecutableDialogResults.OK) {
        WollMuxEventHandler.getInstance().handlePALChangedNotify();
        LOGGER.debug("Datensatz bearbeiten: DatensatzBearbeiten(): ExecutableDialogResult.OK");
      } else {
        LOGGER.debug("Datensatz bearbeiten: DatensatzBearbeiten(): ExecutableDialogResult.CANCEL");
      }
  }
}
