package org.inksnow.cputil.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.inksnow.cputil.AuroraCputil;
import org.inksnow.cputil.AuroraDownloader;
import org.inksnow.cputil.classloader.AuroraClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class AuroraDatabase extends HikariDataSource {
  private static final Logger logger = LoggerFactory.getLogger(AuroraDatabase.class);
  private static final String PASSWORD = "password";
  private static final String USER = "user";

  public AuroraDatabase(HikariConfig configuration) {
    super(configuration);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private static final Path DEFAULT_CACHE_DIRECTORY = Paths.get("plugins", ".aurora", "cache");
    private Path cacheDirectory = DEFAULT_CACHE_DIRECTORY;
    private String jdbcUrl;
    private Properties driverProperties = new Properties();
    private Database databaseType;

    public Builder cacheDirectory(Path cacheDirectory) {
      this.cacheDirectory = (cacheDirectory == null) ? DEFAULT_CACHE_DIRECTORY : cacheDirectory;
      return this;
    }

    public Builder jdbcUrl(String jdbcUrl) {
      this.jdbcUrl = jdbcUrl;
      return this;
    }

    public Builder driverProperties(Properties driverProperties) {
      this.driverProperties = (driverProperties == null) ? new Properties() : driverProperties;
      return this;
    }

    public Builder driverProperty(String key, String value) {
      this.driverProperties.setProperty(key, value);
      return this;
    }

    public Builder username(String username) {
      driverProperties.setProperty(USER, username);
      return this;
    }

    public Builder password(String password) {
      driverProperties.setProperty(PASSWORD, password);
      return this;
    }

    public Builder databaseType(Database databaseType) {
      this.databaseType = databaseType;
      return this;
    }


    private List<Path> downloadDriverClasses() throws IOException {
      if (databaseType == null) {
        throw new IllegalArgumentException("databaseType cannot be null");
      }
      AuroraDownloader downloader = new AuroraDownloader(cacheDirectory);
      return downloader.downloadAll(databaseType.downloadEntries());
    }

    private AuroraClassLoader createClassLoader(List<Path> classpathEntries) {
      return AuroraClassLoader.builder()
          .urls(classpathEntries.stream().map(it -> {
            try {
              return it.toUri().toURL();
            } catch (MalformedURLException e) {
              throw new RuntimeException(e);
            }
          }).collect(Collectors.toList()))
          .parent(AuroraCputil.contextClassLoader())
          .loadPolicies(databaseType.loadPolicies())
          .transformers(databaseType.transformers())
          .build();
    }

    private Class<? extends Driver> loadDriverClass(AuroraClassLoader auroraClassLoader) throws IOException {
      try {
        return auroraClassLoader.loadClass(databaseType.driverClassName()).asSubclass(Driver.class);
      } catch (ClassNotFoundException | ClassCastException e) {
        throw new IOException("Failed to load driver class: " + databaseType.driverClassName(), e);
      }
    }

    private HikariConfig createConfig(Class<? extends Driver> driverClass) {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(jdbcUrl);
      config.setDataSource(new AuroraDataSource(jdbcUrl, driverClass, driverProperties));
      config.setUsername(driverProperties.getProperty(USER));
      config.setPassword(driverProperties.getProperty(PASSWORD));
      config.setMaximumPoolSize(Math.min(4, Runtime.getRuntime().availableProcessors() * 2));
      config.setPoolName("aurora-pool");
      return config;
    }

    public AuroraDatabase build() throws IOException {
      if (jdbcUrl == null) {
        throw new IllegalArgumentException("jdbcUrl cannot be null");
      }

      List<Path> classpathEntries = downloadDriverClasses();
      AuroraClassLoader auroraClassLoader = createClassLoader(classpathEntries);
      Class<? extends Driver> driverClass = loadDriverClass(auroraClassLoader);
      HikariConfig config = createConfig(driverClass);

      return new AuroraDatabase(config);
    }
  }
}
