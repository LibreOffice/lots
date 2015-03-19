/*
 * Dateiname: DruckerController.java
 * Projekt  : WollMux
 * Funktion : Der Controller für den Druckerauswahl Dialog beim Seriendruck.
 *            Siehe auch DruckerModel und DruckerAuswahlDialog.
 * 
 * Copyright (c) 2014-2015 Landeshauptstadt München
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
 * 23.01.2014 | loi | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Judith Baur, Simona Loi
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog.mailmerge;

import java.util.ArrayList;
import java.util.List;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.JFrame;

import de.muenchen.allg.itd51.wollmux.dialog.PrintParametersDialog;

public class DruckerController {
 
  MailMergeParams mmp;
  DruckerModel model;
  DruckerAuswahlDialog view;
  JFrame parent;  
  ArrayList<String> alleDrucker = new ArrayList<String> ( );
  
  public DruckerController(DruckerModel model, JFrame parent, 
      MailMergeParams mmp) {
    
    this.mmp = mmp;
    this.model = model; 
    this.parent = parent;
    model.setDrucker(
      PrintParametersDialog.getCurrentPrinterName(
        mmp.getMMC().getTextDocument()));   
    
    PrintService[] printservices = PrintServiceLookup.lookupPrintServices(null,  null); 
    for ( PrintService service : printservices) {
      alleDrucker.add(service.getName());      
    }
    model.setAlleDrucker(alleDrucker);
  }
  
  public void erzeugeView() {
    view = new DruckerAuswahlDialog(model, this, parent);
    view.erzeugeView(mmp);    
  }
  
  public void setDrucker(String drucker) {
    model.setDrucker(drucker);
  }
  
  public String getDrucker() {
    return model.getDrucker();
  } 
    
  public List<String> getAlleDrucker() {
    return model.getAlleDrucker();
  }  
}
