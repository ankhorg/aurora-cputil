package org.inksnow.cputil.url;

import org.inksnow.cputil.AuroraCputil;
import org.inksnow.cputil.AuroraUrl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class AuroraResourceHandler implements AuroraUrlHandler {
  @Override
  public String name() {
    return "resource";
  }

  @Override
  public URLConnection openConnection(String url) throws IOException {
    URL resource = AuroraCputil.contextClassLoader()
        .getResource(url.substring("resource:".length()));
    if (resource == null) {
      return new NoUrlResourceConnection(url);
    } else {
      return resource.openConnection();
    }
  }

  @Override
  public InputStream openStream(String url) throws IOException {
    return openConnection(url).getInputStream();
  }

  public class NoUrlResourceConnection extends URLConnection {
    private final String urlString;
    protected NoUrlResourceConnection(String urlString) throws MalformedURLException {
      super(new URL("aurora", null, -1, urlString, AuroraUrl.auroraHandler()));
      this.urlString = urlString;
    }

    @Override
    public void connect() {
      //
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return openStream(urlString);
    }
  }
}
