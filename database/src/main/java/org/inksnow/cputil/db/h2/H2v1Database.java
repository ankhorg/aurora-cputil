package org.inksnow.cputil.db.h2;

import org.inksnow.cputil.download.DownloadEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class H2v1Database extends AbstractH2Database {
  private static final List<DownloadEntry> DOWNLOAD_ENTRIES = new ArrayList<>(Collections.singletonList(
      new DownloadEntry(
          "com/h2database/h2/1.4.200/h2-1.4.200.jar",
          "https://repo1.maven.org/maven2/com/h2database/h2/1.4.200/h2-1.4.200.jar",
          "3ad9ac4b6aae9cd9d3ac1c447465e1ed06019b851b893dd6a8d76ddb6d85bca6"
      )
  ));

  @Override
  public String name() {
    return "h2v1";
  }

  public List<DownloadEntry> downloadEntries() {
    return DOWNLOAD_ENTRIES;
  }
}
