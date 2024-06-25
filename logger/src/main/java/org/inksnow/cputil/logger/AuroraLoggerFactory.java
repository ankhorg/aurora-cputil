package org.inksnow.cputil.logger;

import org.inksnow.cputil.logger.impl.jul.JulLoggerFactory;
import org.inksnow.cputil.logger.impl.log4j2.Log4j2LoggerFactory;
import org.inksnow.cputil.logger.impl.parent.AuroraParentLogger;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.function.Function;

public class AuroraLoggerFactory implements ILoggerFactory {
  private static AuroraLoggerFactory INSTANCE;

  private ILoggerFactory provider;
  private Function<String, String> nameMapping;
  private int currentVersion = 0;

  private AuroraLoggerFactory() {
    this.nameMapping = Function.identity();
    this.provider = selectDefaultProvider();
  }

  public static AuroraLoggerFactory instance() {
    AuroraLoggerFactory instance = INSTANCE;
    if (instance == null) {
      synchronized (AuroraLoggerFactory.class) {
        instance = INSTANCE;
        if (instance == null) {
          instance = new AuroraLoggerFactory();
          INSTANCE = instance;
        }
      }
    }
    return instance;
  }

  private ILoggerFactory selectDefaultProvider() {
    try {
      Class.forName(AuroraLogger.UNMAPPED_SLF4J_PACKAGE_NAME + ".Logger", false, Logger.class.getClassLoader());

      if (!Logger.class.getName().equals(AuroraLogger.UNMAPPED_SLF4J_PACKAGE_NAME + ".Logger")) {
        return new AuroraParentLogger();
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

  public ILoggerFactory provider() {
    return provider;
  }

  public void provider(ILoggerFactory provider) {
    if (this.provider != provider) {
      this.provider = provider;
      currentVersion++;
    }
  }

  public Function<String, String> nameMapping() {
    return nameMapping;
  }

  public void nameMapping(Function<String, String> nameMapping) {
    if (this.nameMapping != nameMapping) {
      this.nameMapping = nameMapping;
      currentVersion++;
    }
  }

  @Override
  public Logger getLogger(String name) {
    return new AuroraLogger(this, name);
  }

  /* package-private */ Logger updateDelegate(AuroraLogger auroraLogger) {
    if (auroraLogger.version != currentVersion) {
      Logger newLogger = provider.getLogger(nameMapping.apply(auroraLogger.name));
      auroraLogger.version = currentVersion;
      auroraLogger.delegate = newLogger;
      return newLogger;
    }
    return auroraLogger.delegate;
  }
}
