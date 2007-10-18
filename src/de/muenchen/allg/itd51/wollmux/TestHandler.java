/*
 * Dateiname: TestHandler.java
 * Projekt  : WollMux
 * Funktion : Enthält die DispatchHandler für alle dispatch-Urls, die
 *            mit "wollmux:Test" anfangen
 * 
 * Copyright: Landeshauptstadt München
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 07.05.2007 | LUT | Erstellung als TestHandler.java
 * -------------------------------------------------------------------
 *
 * @author Christoph Lutz (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.PrintModels.PrintModelProps;
import de.muenchen.allg.itd51.wollmux.func.StandardPrint;

/**
 * Enthält die DispatchHandler für alle dispatch-Urls, die mit "wollmux:Test"
 * anfangen und für den automatisierten Test durch wollmux-qatest benötigt
 * werden.
 * 
 * @author Christoph Lutz (D-III-ITD-5.1)
 */
public class TestHandler
{

  /**
   * Dieses File enthält die Argumente, die einem TestHandler übergeben werden
   * sollen und vor dem Aufruf des Teshandlers über das testtool geschrieben
   * wurden.
   */
  public static File WOLLMUX_QATEST_ARGS_FILE = new File(
      "/tmp/wollmux_qatest.args");

  /**
   * Bearbeitet den Test, der im Argument arg spezifiziert ist und im
   * TextDocumentModel model ausgeführt werden soll.
   * 
   * @param model
   * @param arg
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  public static void doTest(TextDocumentModel model, String arg)
  {
    String[] args = arg.split("#");
    String cmd = args[0];

    /** ************************************************************** */
    if (cmd.equalsIgnoreCase("VerfuegungspunktDrucken"))
    {
      Map idsAndValues = getWollmuxTestArgs();
      short count = (short) SachleitendeVerfuegung
          .countVerfuegungspunkte(model.doc);
      short verfPunkt = new Short(idsAndValues.get("VerfPunkt").toString())
          .shortValue();
      boolean isDraft = (verfPunkt == count) ? true : false;
      boolean isOriginal = (verfPunkt == 1) ? true : false;
      XPrintModel pmod = model.createPrintModel(false);
      try
      {
        pmod.setPropertyValue(
            PrintModelProps.PROP_SLV_VERF_PUNKTE,
            new Object[] { new Short(verfPunkt) });
        pmod.setPropertyValue(
            PrintModelProps.PROP_SLV_IS_DRAFT_FLAGS,
            new Object[] { new Boolean(isDraft) });
        pmod.setPropertyValue(
            PrintModelProps.PROP_SLV_IS_ORIGINAL_FLAGS,
            new Object[] { new Boolean(isOriginal) });
        pmod.setPropertyValue(
            PrintModelProps.PROP_SLV_COPY_COUNTS,
            new Object[] { new Short((short) 1) });
        pmod.setPropertyValue(
            PrintModelProps.PROP_SLV_PAGE_RANGE_TYPES,
            new Object[] { new Short(PrintModelProps.PAGE_RANGE_TYPE_ALL) });
        pmod.setPropertyValue(
            PrintModelProps.PROP_SLV_PAGE_RANGE_VALUES,
            new Object[] { "" });
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }
      pmod.useInternalPrintFunction(StandardPrint.getInternalPrintFunction(
          "sachleitendeVerfuegungOutput",
          10));
      pmod.printWithProps();
    }

    /** ************************************************************** */
    if (cmd.equalsIgnoreCase("SchreibeFormularwerte"))
    {
      Map idsAndValues = getWollmuxTestArgs();
      for (Iterator iter = idsAndValues.keySet().iterator(); iter.hasNext();)
      {
        String id = "" + iter.next();
        String value = "" + idsAndValues.get(id);
        WollMuxEventHandler.handleSetFormValueViaPrintModel(
            model.doc,
            id,
            value,
            null);
      }
    }
  }

  /**
   * Liest die Argumente aus der Datei WOLLMUX_QATEST_ARGS_FILE in eine HashMap
   * ein und liefert diese zurück. Die Argumente werden in der Datei in Zeilen
   * der Form "<key>,<value>" abgelegt erwartet (key darf dabei kein ","
   * enthalten).
   * 
   * @return
   * 
   * @author Christoph Lutz (D-III-ITD-5.1)
   */
  private static HashMap getWollmuxTestArgs()
  {
    HashMap args = new HashMap();
    try
    {
      BufferedReader br = new BufferedReader(new FileReader(
          WOLLMUX_QATEST_ARGS_FILE));

      for (String line = null; (line = br.readLine()) != null;)
      {
        String[] keyValue = line.split(",", 2);
        args.put(keyValue[0], keyValue[1]);
      }
    }
    catch (java.lang.Exception e)
    {
      Logger.error(
          "Argumentdatei für wollmux-qatest konnte nicht gelesen werden",
          e);
    }
    return args;
  }
}
