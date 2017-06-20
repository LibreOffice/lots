package de.muenchen.allg.itd51.wollmux.dialog.formmodel;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.muenchen.allg.itd51.wollmux.GlobalFunctions;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.document.TextDocumentModel;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.dialog.DialogFactory;
import de.muenchen.allg.itd51.wollmux.dialog.FormGUI;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;
import de.muenchen.allg.itd51.wollmux.func.FunctionFactory;

/**
 * Repräsentiert ein FormModel dem mehrere Formulardokumente zugeordnet sind, die
 * alle in gleicher Weise über Änderungen (z.B. bei valueChanged()) informiert
 * werden. So ist ein gleichzeitiges Befüllen meherer Dokumente über nur ein
 * Formular möglich. Die Formularbeschreibungen der Einzeldokumente und die in
 * ihnen enthaltenen IDs, Funktionen und Dialogfunktionen müssen dabei vor dem
 * Erzeugen des Objekts zu einer Gesamtbeschreibung zusammengemerged werden.
 * 
 * @author christoph.lutz
 */
public class MultiDocumentFormModel implements FormModel
{
  private Vector<FormModel> formModels;

  private final ConfigThingy formFensterConf;

  private final ConfigThingy formConf;

  private final Map<Object, Object> functionContext;

  private final FunctionLibrary funcLib;

  private final DialogLibrary dialogLib;

  private FormGUI formGUI = null;

  private List<TextDocumentController> documentControllers;

  public static final String MULTI_FORM_TITLE =
  L.m("Mehrere Formulare gleichzeitig ausfüllen");

  /**
   * Konstruktor für ein MultiDocumentFormModel mit den zugehörigen
   * TextDocumentModel Objekten docs und den zugehörigen FormModel Objekten
   * formModels. Das MultiDocumentFormModel leitet alle Anfragen an die
   * mitgelieferten FormModel Objekte weiter.
   * 
   * @param docs
   *          Vektor mit den TextDocumentModel Objekten der Einzeldokumente.
   * @param formModels
   *          Vektor mit den zu den Einzeldokumenten zugehörigen FormModel-Objekten
   *          (muss die selbe Größe und die selbe Reihenfolge wie docs haben).
   * @param mapDocsToFormModels
   *          enthält die zugeordneten TextDocumentModels.
   * @param formFensterConf
   *          Der Formular-Unterabschnitt des Fenster-Abschnitts von wollmux.conf
   *          (wird für createFormGUI() benötigt).
   * @param formConf
   *          der Formular-Knoten, der die Formularbeschreibung enthält (wird für
   *          createFormGUI() benötigt).
   * @param functionContext
   *          der Kontext für Funktionen, die einen benötigen (wird für
   *          createFormGUI() benötigt).
   * @param funcLib
   *          die Funktionsbibliothek, die zur Auswertung von Trafos, Plausis etc.
   *          herangezogen werden soll.
   * @param dialogLib
   *          die Dialogbibliothek, die die Dialoge bereitstellt, die für
   *          automatisch zu befüllende Formularfelder benötigt werden (wird für
   *          createFormGUI() benötigt).
   */
  public MultiDocumentFormModel(List<TextDocumentController> documentControllers,
      Vector<FormModel> formModels, final ConfigThingy formFensterConf,
      final ConfigThingy formConf, final Map<Object, Object> functionContext,
      final FunctionLibrary funcLib, final DialogLibrary dialogLib)
  {
    this.documentControllers = documentControllers;
    this.formModels = formModels;
    this.formFensterConf = formFensterConf;
    this.formConf = formConf;
    this.functionContext = functionContext;
    this.funcLib = funcLib;
    this.dialogLib = dialogLib;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.FormModel#setWindowPosSize(int, int, int,
   * int)
   */
  @Override
  public void setWindowPosSize(int docX, int docY, int docWidth, int docHeight)
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.setWindowPosSize(docX, docY, docWidth, docHeight);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.FormModel#setWindowVisible(boolean)
   */
  @Override
  public void setWindowVisible(boolean vis)
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.setWindowVisible(vis);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.FormModel#close()
   */
  @Override
  public void close()
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.close();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.muenchen.allg.itd51.wollmux.FormModel#setVisibleState(java.lang.String,
   * boolean)
   */
  @Override
  public void setVisibleState(String groupId, boolean visible)
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.setVisibleState(groupId, visible);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.FormModel#valueChanged(java.lang.String,
   * java.lang.String)
   */
  @Override
  public void valueChanged(String fieldId, String newValue)
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.valueChanged(fieldId, newValue);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.FormModel#focusGained(java.lang.String)
   */
  @Override
  public void focusGained(String fieldId)
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.focusGained(fieldId);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.FormModel#focusLost(java.lang.String)
   */
  @Override
  public void focusLost(String fieldId)
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.focusLost(fieldId);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.FormModel#print()
   */
  @Override
  public void print()
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.print();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.FormModel#pdf()
   */
  @Override
  public void pdf()
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.pdf();
    }
  }

  @Override
  public void save()
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.save();
    }
  }

  @Override
  public void saveAs()
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.saveAs();
    }
  }

  @Override
  public void closeAndOpenExt(String ext)
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.closeAndOpenExt(ext);
    }
  }

  @Override
  public void saveTempAndOpenExt(String ext)
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.saveTempAndOpenExt(ext);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.FormModel#formControllerInitCompleted()
   */
  @Override
  public void formControllerInitCompleted()
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      FormModel fm = formModels.get(i);
      fm.formControllerInitCompleted();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.muenchen.allg.itd51.wollmux.FormModel#disposing(de.muenchen.allg.itd51.
   * wollmux.TextDocumentModel)
   * 
   * TESTED
   */
  @Override
  public void closing(Object sender)
  {
    for (int i = 0; i < documentControllers.size(); i++)
    {
      TextDocumentModel doc = documentControllers.get(i).getModel();
      FormModel fm = formModels.get(i);

      if (doc.equals(sender))
      {
        fm.closing(sender);
        documentControllers.remove(i);
        formModels.remove(i);
      }
    }

    // FormGUI beenden (falls bisher eine gesetzt ist)
    if (documentControllers.size() == 0 && formGUI != null)
    {
      formGUI.dispose();
      formGUI = null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.FormModel#setValue(java.lang.String,
   * java.lang.String, java.awt.event.ActionListener)
   */
  @Override
  public void setValue(String fieldId, String value, ActionListener listener)
  {
    if (formGUI != null)
      formGUI.getController().setValue(fieldId, value, listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.muenchen.allg.itd51.wollmux.FormModel#startFormGUI()
   */
  @Override
  public void startFormGUI()
  {
    // Als mapIDToPresetValue kann hier eine leere Map übergeben werden, da
    // Multiform nur beim erstmaligen Öffnen der Vorlagen ausgeführt wird. Zu
    // diesem Zeitpunkt sind noch keine Formularwerte durch Benutzer eingegeben
    // worden - weder in der FormGUI noch in den Einfügungen im Dokument - und
    // somit darf auch diese HashMap keinen Inhalt haben.
    formGUI =
      new FormGUI(formFensterConf, formConf, this, new HashMap<String, String>(),
        functionContext, funcLib, dialogLib, true);
  }

  @Override
  public String getWindowTitle()
  {
    return null;
  }
  
  @Override
  public void openTemplateOrDocument(List<String> fragIds){
	  //TODO Auto-generated method stub
  }

  /**
   * Erzeugt ein FormModel dem mehrere Formulardokumente zugeordnet sind, die alle in
   * gleicher Weise über Änderungen (z.B. bei valueChanged()) informiert werden. So
   * ist ein gleichzeitiges Befüllen meherer Dokumente über nur ein Formular möglich.
   * Die Formularbeschreibungen der Einzeldokumente und die in ihnen enthaltenen IDs,
   * Funktionen und Dialogfunktionen werden dabei zu einer Gesamtbeschreibung im
   * selben Namensraum zusammengemerged.
   * 
   * @param docs
   *          Ein Vector mit TextDocumentModel Objekten die dem neuen
   *          MultiDocumentFormModel zugeordnet werden sollen.
   * @return Ein FormModel, das alle Änderungen auf alle in docs enthaltenen
   *         Formulardokumente überträgt.
   * @throws InvalidFormDescriptorException
   */
  public static FormModel createMultiDocumentFormModel(
      List<TextDocumentController> documentControllers, ConfigThingy buttonAnpassung)
      throws InvalidFormDescriptorException
  {
  
    // Formular-Abschnitte aller TextDocumentModels sammeln...
    ArrayList<ConfigThingy> formularSections = new ArrayList<ConfigThingy>();
    for (TextDocumentController documentController : documentControllers)
    {
      try
      {
        ConfigThingy formular = documentController.getFormDescription().get("Formular");
        formularSections.add(formular);
      }
      catch (NodeNotFoundException e)
      {
        Logger.error(L.m("Dokument '%1' enthält keine gültige Formularbeschreibung",
          documentController.getFrameController().getTitle()), e);
      }
    }
  
    // ...und mergen
    ConfigThingy formConf =
      TextDocumentModel.mergeFormDescriptors(formularSections, buttonAnpassung,
        MultiDocumentFormModel.MULTI_FORM_TITLE);
  
    // FunctionContext erzeugen und im Formular definierte
    // Funktionen/DialogFunktionen parsen:
    Map<Object, Object> functionContext = new HashMap<Object, Object>();
    DialogLibrary dialogLib =
      DialogFactory.parseFunctionDialogs(formConf, GlobalFunctions.getInstance().getFunctionDialogs(),
        functionContext);
    // FIXME: hier müsste eine gemergte Variante der Funktionsbibliotheken der
    // einzel-TextDocumentModels erzeugt werden, damit auch dokumentlokale
    // Trafos funktionieren - aber wer verwendet schon Multiform? Warten wir mit
    // der Änderung sie jemand benötigt.
    FunctionLibrary funcLib =
      FunctionFactory.parseFunctions(formConf, dialogLib, functionContext,
        GlobalFunctions.getInstance().getGlobalFunctions());
  
    // Abschnitt Fenster/Formular aus wollmuxConf holen:
    ConfigThingy formFensterConf = new ConfigThingy("");
    try
    {
      formFensterConf =
        WollMuxFiles.getWollmuxConf().query("Fenster").query("Formular").getLastChild();
    }
    catch (NodeNotFoundException x)
    {}
  
    // FormModels für die Einzeldokumente erzeugen
    Vector /* of FormModel */<FormModel> fms = new Vector<FormModel>();
    for (TextDocumentController documentController : documentControllers)
    {
      FormModel fm =
        new SingleDocumentFormModel(documentController, formFensterConf, formConf,
          functionContext, funcLib, dialogLib, true);
      fms.add(fm);
    }
  
    return new MultiDocumentFormModel(documentControllers, fms, formFensterConf,
      formConf, functionContext, funcLib, dialogLib);
  }
}