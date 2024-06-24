package org.inksnow.cputil.logger.impl.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.AbstractLoggerAdapter;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.inksnow.cputil.logger.AuroraLogger;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.function.Predicate;

/**
 * Log4j implementation of SLF4J ILoggerFactory interface.
 */
public class Log4j2LoggerFactory extends AbstractLoggerAdapter<Logger> implements ILoggerFactory {

  private static final StatusLogger LOGGER = StatusLogger.getLogger();
  private static final Predicate<Class<?>> CALLER_PREDICATE = clazz ->
      !AbstractLoggerAdapter.class.equals(clazz) && !clazz.getName().startsWith(AuroraLogger.SLF4J_PACKAGE_NAME);

  @Override
  protected Logger newLogger(final String name, final LoggerContext context) {
    final String key = Logger.ROOT_LOGGER_NAME.equals(name) ? LogManager.ROOT_LOGGER_NAME : name;
    return new Log4j2Logger(context.getLogger(key), name);
  }

  @Override
  protected LoggerContext getContext() {
    final Class<?> anchor = LogManager.getFactory().isClassLoaderDependent()
        ? StackLocatorUtil.getCallerClass(Log4j2LoggerFactory.class, CALLER_PREDICATE)
        : null;
    LOGGER.trace("Log4jLoggerFactory.getContext() found anchor {}", anchor);
    return anchor == null ? LogManager.getContext(false) : getContext(anchor);
  }
}
