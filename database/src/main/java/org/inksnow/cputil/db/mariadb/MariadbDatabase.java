package org.inksnow.cputil.db.mariadb;

import org.inksnow.cputil.classloader.LoadPolicy;
import org.inksnow.cputil.db.Database;
import org.inksnow.cputil.db.transform.DisableRegisterDriverTransformer;
import org.inksnow.cputil.download.DownloadEntry;
import org.inksnow.cputil.transform.Transformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MariadbDatabase implements Database {
  private static final String DRIVER_CLASS_NAME = "org.mariadb.jdbc.Driver";
  private static final Map<String, LoadPolicy> LOAD_POLICIES = createLoadPolicies();
  private static final Map<String, List<Transformer>> TRANSFORMERS = createTransformers();
  private static final List<DownloadEntry> DOWNLOAD_ENTRIES = Collections.singletonList(
      new DownloadEntry(
          "org/mariadb/jdbc/mariadb-java-client/3.4.1/mariadb-java-client-3.4.1.jar",
          "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.4.1/mariadb-java-client-3.4.1.jar",
          "f60e4b282f1f4bdb74f0a26436ba7078a5e480b6f6702f6a7b45d9ba5e604a24"
      )
  );

  private static Map<String, LoadPolicy> createLoadPolicies() {
    Map<String, LoadPolicy> loadPolicies = new HashMap<>();
    loadPolicies.put("org/mariadb/jdbc", LoadPolicy.SELF_ONLY);
    return Collections.unmodifiableMap(loadPolicies);
  }

  private static Map<String, List<Transformer>> createTransformers() {
    Map<String, List<Transformer>> transformers = new HashMap<>();
    transformers.put("org/mariadb/jdbc/Driver", Collections.singletonList(new DisableRegisterDriverTransformer()));
    return Collections.unmodifiableMap(transformers);
  }

  @Override
  public String name() {
    return "mariadb";
  }

  @Override
  public List<DownloadEntry> downloadEntries() {
    return DOWNLOAD_ENTRIES;
  }

  @Override
  public Map<String, LoadPolicy> loadPolicies() {
    return LOAD_POLICIES;
  }

  @Override
  public Map<String, List<Transformer>> transformers() {
    return TRANSFORMERS;
  }

  @Override
  public String driverClassName() {
    return DRIVER_CLASS_NAME;
  }
}
