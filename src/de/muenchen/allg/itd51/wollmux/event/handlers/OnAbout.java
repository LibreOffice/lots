package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Align;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.ControlType;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlModel.Orientation;
import de.muenchen.allg.itd51.wollmux.core.dialog.ControlProperties;
import de.muenchen.allg.itd51.wollmux.core.dialog.SimpleDialogLayout;
import de.muenchen.allg.itd51.wollmux.core.dialog.UNODialogFactory;

/**
 * Erzeugt ein neues WollMuxEvent, das einen modalen Dialog anzeigt, der wichtige
 * Versionsinformationen über den WollMux, die Konfiguration und die WollMuxBar
 * (nur falls wollmuxBarVersion nicht der Leersting ist) enthält. Anmerkung: das
 * WollMux-Modul hat keine Ahnung, welche WollMuxBar verwendet wird. Daher ist es
 * möglich, über den Parameter wollMuxBarVersion eine Versionsnummer der WollMuxBar
 * zu übergeben, die im Dialog angezeigt wird, falls wollMuxBarVersion nicht der
 * Leerstring ist.
 *
 * Dieses Event wird vom WollMux-Service (...comp.WollMux) ausgelöst, wenn die
 * WollMux-url "wollmux:about" aufgerufen wurde.
 */
public class OnAbout extends BasicEvent
{
  private static final Logger LOGGER = LoggerFactory
      .getLogger(OnAbout.class);
  private String wollMuxBarVersion;
  private UNODialogFactory dialogFactory;
  private SimpleDialogLayout layout;

  private final URL WM_URL = this.getClass().getClassLoader().getResource(
      "data/wollmux.jpg");

  public OnAbout(String wollMuxBarVersion)
  {
    this.wollMuxBarVersion = wollMuxBarVersion;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    
    dialogFactory = new UNODialogFactory();
    XWindow dialogWindow = dialogFactory.createDialog(780, 450, 0xF2F2F2);

    dialogFactory.showDialog();

    layout = new SimpleDialogLayout(dialogWindow);
    layout.setMarginBetweenControls(15);
    layout.setMarginTop(20);
    layout.setMarginLeft(20);
    layout.setWindowBottomMargin(10);

    layout.addControlsToList(addIntroControls());
    layout.addControlsToList(addAuthors());
    layout.addControlsToList(addInfo());

    layout.draw();
  }
  
  private ControlModel addInfo()
  {
    List<ControlProperties> introControls = new ArrayList<>();
    
    ControlProperties info = new ControlProperties(ControlType.LABEL, "info");
    info.setControlPercentSize(100, 20);
    info.setLabel("Info");
    
    ControlProperties hLine = new ControlProperties(ControlType.LINE, "hLine2");
    hLine.setControlPercentSize(100, 10);
    
    ControlProperties wmVersion = new ControlProperties(ControlType.LABEL, "wmVersion");
    wmVersion.setControlPercentSize(100, 20);
    wmVersion.setLabel("WollMux Version: " + WollMuxSingleton.getVersion());
    
    ControlProperties wmBar = new ControlProperties(ControlType.LABEL, "wmBar");
    wmBar.setControlPercentSize(100, 20);
    wmBar.setLabel("WollMux-Leiste Version: " + wollMuxBarVersion);
    
    ControlProperties wmConfig = new ControlProperties(ControlType.LABEL, "wmConfig");
    wmConfig.setControlPercentSize(100, 20);
    wmConfig.setLabel("WollMux-Konfiguration: " + WollMuxSingleton.getInstance().getConfVersionInfo());
    
    introControls.add(info);
    introControls.add(hLine);
    introControls.add(wmVersion);
    introControls.add(wmBar);
    introControls.add(wmConfig);
    
    return new ControlModel(Orientation.VERTICAL, Align.NONE, introControls, Optional.empty());
  }
  
  private ControlModel addAuthors()
  {
    List<ControlProperties> introControls = new ArrayList<>();

    ControlProperties autoren = new ControlProperties(ControlType.LABEL, "labelAutors");
    autoren.setControlPercentSize(100, 20);
    autoren.setLabel("Autoren");
    
    ControlProperties hLine = new ControlProperties(ControlType.LINE, "hLine");
    hLine.setControlPercentSize(100, 10);
    
    ControlProperties bk = new ControlProperties(ControlType.LABEL, "bk");
    bk.setControlPercentSize(100, 20);
    bk.setLabel("Matthias S. Benkmann");
    
    ControlProperties cl = new ControlProperties(ControlType.LABEL, "cl");
    cl.setControlPercentSize(100, 20);
    cl.setLabel("Christoph Lutz");
    
    ControlProperties bk1 = new ControlProperties(ControlType.LABEL, "bk1");
    bk1.setControlPercentSize(100, 20);
    bk1.setLabel("Daniel Benkmann");
    
    ControlProperties bb = new ControlProperties(ControlType.LABEL, "bb");
    bb.setControlPercentSize(100, 20);
    bb.setLabel("Bettina Bauer");
    
    ControlProperties ae = new ControlProperties(ControlType.LABEL, "ae");
    ae.setControlPercentSize(100, 20);
    ae.setLabel("Andor Ertsey");
    
    ControlProperties mm = new ControlProperties(ControlType.LABEL, "mm");
    mm.setControlPercentSize(100, 20);
    mm.setLabel("Max Meier");
    
    introControls.add(autoren);
    introControls.add(hLine);
    introControls.add(bk);
    introControls.add(cl);
    introControls.add(bk1);
    introControls.add(ae);
    introControls.add(mm);
    
    return new ControlModel(Orientation.VERTICAL, Align.NONE, introControls, Optional.empty());
  }
  
  private ControlModel addIntroControls()
  {
    List<ControlProperties> introControls = new ArrayList<>();

    ControlProperties image = new ControlProperties(ControlType.IMAGE_CONTROL, "wmImage");
    image.setControlPercentSize(30, 100);

    XPropertySet props = UNO.XPropertySet(image.getXControl().getModel());
    try
    {
      props.setPropertyValue("ImageURL", WM_URL.toString());
    } catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e)
    {
      LOGGER.error("", e);
    } 
    
    ControlProperties label = new ControlProperties(ControlType.LABEL, "introLabel");
    label.setControlPercentSize(70, 20);
    label.setLabel("WollMux " + WollMuxSingleton.getVersion());
    
    introControls.add(image);
    introControls.add(label);
    
    return new ControlModel(Orientation.HORIZONTAL, Align.NONE, introControls, Optional.empty());
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "()";
  }
}