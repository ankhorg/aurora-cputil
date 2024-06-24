package org.inksnow.cputil.logger.impl.jul;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class JulLoggerFactory implements ILoggerFactory {
  public JulLoggerFactory() {
    java.util.logging.Logger.getLogger("");
  }

  public Logger getLogger(String name) {
    // the root logger is called "" in JUL
    if (name.equalsIgnoreCase(Logger.ROOT_LOGGER_NAME)) {
      name = "";
    }

    return new JulLoggerAdapter(java.util.logging.Logger.getLogger(name));
  }
}