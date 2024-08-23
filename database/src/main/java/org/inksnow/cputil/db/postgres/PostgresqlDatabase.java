package org.inksnow.cputil.db.postgres;

import org.inksnow.cputil.classloader.LoadPolicy;
import org.inksnow.cputil.db.Database;
import org.inksnow.cputil.db.transform.DisableRegisterDriverTransformer;
import org.inksnow.cputil.download.DownloadEntry;
import org.inksnow.cputil.transform.Transformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostgresqlDatabase implements Database {
  private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
  private static final Map<String, LoadPolicy> LOAD_POLICIES = createLoadPolicies();
  private static final Map<String, List<Transformer>> TRANSFORMERS = createTransformers();
  private static final List<DownloadEntry> DOWNLOAD_ENTRIES = Collections.singletonList(
      new DownloadEntry(
          "org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar",
          "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar",
          "188976721ead8e8627eb6d8389d500dccc0c9bebd885268a3047180274a6031e"
      )
  );

  private static Map<String, LoadPolicy> createLoadPolicies() {
    Map<String, LoadPolicy> loadPolicies = new HashMap<>();
    loadPolicies.put("org/postgresql", LoadPolicy.SELF_ONLY);
    return Collections.unmodifiableMap(loadPolicies);
  }

  private static Map<String, List<Transformer>> createTransformers() {
    Map<String, List<Transformer>> transformers = new HashMap<>();
    transformers.put("org/postgresql/Driver", Collections.singletonList(new DisableRegisterDriverTransformer()));
    return Collections.unmodifiableMap(transformers);
  }

  @Override
  public String name() {
    return "postgresql";
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