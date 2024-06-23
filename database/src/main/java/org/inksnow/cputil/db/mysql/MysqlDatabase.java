package org.inksnow.cputil.db.mysql;

import org.inksnow.cputil.classloader.LoadPolicy;
import org.inksnow.cputil.db.Database;
import org.inksnow.cputil.download.DownloadEntry;

import java.util.*;

public class MysqlDatabase implements Database {
  private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
  private static final Map<String, LoadPolicy> LOAD_POLICIES = createLoadPolicies();
  private static final List<DownloadEntry> DOWNLOAD_ENTRIES = new ArrayList<>(Arrays.asList(
      new DownloadEntry(
          "com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar",
          "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar",
          "d77962877d010777cff997015da90ee689f0f4bb76848340e1488f2b83332af5"
      ),
      new DownloadEntry(
          "com/google/protobuf/protobuf-java/4.27.1/protobuf-java-4.27.1.jar",
          "https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/4.27.1/protobuf-java-4.27.1.jar",
          "798c79f6c9fc6859cc76aa5474228aad250821438e1004dad34880d53aa745f9"
      )
  ));

  private static Map<String, LoadPolicy> createLoadPolicies() {
    Map<String, LoadPolicy> loadPolicies = new HashMap<>();
    loadPolicies.put("com/mysql/cj", LoadPolicy.SELF_ONLY);
    loadPolicies.put("com/mysql/jdbc", LoadPolicy.SELF_ONLY);
    loadPolicies.put("com/google/protobuf", LoadPolicy.SELF_ONLY);
    return Collections.unmodifiableMap(loadPolicies);
  }

  @Override
  public String name() {
    return "mysql";
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
  public String driverClassName() {
    return DRIVER_CLASS_NAME;
  }
}
