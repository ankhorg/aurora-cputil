package org.inksnow.cputil.logger.impl.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.AbstractLoggerAdapter;
import org.apache.logging.log4j.spi.LoggerContext;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * Log4j implementation of SLF4J ILoggerFactory interface.
 */
public class Log4j2LoggerFactory extends AbstractLoggerAdapter<Logger> implements ILoggerFactory {
  @Override
  protected Logger newLogger(final String name, final LoggerContext context) {
    final String key = Logger.ROOT_LOGGER_NAME.equals(name) ? LogManager.ROOT_LOGGER_NAME : name;
    return new Log4j2Logger(context.getLogger(key), name);
  }

  @Override
  protected LoggerContext getContext() {
    return LogManager.getContext(false);
  }
}
