package org.inksnow.cputil.logger.impl.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.slf4j.Logger;
import org.slf4j.helpers.MarkerIgnoringBase;

public class Log4j2Logger extends MarkerIgnoringBase implements Logger {

  public static final String FQCN = Log4j2Logger.class.getName();
  private final String name;
  private transient ExtendedLogger logger;

  public Log4j2Logger(final ExtendedLogger logger, final String name) {
    this.logger = logger;
    this.name = name;
  }

  @Override
  public void trace(final String format) {
    logger.logIfEnabled(FQCN, Level.TRACE, null, format);
  }

  @Override
  public void trace(final String format, final Object o) {
    logger.logIfEnabled(FQCN, Level.TRACE, null, format, o);
  }

  @Override
  public void trace(final String format, final Object arg1, final Object arg2) {
    logger.logIfEnabled(FQCN, Level.TRACE, null, format, arg1, arg2);
  }

  @Override
  public void trace(final String format, final Object... args) {
    logger.logIfEnabled(FQCN, Level.TRACE, null, format, args);
  }

  @Override
  public void trace(final String format, final Throwable t) {
    logger.logIfEnabled(FQCN, Level.TRACE, null, format, t);
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isEnabled(Level.TRACE, null, null);
  }

  @Override
  public void debug(final String format) {
    logger.logIfEnabled(FQCN, Level.DEBUG, null, format);
  }

  @Override
  public void debug(final String format, final Object o) {
    logger.logIfEnabled(FQCN, Level.DEBUG, null, format, o);
  }

  @Override
  public void debug(final String format, final Object arg1, final Object arg2) {
    logger.logIfEnabled(FQCN, Level.DEBUG, null, format, arg1, arg2);
  }

  @Override
  public void debug(final String format, final Object... args) {
    logger.logIfEnabled(FQCN, Level.DEBUG, null, format, args);
  }

  @Override
  public void debug(final String format, final Throwable t) {
    logger.logIfEnabled(FQCN, Level.DEBUG, null, format, t);
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isEnabled(Level.DEBUG, null, null);
  }

  @Override
  public void info(final String format) {
    logger.logIfEnabled(FQCN, Level.INFO, null, format);
  }

  @Override
  public void info(final String format, final Object o) {
    logger.logIfEnabled(FQCN, Level.INFO, null, format, o);
  }

  @Override
  public void info(final String format, final Object arg1, final Object arg2) {
    logger.logIfEnabled(FQCN, Level.INFO, null, format, arg1, arg2);
  }

  @Override
  public void info(final String format, final Object... args) {
    logger.logIfEnabled(FQCN, Level.INFO, null, format, args);
  }

  @Override
  public void info(final String format, final Throwable t) {
    logger.logIfEnabled(FQCN, Level.INFO, null, format, t);
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isEnabled(Level.INFO, null, null);
  }

  @Override
  public void warn(final String format) {
    logger.logIfEnabled(FQCN, Level.WARN, null, format);
  }

  @Override
  public void warn(final String format, final Object o) {
    logger.logIfEnabled(FQCN, Level.WARN, null, format, o);
  }

  @Override
  public void warn(final String format, final Object arg1, final Object arg2) {
    logger.logIfEnabled(FQCN, Level.WARN, null, format, arg1, arg2);
  }

  @Override
  public void warn(final String format, final Object... args) {
    logger.logIfEnabled(FQCN, Level.WARN, null, format, args);
  }

  @Override
  public void warn(final String format, final Throwable t) {
    logger.logIfEnabled(FQCN, Level.WARN, null, format, t);
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isEnabled(Level.WARN, null, null);
  }

  @Override
  public void error(final String format) {
    logger.logIfEnabled(FQCN, Level.ERROR, null, format);
  }

  @Override
  public void error(final String format, final Object o) {
    logger.logIfEnabled(FQCN, Level.ERROR, null, format, o);
  }

  @Override
  public void error(final String format, final Object arg1, final Object arg2) {
    logger.logIfEnabled(FQCN, Level.ERROR, null, format, arg1, arg2);
  }

  @Override
  public void error(final String format, final Object... args) {
    logger.logIfEnabled(FQCN, Level.ERROR, null, format, args);
  }

  @Override
  public void error(final String format, final Throwable t) {
    logger.logIfEnabled(FQCN, Level.ERROR, null, format, t);
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isEnabled(Level.ERROR, null, null);
  }

  @Override
  public String getName() {
    return name;
  }
}
