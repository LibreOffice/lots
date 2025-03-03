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
package org.libreoffice.lots.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.libreoffice.lots.config.ConfigThingy;
import org.libreoffice.lots.config.ConfigurationErrorException;
import org.libreoffice.lots.config.NodeNotFoundException;
import org.libreoffice.lots.former.FormMaxConstants;
import org.libreoffice.lots.ui.UIElementConfig;
import org.libreoffice.lots.ui.UIElementType;
import org.libreoffice.lots.util.L;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description of a control element (menu entry, button, input field, ...)
 */
public class UIElementConfig
{

  private static final Logger LOGGER = LoggerFactory.getLogger(UIElementConfig.class);

  /**
   * A unique id.
   */
  private String id;

  /**
   * The label of the control.
   */
  private String label;

  /**
   * The type of the control.
   */
  private UIElementType type;

  /**
   * A tip to display on hover, may be null.
   */
  private String tip;

  /**
   * Options of {@link UIElementType#COMBOBOX}.
   */
  private List<String> options = new ArrayList<>();

  /**
   * Number of lines for {@link UIElementType#TEXTAREA}.
   */
  private int lines = 3;

  /**
   * Should text automatically wrap if type is {@link UIElementType#TEXTAREA}.
   */
  private boolean wrap = true;

  /**
   * Is the control read only?
   */
  private boolean readonly = false;

  /**
   * If type is {@link UIElementType#COMBOBOX} can there be new options?
   */
  private boolean editable = false;

  /**
   * The minimal size.
   */
  private int minsize = 0;

  /**
   * The preferred size.
   */
  private int prefsize = 0;

  /**
   * The maximum size.
   */
  private int maxsize = Integer.MAX_VALUE;

  /**
   * Hot key of the control.
   */
  private char hotkey = 0;

  /**
   * The action if the button is pressed.
   */
  private String action;

  /**
   * The dialog which is opened by the action funcDialog
   */
  private String dialog;

  /**
   * The external application to launch if action is openExt.
   */
  private String ext;

  /**
   * The URL used by the external application.
   */
  private String url;

  /**
   * ConfigThingy describing the plausibility function.
   */
  private ConfigThingy plausi;

  /**
   * The window to activate with the action switchWindow.
   */
  private String window;

  /**
   * FragIDs for openTemplate and openDocument actions.
   */
  private String fragId;

  /**
   * ConfigThingy describing the AutoFill function.
   */
  private ConfigThingy autofill;

  /**
   * The IDs of the visibility groups this element is in.
   */
  private List<String> groups = new ArrayList<>(1);

  /**
   * The sub menu to show.
   */
  private String menu;

  /**
   * Is this element visible in the sidebar?
   */
  private boolean sidebar;

  /**
   * The data source column this element contains.
   */
  private String dbSpalte;

  /**
   * The format of the column value.
   */
  private String display;

  /**
   * Create a new control element configuration.
   *
   * @param controlConf
   *          The description of the control element.
   * @throws ConfigurationErrorException
   *           Invalid control type.
   */
  public UIElementConfig(ConfigThingy controlConf)
  {
    type = UIElementType.getType(controlConf.getString("TYPE", ""));
    id = controlConf.getString("ID", RandomStringUtils.randomAlphanumeric(10));
    if (id.isEmpty())
    {
      id = RandomStringUtils.randomAlphanumeric(10);
    }
    label = controlConf.getString("LABEL", "");
    tip = controlConf.getString("TIP", "");
    String hotkeyString = controlConf.getString("HOTKEY", "");
    if (!hotkeyString.isEmpty())
    {
      hotkey = hotkeyString.toUpperCase().charAt(0);
    }
    action = controlConf.getString("ACTION", null);
    dialog = controlConf.getString("DIALOG", null);
    window = controlConf.getString("WINDOW", null);
    ConfigThingy fids = controlConf.query("FRAG_ID");
    StringBuilder fragIdBuilder = new StringBuilder();
    Iterator<ConfigThingy> i = fids.iterator();
    if (i.hasNext())
    {
      fragIdBuilder.append(i.next().toString());
      while (i.hasNext())
      {
        fragIdBuilder.append("&");
        fragIdBuilder.append(i.next().toString());
      }
    }
    fragId = fragIdBuilder.toString();
    ext = controlConf.getString("EXT", null);
    url = controlConf.getString("URL", null);
    readonly = Boolean.parseBoolean(controlConf.getString("READONLY", ""));
    editable = Boolean.parseBoolean(controlConf.getString("EDIT", ""));
    lines = Integer.parseInt(controlConf.getString("LINES", "3"));
    wrap = Boolean.parseBoolean(controlConf.getString("WRAP", "true"));
    minsize = Integer.parseInt(controlConf.getString("MINSIZE", "0"));
    prefsize = Integer.parseInt(controlConf.getString("PREFSIZE", "0"));
    maxsize = Integer.parseInt(controlConf.getString("MAXSIZE", "" + Integer.MAX_VALUE));
    try
    {
      for (ConfigThingy val : controlConf.get("VALUES"))
      {
        options.add(val.toString());
      }
    } catch (NodeNotFoundException x)
    {
      if (getType() == UIElementType.COMBOBOX)
      {
        LOGGER.error("Incorrect element of the type \"combobox\"", x);
      }
    }
    plausi = controlConf.query("PLAUSI");
    autofill = controlConf.query(FormMaxConstants.AUTOFILL);
    for (ConfigThingy groupsConf : controlConf.query("GROUPS"))
    {
      for (ConfigThingy groupConf : groupsConf)
      {
        groups.add(groupConf.toString());
      }
    }
    menu = controlConf.getString("MENU", null);
    dbSpalte = controlConf.getString("DB_COLUMN", null);
    display = controlConf.getString("DISPLAY", null);
    sidebar = Boolean.parseBoolean(controlConf.getString("SIDEBAR", "true"));
  }

  public String getId()
  {
    return id;
  }

  public String getLabel()
  {
    return label;
  }

  public UIElementType getType()
  {
    return type;
  }

  public String getTip()
  {
    return tip;
  }

  public List<String> getOptions()
  {
    return options;
  }

  public int getLines()
  {
    return lines;
  }

  public boolean isWrap()
  {
    return wrap;
  }

  public boolean isReadonly()
  {
    return readonly;
  }

  public boolean isEditable()
  {
    return editable;
  }

  public int getMinsize()
  {
    return minsize;
  }

  public char getHotkey()
  {
    return hotkey;
  }

  public String getAction()
  {
    return action;
  }

  public String getDialog()
  {
    return dialog;
  }

  public String getExt()
  {
    return ext;
  }

  public ConfigThingy getPlausi()
  {
    return plausi;
  }

  public int getPrefsize()
  {
    return prefsize;
  }

  public int getMaxsize()
  {
    return maxsize;
  }

  public String getWindow()
  {
    return window;
  }

  public String getFragId()
  {
    return fragId;
  }

  public String getUrl()
  {
    return url;
  }

  public ConfigThingy getAutofill()
  {
    return autofill;
  }

  public List<String> getGroups()
  {
    return groups;
  }

  public String getMenu()
  {
    return menu;
  }

  public String getDbSpalte()
  {
    return dbSpalte;
  }

  public String getDisplay()
  {
    return display;
  }

  public boolean isSidebar()
  {
    return sidebar;
  }
}
