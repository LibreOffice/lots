package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.util.Set;

import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.XPrintModel;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.print.PrintModels;

/**
 * Erzeugt ein neues WollMuxEvent das signaisiert, dass die Druckfunktion
 * aufgerufen werden soll, die im TextDocumentModel model aktuell definiert ist.
 * Die Methode erwartet, dass vor dem Aufruf geprüft wurde, ob model eine
 * Druckfunktion definiert. Ist dennoch keine Druckfunktion definiert, so erscheint
 * eine Fehlermeldung im Log.
 *
 * Das Event wird ausgelöst, wenn der registrierte WollMuxDispatchInterceptor eines
 * Dokuments eine entsprechende Nachricht bekommt.
 */
public class OnExecutePrintFunction extends BasicEvent 
{
    private TextDocumentController documentController;

    public OnExecutePrintFunction(TextDocumentController documentController)
    {
      this.documentController = documentController;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      // Prüfen, ob alle gesetzten Druckfunktionen im aktuellen Kontext noch
      // Sinn machen:
      checkPrintPreconditions(documentController);
      stabilize();

      // Die im Dokument gesetzten Druckfunktionen ausführen:
      final XPrintModel pmod = PrintModels.createPrintModel(documentController, true);

      // Drucken im Hintergrund, damit der WollMuxEventHandler weiterläuft.
      new Thread()
      {
        @Override
        public void run()
        {
          pmod.printWithProps();
        }
      }.start();
    }

    /**
     * Es kann sein, dass zum Zeitpunkt des Drucken-Aufrufs eine Druckfunktion
     * gesetzt hat, die in der aktuellen Situation nicht mehr sinnvoll ist; Dieser
     * Umstand wird in checkPreconditons geprüft und die betroffene Druckfunktion
     * ggf. aus der Liste der Druckfunktionen entfernt.
     *
     * @param printFunctions
     *          Menge der aktuell gesetzten Druckfunktionen.
     *
     * @author Christoph Lutz (D-III-ITD-5.1)
     */
    protected static void checkPrintPreconditions(TextDocumentController documentController)
    {
      Set<String> printFunctions = documentController.getModel().getPrintFunctions();

      // Ziffernanpassung der Sachleitenden Verfügungen durlaufen lassen, um zu
      // erkennen, ob Verfügungspunkte manuell aus dem Dokument gelöscht
      // wurden ohne die entsprechenden Knöpfe zum Einfügen/Entfernen von
      // Ziffern zu drücken.
      if (printFunctions.contains(SachleitendeVerfuegung.PRINT_FUNCTION_NAME))
      {
        SachleitendeVerfuegung.ziffernAnpassen(documentController);
      }

      // ...Platz für weitere Prüfungen.....
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "(" + documentController.getModel() + ")";
    }
  }
