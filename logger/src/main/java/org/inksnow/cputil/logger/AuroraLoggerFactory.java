package org.inksnow.cputil.logger;

import org.inksnow.cputil.logger.impl.jul.JulLoggerFactory;
import org.inksnow.cputil.logger.impl.log4j2.Log4j2LoggerFactory;
import org.inksnow.cputil.logger.impl.parent.AuroraParentLogger;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class AuroraLoggerFactory implements ILoggerFactory {
  private static AuroraLoggerFactory INSTANCE;

  private ILoggerFactory provider;
  private int currentVersion = 0;

  public AuroraLoggerFactory() {
    if (INSTANCE != null) {
      throw new IllegalStateException("AuroraLoggerFactory is a singleton");
    }
    INSTANCE = this;
    this.provider = selectDefaultProvider();
  }

  public static AuroraLoggerFactory instance() {
    return INSTANCE;
  }

  private ILoggerFactory selectDefaultProvider() {
    try {
      Class.forName(AuroraLogger.UNMAPPED_SLF4J_PACKAGE_NAME + ".Logger", false, Logger.class.getClassLoader());

      if (!Logger.class.getName().equals(AuroraLogger.UNMAPPED_SLF4J_PACKAGE_NAME + ".Logger")) {
        return new AuroraParentLogger(false);
      }
    } catch (ClassNotFoundException e) {
      //
    }

    try {
      Class.forName("org.apache.logging.log4j.Logger", false, Logger.class.getClassLoader());
      return new Log4j2LoggerFactory();
    } catch (ClassNotFoundException e) {
      //
    }

    return new JulLoggerFactory();
  }

  public void setProvider(ILoggerFactory provider) {
    if (this.provider != provider) {
      this.provider = provider;
      currentVersion++;
    }
  }

  @Override
  public Logger getLogger(String name) {
    return new AuroraLogger(this, name);
  }

  public Logger updateDelegate(AuroraLogger auroraLogger) {
    if (auroraLogger.version != currentVersion) {
      Logger newLogger = provider.getLogger(auroraLogger.name);
      auroraLogger.version = currentVersion;
      auroraLogger.delegate = newLogger;
      return newLogger;
    }
    return auroraLogger.delegate;
  }
}
