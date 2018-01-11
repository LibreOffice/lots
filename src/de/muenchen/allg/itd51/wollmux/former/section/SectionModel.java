/*
 * Dateiname: SectionModel.java
 * Projekt  : WollMux
 * Funktion : Stellt einen Bereich da (Format/Bereiche...)
 * 
 * Copyright (c) 2008-2018 Landeshauptstadt München
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
 * 24.03.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD D.10)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.former.section;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextSectionsSupplier;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.IDManager;
import de.muenchen.allg.itd51.wollmux.former.group.GroupsProvider;
import de.muenchen.allg.itd51.wollmux.former.insertion.InsertionModel;

/**
 * Stellt einen Bereich da (Format/Bereiche...)
 * 
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class SectionModel
{
  /**
   * Attribut-ID-Konstante für
   * {@link ModelChangeListener#attributeChanged(SectionModel, int, Object)}.
   */
  public static final int NAME_ATTR = 0;

  private static final Pattern SECTION_NAME_PATTERN =
    Pattern.compile("\\A(.*)(GROUPS.*[^\\d])\\d*\\z");

  /**
   * Der FormularMax4000 zu dem dieses Model gehört.
   */
  private FormularMax4kController formularMax4000;

  /**
   * Die {@link ModelChangeListener}, die über Änderungen dieses Models informiert
   * werden wollen.
   */
  private List<ModelChangeListener> listeners = new Vector<ModelChangeListener>(1);

  /** GROUPS. */
  private GroupsProvider groups;

  /**
   * Der TextSections Service über den der Bereich zu diesem SectionModel ansprechbar
   * ist.
   */
  private XNameAccess textSections;

  /**
   * Der vollständige Name des Bereichs unter dem er von {@link #textSections}
   * verwaltet wird. ACHTUNG! Wegen des verzögerten Updates des Dokuments kann dieser
   * Wert kurzzeitig nicht mit {@link #sectionNamePrefix} zusammenpassen.
   * sectionNamePrefix wird sofort geupdatet, wenn der Benutzer in der GUI den Namen
   * eines Bereichs ändert, aber {@link #sectionNameComplete}
   */
  private String sectionNameComplete;

  /**
   * Der Teil von {@link #sectionNameComplete}, der vor der GROUPS-Angabe (falls
   * vorhanden) oder einen numerischen Suffix (falls vorhanden) steht. Falls diese
   * TextSection keine Sichtbarkeitsgruppen definiert hat, ist dieser Wert der selbe
   * wie {@link #sectionNameComplete} minus ein evtl. vorhandenes numerisches Suffix.
   * ACHTUNG! Wegen des verzögerten Updates des Dokuments kann dieser Wert kurzzeitig
   * nicht mit {@link #sectionNameComplete} zusammenpassen. sectionNamePrefix wird
   * sofort geupdatet, wenn der Benutzer in der GUI den Namen eines Bereichs ändert,
   * aber {@link #sectionNameComplete} wird erst nachgezogen, wenn das Dokument
   * geupdatet wird.
   */
  private String sectionNamePrefix;

  /**
   * Erzeugt ein neues SectionModel für einen Bereich names sectionName, der über doc
   * ansprechbar ist.
   * 
   * @param formularMax4000
   *          der dieses SectionModel verwaltet
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  public SectionModel(String sectionName, XTextSectionsSupplier doc,
      FormularMax4kController formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
    parseName(sectionName);
    this.textSections = doc.getTextSections();
  }

  /**
   * Nimmt den vollständigen Bereichsnamen entgegen und initialisiert
   * {@link #sectionNameComplete} und {@link #sectionNamePrefix} sowie
   * {@link #groups}.
   * 
   * @param name
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  private void parseName(String name)
  {
    this.groups = new GroupsProvider(formularMax4000);
    sectionNameComplete = name;
    sectionNamePrefix = name;
    Matcher m = SECTION_NAME_PATTERN.matcher(name);
    if (m.matches())
    {
      String prefix = m.group(1);
      if (prefix.endsWith(" ")) prefix = prefix.substring(0, prefix.length() - 1);
      try
      {
        ConfigThingy conf = new ConfigThingy("GROUPS", m.group(2)); // GROUPS-GROUPS-(...)
        conf = conf.getFirstChild(); // der eigentliche "GROUPS" Knoten
        HashSet<IDManager.ID> set = new HashSet<IDManager.ID>(conf.count());
        Iterator<ConfigThingy> iter = conf.iterator();
        while (iter.hasNext())
        {
          set.add(formularMax4000.getIDManager().getID(
            FormularMax4kController.NAMESPACE_GROUPS, iter.next().toString()));
        }
        // Prefix erst hier ins globale Feld übertragen, wenn keine Exception
        // geflogen ist.
        sectionNamePrefix = prefix;
        groups.initGroups(set);
      }
      catch (Exception x)
      {
        // offenbar keine WollMux-Sichtbarkeitsgruppenliste
      }
    }
  }

  /**
   * Liefert den FormularMax4000 zu dem dieses Model gehört.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public FormularMax4kController getFormularMax4000()
  {
    return formularMax4000;
  }

  /**
   * Wenn der Name dieses Bereichs eine GROUPS-Angabe oder/und ein numerisches Suffix
   * hat, so wird der Name ohne diese Teile zurückgeliefert, ansonsten der komplette
   * Name.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public String getNamePrefix()
  {
    return sectionNamePrefix;
  }

  /**
   * Setzt name als neuen Namen dieses Bereichs (exklusive GROUPS-Angabe).
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  public void setNamePrefix(String name)
  {
    sectionNamePrefix = name;
    notifyListeners(NAME_ATTR, name);
    // formularMax4000.documentNeedsUpdating(); ist bereits in notifyListeners()
  }

  /**
   * Entfernt den Bereich aus dem Dokument.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  public void removeFromDocument()
  {
    try
    {
      XComponent textSection =
        UNO.XComponent(textSections.getByName(sectionNameComplete));
      textSection.dispose();
    }
    catch (Exception x)
    {
      Logger.error(L.m("Fehler beim Versuch, Bereich zu entfernen: \"%1\"",
        sectionNameComplete), x);
    }
  }

  /**
   * Updatet den Namen des zu diesem Model gehörenden Bereichs.
   * 
   * @return false, wenn beim Update etwas schief geht (typischerweise weil der
   *         Benutzer den Bereich hinterrücks gelöscht oder umbenannt hat)
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  public boolean updateDocument()
  {
    try
    {
      XNamed textSection = UNO.XNamed(textSections.getByName(sectionNameComplete));
      String nameBase = generateCompleteName();

      String name = nameBase;
      int count = 1;
      while (textSections.hasByName(name))
      {
        /*
         * Wenn unser alter Name ein passender neuer Name ist, müssen wir nix tun.
         */
        if (name.equals(sectionNameComplete)) return true;
        name = nameBase + (count++);
      }

      textSection.setName(name);
      sectionNameComplete = name;
      return true;
    }
    catch (Exception x)
    {
      Logger.error(L.m("Fehler beim Versuch, Bereich zu updaten: \"%1\"",
        sectionNameComplete), x);
      return false;
    }
  }

  /**
   * Liefert true gdw diese Section sichtbar ist.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TODO Testen
   */
  public boolean isVisible()
  {
    try
    {
      Object textSection = textSections.getByName(sectionNameComplete);
      return textSection != null
        && Boolean.TRUE.equals(UNO.getProperty(textSection, "IsVisible"));
    }
    catch (Exception x)
    {
      Logger.error(x);
      return false;
    }
  }

  /**
   * Setzt die Sichtbarkeit dieses Bereichs auf visible.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  public void setVisible(boolean visible)
  {
    try
    {
      Object textSection = textSections.getByName(sectionNameComplete);
      if (textSection == null)
        Logger.error(L.m("Bereich ist plötzlich verschwunden: \"%1\"",
          sectionNameComplete));
      else
        UNO.setProperty(textSection, "IsVisible", Boolean.valueOf(visible));
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Liefert true gdw, die GROUPS-Angabe nicht leer ist.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public boolean hasGroups()
  {
    return groups.hasGroups();
  }

  /**
   * Liefert den GroupsProvider, der die GROUPS dieses Models managet.
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   */
  public GroupsProvider getGroupsProvider()
  {
    return groups;
  }

  /**
   * Setzt {@link #sectionNamePrefix} und {@link #groups} zu einem kompletten
   * Bereichsnamen zusammen (noch ohne numerisches Suffix).
   * 
   * @author Matthias Benkmann (D-III-ITD-D101)
   * 
   * TESTED
   */
  private String generateCompleteName()
  {
    if (groups.hasGroups())
    {
      StringBuilder buffy = new StringBuilder(sectionNamePrefix);
      buffy.append(' ');
      ConfigThingy grps = new ConfigThingy("GROUPS");
      for (IDManager.ID gid : groups)
      {
        grps.add(gid.toString());
      }

      buffy.append(grps.stringRepresentation(false, '"', false));
      return buffy.toString();
    }
    else
      return sectionNamePrefix;
  }

  /**
   * listener wird über Änderungen des Models informiert.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void addListener(ModelChangeListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  /**
   * Benachrichtigt alle auf diesem Model registrierten Listener, dass das Model aus
   * seinem Container entfernt wurde. ACHTUNG! Darf nur von einem entsprechenden
   * Container aufgerufen werden, der das Model enthält.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  public void hasBeenRemoved()
  {
    Iterator<ModelChangeListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = iter.next();
      listener.modelRemoved(this);
    }
    formularMax4000.documentNeedsUpdating();
  }

  /**
   * Ruft für jeden auf diesem Model registrierten {@link ModelChangeListener} die
   * Methode
   * {@link ModelChangeListener#attributeChanged(InsertionModel, int, Object)} auf.
   */
  protected void notifyListeners(int attributeId, Object newValue)
  {
    Iterator<ModelChangeListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = iter.next();
      listener.attributeChanged(this, attributeId, newValue);
    }
    formularMax4000.documentNeedsUpdating();
  }

  /**
   * Interface für Listener, die über Änderungen eines Models informiert werden
   * wollen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static interface ModelChangeListener
  {
    /**
     * Wird aufgerufen wenn ein Attribut des Models sich geändert hat.
     * 
     * @param model
     *          das SectionModel, das sich geändert hat.
     * @param attributeId
     *          Welches Attribut hat sich geändert?
     * @param newValue
     *          der neue Wert des Attributs. Numerische Attribute werden als Integer
     *          übergeben.
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void attributeChanged(SectionModel model, int attributeId, Object newValue);

    /**
     * Wird aufgerufen, wenn model aus seinem Container entfernt wird (und damit in
     * keiner View mehr angezeigt werden soll).
     * 
     * @author Matthias Benkmann (D-III-ITD 5.1)
     */
    public void modelRemoved(SectionModel model);
  }

}
