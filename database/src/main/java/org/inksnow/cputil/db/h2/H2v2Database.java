package org.inksnow.cputil.db.h2;

import org.inksnow.cputil.download.DownloadEntry;

import java.util.*;

public class H2v2Database extends AbstractH2Database {
  private static final List<DownloadEntry> DOWNLOAD_ENTRIES = new ArrayList<>(Arrays.asList(
      new DownloadEntry(
          "com/h2database/h2/2.2.224/h2-2.2.224.jar",
          "https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar",
          "b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497"
      )
  ));

  @Override
  public String name() {
    return "h2v2";
  }

  public List<DownloadEntry> downloadEntries() {
    return DOWNLOAD_ENTRIES;
  }
}
