package org.inksnow.cputil.url;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

public interface AuroraUrlHandler {
  String name();
  URLConnection openConnection(String url) throws IOException;
  InputStream openStream(String url) throws IOException;
}
