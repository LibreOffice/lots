/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2022 Landeshauptstadt München
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
import de.muenchen.allg.itd51.wollmux.ComponentRegistration;
import de.muenchen.allg.itd51.wollmux.comp.WollMuxRegistration;
import de.muenchen.allg.itd51.wollmux.dispatch.Dispatcher;
import de.muenchen.allg.itd51.wollmux.dispatch.WollMuxDispatcher;
import de.muenchen.allg.itd51.wollmux.document.commands.OnProcessTextDocument;
import de.muenchen.allg.itd51.wollmux.event.CheckInstallationListener;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventListener;
import de.muenchen.allg.itd51.wollmux.event.WollMuxEventListenerImpl;
import de.muenchen.allg.itd51.wollmux.form.sidebar.FormSidebarRegistration;
import de.muenchen.allg.itd51.wollmux.func.print.PrintFunction;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.SetFormValue;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.ToOdtEmail;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.ToOdtFile;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.ToPdfEmail;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.ToPdfFile;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.ToPrinter;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.ToShowOdtFile;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.ToSingleODT;
import de.muenchen.allg.itd51.wollmux.mailmerge.print.ToSinglePDF;
import de.muenchen.allg.itd51.wollmux.mailmerge.sidebar.MailMergeRegistration;
import de.muenchen.allg.itd51.wollmux.print.ShowDocument;
import de.muenchen.allg.itd51.wollmux.sender.SenderEventListener;
import de.muenchen.allg.itd51.wollmux.sender.dispatch.SenderDispatcher;
import de.muenchen.allg.itd51.wollmux.sidebar.WollMuxBarRegistration;
import de.muenchen.allg.itd51.wollmux.slv.dispatch.ContentBasedDirectiveDispatcher;
import de.muenchen.allg.itd51.wollmux.slv.events.ContentBasedDirectiveEventListener;
import de.muenchen.allg.itd51.wollmux.slv.print.ContentBasedDirectivePrint;
import de.muenchen.allg.itd51.wollmux.slv.print.ContentBasedDirectivePrintCollect;
import de.muenchen.allg.itd51.wollmux.slv.print.ContentBasedDirectivePrintOutput;

module wollmux
{
  exports de.muenchen.allg.itd51.wollmux;
  exports de.muenchen.allg.itd51.wollmux.comp;
  exports de.muenchen.allg.itd51.wollmux.config;
  exports de.muenchen.allg.itd51.wollmux.config.generator.xml;
  exports de.muenchen.allg.itd51.wollmux.config.scanner;
  exports de.muenchen.allg.itd51.wollmux.db;
  exports de.muenchen.allg.itd51.wollmux.dialog;
  exports de.muenchen.allg.itd51.wollmux.dispatch;
  exports de.muenchen.allg.itd51.wollmux.document;
  exports de.muenchen.allg.itd51.wollmux.document.commands;
  exports de.muenchen.allg.itd51.wollmux.document.nodes;
  exports de.muenchen.allg.itd51.wollmux.event;
  exports de.muenchen.allg.itd51.wollmux.event.handlers;
  exports de.muenchen.allg.itd51.wollmux.form.config;
  exports de.muenchen.allg.itd51.wollmux.form.control;
  exports de.muenchen.allg.itd51.wollmux.form.model;
  exports de.muenchen.allg.itd51.wollmux.form.sidebar;
  exports de.muenchen.allg.itd51.wollmux.former;
  exports de.muenchen.allg.itd51.wollmux.former.model;
  exports de.muenchen.allg.itd51.wollmux.former.control;
  exports de.muenchen.allg.itd51.wollmux.former.control.model;
  exports de.muenchen.allg.itd51.wollmux.former.document;
  exports de.muenchen.allg.itd51.wollmux.former.function;
  exports de.muenchen.allg.itd51.wollmux.former.group;
  exports de.muenchen.allg.itd51.wollmux.former.group.model;
  exports de.muenchen.allg.itd51.wollmux.former.insertion;
  exports de.muenchen.allg.itd51.wollmux.former.insertion.model;
  exports de.muenchen.allg.itd51.wollmux.former.section;
  exports de.muenchen.allg.itd51.wollmux.former.section.model;
  exports de.muenchen.allg.itd51.wollmux.former.view;
  exports de.muenchen.allg.itd51.wollmux.func;
  exports de.muenchen.allg.itd51.wollmux.func.print;
  exports de.muenchen.allg.itd51.wollmux.mailmerge;
  exports de.muenchen.allg.itd51.wollmux.mailmerge.ds;
  exports de.muenchen.allg.itd51.wollmux.mailmerge.mail;
  exports de.muenchen.allg.itd51.wollmux.mailmerge.print;
  exports de.muenchen.allg.itd51.wollmux.mailmerge.printsettings;
  exports de.muenchen.allg.itd51.wollmux.mailmerge.sidebar;
  exports de.muenchen.allg.itd51.wollmux.mailmerge.ui;
  exports de.muenchen.allg.itd51.wollmux.print;
  exports de.muenchen.allg.itd51.wollmux.sender;
  exports de.muenchen.allg.itd51.wollmux.sidebar;
  exports de.muenchen.allg.itd51.wollmux.slv;
  exports de.muenchen.allg.itd51.wollmux.slv.dialog;
  exports de.muenchen.allg.itd51.wollmux.slv.dispatch;
  exports de.muenchen.allg.itd51.wollmux.slv.events;
  exports de.muenchen.allg.itd51.wollmux.slv.print;
  exports de.muenchen.allg.itd51.wollmux.ui;
  exports de.muenchen.allg.itd51.wollmux.ui.layout;
  exports de.muenchen.allg.itd51.wollmux.util;

  requires transitive org.libreoffice.uno;
  requires transitive org.libreoffice.unoloader;

  requires transitive unohelper;
  requires transitive wollmux.interfaces;

  requires transitive org.apache.logging.log4j;
  requires transitive org.apache.logging.log4j.core;
  requires slf4j.api;

  requires java.naming;
  requires transitive java.desktop;
  requires java.net.http;
  requires java.management;
  requires java.scripting;

  requires com.google.common;
  requires transitive rstaui;
  requires autocomplete;
  requires transitive rsyntaxtextarea;
  requires org.apache.commons.lang3;
  requires org.apache.commons.io;
  requires jakarta.mail.api;
  requires roman.numeral.converter;
  requires com.sun.jna.platform;
  requires com.sun.jna;
  requires org.apache.commons.collections4;
  requires org.apache.pdfbox;
  requires javafx.graphics;
  requires org.jsoup;

  uses ComponentRegistration;
  uses Dispatcher;
  uses WollMuxEventListener;
  uses PrintFunction;

  /**
   * Unfortunately we have to use META-INF/services as well because LibreOffice puts the jar on its
   * class-path and not module-path.
   */
  provides ComponentRegistration
      with WollMuxRegistration, WollMuxBarRegistration, MailMergeRegistration, FormSidebarRegistration;
  provides Dispatcher with WollMuxDispatcher, ContentBasedDirectiveDispatcher, SenderDispatcher;
  provides WollMuxEventListener with WollMuxEventListenerImpl, SenderEventListener, CheckInstallationListener,
      OnProcessTextDocument, ContentBasedDirectiveEventListener;
  provides PrintFunction with ContentBasedDirectivePrint, ContentBasedDirectivePrintCollect,
      ContentBasedDirectivePrintOutput, SetFormValue, ToOdtEmail, ToPdfFile, ToOdtFile, ToPdfEmail, ToPrinter,
      ToShowOdtFile, ToSingleODT, ToSinglePDF, ShowDocument;
}
