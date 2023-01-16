/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2023 Landeshauptstadt München
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
package de.muenchen.allg.itd51.wollmux.util;

import java.io.File;
import java.io.Writer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Helper for changing the configuration of the logger. Basically there's a log to
 * ${sys:user.home}/.wollmux/wollmux.log already configured in log4j2.xml.
 */
public class LogConfig
{

  /**
   * If true subsequent calls of any init method have no effect..
   */
  private static boolean ignoreInit = false;

  private LogConfig()
  {
    // nothing to initialize
  }

  /**
   * Reconfigure the logger to use a writer and a new log level.
   *
   * @param writer
   *          The sink of the logger.
   * @param loggingMode
   *          The log level.
   */
  public static void init(Writer writer, Level loggingMode)
  {
    if (ignoreInit)
    {
      return;
    }
    LoggerContext context = LoggerContext.getContext(false);
    Configuration config = context.getConfiguration();
    StringLayout layout = PatternLayout.newBuilder().withPattern("%d{yyyy-MM-dd HH:mm:ss} %-5p %c:%L - %m%n").build();
    Appender appender = WriterAppender.createAppender(layout, null, writer, "outputStream", false, true);
    config.getRootLogger().getAppenders().forEach((name, a) -> config.getRootLogger().removeAppender(name));
    appender.start();
    config.getRootLogger().addAppender(appender, loggingMode, null);
    config.getRootLogger().setLevel(loggingMode);
    context.updateLoggers();
  }

  /**
   * Reconfigure the logger to use a file and a new log level.
   *
   * @param outputFile
   *          The sink of the logger.
   * @param loggingMode
   *          The log level.
   */
  public static void init(File outputFile, Level loggingMode)
  {
    if (ignoreInit)
    {
      return;
    }

    LoggerContext context = LoggerContext.getContext(false);
    Configuration config = context.getConfiguration();
    StringLayout layout = PatternLayout.newBuilder().withPattern("%d{yyyy-MM-dd HH:mm:ss} %-5p %c:%L - %m%n").build();
    Appender appender = FileAppender.newBuilder().setName("file").setLayout(layout).withAppend(true)
        .withCreateOnDemand(true).withFileName(outputFile.getAbsolutePath()).build();
    config.getRootLogger().getAppenders().forEach((name, a) -> config.getRootLogger().removeAppender(name));
    appender.start();
    config.getRootLogger().addAppender(appender, loggingMode, null);
    config.getRootLogger().setLevel(loggingMode);
    context.updateLoggers();
  }

  /**
   * Reconfigure the logger to log with a new level. If an invalid log level is provided, the logger
   * uses {@link Level#INFO}.
   *
   * @param loggingMode
   *          The new log level. Possible values are "NONE" and the values of {@link Level}.
   */
  public static void init(String loggingMode)
  {
    if (ignoreInit) {
      return;
    }

    LoggerContext context = LoggerContext.getContext(false);
    Configuration config = context.getConfiguration();
    if ("NONE".equals(loggingMode)) {
      config.getRootLogger().setLevel(Level.OFF);
    }
    config.getRootLogger().setLevel(Level.toLevel(loggingMode, Level.INFO));
    context.updateLoggers();
  }

  /**
   * Should subsequent calls to init methods have any effect?
   *
   * @param ignoreInit
   *          If true, calls have no effect, otherwise calls change the configuration of the logger.
   */
  public static void setIgnoreInit(boolean ignoreInit)
  {
    LogConfig.ignoreInit = ignoreInit;
  }
}
