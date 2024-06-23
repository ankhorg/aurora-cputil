package org.inksnow.cputil.download;

import java.util.Objects;

public final class DownloadEntry {
  private final String name;
  private final String url;
  private final String hash;

  public DownloadEntry(String name, String url, String hash) {
    this.name = Objects.requireNonNull(name, "name");
    this.url = Objects.requireNonNull(url, "url");
    this.hash = Objects.requireNonNull(hash, "hash");
  }

  public String name() {
    return name;
  }

  public String url() {
    return url;
  }

  public String hash() {
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DownloadEntry that = (DownloadEntry) o;
    return name.equals(that.name) && url.equals(that.url) && hash.equals(that.hash);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + url.hashCode();
    result = 31 * result + hash.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "DownloadEntry{" +
        "name='" + name + '\'' +
        ", url='" + url + '\'' +
        ", hash='" + hash + '\'' +
        '}';
  }
}
