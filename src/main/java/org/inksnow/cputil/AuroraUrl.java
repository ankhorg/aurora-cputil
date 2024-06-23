package org.inksnow.cputil;

import org.inksnow.cputil.url.AuroraUrlHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public final class AuroraUrl {
  private static Map<String, AuroraUrlHandler> handlers = null;
  private static final URLStreamHandler auroraHandler = new AuroraUrlHandlerImpl();
  private AuroraUrl() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  private static Map<String, AuroraUrlHandler> handlers() {
    Map<String, AuroraUrlHandler> handlers = AuroraUrl.handlers;
    if (handlers == null) {
      handlers = new HashMap<>();
      for (AuroraUrlHandler handler : ServiceLoader.load(AuroraUrlHandler.class, AuroraCputil.contextClassLoader())) {
        handlers.put(handler.name(), handler);
      }
      AuroraUrl.handlers = handlers;
    }
    return handlers;
  }

  public static URLStreamHandler auroraHandler() {
    return auroraHandler;
  }

  public static URLConnection openConnection(String url) throws IOException {
    URLConnection conn = openAuroraConnection(url);
    if (conn == null) {
      conn = new URL(url).openConnection();
    }
    if (conn instanceof HttpURLConnection) {
      conn.setRequestProperty("User-Agent", "AuroraCputil/1.0 (im@inker.bot)");
    }
    return conn;
  }

  public static InputStream openStream(String url) throws IOException {
    return openConnection(url).getInputStream();
  }

  public static URLConnection openAuroraConnection(String url) throws IOException {
    int protocolSplit = url.indexOf(':');
    if (protocolSplit == -1) {
      return null;
    }
    String protocol = url.substring(0, protocolSplit);
    AuroraUrlHandler handler = handlers().get(protocol);
    if (handler == null) {
      return null;
    }
    return handler.openConnection(url);
  }

  public static InputStream openAuroraStream(String url) throws IOException {
    URLConnection connection = openAuroraConnection(url);
    if (connection == null) {
      return null;
    }
    return connection.getInputStream();
  }

  public static final class AuroraUrlHandlerImpl extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL u) throws IOException {
      if ("aurora".equals(u.getProtocol())) {
        return openAuroraConnection(u.toString().substring("aurora:".length()));
      }
      return null;
    }
  }
}
