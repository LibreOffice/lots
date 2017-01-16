/*
 * Dateiname: FormController.java
 * Projekt  : WollMux
 * Funktion : Stellt UI bereit, um ein Formulardokument zu bearbeiten.
 * 
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
 * 27.12.2005 | BNK | Erstellung
 * 27.01.2006 | BNK | JFrame-Verwaltung nach FormGUI ausgelagert.
 * 02.02.2006 | BNK | Ein/Ausblendungen begonnen
 * 05.05.2006 | BNK | Condition -> Function, kommentiert
 * 17.05.2006 | BNK | AUTOFILL, PLAUSI, Übergabe an FormModel
 * 18.05.2006 | BNK | Fokus-Änderungen an formModel kommunizieren
 *                  | TIP und HOTKEY bei Tabs unterstützen
 *                  | leere Tabs ausgrauen
 *                  | nextTab und prevTab implementiert
 * 29.05.2006 | BNK | Umstellung auf UIElementFactory.Context
 * 31.05.2006 | BNK | ACTION "funcDialog"
 * 19.06.2006 | BNK | Auch Werte für Felder, die nicht geautofilled sind an FormModel kommunizieren bei Startup
 * 10.09.2006 | BNK | [P1007]Abfangen von mehr als 512 Elementen auf einem Tab.
 * 10.09.2006 | BNK | Tabs scrollen, nicht hintereinander gruppieren.
 * 17.11.2006 | BNK | +setValue()
 * 08.01.2007 | BNK | intelligentere Behandlung der TAB-Taste
 * 28.03.2007 | BNK | Buttonanpassung verarbeiten bei mergeFormDescriptors().
 * 10.12.2007 | BNK | [R3582]Vertikale Scrollbar immer anzeigen
 * 28.04.2008 | BNK | [R19465]Fokus auf erstem Element, nicht auf Tab
 * 08.03.2010 | ERT | [R6331]Scrollfunktion in der wollmux formular gui
 * 02.06.2010 | BED | Unterstützung von ACTION "saveTempAndOpenExt"
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.dialog.Dialog;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.functions.Function;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.Values;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.controls.Textarea;
import de.muenchen.allg.itd51.wollmux.dialog.controls.UIElement;
import de.muenchen.allg.itd51.wollmux.dialog.formmodel.FormModel;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;

/**
 * Stellt UI bereit, um ein Formulardokument zu bearbeiten.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class FormController implements UIElementEventHandler
{
  /**
   * Rand um Textfelder (wird auch für ein paar andere Ränder verwendet) in Pixeln.
   */
  private final static int TF_BORDER = 4;

  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;

  /**
   * GridBagLayout hat eine Begrenzung auf maximal 512 Grid-Elemente pro
   * Koordinatenrichtung, deshalb wird verhindert, dass zuviele Elemente auf einem
   * Tab eingefügt werden, indem maximal soviele wie hier angegeben angezeigt werden.
   * Der Wert hier ist etwas weniger als 512, um Puffer zu bieten für das Hinzufügen
   * von Elementen, die immer vorhanden sein sollen.
   */
  private final static int GRID_MAX = 500;

  /**
   * Die Farbe mit der Felder mit fehlerhafter Eingabe (PLAUSI oder FISHY) eingefärbt
   * werden.
   */
  private Color plausiMarkerColor;

  /**
   * Die Farbe mit der Felder normalerweise eingefärbt sind. Gegenstück zu
   * {@link #plausiMarkerColor}.
   */
  private Color normalColor;

  /**
   * Die JTabbedPane, die die ganzen Tabs der GUI enthält.
   */
  private JTabbedPane myTabbedPane;

  /**
   * tabVisibleCount[i] gibt an, wieviele sichtbare Eingabeelemente (Buttonleiste
   * wird nicht gezählt) das Tab mit Index i hat. ACHTUNG! Muss mit leerem Array
   * starten, weil es ansonsten in increaseTabVisibleCount() eine
   * ArrayIndexOutOfBoundsException gibt!
   */
  private int[] tabVisibleCount = new int[] {};

  /**
   * Die für die Erzeugung der UI Elemente verwendete Factory.
   */
  private UIElementFactory uiElementFactory;

  /**
   * Die Funktionsbibliothek, die für das Interpretieren von Plausis etc,
   * herangezogen wird.
   */
  private FunctionLibrary funcLib;

  /**
   * Die Dialogbibliothek, die die Dialoge liefert, die für die automatische
   * Befüllung von Formularfeldern benötigt werden.
   */
  private DialogLibrary dialogLib;

  /**
   * Ein Kontext für {@link UIElementFactory#createUIElement(UIElementContext, ConfigThingy)},
   * der verwendet wird für das Erzeugen der vertikal angeordneten UI Elemente, die
   * die Formularfelder darstellen.
   */
  private UIElementContext panelContext;

  /**
   * Ein Kontext für {@link UIElementFactory#createUIElement(UIElementContext, ConfigThingy)},
   * der verwendet wird für das Erzeugen der horizontal angeordneten Buttons unter
   * den Formularfeldern.
   */
  private UIElementContext buttonContext;

  /**
   * Der Kontext, in dem Funktionen geparst werden.
   */
  private Map<Object, Object> functionContext;

  /**
   * Bildet IDs auf die dazugehörigen UIElements ab.
   */
  private Map<String, UIElement> mapIdToUIElement = new HashMap<String, UIElement>();

  /**
   * Bildet IDs auf Lists von UIElements ab, deren Plausi vom UIElement ID abhängt.
   */
  private Map<String, List<UIElement>> mapIdToListOfUIElementsWithDependingPlausi =
    new HashMap<String, List<UIElement>>();

  /**
   * Bildet IDs auf Lists von UIElements ab, deren AUTOFILL vom UIElement ID abhängt.
   */
  private Map<String, List<UIElement>> mapIdToListOfUIElementsWithDependingAutofill =
    new HashMap<String, List<UIElement>>();

  /**
   * Bildet Namen von Funktionsdialogen auf Lists von UIElements ab, deren AUTOFILL
   * von diesem Funktionsdialog abhängen.
   */
  private Map<String, List<UIElement>> mapDialogNameToListOfUIElementsWithDependingAutofill =
    new HashMap<String, List<UIElement>>();

  /**
   * Bildet die ID eines UIElements ab auf eine List der Groups, die von Änderungen
   * des UIElements betroffen sind (zum Beispiel weil die Sichtbarkeitsfunktion der
   * Gruppe von dem UIElement abhängt).
   */
  private Map<String, List<Group>> mapIdToListOfDependingGroups =
    new HashMap<String, List<Group>>();

  /**
   * Bildet GROUPS Bezeichner auf die entsprechenden Group-Instanzen ab.
   */
  private Map<String, Group> mapGroupIdToGroup = new HashMap<String, Group>();

  /**
   * Diese Liste enthält alle UIElements.
   */
  private Vector<UIElement> uiElements = new Vector<UIElement>();

  /**
   * Die Inhalte der UIElemente aus {@link #mapIdToUIElement} als Values zur
   * Verfügung gestellt.
   */
  private Values myUIElementValues = new UIElementMapValues(mapIdToUIElement);

  /**
   * Solange dieses Flag false ist, werden Events von UI Elementen ignoriert.
   */
  private boolean processUIElementEvents = false;

  /**
   * Das Writer-Dokument, das zum Formular gehört (gekapselt als FormModel).
   */
  private FormModel formModel;

  /**
   * Wird aufgerufen, wenn eine Aktion einen Abbruch des Dialogs erwirken soll.
   */
  private ActionListener abortRequestListener;

  /**
   * ACHTUNG! Darf nur im Event Dispatching Thread aufgerufen werden.
   * 
   * @param conf
   *          der Formular-Knoten, der die Formularbeschreibung enthält.
   * @param model
   *          das zum Formular gehörende Writer-Dokument (gekapselt als FormModel)
   * @param mapIdToPresetValue
   *          bildet IDs von Formularfeldern auf Vorgabewerte ab. Falls hier ein Wert
   *          für ein Formularfeld vorhanden ist, so wird dieser allen anderen
   *          automatischen Befüllungen vorgezogen. Wird das Objekt {@link TextDocumentModel#FISHY}
   *          als Wert für ein Feld übergeben, so wird dieses Feld speziell markiert
   *          als ungültig bis der Benutzer es manuell ändert.
   * @param functionContext
   *          der Kontext für Funktionen, die einen benötigen.
   * @param funcLib
   *          die Funktionsbibliothek, die zur Auswertung von Plausis etc.
   *          herangezogen werden soll.
   * @param dialogLib
   *          die Dialogbibliothek, die die Dialoge bereitstellt, die für automatisch
   *          zu befüllende Formularfelder benötigt werden.
   * @param abortRequestListener
   *          falls nicht null, wird die
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *          Methode aufgerufen (im Event Dispatching Thread), wenn der Benutzer
   *          eine Aktion aktiviert hat, die das Beenden des Dialogs verursachen
   *          soll. Das actionCommand des ActionEvents gibt die Aktion an, die
   *          verantwortlich ist.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormController(ConfigThingy conf, FormModel model,
      final Map<String, String> mapIdToPresetValue,
      Map<Object, Object> functionContext, FunctionLibrary funcLib,
      DialogLibrary dialogLib, ActionListener abortRequestListener)
      throws ConfigurationErrorException
  {
    this.functionContext = functionContext;
    this.formModel = model;
    this.funcLib = funcLib;
    this.dialogLib = dialogLib;
    this.abortRequestListener = abortRequestListener;

    final ConfigThingy fensterDesc = conf.query("Fenster");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException(L.m("Schlüssel 'Fenster' fehlt in %1",
        conf.getName()));

    initFactories();

    plausiMarkerColor = Color.PINK;
    try
    {
      plausiMarkerColor =
        Color.decode(conf.get("PLAUSI_MARKER_COLOR", 1).getLastChild().toString());
    }
    catch (Exception x)
    {}

    try
    {
      ConfigThingy visibilityDesc = conf.query("Sichtbarkeit");
      if (visibilityDesc.count() > 0)
        visibilityDesc = visibilityDesc.getLastChild();
      final ConfigThingy visDesc = visibilityDesc;
      createGUI(fensterDesc.getLastChild(), visDesc, mapIdToPresetValue);
    }
    catch (Exception x)
    {
      Logger.error(x);
    }

  }

  /**
   * Liefert ein JPanel, das das gesamte GUI des FormControllers enthält.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public JComponent JComponent()
  {
    return myTabbedPane;
  }

  /**
   * Baut die GUI auf (darf nur im EDT aufgerufen werden).
   * 
   * @param fensterDesc
   *          der Fenster()-Knoten der Formularbeschreibung.
   * @param visibilityDesc
   *          der Sichtbarkeit-Knoten der Formularbeschreibung oder ein leeres
   *          ConfigThingy falls der Knoten nicht existiert.
   * @param mapIdToPresetValue
   *          bildet IDs von Formularfeldern auf Vorgabewerte ab. Falls hier ein Wert
   *          für ein Formularfeld vorhanden ist, so wird dieser allen anderen
   *          automatischen Befüllungen vorgezogen. Wird das Objekt {@link TextDocumentModel#FISHY}
   *          als Wert für ein Feld übergeben, so wird dieses Feld speziell markiert
   *          als ungültig bis der Benutzer es manuell ändert.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void createGUI(ConfigThingy fensterDesc, ConfigThingy visibilityDesc,
      Map<String, String> mapIdToPresetValue)
  {
    Common.setLookAndFeelOnce();

    normalColor = new JTextField().getBackground();

    /*
     * ACHTUNG! Wenn die JTabbedPane noch in ein Panel eingebettet werden soll (dazu
     * gibt es im Augenblick allerdings keinen Grund), so muss bei diesem Panel ein
     * BorderLayout o.ä. verwendet werden, damit die Weitergabe des Fenster-Resize
     * ordentlich klappt und die ScrollPane ihre ScrollBalken einblendet.
     */
    // contentPanel = new JPanel();
    myTabbedPane =
      new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
    // contentPanel.add(myTabbedPane);

    /********************************************************************************
     * Tabs erzeugen.
     *******************************************************************************/
    int tabIndex = 0;
    for (ConfigThingy neuesFenster : fensterDesc)
    {
      /*
       * Die folgende Schleife ist nicht nur eleganter als mehrere try-catch-Blöcke
       * um get()-Befehle, sie verhindert auch, dass TIP oder HOTKEY aus Versehen von
       * einem enthaltenen Button aufgeschnappt werden.
       */
      String tabTitle = "Eingabe";
      char hotkey = 0;
      String tip = "";
      Iterator<ConfigThingy> childIter = neuesFenster.iterator();
      while (childIter.hasNext())
      {
        ConfigThingy childConf = childIter.next();
        String name = childConf.getName();
        if (name.equals("TIP"))
          tip = childConf.toString();
        else if (name.equals("TITLE"))
          tabTitle = childConf.toString();
        else if (name.equals("HOTKEY"))
        {
          String str = childConf.toString();
          if (str.length() > 0) hotkey = str.toUpperCase().charAt(0);
        }
      }

      DialogWindow newWindow =
        new DialogWindow(tabIndex, neuesFenster, mapIdToPresetValue);

      myTabbedPane.addTab(tabTitle, null, newWindow.JPanel(), tip);
      if (hotkey != 0) myTabbedPane.setMnemonicAt(tabIndex, hotkey);

      ++tabIndex;
    }

    uiElements.trimToSize(); // verschwendeten Platz freigeben.

    /********************************************************************************
     * Fokus auf das erste Element setzen (nicht den Tab-Reiter)
     *******************************************************************************/
    if (!uiElements.isEmpty())
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          uiElements.firstElement().getComponent().requestFocusInWindow();
        }
      });
    }

    /*
     * AUTOFILL Funktionen berechnen und Felder entsprechend befüllen. Werte (egal ob
     * AUTOFILL oder nicht) an FormModel kommunizieren.
     */
    initialStateForUIElementsNotInMapIdToPresetValue(mapIdToPresetValue);

    /********************************************************************************
     * Sichtbarkeit auswerten.
     *******************************************************************************/
    setVisibility(visibilityDesc);

    /********************************************************************************
     * Plausis und fishy-Zustand checken und Hintergrundfarben entsprechend setzen.
     *******************************************************************************/
    for (UIElement uiElement : uiElements)
    {
      checkPlausi(uiElement);
    }

    /*
     * FormModel informieren dass ich fertig bin und z.B. der Modified-Status des
     * Dokuments auf false gesetzt werden kann.
     */
    formModel.formControllerInitCompleted();

    /*
     * Event-Verarbeitung starten.
     */
    processUIElementEvents = true;
  }

  /**
   * Parst die Sichtbarkeitsinformationen und setzt die Sichtbarkeit der UIElemente
   * entsprechend und benachrichtigt das FormModel.
   * 
   * @param visibilityDesc
   *          der Sichtbarkeit-Knoten der Formularbeschreibung oder ein leeres
   *          ConfigThingy falls der Knoten nicht existiert.
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void setVisibility(ConfigThingy visibilityDesc)
  {
    for (ConfigThingy visRule : visibilityDesc)
    {
      /*
       * Sichtbarkeitsfunktion parsen.
       */
      String groupId = visRule.getName();
      Function cond;
      try
      {
        cond =
          FunctionFactory.parseChildren(visRule, funcLib, dialogLib, functionContext);
      }
      catch (ConfigurationErrorException x)
      {
        Logger.error(x);
        continue;
      }

      /*
       * Falls keine Gruppe mit entsprechender Id existiert, dann legen wir einfach
       * eine leere Gruppe an.
       */
      if (!mapGroupIdToGroup.containsKey(groupId))
        mapGroupIdToGroup.put(groupId, new Group(groupId));

      /*
       * Group mit der entsprechenden Bezeichnung heraussuchen und ihr condition-Feld
       * setzten. Fehler ausgeben wenn condition bereits gesetzt.
       */
      Group group = mapGroupIdToGroup.get(groupId);
      if (group.condition != null)
        Logger.error(L.m("Mehrere Sichtbarkeitsregeln für Gruppe \"%1\" angegeben.",
          groupId));
      group.condition = cond;

      /*
       * Für jeden Parameter der condition-Funktion ein Mapping in
       * mapIdToListOfDependingGroups erzeugen (falls noch nicht geschehen) und die
       * neue Group in diese Liste eintragen.
       */
      String[] deps = cond.parameters();
      for (int i = 0; i < deps.length; ++i)
      {
        String elementId = deps[i];
        if (!mapIdToListOfDependingGroups.containsKey(elementId))
          mapIdToListOfDependingGroups.put(elementId, new Vector<Group>(1));

        mapIdToListOfDependingGroups.get(elementId).add(group);
      }

      /*
       * Sichtbarkeitsstatus berechnen und auf die Mitglieder der Gruppe anwenden.
       * Benachrichtigt auch formModel.
       */
      setGroupVisibility(group, cond.getBoolean(myUIElementValues));
    }
  }

  /**
   * Berechnet für jedes UIElement, dessen ID nicht als Schlüssel in
   * mapIdToPresetValue ist den AUTOFILL-Wert, setzt das Feld entsprechend und teilt
   * den Wert des Feldes (egal ob AUTOFILL oder nicht) dem FormModel mit.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void initialStateForUIElementsNotInMapIdToPresetValue(
      Map<String, String> mapIdToPresetValue)
  {
    Iterator<UIElement> iter = uiElements.iterator();
    while (iter.hasNext())
    {
      UIElement uiElement = iter.next();
      String id = uiElement.getId();
      if (mapIdToPresetValue.containsKey(id)) continue;
      UIElementState state = (UIElementState) uiElement.getAdditionalData();
      if (state.autofill != null)
      {
        String str = state.autofill.getString(myUIElementValues);
        uiElement.setString(str);
      }
      formModel.valueChanged(id, uiElement.getString());
    }
  }

  /**
   * Falls func nicht null ist, wird uiElement für alle Funktionsdialoge, die func
   * referenziert in die entsprechende Liste in
   * {@link #mapDialogNameToListOfUIElementsWithDependingAutofill} eingetragen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void storeAutofillFunctionDialogDeps(UIElement uiElement, Function func)
  {
    if (func == null) return;

    Set<String> funcDialogNames = new HashSet<String>();
    func.getFunctionDialogReferences(funcDialogNames);
    Iterator<String> iter = funcDialogNames.iterator();
    while (iter.hasNext())
    {
      String dialogName = iter.next();
      if (!mapDialogNameToListOfUIElementsWithDependingAutofill.containsKey(dialogName))
        mapDialogNameToListOfUIElementsWithDependingAutofill.put(dialogName,
          new Vector<UIElement>(1));

      List<UIElement> l =
        mapDialogNameToListOfUIElementsWithDependingAutofill.get(dialogName);
      l.add(uiElement);
    }
  }

  /**
   * Ein Tab der Formular-GUI.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class DialogWindow
  {
    /**
     * Das Panel das die GUI-Elemente enthält.
     */
    private JPanel myPanel;

    /**
     * Erzeugt ein neues Tab.
     * 
     * @param tabIndex
     *          Die Nummer (von 0 gezählt) des Tabs, das dieses DialogWindow
     *          darstellt.
     * @param conf
     *          der Kind-Knoten des Fenster-Knotens der das Formular beschreibt. conf
     *          ist direkter Elternknoten des Knotens "Eingabefelder".
     * @param mapIdToPresetValue
     *          bildet IDs von Formularfeldern auf Vorgabewerte ab. Falls hier ein
     *          Wert für ein Formularfeld vorhanden ist, so wird dieser allen anderen
     *          automatischen Befüllungen vorgezogen. Wird das Objekt {@link TextDocumentModel#FISHY}
     *          als Wert für ein Feld übergeben, so wird dieses Feld speziell
     *          markiert als ungültig bis der Benutzer es manuell ändert.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public DialogWindow(int tabIndex, ConfigThingy conf,
        Map<String, String> mapIdToPresetValue)
    {
      myPanel = new JPanel(new GridBagLayout());
      JPanel mainPanel = new JPanel(new GridBagLayout());
      // int gridx, int gridy, int gridwidth, int gridheight, double weightx,
      // double weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
      GridBagConstraints gbcMainPanel =
        new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST,
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
      JScrollPane scrollPane =
        new JScrollPane(mainPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scrollPane.getVerticalScrollBar().setUnitIncrement(
        Common.getVerticalScrollbarUnitIncrement());
      scrollPane.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
      myPanel.add(scrollPane, gbcMainPanel);

      int y = 0;

      Iterator<ConfigThingy> parentiter = conf.query("Eingabefelder").iterator();
      while (parentiter.hasNext())
      {
        Iterator<ConfigThingy> iter = parentiter.next().iterator();
        while (iter.hasNext())
        {
          ConfigThingy uiConf = iter.next();
          UIElement uiElement;
          try
          {
            uiElement = uiElementFactory.createUIElement(panelContext, uiConf);
            UIElementState state = new UIElementState();
            state.plausi =
              FunctionFactory.parseGrandchildren(uiConf.query("PLAUSI"), funcLib,
                dialogLib, functionContext);
            state.autofill =
              FunctionFactory.parseGrandchildren(uiConf.query("AUTOFILL"), funcLib,
                dialogLib, functionContext);
            storeAutofillFunctionDialogDeps(uiElement, state.autofill);
            state.tabIndex = tabIndex;
            uiElement.setAdditionalData(state);
          }
          catch (ConfigurationErrorException x)
          {
            Logger.error(x);
            continue;
          }

          uiElements.add(uiElement);

          addFocusListener(uiElement);

          /*
           * Überprüfen, dass die ID des neuen Elements nicht schon verwendet wurde
           * und Fehler ausgeben, falls doppelte Verwendung. Es wird auf jeden Fall
           * weitergemacht und das neue Mapping in mapIdToUIElement gespeichert (aber
           * nur falls die ID nicht leer ist).
           */
          if (mapIdToUIElement.containsKey(uiElement.getId()))
          {
            String label = L.m("nicht vorhanden");
            try
            {
              label = uiConf.get("LABEL").toString();
            }
            catch (Exception x)
            {}
            ;
            Logger.error(L.m(
              "ID \"%1\" mehrfach vergeben bei Element mit Label \"%2\"",
              uiElement.getId(), label));
          }
          if (uiElement.getId().length() > 0)
            mapIdToUIElement.put(uiElement.getId(), uiElement);

          /*
           * Preset-Wert auswerten und Element entsprechend initialisieren.
           */
          Object preset = mapIdToPresetValue.get(uiElement.getId());
          if (preset != null)
          {
            if (preset == TextDocumentModel.FISHY)
            {
              ((UIElementState) uiElement.getAdditionalData()).fishy = true;
            }

            uiElement.setString(preset.toString());
          }

          /*
           * GROUPS auswerten und entsprechende Mappings speichern.
           */
          parseGROUPS(uiConf, uiElement);

          /*
           * Plausi-Parameter auswerten und entsprechende Abhängigkeitsmappings
           * speichern.
           */
          storeDeps(uiElement);

          /**
           * Dafür sorgen, dass uiElement immer in seiner eigenen Abhängigenliste
           * steht, damit bei jeder Änderung an uiElement auf jeden Fall die Plausi
           * neu ausgewertet wird, auch wenn sie nicht von diesem Element abhängt.
           * Man denke sich zum Beispiel einen Zufallsgenerator als Plausi. Er hängt
           * zwar nicht vom Wert des Felds ab, sollte aber bei jeder Änderung des
           * Feldes erneut befragt werden.
           * 
           * Neben der Bedeutung für Plausis ist dies ebenfalls wichtig, damit der
           * FISHY-Zustand neu gesetzt wird, wenn sich das Feld ändert.
           */
          if (!mapIdToListOfUIElementsWithDependingPlausi.containsKey(uiElement.getId()))
            mapIdToListOfUIElementsWithDependingPlausi.put(uiElement.getId(),
              new Vector<UIElement>(1));
          List<UIElement> deps =
            mapIdToListOfUIElementsWithDependingPlausi.get(uiElement.getId());
          if (!deps.contains(uiElement)) deps.add(uiElement);

          /**************************************************************************
           * UI Element und evtl. vorhandenes Zusatzlabel zum GUI hinzufügen.
           *************************************************************************/
          int compoX = 0;
          int compoWidthIncrement = 0;
          if (!uiElement.getLabelType().equals(UIElement.LABEL_NONE))
          {
            Component label = uiElement.getLabel();
            int labelX = 0;
            boolean labelIsEmpty = false;
            if (uiElement.getLabelType().equals(UIElement.LABEL_LEFT))
            {
              compoX = 1;
              try
              {
                labelIsEmpty = ((JLabel) label).getText().length() == 0;
                if (labelIsEmpty)
                {
                  compoWidthIncrement = 1;
                  compoX = 0;
                }
              }
              catch (Exception x)
              {}
            }
            else
              labelX = 1;

            if (label != null && !labelIsEmpty)
            {
              GridBagConstraints gbc =
                (GridBagConstraints) uiElement.getLabelLayoutConstraints();
              gbc.gridx = labelX;
              gbc.gridy = y;
              mainPanel.add(label, gbc);
            }
          }
          GridBagConstraints gbc =
            (GridBagConstraints) uiElement.getLayoutConstraints();
          gbc.gridx = compoX;
          gbc.gridwidth += compoWidthIncrement; // wird nachher wieder abgezogen weil
          // Objekt shared ist
          gbc.gridy = y;
          ++y;
          mainPanel.add(uiElement.getComponent(), gbc);
          gbc.gridwidth -= compoWidthIncrement; // wieder abziehen, weil Objekt ja
          // shared ist
          if (!uiElement.isStatic()) increaseTabVisibleCount(tabIndex);

          if (y > GRID_MAX) break;
        }

        if (y > GRID_MAX) break;
      }

      if (y > GRID_MAX)
        Logger.error(L.m("Zu viele Formularelemente auf einem Tab => nicht alle werden angezeigt"));

      /******************************************************************************
       * Für die Buttons ein eigenes Panel anlegen und mit UIElementen befüllen.
       *****************************************************************************/
      createButtonPanel(conf, 1);
    }

    /**
     * Fügt myPanel an Koordinate y ein Panel hinzu, das mit Buttons gemäß der
     * Beschreibung in conf,query("Buttons") befüllt wird.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void createButtonPanel(ConfigThingy conf, int y)
    {
      JPanel buttonPanel = new JPanel(new GridBagLayout());
      // int gridx, int gridy, int gridwidth, int gridheight, double weightx, double
      // weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
      GridBagConstraints gbcButtonPanel =
        new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
            TF_BORDER), 0, 0);
      gbcButtonPanel.gridx = 0;
      gbcButtonPanel.gridy = y;
      // ++y;
      myPanel.add(buttonPanel, gbcButtonPanel);

      addTabSwitcher(buttonPanel);
      int x = 0;

      Iterator<ConfigThingy> parentiter = conf.query("Buttons").iterator();
      while (parentiter.hasNext())
      {
        Iterator<ConfigThingy> iter = parentiter.next().iterator();
        while (iter.hasNext())
        {
          ConfigThingy uiConf = iter.next();
          UIElement uiElement;
          
          try
          {
            uiElement = uiElementFactory.createUIElement(buttonContext, uiConf);
            UIElementState state = new UIElementState();
            state.setIsButton(true);
            uiElement.setAdditionalData(state);
          }
          catch (ConfigurationErrorException e)
          {
            Logger.error(e);
            continue;
          }

          int compoX = x;
          if (!uiElement.getLabelType().equals(UIElement.LABEL_NONE))
          {
            int labelX = x;
            ++x;
            if (uiElement.getLabelType().equals(UIElement.LABEL_LEFT))
              compoX = x;
            else
              labelX = x;

            Component label = uiElement.getLabel();
            if (label != null)
            {
              GridBagConstraints gbc =
                (GridBagConstraints) uiElement.getLabelLayoutConstraints();
              gbc.gridx = labelX;
              gbc.gridy = 0;
              buttonPanel.add(label, gbc);
            }

          }
          
          parseGROUPS(uiConf, uiElement);
          
          GridBagConstraints gbc =
            (GridBagConstraints) uiElement.getLayoutConstraints();
          gbc.gridx = compoX;
          gbc.gridy = 0;
          ++x;
          buttonPanel.add(uiElement.getComponent(), gbc);

          if (x > GRID_MAX) break;
        }
        if (x > GRID_MAX) break;
      }

      if (x > GRID_MAX)
        Logger.error(L.m("Zu viele Buttons auf einem Tab => nicht alle werden angezeigt"));
    }

    /**
     * Fügt buttonPanel (muss ein GridBagLayout verwenden) an Index 0 eine
     * unsichtbare Komponente hinzu, die wenn sie den Fokus bekommt (was nur über die
     * TAB-Taste geschehen kann) und zwar von einer anderen Komponente als der
     * 2,Komponente des buttonPanels, einen action "nextTab" Event absetzt, falls es
     * noch ein auf das aktuelle Tab folgendes Tab gibt, das aktiv ist. Falls es kein
     * passendes Tab gibt, bekommt die letzte Komponente von buttonPanel den Fokus.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void addTabSwitcher(final JPanel buttonPanel)
    {
      GridBagConstraints gbc =
        new GridBagConstraints(0, 0, 0, 0, 0.0, 0.0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
      final JPanel tabSwitcherCompo = new JPanel();
      tabSwitcherCompo.setSize(1, 1);
      tabSwitcherCompo.setFocusable(true);
      tabSwitcherCompo.setOpaque(false);
      tabSwitcherCompo.setFocusable(true);
      tabSwitcherCompo.setRequestFocusEnabled(false);
      tabSwitcherCompo.addFocusListener(new FocusListener()
      {

        public void focusGained(FocusEvent e)
        {
          if (buttonPanel.getComponentCount() > 1
            && e.getOppositeComponent() == buttonPanel.getComponent(1))
          {
            tabSwitcherCompo.transferFocusBackward();
          }
          else
          {
            int startIdx = myTabbedPane.getSelectedIndex();
            int idx = startIdx;
            do
            {
              ++idx;
              if (idx >= myTabbedPane.getTabCount())
              {
                idx = -1;
                break;
              }
              if (myTabbedPane.isEnabledAt(idx)) break;
            } while (idx != startIdx);
            if (idx > -1)
              processUiElementEvent(null, "action", new String[] { "nextTab" });
            else
              buttonPanel.getComponent(buttonPanel.getComponentCount() - 1).requestFocusInWindow();
          }
        }

        public void focusLost(FocusEvent e)
        {}
      });

      buttonPanel.add(tabSwitcherCompo, gbc, 0);
    }

    /**
     * Fügt einem Steuerelement einen FocusListener hinzu, der dafür sorgt, daß der
     * Container zu dem Element hinscrollt, sobald es den Fokus erhält.
     * 
     * @param uiElement
     *          das Steuerelement, das überwacht werden soll.
     * @author Andor Ertsey (D-III-ITD-D101)
     */
    private void addFocusListener(UIElement uiElement)
    {
      Component c = uiElement.getComponent();
      if (uiElement instanceof Textarea) c = ((Textarea) uiElement).getTextArea();

      c.addFocusListener(new FocusAdapter()
      {
        public void focusGained(FocusEvent e)
        {
          Container c = e.getComponent().getParent();
          if (e.getComponent() instanceof JTextArea) c = c.getParent();

          if (c != null && c instanceof JComponent)
          {
            java.awt.Rectangle b = e.getComponent().getBounds();
            b.x = 0;
            ((JComponent) c).scrollRectToVisible(b);
            c.repaint();
          }
        }
      });
    }

    /**
     * Falls uiElement eine Plausi und oder ein Autofill hat, werden entsprechende
     * Abhängigkeiten in den Maps erfasst.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
     */
    private void storeDeps(UIElement uiElement)
    {
      UIElementState state = ((UIElementState) uiElement.getAdditionalData());
      Function func = state.plausi;
      Map<String, List<UIElement>> mapIdToListOfDependingUIElements =
        mapIdToListOfUIElementsWithDependingPlausi;

      /**
       * Für alle Felder von denen die Function abhängt das uiElement in die Liste
       * der abhängigen UI Elemente einfügen.
       */
      for (int f = 0; f < 2; ++f, func = state.autofill, mapIdToListOfDependingUIElements =
        mapIdToListOfUIElementsWithDependingAutofill)
      {
        if (func != null)
        {
          String[] params = func.parameters();
          for (int i = 0; i < params.length; ++i)
          {
            String dependency = params[i];
            if (!mapIdToListOfDependingUIElements.containsKey(dependency))
              mapIdToListOfDependingUIElements.put(dependency,
                new Vector<UIElement>(1));

            List<UIElement> deps = mapIdToListOfDependingUIElements.get(dependency);
            deps.add(uiElement);
          }
        }
      }
    }

    /**
     * Verarbeitet die GROUPS-Attribute von uiConf (was zu uiElement gehören muss).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    private void parseGROUPS(ConfigThingy uiConf, UIElement uiElement)
    {
      ConfigThingy groupsConf = uiConf.query("GROUPS");
      Iterator<ConfigThingy> groupsIter = groupsConf.iterator();
      while (groupsIter.hasNext())
      {
        ConfigThingy groups = groupsIter.next();
        Iterator<ConfigThingy> groupIter = groups.iterator();
        while (groupIter.hasNext())
        {
          String groupId = groupIter.next().toString();
          if (!mapGroupIdToGroup.containsKey(groupId))
            mapGroupIdToGroup.put(groupId, new Group(groupId));

          Group g = mapGroupIdToGroup.get(groupId);
          g.uiElements.add(uiElement);

        }
      }
    }

    /**
     * Liefert das Panel zurück, dass den ganzen Inhalt dieses Tabs enthält.
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public JPanel JPanel()
    {
      return myPanel;
    }
  }

  /**
   * Initialisiert die UIElementFactory, die zur Erzeugung der UIElements verwendet
   * wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void initFactories()
  {
    Map<String, GridBagConstraints> mapTypeToLayoutConstraints =
      new HashMap<String, GridBagConstraints>();
    Map<String, Integer> mapTypeToLabelType = new HashMap<String, Integer>();
    Map<String, GridBagConstraints> mapTypeToLabelLayoutConstraints =
      new HashMap<String, GridBagConstraints>();

    // int gridx, int gridy, int gridwidth, int gridheight, double weightx, double
    // weighty, int anchor, int fill, Insets insets, int ipadx, int ipady)
    GridBagConstraints gbcTextfield =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcCombobox =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcTextarea =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcLabelLeft =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcCheckbox =
      new GridBagConstraints(0, 0, 2/* JA */, 1, 1.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new Insets(
          TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcLabel =
      new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL, new Insets(TF_BORDER, TF_BORDER, TF_BORDER,
          TF_BORDER), 0, 0);
    GridBagConstraints gbcButton =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(BUTTON_BORDER, BUTTON_BORDER,
          BUTTON_BORDER, BUTTON_BORDER), 0, 0);
    GridBagConstraints gbcHsep =
      new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.HORIZONTAL,
        new Insets(3 * TF_BORDER, 0, 2 * TF_BORDER, 0), 0, 0);
    GridBagConstraints gbcVsep =
      new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER,
        GridBagConstraints.VERTICAL, new Insets(0, TF_BORDER, 0, TF_BORDER), 0, 0);
    GridBagConstraints gbcGlue =
      new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

    mapTypeToLayoutConstraints.put("default", gbcTextfield);
    mapTypeToLabelType.put("default", UIElement.LABEL_LEFT);
    mapTypeToLabelLayoutConstraints.put("default", gbcLabelLeft);

    mapTypeToLayoutConstraints.put("textfield", gbcTextfield);
    mapTypeToLabelType.put("textfield", UIElement.LABEL_LEFT);
    mapTypeToLabelLayoutConstraints.put("textfield", gbcLabelLeft);

    mapTypeToLayoutConstraints.put("combobox", gbcCombobox);
    mapTypeToLabelType.put("combobox", UIElement.LABEL_LEFT);
    mapTypeToLabelLayoutConstraints.put("combobox", gbcLabelLeft);

    mapTypeToLayoutConstraints.put("h-glue", gbcGlue);
    mapTypeToLabelType.put("h-glue", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("h-glue", null);
    mapTypeToLayoutConstraints.put("v-glue", gbcGlue);
    mapTypeToLabelType.put("v-glue", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("v-glue", null);

    mapTypeToLayoutConstraints.put("textarea", gbcTextarea);
    mapTypeToLabelType.put("textarea", UIElement.LABEL_LEFT);
    mapTypeToLabelLayoutConstraints.put("textarea", gbcLabelLeft);

    mapTypeToLayoutConstraints.put("label", gbcLabel);
    mapTypeToLabelType.put("label", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("label", null);

    mapTypeToLayoutConstraints.put("checkbox", gbcCheckbox);
    mapTypeToLabelType.put("checkbox", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("checkbox", null); // hat label integriert

    mapTypeToLayoutConstraints.put("button", gbcButton);
    mapTypeToLabelType.put("button", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("button", null);

    mapTypeToLayoutConstraints.put("h-separator", gbcHsep);
    mapTypeToLabelType.put("h-separator", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("h-separator", null);
    mapTypeToLayoutConstraints.put("v-separator", gbcVsep);
    mapTypeToLabelType.put("v-separator", UIElement.LABEL_NONE);
    mapTypeToLabelLayoutConstraints.put("v-separator", null);

    panelContext = new UIElementContext();
    panelContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
    panelContext.mapTypeToLabelType = mapTypeToLabelType;
    panelContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
    panelContext.uiElementEventHandler = this;
    panelContext.mapTypeToType = new HashMap<String, String>();
    panelContext.mapTypeToType.put("separator", "h-separator");
    panelContext.mapTypeToType.put("glue", "v-glue");

    buttonContext = new UIElementContext();
    buttonContext.mapTypeToLabelLayoutConstraints = mapTypeToLabelLayoutConstraints;
    buttonContext.mapTypeToLabelType = mapTypeToLabelType;
    buttonContext.mapTypeToLayoutConstraints = mapTypeToLayoutConstraints;
    buttonContext.uiElementEventHandler = this;
    buttonContext.mapTypeToType = new HashMap<String, String>();
    buttonContext.mapTypeToType.put("separator", "v-separator");
    buttonContext.mapTypeToType.put("glue", "h-glue");

    Set<String> supportedActions = new HashSet<String>();
    supportedActions.add("abort");
    supportedActions.add("nextTab");
    supportedActions.add("prevTab");
    supportedActions.add("funcDialog");
    supportedActions.add("form2PDF");
    supportedActions.add("save");
    supportedActions.add("saveAs");
    supportedActions.add("printForm");
    supportedActions.add("closeAndOpenExt");
    supportedActions.add("saveTempAndOpenExt");
    panelContext.supportedActions = supportedActions;
    buttonContext.supportedActions = supportedActions;

    uiElementFactory = new UIElementFactory();

  }

  private class FunctionDialogEndListener implements ActionListener
  {
    private String funcDialogName;

    public FunctionDialogEndListener(String dialogName)
    {
      funcDialogName = dialogName;
    }

    public void actionPerformed(ActionEvent e)
    {
      if (e.getActionCommand().equals("select"))
        processUiElementEvent(null, "funcDialogSelect",
          new Object[] { funcDialogName });
    }
  }

  /**
   * Liefert true gdw keine SICHTBARE PLAUSI mit negativen Testresultat und keine
   * SICHTBAREN als fishy markierten Felder existieren, in anderen Worten es wird
   * true geliefert, genau dann wenn kein Feld mit Warnfarbe markiert ist. Kann
   * sowohl aus dem EDT als auch aus einem anderen Thread aufgerufen werden.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   *         TODO Testen
   */
  public boolean isInputOkay()
  {
    final boolean[] result = new boolean[] { false };
    try
    {
      Runnable runner = new Runnable()
      {
        public void run()
        {
          for (UIElement ele : uiElements)
          {
            UIElementState state = ((UIElementState) ele.getAdditionalData());
            if ((!state.okay) && state.visible()) return;
          }
          result[0] = true;
        }
      };

      if (SwingUtilities.isEventDispatchThread())
        runner.run();
      else
        SwingUtilities.invokeAndWait(runner);
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
    return result[0];
  }

  /**
   * Setzt den Wert des {@link UIElement}s mit ID uiElementId auf value und behandelt
   * die Änderung so als wäre sie durch den Benutzer erfolgt. ACHTUNG! Die
   * Verarbeitung läuft asynchron im Event-Dispatching Thread, d.h. diese Funktion
   * kehrt evtl. bereits vor Abarbeitung zurück.
   * 
   * @param callback
   *          Nach dem Abarbeiten dieser Änderung wird callback.actionPerformed()
   *          aufgerufen falls callback != null. ACHTUNG! Der Aufruf erfolgt im
   *          Event-Dispatching Thread!
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void setValue(final String uiElementId, final String value,
      final ActionListener callback)
  {
    SwingUtilities.invokeLater(new Runnable()
    {
      public void run()
      {
        UIElement uiElement = mapIdToUIElement.get(uiElementId);
        if (uiElement != null)
        {
          processUIElementEvents = false;
          uiElement.setString(value);
          processUIElementEvents = true;
          processUiElementEvent(uiElement, "valueChanged", new Object[] {});
        }
        else
        {
          formModel.valueChanged(uiElementId, value);
        }
        callback.actionPerformed(new ActionEvent(this, 0, "setValue"));
      }
    });
  }

  /**
   * Die zentrale Anlaufstelle für alle von UIElementen ausgelösten Events (siehe
   * {@link UIElementEventHandler#processUiElementEvent(UIElement, String, Object[])}
   * ).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void processUiElementEvent(UIElement source, String eventType, Object[] args)
  {
    if (!processUIElementEvents) return;
    try
    {
      processUIElementEvents = false; // Reentranz bei setString() unterbinden
      if (WollMuxFiles.isDebugMode())
      {
        StringBuffer buffy = new StringBuffer("UIElementEvent: " + eventType + "(");
        for (int i = 0; i < args.length; ++i)
          buffy.append((i == 0 ? "" : ",") + args[i]);
        if (source != null) buffy.append(") on UIElement " + source.getId());
        Logger.debug(buffy.toString());
      }

      if (eventType.equals("valueChanged"))
      {
        // FISHY-Zustand löschen, weil Wert geändert
        ((UIElementState) source.getAdditionalData()).fishy = false;

        // FormModel benachrichtigen über Änderung von source
        // da recomputeAutofills() das FormModel über genau diese Änderung nicht
        // informiert.
        formModel.valueChanged(source.getId(), source.getString());

        Set<UIElement> todo = new HashSet<UIElement>();
        todo.add(source);
        /*
         * Der folgende Code wird ebenfalls für den eventType funcDialogSelect
         * verwendet. Die beiden sollten vermutlich immer synchron gehalten werden.
         */
        Set<UIElement> changedElements = computeChangesCausedByChangeOf(todo);
        recomputeAutofills(changedElements, source);
        checkDependingPlausis(changedElements);
        checkDependingVisibilityGroups(changedElements);
      }
      else if (eventType.equals("action"))
      {
        String action = (String) args[0];
        if (action.equals("abort"))
        {
          abortRequestListener.actionPerformed(new ActionEvent(this, 0, "abort"));
        }
        else if (action.equals("nextTab"))
        {
          int startIdx = myTabbedPane.getSelectedIndex();
          int idx = startIdx;
          do
          {
            ++idx;
            if (idx >= myTabbedPane.getTabCount()) idx = 0;
            if (myTabbedPane.isEnabledAt(idx)) break;
          } while (idx != startIdx);

          myTabbedPane.setSelectedIndex(idx);
        }
        else if (action.equals("prevTab"))
        {
          int startIdx = myTabbedPane.getSelectedIndex();
          int idx = startIdx;
          do
          {
            if (idx == 0) idx = myTabbedPane.getTabCount();
            --idx;
            if (myTabbedPane.isEnabledAt(idx)) break;
          } while (idx != startIdx);

          myTabbedPane.setSelectedIndex(idx);
        }
        else if (action.equals("funcDialog"))
        {
          String dialogName = (String) args[1];
          Dialog dlg = dialogLib.get(dialogName);
          if (dlg == null)
            Logger.error(L.m("Funktionsdialog \"%1\" ist nicht definiert",
              dialogName));
          else
          {
            dlg.instanceFor(functionContext).show(
              new FunctionDialogEndListener(dialogName), funcLib, dialogLib);
          }
        }
        else if (action.equals("closeAndOpenExt"))
        {
          formModel.closeAndOpenExt((String) args[1]);
        }
        else if (action.equals("saveTempAndOpenExt"))
        {
          formModel.saveTempAndOpenExt((String) args[1]);
        }
        else if (action.equals("printForm"))
        {
          formModel.print();
        }
        else if (action.equals("form2PDF"))
        {
          formModel.pdf();
        }
        else if (action.equals("save"))
        {
          formModel.save();
        }
        else if (action.equals("saveAs"))
        {
          formModel.saveAs();
        }
      }
      else if (eventType.equals("focus"))
      {
        if (args[0].equals("lost"))
          formModel.focusLost(source.getId());
        else
          formModel.focusGained(source.getId());
      }
      else if (eventType.equals("funcDialogSelect"))
      {
        String dialogName = (String) args[0];
        Set<UIElement> todo = new HashSet<UIElement>();
        List<UIElement> depending =
          mapDialogNameToListOfUIElementsWithDependingAutofill.get(dialogName);
        if (depending != null)
        {
          Iterator<UIElement> iter = depending.iterator();
          while (iter.hasNext())
          {
            UIElement uiElement = iter.next();
            todo.add(uiElement);
          }
        }
        /*
         * Der folgende Code wird ebenfalls für den eventType valueChanged verwendet.
         * Die beiden sollten vermutlich immer synchron gehalten werden.
         */
        Set<UIElement> changedElements = computeChangesCausedByChangeOf(todo);
        recomputeAutofills(changedElements, source);
        checkDependingPlausis(changedElements);
        checkDependingVisibilityGroups(changedElements);
      }
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
    finally
    {
      processUIElementEvents = true;
    }
  }

  /**
   * Liefert die Menge aller UIElemente (inklusive der aus todo), deren Wert sich
   * ändert, wenn sich der Wert eines UIElements aus todo ändert (z,B, wegen
   * AUTOFILLs). ACHTUNG! todo wird durch diese Methode verändert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public Set<UIElement> computeChangesCausedByChangeOf(Set<UIElement> todo)
  {
    Set<UIElement> elements = new HashSet<UIElement>();
    while (!todo.isEmpty())
    {
      Iterator<UIElement> iter = todo.iterator();
      UIElement uiElement = iter.next();
      iter.remove();
      if (!elements.contains(uiElement))
      {
        elements.add(uiElement);
        List<UIElement> deps =
          mapIdToListOfUIElementsWithDependingAutofill.get(uiElement.getId());
        if (deps == null) continue;
        Iterator<UIElement> iter2 = deps.iterator();
        while (iter2.hasNext())
        {
          uiElement = iter2.next();
          if (!elements.contains(uiElement)) todo.add(uiElement);
        }
      }
    }
    return elements;
  }

  /**
   * Berechnet die Werte mit den AUTOFILL-Funktionen neu für alle UIElemente aus
   * elements mit Ausnahme von exception (falls nicht null). Das FormModel wird
   * ebenfalls benachrichtigt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void recomputeAutofills(Set<UIElement> elements, UIElement exception)
  {
    // Alle UIElemente in der Reihenfolge ihrer Definition durchgehen,
    // damit die AUTOFILLs in der richtigen Reihenfolge ausgewertet werden
    // (Annahme ist hier, dass jedes AUTOFILL maximal von vorher definierten
    // Feldern abhängt)
    Iterator<UIElement> iter = uiElements.iterator();
    while (iter.hasNext())
    {
      UIElement uiElement = iter.next();
      if (uiElement == exception) continue;
      if (!elements.contains(uiElement)) continue;
      UIElementState state = (UIElementState) uiElement.getAdditionalData();
      if (state.autofill != null)
      {
        state.fishy = false;
        uiElement.setString(state.autofill.getString(myUIElementValues));
        formModel.valueChanged(uiElement.getId(), uiElement.getString());
      }
    }
  }

  /**
   * Bestimmt und setzt die Sichtbarkeit neu für alle Groups, die von mindestens
   * einem UIElement mit id aus ids abhängen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void checkDependingVisibilityGroups(Set<UIElement> elements)
  {
    Iterator<UIElement> eleIter = elements.iterator();
    while (eleIter.hasNext())
    {
      UIElement uiElement = eleIter.next();
      List<Group> dependingGroups =
        mapIdToListOfDependingGroups.get(uiElement.getId());
      if (dependingGroups != null)
      {
        Iterator<Group> iter = dependingGroups.iterator();
        while (iter.hasNext())
        {
          Group dependingGroup = iter.next();
          Function cond = dependingGroup.condition;
          if (cond == null) continue;
          boolean result = cond.getBoolean(myUIElementValues);
          if (result == dependingGroup.visible) continue;

          setGroupVisibility(dependingGroup, result);
        }
      }
    }
  }

  /**
   * Setzt die Sichtbarkeit aller Mitglieder von group auf visible und benachrichtigt
   * das FormModel entsprechend.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setGroupVisibility(Group group, boolean visible)
  {
    group.visible = visible;
    Iterator<UIElement> uiElementIter = group.uiElements.iterator();
    while (uiElementIter.hasNext())
    {
      UIElement ele = uiElementIter.next();
      UIElementState state = (UIElementState) ele.getAdditionalData();
      boolean oldElementVisible = state.visible();

      if (visible)
      {
        if (state.invisibleGroups != null) state.invisibleGroups.remove(group);
      }
      else
      {
        if (state.invisibleGroups == null)
          state.invisibleGroups = new ArrayList<Group>(1);
        if (!state.invisibleGroups.contains(group))
          state.invisibleGroups.add(group);
      }

      boolean newElementVisible = state.visible();

      /*
       * Der folgende Test ist erforderlich, weil Elemente mehreren Gruppen angehören
       * können, so dass eine Änderung des Status der Gruppe nicht bedeutet, dass
       * sich alle Elemente ändern. Falls sich der Zustand eines Elements nicht
       * geändert hat, dann darf weder increaseTabVisibleCount() noch
       * decreaseTabVisibleCount() aufgerufen werden.
       */
      if (oldElementVisible != newElementVisible)
      {
        if(state.getIsButton()){
          if (newElementVisible)
            ele.setEnabled(true);
          else
            ele.setEnabled(false);
          }
          else
            ele.setVisible(newElementVisible);
        
        if (!ele.isStatic())
        {
          if (newElementVisible)
            increaseTabVisibleCount(state.tabIndex);
          else
            decreaseTabVisibleCount(state.tabIndex);
        }
      }

    }

    formModel.setVisibleState(group.id, group.visible);
  }

  /**
   * Erhöht den Zähler von tabVisibleCount[tabIndex] um 1. Sollte das Array nicht
   * lang genug sein, wird es verlängert. Falls dadurch auf einem Tab eine
   * nicht-leere Menge von nicht-statischen Elementen sichtbar ist, so wird das Tab
   * sichtbar geschaltet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void increaseTabVisibleCount(int tabIndex)
  {
    /*
     * Achtung! Der Aufbau des folgenden Codes ist wichtig! Der Befehl
     * myTabbedPane.setEnabledAt(tabIndex, true); darf beim ersten erhöhen von 0 auf
     * 1 noch nicht ausgeführt werden, weil dies geschieht bevor das Tab dem
     * JTabbedPane hinzugefügt wurde. Deshalb beginnt tabVisibleCount mit dem leeren
     * Array, damit wir diesen Fall erkennen können.
     */
    if (tabIndex >= tabVisibleCount.length)
    {
      int[] newTVC = new int[tabIndex + 1];
      System.arraycopy(tabVisibleCount, 0, newTVC, 0, tabVisibleCount.length);
      newTVC[tabIndex] = 1;
      tabVisibleCount = newTVC;
    }
    else
    {
      if (++tabVisibleCount[tabIndex] == 1)
        myTabbedPane.setEnabledAt(tabIndex, true);
    }
  }

  /**
   * Erniedrigt den Zähler von tabVisibleCount[tabIndex] um 1. Falls dadurch auf
   * einem Tab keine nicht-statischen Elemente mehr sichtbar ist, so wird das Tab
   * unsichtbar geschaltet.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void decreaseTabVisibleCount(int tabIndex)
  {
    if (--tabVisibleCount[tabIndex] == 0)
      myTabbedPane.setEnabledAt(tabIndex, false);
  }

  /**
   * Berechnet alle Plausi-Zustände neu für Elemente, die von mindestens einem
   * Element mit ID aus ids abhängen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void checkDependingPlausis(Set<UIElement> elements)
  {
    Iterator<UIElement> eleIter = elements.iterator();
    while (eleIter.hasNext())
    {
      UIElement uiElement = eleIter.next();

      List<UIElement> dependingUIElements =
        mapIdToListOfUIElementsWithDependingPlausi.get(uiElement.getId());
      if (dependingUIElements != null)
      {
        Iterator<UIElement> iter = dependingUIElements.iterator();
        while (iter.hasNext())
        {
          UIElement dependingUIElement = iter.next();
          checkPlausi(dependingUIElement);
        }
      }
    }
  }

  /**
   * Überprüft die Plausi und den fishy-Zustand von uiElement und setzt den
   * Hintergrund entsprechend.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private void checkPlausi(UIElement uiElement)
  {
    UIElementState state = ((UIElementState) uiElement.getAdditionalData());
    Function plausi = state.plausi;

    boolean newOkay = !state.fishy;

    if (plausi != null) newOkay = newOkay && plausi.getBoolean(myUIElementValues);

    if (state.okay == newOkay) return;
    state.okay = newOkay;

    if (newOkay)
      uiElement.setBackground(normalColor);
    else
      uiElement.setBackground(plausiMarkerColor);
  }

  /**
   * Eine Sichtbarkeitsgruppe von UIElementen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class Group
  {
    /**
     * Die Mitglieder der Gruppe.
     */
    public List<UIElement> uiElements = new Vector<UIElement>(1);

    /**
     * Die Bedingung für die Sichtbarkeit (true = sichtbar) oder null, wenn keine
     * Sichtbarkeitsbedingung definiert.
     */
    public Function condition = null;

    /**
     * true, wenn die Gruppe im Augenblick sichtbar ist.
     */
    public boolean visible = true;

    /**
     * Die GROUP id dieser Gruppe.
     */
    public String id;

    public Group(String groupId)
    {
      id = groupId;
    }
  }

  /**
   * Speichert diverse Daten über den Zustand eines UIElements.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class UIElementState
  {
    /**
     * true, wenn es sich bei dem Element um einen Button handelt
     */
    private boolean isButton = false;
    
    /**
     * Die Plausi, die dieses Element überprüft (falls vorhanden).
     */
    public Function plausi = null;

    /**
     * Die AUTOFILL-Funktion (falls vorhanden).
     */
    public Function autofill = null;

    /**
     * true, wenn das Element als zu prüfen markiert werden soll, solange bis der
     * Benutzer es editiert.
     */
    public boolean fishy = false;

    /**
     * Cachet das Ergebnis der letzten Prüfung von plausi und fishy.
     */
    public boolean okay = true;

    /**
     * Auf welchem Tab befindet sich das UI Element.
     */
    public int tabIndex = -1;

    /**
     * Falls nicht null, so enthält diese Collection alle {@link Group}s, zu denen
     * das UIElement gehört, die im Augenblick unsichtbar sind. D.h. das UIElement
     * ist genau dann sichtbar, wenn hier null oder eine leere Collection steht.
     */
    public Collection<Group> invisibleGroups = null;

    /**
     * Liefer true gdw invisibleGroups == null oder invisibleGroups leer ist.
     * 
     * @author Matthias Benkmann (D-III-ITD-D101)
     */
    public boolean visible()
    {
      return (invisibleGroups == null || invisibleGroups.isEmpty());
    }
    
    public void setIsButton(boolean isButton)
    {
      this.isButton = isButton;
    }
    
    public boolean getIsButton()
    {
      return isButton;
    }
  }

  /**
   * Stellt den Inhalt einer Map von IDs auf UIElemente als Values zur Verfügung.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class UIElementMapValues implements Values
  {
    private Map<String, UIElement> mapIdToUIElement;

    public UIElementMapValues(Map<String, UIElement> mapIdToUIElement)
    {
      this.mapIdToUIElement = mapIdToUIElement;
    }

    public boolean hasValue(String id)
    {
      return mapIdToUIElement.containsKey(id);
    }

    public String getString(String id)
    {
      UIElement uiElement = mapIdToUIElement.get(id);
      if (uiElement == null) return "";
      return uiElement.getString();
    }

    public boolean getBoolean(String id)
    {
      UIElement uiElement = mapIdToUIElement.get(id);
      if (uiElement == null) return false;
      return uiElement.getBoolean();
    }
  }

  public static void main(String[] args) throws Exception
  {
    File curDir = new File(System.getProperty("user.dir"));
    URL context = curDir.toURI().toURL();
    URL merge1URL = new URL(context, "testdata/merge_form_1.conf");
    URL merge2URL = new URL(context, "testdata/merge_form_2.conf");
    URL anpassungURL = new URL(context, "testdata/buttonanpassung.conf");
    ConfigThingy merge1Conf = new ConfigThingy("", merge1URL).get("Formular");
    ConfigThingy merge2Conf = new ConfigThingy("", merge2URL).get("Formular");
    ConfigThingy buttonanpassung = new ConfigThingy("Buttonanpassung", anpassungURL);
    List<ConfigThingy> formDesc = new Vector<ConfigThingy>();
    formDesc.add(merge1Conf);
    formDesc.add(merge2Conf);
    ConfigThingy merged =
      TextDocumentModel.mergeFormDescriptors(formDesc, buttonanpassung, "Multi-Form");
    System.out.println(merged.stringRepresentation());
  }

}
