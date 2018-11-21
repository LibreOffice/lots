package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.WollMuxSingleton;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.Common;

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
    private String wollMuxBarVersion;

    private final URL WM_URL = this.getClass().getClassLoader().getResource(
      "data/wollmux_klein.jpg");

    public OnAbout(String wollMuxBarVersion)
    {
      this.wollMuxBarVersion = wollMuxBarVersion;
    }

    @Override
    protected void doit() throws WollMuxFehlerException
    {
      Common.setLookAndFeelOnce();

      // non-modal dialog. Set 3rd param to true to make modal
      final JDialog dialog =
        new JDialog((Frame) null, L.m("Info über Vorlagen und Formulare (WollMux)"),
          false);
      JPanel myPanel = new JPanel();
      myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));
      myPanel.setBackground(Color.WHITE);
      dialog.setBackground(Color.WHITE);
      dialog.setContentPane(myPanel);
      JPanel imagePanel = new JPanel(new BorderLayout());
      imagePanel.add(new JLabel(new ImageIcon(WM_URL)), BorderLayout.CENTER);
      imagePanel.setOpaque(false);
      Box copyrightPanel = Box.createVerticalBox();
      copyrightPanel.setOpaque(false);
      Box hbox = Box.createHorizontalBox();
      hbox.add(imagePanel);
      hbox.add(copyrightPanel);
      myPanel.add(hbox);

      copyrightPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      JLabel label = new JLabel(L.m("WollMux") + " " + WollMuxSingleton.getVersion());
      Font largeFont = label.getFont().deriveFont(15.0f);
      label.setFont(largeFont);
      copyrightPanel.add(label);
      label = new JLabel(L.m("Copyright (c) 2005-2018 Landeshauptstadt München"));
      label.setFont(largeFont);
      copyrightPanel.add(label);
      label = new JLabel(L.m("Lizenz: %1", "European Union Public License"));
      label.setFont(largeFont);
      copyrightPanel.add(label);
      label = new JLabel(L.m("Homepage: %1", "www.wollmux.org"));
      label.setFont(largeFont);
      copyrightPanel.add(label);

      Box authorOuterPanel = Box.createHorizontalBox();
      authorOuterPanel.add(Box.createHorizontalStrut(8));

      Box authorPanelHBox = Box.createHorizontalBox();
      authorPanelHBox.setBorder(BorderFactory.createTitledBorder(L.m("Autoren (sortiert nach LOC)")));
      authorOuterPanel.add(authorPanelHBox);
      authorOuterPanel.add(Box.createHorizontalStrut(8));

      Box authorPanel = Box.createVerticalBox();
      authorPanelHBox.add(authorPanel);
      authorPanel.setOpaque(false);
      myPanel.add(authorOuterPanel);

      int authorSpacing = 1;

      hbox = Box.createHorizontalBox();
      // hbox.add(new JLabel(new ImageIcon(MB_URL)));
      // hbox.add(Box.createHorizontalStrut(8));
      hbox.add(new JLabel("Matthias S. Benkmann"));
      hbox.add(Box.createHorizontalGlue());
      authorPanel.add(hbox);

      hbox = Box.createHorizontalBox();
      // hbox.add(new JLabel(new ImageIcon(CL_URL)));
      // hbox.add(Box.createHorizontalStrut(8));
      hbox.add(new JLabel("Christoph Lutz"));
      hbox.add(Box.createHorizontalGlue());
      authorPanel.add(Box.createVerticalStrut(authorSpacing));
      authorPanel.add(hbox);

      authorPanel.add(Box.createVerticalGlue());

      // Box authorPanel2 = Box.createVerticalBox();
      // authorPanelHBox.add(authorPanel2);
      Box authorPanel2 = authorPanel;

      hbox = Box.createHorizontalBox();
      hbox.add(new JLabel("Daniel Benkmann"));
      hbox.add(Box.createHorizontalGlue());
      authorPanel2.add(Box.createVerticalStrut(authorSpacing));
      authorPanel2.add(hbox);
      hbox = Box.createHorizontalBox();
      hbox.add(new JLabel("Bettina Bauer"));
      hbox.add(Box.createHorizontalGlue());
      authorPanel2.add(Box.createVerticalStrut(authorSpacing));
      authorPanel2.add(hbox);
      hbox = Box.createHorizontalBox();
      hbox.add(new JLabel("Andor Ertsey"));
      hbox.add(Box.createHorizontalGlue());
      authorPanel2.add(Box.createVerticalStrut(authorSpacing));
      authorPanel2.add(hbox);
      hbox = Box.createHorizontalBox();
      hbox.add(new JLabel("Max Meier"));
      hbox.add(Box.createHorizontalGlue());
      authorPanel2.add(Box.createVerticalStrut(authorSpacing));
      authorPanel2.add(hbox);

      authorPanel2.add(Box.createVerticalGlue());

      myPanel.add(Box.createVerticalStrut(8));
      Box infoOuterPanel = Box.createHorizontalBox();
      infoOuterPanel.add(Box.createHorizontalStrut(8));
      JPanel infoPanel = new JPanel(new GridLayout(4, 1));
      infoOuterPanel.add(infoPanel);
      myPanel.add(infoOuterPanel);
      infoOuterPanel.add(Box.createHorizontalStrut(8));
      infoPanel.setOpaque(false);
      infoPanel.setBorder(BorderFactory.createTitledBorder(L.m("Info")));

      infoPanel.add(new JLabel(L.m("WollMux") + " " + WollMuxSingleton.getBuildInfo()));

      if (wollMuxBarVersion != null && !wollMuxBarVersion.equals(""))
        infoPanel.add(new JLabel(L.m("WollMux-Leiste") + " " + wollMuxBarVersion));

      infoPanel.add(new JLabel(L.m("WollMux-Konfiguration:") + " "
        + WollMuxSingleton.getInstance().getConfVersionInfo()));

      infoPanel.add(new JLabel("DEFAULT_CONTEXT: "
        + WollMuxFiles.getDEFAULT_CONTEXT().toExternalForm()));

      myPanel.add(Box.createVerticalStrut(4));

      hbox = Box.createHorizontalBox();
      hbox.add(Box.createHorizontalGlue());
      hbox.add(new JButton(new AbstractAction(L.m("OK"))
      {
        private static final long serialVersionUID = 4527702807001201116L;

        @Override
        public void actionPerformed(ActionEvent e)
        {
          dialog.dispose();
        }
      }));
      hbox.add(Box.createHorizontalGlue());
      myPanel.add(hbox);

      myPanel.add(Box.createVerticalStrut(4));

      dialog.setBackground(Color.WHITE);
      myPanel.setBackground(Color.WHITE);
      dialog.pack();
      int frameWidth = dialog.getWidth();
      int frameHeight = dialog.getHeight();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      int x = screenSize.width / 2 - frameWidth / 2;
      int y = screenSize.height / 2 - frameHeight / 2;
      dialog.setLocation(x, y);
      dialog.setAlwaysOnTop(true);
      dialog.setVisible(true);
    }

    @Override
    public String toString()
    {
      return this.getClass().getSimpleName() + "()";
    }
  }