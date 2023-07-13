/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München and LibreOffice contributors
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
import org.libreoffice.lots.ComponentRegistration;
import org.libreoffice.lots.comp.WollMuxRegistration;
import org.libreoffice.lots.dispatch.Dispatcher;
import org.libreoffice.lots.dispatch.WollMuxDispatcher;
import org.libreoffice.lots.document.commands.OnProcessTextDocument;
import org.libreoffice.lots.event.CheckInstallationListener;
import org.libreoffice.lots.event.WollMuxEventListener;
import org.libreoffice.lots.event.WollMuxEventListenerImpl;
import org.libreoffice.lots.form.sidebar.FormSidebarRegistration;
import org.libreoffice.lots.func.print.PrintFunction;
import org.libreoffice.lots.mailmerge.print.SetFormValue;
import org.libreoffice.lots.mailmerge.print.ToOdtEmail;
import org.libreoffice.lots.mailmerge.print.ToOdtFile;
import org.libreoffice.lots.mailmerge.print.ToPdfEmail;
import org.libreoffice.lots.mailmerge.print.ToPdfFile;
import org.libreoffice.lots.mailmerge.print.ToPrinter;
import org.libreoffice.lots.mailmerge.print.ToShowOdtFile;
import org.libreoffice.lots.mailmerge.print.ToSingleODT;
import org.libreoffice.lots.mailmerge.print.ToSinglePDF;
import org.libreoffice.lots.mailmerge.sidebar.MailMergeRegistration;
import org.libreoffice.lots.print.ShowDocument;
import org.libreoffice.lots.sender.SenderEventListener;
import org.libreoffice.lots.sender.dispatch.SenderDispatcher;
import org.libreoffice.lots.sidebar.WollMuxBarRegistration;
import org.libreoffice.lots.slv.dispatch.ContentBasedDirectiveDispatcher;
import org.libreoffice.lots.slv.events.ContentBasedDirectiveEventListener;
import org.libreoffice.lots.slv.print.ContentBasedDirectivePrint;
import org.libreoffice.lots.slv.print.ContentBasedDirectivePrintCollect;
import org.libreoffice.lots.slv.print.ContentBasedDirectivePrintOutput;

module wollmux
{
  exports org.libreoffice.lots;
  exports org.libreoffice.lots.comp;
  exports org.libreoffice.lots.config;
  exports org.libreoffice.lots.config.generator.xml;
  exports org.libreoffice.lots.config.scanner;
  exports org.libreoffice.lots.db;
  exports org.libreoffice.lots.dialog;
  exports org.libreoffice.lots.dispatch;
  exports org.libreoffice.lots.document;
  exports org.libreoffice.lots.document.commands;
  exports org.libreoffice.lots.document.nodes;
  exports org.libreoffice.lots.event;
  exports org.libreoffice.lots.event.handlers;
  exports org.libreoffice.lots.form.config;
  exports org.libreoffice.lots.form.control;
  exports org.libreoffice.lots.form.model;
  exports org.libreoffice.lots.form.sidebar;
  exports org.libreoffice.lots.former;
  exports org.libreoffice.lots.former.model;
  exports org.libreoffice.lots.former.control;
  exports org.libreoffice.lots.former.control.model;
  exports org.libreoffice.lots.former.document;
  exports org.libreoffice.lots.former.function;
  exports org.libreoffice.lots.former.group;
  exports org.libreoffice.lots.former.group.model;
  exports org.libreoffice.lots.former.insertion;
  exports org.libreoffice.lots.former.insertion.model;
  exports org.libreoffice.lots.former.section;
  exports org.libreoffice.lots.former.section.model;
  exports org.libreoffice.lots.former.view;
  exports org.libreoffice.lots.func;
  exports org.libreoffice.lots.func.print;
  exports org.libreoffice.lots.mailmerge;
  exports org.libreoffice.lots.mailmerge.ds;
  exports org.libreoffice.lots.mailmerge.mail;
  exports org.libreoffice.lots.mailmerge.print;
  exports org.libreoffice.lots.mailmerge.printsettings;
  exports org.libreoffice.lots.mailmerge.sidebar;
  exports org.libreoffice.lots.mailmerge.ui;
  exports org.libreoffice.lots.print;
  exports org.libreoffice.lots.sender;
  exports org.libreoffice.lots.sidebar;
  exports org.libreoffice.lots.slv;
  exports org.libreoffice.lots.slv.dialog;
  exports org.libreoffice.lots.slv.dispatch;
  exports org.libreoffice.lots.slv.events;
  exports org.libreoffice.lots.slv.print;
  exports org.libreoffice.lots.ui;
  exports org.libreoffice.lots.util;

  requires transitive org.libreoffice.uno;
  requires transitive org.libreoffice.unoloader;

  requires transitive org.libreoffice.ext.unohelper;
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
  requires gettext.commons;

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
