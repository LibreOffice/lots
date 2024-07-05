/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2024 Landeshauptstadt München and LibreOffice contributors
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package org.libreoffice.lots.former.section.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.XNamed;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;

import org.libreoffice.ext.unohelper.common.UNO;
import org.libreoffice.ext.unohelper.common.UnoDictionary;
import org.libreoffice.ext.unohelper.util.UnoProperty;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.former.FormularMax4kController;
import org.libreoffice.lots.former.group.GroupsProvider;
import org.libreoffice.lots.former.model.IdModel;

/**
 * Stellt einen Bereich da (Format/Bereiche...)
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class SectionModel
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SectionModel.class);

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
  private List<ModelChangeListener> listeners = new ArrayList<>(1);

  /** GROUPS. */
  private GroupsProvider groups;

  /**
   * Der TextSections Service über den der Bereich zu diesem SectionModel ansprechbar
   * ist.
   */
  private UnoDictionary<XTextSection> textSections;

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
   */
  public SectionModel(String sectionName, XTextSectionsSupplier doc,
      FormularMax4kController formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
    parseName(sectionName);
    this.textSections = UnoDictionary.create(doc.getTextSections(), XTextSection.class);
  }

  /**
   * Nimmt den vollständigen Bereichsnamen entgegen und initialisiert
   * {@link #sectionNameComplete} und {@link #sectionNamePrefix} sowie
   * {@link #groups}.
   *
   * @param name
   */
  private void parseName(String name)
  {
    this.groups = new GroupsProvider();
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
        HashSet<IdModel> set = new HashSet<>(conf.count());
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
   */
  public FormularMax4kController getFormularMax4000()
  {
    return formularMax4000;
  }

  /**
   * Wenn der Name dieses Bereichs eine GROUPS-Angabe oder/und ein numerisches Suffix
   * hat, so wird der Name ohne diese Teile zurückgeliefert, ansonsten der komplette
   * Name.
   */
  public String getNamePrefix()
  {
    return sectionNamePrefix;
  }

  /**
   * Setzt name als neuen Namen dieses Bereichs (exklusive GROUPS-Angabe).
   */
  public void setNamePrefix(String name)
  {
    sectionNamePrefix = name;
    notifyListeners(NAME_ATTR, name);
  }

  /**
   * Entfernt den Bereich aus dem Dokument.
   */
  public void removeFromDocument()
  {
    try
    {
      XComponent textSection =
          UNO.XComponent(textSections.get(sectionNameComplete));
      textSection.dispose();
    }
    catch (Exception x)
    {
      LOGGER.error("Error while trying to remove section: \"{}\"", sectionNameComplete, x);
    }
  }

  /**
   * Updatet den Namen des zu diesem Model gehörenden Bereichs.
   *
   * @return false, wenn beim Update etwas schief geht (typischerweise weil der
   *         Benutzer den Bereich hinterrücks gelöscht oder umbenannt hat)
   */
  public boolean updateDocumentSection()
  {
    try
    {
      XNamed textSection = UNO.XNamed(textSections.get(sectionNameComplete));
      String nameBase = generateCompleteName();

      String name = nameBase;
      int count = 1;
      while (textSections.containsKey(name))
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
      LOGGER.error("Error while trying to update section: \"{}\"", sectionNameComplete, x);
      return false;
    }
  }

  /**
   * Liefert true gdw diese Section sichtbar ist.
   */
  public boolean isVisible()
  {
    try
    {
      Object textSection = textSections.get(sectionNameComplete);
      return textSection != null
          && Boolean.TRUE.equals(UnoProperty.getProperty(textSection, UnoProperty.IS_VISIBLE));
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
      return false;
    }
  }

  /**
   * Setzt die Sichtbarkeit dieses Bereichs auf visible.
   */
  public void setVisible(boolean visible)
  {
    try
    {
      Object textSection = textSections.get(sectionNameComplete);
      if (textSection == null)
        LOGGER.error("Section suddenly disappeared: \"{}\"", sectionNameComplete);
      else
        UnoProperty.setProperty(textSection, UnoProperty.IS_VISIBLE, Boolean.valueOf(visible));
    }
    catch (Exception x)
    {
      LOGGER.error("", x);
    }
  }

  /**
   * Liefert true gdw, die GROUPS-Angabe nicht leer ist.
   */
  public boolean hasGroups()
  {
    return groups.hasGroups();
  }

  /**
   * Liefert den GroupsProvider, der die GROUPS dieses Models managet.
   */
  public GroupsProvider getGroupsProvider()
  {
    return groups;
  }

  /**
   * Setzt {@link #sectionNamePrefix} und {@link #groups} zu einem kompletten
   * Bereichsnamen zusammen (noch ohne numerisches Suffix).
   */
  private String generateCompleteName()
  {
    if (groups.hasGroups())
    {
      StringBuilder buffy = new StringBuilder(sectionNamePrefix);
      buffy.append(' ');
      ConfigThingy grps = new ConfigThingy("GROUPS");
      for (IdModel gid : groups)
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
   */
  public void addListener(ModelChangeListener listener)
  {
    if (!listeners.contains(listener)) listeners.add(listener);
  }

  /**
   * Benachrichtigt alle auf diesem Model registrierten Listener, dass das Model aus
   * seinem Container entfernt wurde. ACHTUNG! Darf nur von einem entsprechenden
   * Container aufgerufen werden, der das Model enthält.
   */
  public void hasBeenRemoved()
  {
    Iterator<ModelChangeListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = iter.next();
      listener.modelRemoved(this);
    }
    formularMax4000.updateDocument();
  }

  /**
   * Ruft für jeden auf diesem Model registrierten {@link ModelChangeListener} die Methode
   * {@link ModelChangeListener#attributeChanged(SectionModel, int, Object)} auf.
   */
  protected void notifyListeners(int attributeId, Object newValue)
  {
    Iterator<ModelChangeListener> iter = listeners.iterator();
    while (iter.hasNext())
    {
      ModelChangeListener listener = iter.next();
      listener.attributeChanged(this, attributeId, newValue);
    }
  }

  /**
   * Interface für Listener, die über Änderungen eines Models informiert werden
   * wollen.
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
     */
    public void attributeChanged(SectionModel model, int attributeId, Object newValue);

    /**
     * Wird aufgerufen, wenn model aus seinem Container entfernt wird (und damit in
     * keiner View mehr angezeigt werden soll).
     */
    public void modelRemoved(SectionModel model);
  }

}
