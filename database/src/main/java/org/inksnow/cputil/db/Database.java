package org.inksnow.cputil.db;

import org.inksnow.cputil.classloader.LoadPolicy;
import org.inksnow.cputil.download.DownloadEntry;

import java.util.List;
import java.util.Map;

public interface Database {
  String name();

  List<DownloadEntry> downloadEntries();

  Map<String, LoadPolicy> loadPolicies();

  String driverClassName();
}
