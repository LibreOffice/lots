package de.muenchen.allg.itd51.wollmux.form.config;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.dialog.UIElementConfig;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.form.model.FormModelException;

/**
 * Description of a form.
 */
public class FormConfig
{

  private static final Logger LOGGER = LoggerFactory.getLogger(FormConfig.class);

  /**
   * List of visibilities in the form.
   */
  private final List<VisibilityGroupConfig> visiblities = new ArrayList<>();

  /**
   * List of all tabs.
   */
  private List<TabConfig> tabs = new ArrayList<>();

  /**
   * The title.
   */
  private String title;

  /**
   * The color of invalid fields.
   */
  private Color plausiMarkerColor;

  /**
   * A new form configuration.
   *
   * @param conf
   *          The description of the form.
   * @param frameTitle
   *          The title of the LibreOffic window.
   * @throws FormModelException
   *           Invalid form.
   */
  public FormConfig(ConfigThingy conf, String frameTitle) throws FormModelException
  {
    String formTitle = conf.getString("TITLE", L.m("Unbenanntes Formular"));
    if (frameTitle != null)
    {
      title = frameTitle + " - " + formTitle;
    } else
    {
      title = formTitle;
    }

    final ConfigThingy fensterDesc = conf.query("Fenster");
    try
    {
      for (ConfigThingy tabConf : fensterDesc.getLastChild())
      {
        tabs.add(new TabConfig(tabConf));
      }
    } catch (NodeNotFoundException e)
    {
      throw new FormModelException(L.m("Schlüssel 'Fenster' fehlt in %1", conf.getName()),
          e);
    }

    try
    {
      plausiMarkerColor = Color
          .decode(conf.get("PLAUSI_MARKER_COLOR", 1).getLastChild().toString());
    } catch (Exception x)
    {
      plausiMarkerColor = Color.RED;
    }

    try
    {
      ConfigThingy visibilityDesc = conf.query("Sichtbarkeit");
      if (visibilityDesc.count() > 0)
      {
        visibilityDesc = visibilityDesc.getLastChild();
      }
      for (ConfigThingy visRule : visibilityDesc)
      {
        visiblities.add(new VisibilityGroupConfig(visRule));
      }
    } catch (NodeNotFoundException x)
    {
      LOGGER.error("", x);
    }
  }

  public String getTitle()
  {
    return title;
  }

  public List<TabConfig> getTabs()
  {
    return tabs;
  }

  public List<VisibilityGroupConfig> getVisibilities()
  {
    return visiblities;
  }

  public Color getPlausiMarkerColor()
  {
    return plausiMarkerColor;
  }

  /**
   * Get all the controls (including buttons) of all tabs.
   *
   * @return Stream of all control configurations.
   */
  public Stream<UIElementConfig> getControls()
  {
    return Stream.concat(tabs.stream().flatMap(t -> t.getControls().stream()),
        tabs.stream().flatMap(t -> t.getButtons().stream()));
  }
}
