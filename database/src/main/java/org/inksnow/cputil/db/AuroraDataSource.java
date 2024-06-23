package org.inksnow.cputil.db;

import com.zaxxer.hikari.util.DriverDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.Map;
import java.util.Properties;

public class AuroraDataSource implements DataSource {
  private static final Logger LOGGER = LoggerFactory.getLogger(DriverDataSource.class);
  private static final String PASSWORD = "password";
  private static final String USER = "user";

  private final String jdbcUrl;
  private final Properties driverProperties;
  private final Driver driver;

  public AuroraDataSource(String jdbcUrl, Class<? extends Driver> driverClass, Properties properties) {
    this(jdbcUrl, createDriver(driverClass), properties);
  }

  public AuroraDataSource(String jdbcUrl, Driver driver, Properties properties) {
    this.jdbcUrl = jdbcUrl;
    this.driverProperties = new Properties();

    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
      driverProperties.setProperty(entry.getKey().toString(), entry.getValue().toString());
    }

    final String sanitizedUrl = jdbcUrl.replaceAll("([?&;]password=)[^&#;]*(.*)", "$1<masked>$2");
    try {
      if (driver == null) {
        driver = DriverManager.getDriver(jdbcUrl);
        LOGGER.debug("Loaded driver with class name {} for jdbcUrl={}", driver.getClass().getName(), sanitizedUrl);
      } else if (!driver.acceptsURL(jdbcUrl)) {
        throw new RuntimeException("Driver " + driver.getClass() + " claims to not accept jdbcUrl, " + sanitizedUrl);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to get driver instance for jdbcUrl=" + sanitizedUrl, e);
    }

    this.driver = driver;
  }

  private static Driver createDriver(Class<? extends Driver> driverClass) {
    try {
      return driverClass.getDeclaredConstructor().newInstance();
    } catch (IllegalAccessException | NoSuchMethodException e) {
      throw new RuntimeException("Failed to create driver instance for driverClass=" + driverClass, e);
    } catch (InstantiationException | InvocationTargetException e) {
      throw new RuntimeException("Failed to create driver instance for driverClass=" + driverClass, e.getCause());
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    return driver.connect(jdbcUrl, driverProperties);
  }

  @Override
  public Connection getConnection(final String username, final String password) throws SQLException {
    final Properties cloned = (Properties) driverProperties.clone();
    if (username != null) {
      cloned.put(USER, username);
      if (cloned.containsKey("username")) {
        cloned.put("username", username);
      }
    }
    if (password != null) {
      cloned.put(PASSWORD, password);
    }

    return driver.connect(jdbcUrl, cloned);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void setLogWriter(PrintWriter logWriter) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return DriverManager.getLoginTimeout();
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    DriverManager.setLoginTimeout(seconds);
  }

  @Override
  public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return driver.getParentLogger();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return false;
  }
}
