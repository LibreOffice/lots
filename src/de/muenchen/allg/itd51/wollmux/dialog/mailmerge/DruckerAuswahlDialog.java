/*
 * Dateiname: DruckerAuswahlDialog.java
 * Projekt  : WollMux
 * Funktion : Die View für den Druckerauswahl Dialog beim Seriendruck.
 *            Siehe auch DruckerModel und DruckerControlller.
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import de.muenchen.allg.itd51.wollmux.dialog.Common;

public class DruckerAuswahlDialog implements ActionListener {
  
  DruckerModel model;
  DruckerController controller;
  JFrame parent;
  JDialog printerDialog;
  JComboBox<String> viewComboBox;
  JLabel viewLabel;
  JButton okButton;
  JButton abbrechButton;
    
  public DruckerAuswahlDialog(DruckerModel model, 
      DruckerController controller, JFrame parent) {
    this.model = model;
    this.controller = controller;  
    this.parent = parent;
  }
  
  public void erzeugeView(MailMergeParams mmp) {
    
    printerDialog = new JDialog(parent, "Drucker", true);
    viewLabel = new JLabel("Drucker Name: ");
    
    okButton = new JButton("OK");
    okButton.addActionListener(this);
    
    abbrechButton = new JButton("Abbrechen");
    abbrechButton.addActionListener(this);

    viewComboBox = new JComboBox<String>(new Vector<String>(controller.getAlleDrucker()));
    viewComboBox.setSelectedItem(controller.getDrucker());
    
    parent.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    Common.setWollMuxIcon(parent);
    printerDialog.setLayout(new GridBagLayout());
    
    GridBagConstraints c = new GridBagConstraints();
    c.insets = new Insets(30,10,10,10);  //padding

    c.gridx = 0;
    c.gridy = 0;
    printerDialog.add(viewLabel, c);

    c.gridx = 1;
    c.gridy = 0;
    printerDialog.add(viewComboBox, c);

    c.insets = new Insets(60,10,30,10);
    c.gridx = 0;
    c.gridy = 1;
    c.anchor = GridBagConstraints.WEST;
    printerDialog.add(okButton, c);

    c.gridx = 1;
    c.gridy = 1;
    c.anchor = GridBagConstraints.EAST;
    printerDialog.add(abbrechButton, c); 
    
    mmp.updateView();
    printerDialog.setLocation(500, 500);
    printerDialog.pack();
    printerDialog.setResizable(true);
    printerDialog.setVisible(true);     
  }
  
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == this.okButton) {
      controller.setDrucker((String)viewComboBox.getSelectedItem());
      printerDialog.setVisible(false);
      printerDialog.dispose();
    } else if (event.getSource() == this.abbrechButton) {
      printerDialog.setVisible(false);
      printerDialog.dispose();      
    }
  }
}
