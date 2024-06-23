package org.inksnow.cputil;

import org.inksnow.cputil.download.DownloadEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public final class AuroraDownloader {
  private static final Logger logger = LoggerFactory.getLogger(AuroraDownloader.class);
  private final Path cacheDirectory;

  public AuroraDownloader(Path cacheDirectory) {
    this.cacheDirectory = cacheDirectory;
  }

  public String checksum(Path path) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw UncheckUtil.uncheckThrow(e);
    }
    try (InputStream in = Files.newInputStream(path)) {
      byte[] buffer = new byte[4096];
      int read;
      while ((read = in.read(buffer)) != -1) {
        digest.update(buffer, 0, read);
      }
    }

    return HexUtil.of().formatHex(digest.digest());
  }

  public Path download(DownloadEntry entry) throws IOException {
    Path targetPath = cacheDirectory.resolve(entry.hash()).resolve(entry.name());
    if (Files.exists(targetPath)) {
      String checksum = checksum(targetPath);
      if (entry.hash().equals(checksum)) {
        return targetPath;
      } else {
        logger.warn("Checksum mismatch for entry {}. Expected: {}, Actual: {}", entry.name(), entry.hash(), checksum);
        Files.delete(targetPath);
      }
    }

    Path tempPath = Files.createTempFile("aurora-download-", ".tmp");

    try {
      URLConnection connection = AuroraUrl.openConnection(entry.url());

      if (connection instanceof HttpURLConnection) {
        HttpURLConnection httpConnection = (HttpURLConnection) connection;
        httpConnection.setInstanceFollowRedirects(true);
        httpConnection.setRequestProperty("Accept", "application/octet-stream");
      }

      try (InputStream in = connection.getInputStream()) {
        Files.copy(in, tempPath, StandardCopyOption.REPLACE_EXISTING);
      }

      if (entry.hash().equals(checksum(tempPath))) {
        Files.createDirectories(targetPath.getParent());
        Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath;
      } else {
        throw new IOException("Checksum mismatch for download " + entry.name());
      }
    } finally {
      if (Files.exists(tempPath)) {
        Files.delete(tempPath);
      }
    }
  }

  public List<Path> downloadAll(List<DownloadEntry> entries) throws IOException {
    List<Path> paths = new ArrayList<>(entries.size());
    if (entries.size() > 8) {
      logger.info("Downloading {} entries concurrently", entries.size());
      ThreadPoolExecutor executor = new ThreadPoolExecutor(
          8,
          8,
          0, TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadFactory() {
            private int count = 0;

            @Override
            public Thread newThread(Runnable runnable) {
              Thread thread = new Thread(runnable);
              thread.setName("aurora-downloader-" + count++);
              thread.setDaemon(true);
              return thread;
            }
          }
      );
      try {
        CompletableFuture<Path>[] futures = new CompletableFuture[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
          DownloadEntry entry = entries.get(i);
          futures[i] = CompletableFuture.supplyAsync(() -> {
            try {
              return download(entry);
            } catch (IOException e) {
              logger.warn("Failed to download entry: {}", entry, e);
              return null;
            }
          }, executor);
        }
        CompletableFuture.allOf(futures).join();
        for (CompletableFuture<Path> future : futures) {
          paths.add(future.join());
        }
        return paths;
      } finally {
        executor.shutdownNow();
      }
    } else {
      logger.info("Downloading {} entries sequentially", entries.size());
      for (DownloadEntry entry : entries) {
        paths.add(download(entry));
      }
      return paths;
    }
  }
}
