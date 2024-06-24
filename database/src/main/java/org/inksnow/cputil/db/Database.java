package org.inksnow.cputil.db;

import org.inksnow.cputil.classloader.LoadPolicy;
import org.inksnow.cputil.download.DownloadEntry;
import org.inksnow.cputil.transform.Transformer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface Database {
  String name();

  List<DownloadEntry> downloadEntries();

  Map<String, LoadPolicy> loadPolicies();

  default Map<String, List<Transformer>> transformers() {
    return Collections.emptyMap();
  }

  String driverClassName();
}
