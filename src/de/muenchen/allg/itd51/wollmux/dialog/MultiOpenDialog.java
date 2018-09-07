package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.event.Dispatch;

public class MultiOpenDialog extends JFrame
{

  private static final Logger LOGGER = LoggerFactory
      .getLogger(MultiOpenDialog.class);

  private static final long serialVersionUID = 1L;

  private ConfigThingy conf;
  private WollMuxBarEventHandler eventHandler;

  public MultiOpenDialog(String title, ConfigThingy conf, WollMuxBarEventHandler eventHandler) throws HeadlessException
  {
    super(title);
    this.conf = conf;
    this.eventHandler = eventHandler;

    init();
  }

  private void init()
  {
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    Box vbox = Box.createVerticalBox();
    getContentPane().add(vbox);
    vbox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    Box hbox;
    /*
     * hbox = Box.createHorizontalBox(); hbox.add(new JLabel(L.m("Was möchten Sie
     * öffnen ?"))); hbox.add(Box.createHorizontalGlue()); vbox.add(hbox);
     * vbox.add(Box.createVerticalStrut(5));
     */
    final ConfigThingy openConf = new ConfigThingy(conf); // Kopie machen, die
    // manipuliert werden darf.
    Iterator<ConfigThingy> iter;
    try
    {
      iter = conf.get("Labels").iterator();
    }
    catch (NodeNotFoundException e2)
    {
      LOGGER.error(L.m("ACTION \"open\" erfordert Abschnitt \"Labels\" in den OPEN-Angaben"));
      return;
    }
    final List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();
    while (iter.hasNext())
    {
      hbox = Box.createHorizontalBox();
      String label = iter.next().toString();
      JCheckBox checkbox = new JCheckBox(label, true);
      checkBoxes.add(checkbox);
      hbox.add(checkbox);
      hbox.add(Box.createHorizontalGlue());
      vbox.add(hbox);
      vbox.add(Box.createVerticalStrut(5));
    }

    hbox = Box.createHorizontalBox();
    JButton button = new JButton(L.m("Abbrechen"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        dispose();
      }
    });
    hbox.add(button);
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(Box.createHorizontalGlue());

    button = new JButton(L.m("Alle"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        Iterator<JCheckBox> iter = checkBoxes.iterator();
        while (iter.hasNext())
          iter.next().setSelected(true);
      }
    });
    hbox.add(button);
    hbox.add(Box.createHorizontalStrut(5));

    button = new JButton(L.m("Keine"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        Iterator<JCheckBox> iter = checkBoxes.iterator();
        while (iter.hasNext())
          iter.next().setSelected(false);
      }
    });
    hbox.add(button);
    hbox.add(Box.createHorizontalStrut(5));
    hbox.add(Box.createHorizontalGlue());

    button = new JButton(L.m("Öffnen"));
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        dispose();
        Iterator<JCheckBox> iter = checkBoxes.iterator();
        ConfigThingy fragConf;
        try
        {
          fragConf = openConf.get("Fragmente", 1);
        }
        catch (NodeNotFoundException e1)
        {
          LOGGER.error(L.m("Abschnitt \"Fragmente\" fehlt in OPEN-Angabe"));
          return;
        }
        Iterator<ConfigThingy> fragIter = fragConf.iterator();
        while (iter.hasNext() && fragIter.hasNext())
        {
          fragIter.next();
          JCheckBox checkbox = iter.next();
          if (!checkbox.isSelected()) {
            fragIter.remove();
          }
        }

        eventHandler.handleWollMuxUrl(Dispatch.DISP_wmOpen,
          openConf.stringRepresentation(true, '"', false));
      }
    });
    hbox.add(button);

    vbox.add(hbox);

    Common.setWollMuxIcon(this);
    setAlwaysOnTop(true);
    pack();
    int frameWidth = getWidth();
    int frameHeight = getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    setLocation(x, y);
  }
}
