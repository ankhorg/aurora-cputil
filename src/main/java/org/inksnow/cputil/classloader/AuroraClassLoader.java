package org.inksnow.cputil.classloader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.*;

public class AuroraClassLoader extends URLClassLoader {
  public static final LoadPolicy DEFAULT_LOAD_POLICY = LoadPolicy.PARENT_THEN_SELF;
  private final Map<String, LoadPolicy> loadPolicies;

  private AuroraClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory, Map<String, LoadPolicy> loadPolicies) {
    super(urls, parent, factory);
    this.loadPolicies = loadPolicies;
  }

  public static Builder builder() {
    return new Builder();
  }

  public LoadPolicy getLoadPolicy(String name) {
    while (name.startsWith("/")) {
      name = name.substring(1);
    }

    while (!name.isEmpty()) {
      int index = name.lastIndexOf('/');
      name = name.substring(0, (index == -1) ? 0 : index);

      LoadPolicy loadPolicy = loadPolicies.get(name);
      if (loadPolicy != null) {
        return loadPolicy;
      }
    }

    return DEFAULT_LOAD_POLICY;
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      Class<?> clazz = findLoadedClass(name);
      if (clazz != null) {
        if (resolve) {
          resolveClass(clazz);
        }
        return clazz;
      }

      LoadPolicy loadPolicy = getLoadPolicy(name.replace('.', '/'));

      if (loadPolicy.selfFirst()) {
        try {
          clazz = super.findClass(name);
        } catch (ClassNotFoundException e) {
          // ignore
        }
      }

      if (clazz == null && loadPolicy.parentThen()) {
        try {
          clazz = getParent().loadClass(name);
        } catch (ClassNotFoundException e) {
          // ignore
        }
      }

      if (clazz == null && loadPolicy.selfLast()) {
        try {
          clazz = super.findClass(name);
        } catch (ClassNotFoundException e) {
          // ignore
        }
      }

      if (clazz == null) {
        throw new ClassNotFoundException(name);
      }

      if (resolve) {
        resolveClass(clazz);
      }
      return clazz;
    }
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    LoadPolicy loadPolicy = getLoadPolicy(name.replace('.', '/'));
    if (loadPolicy.selfFirst() || loadPolicy.selfLast()) {
      return super.findClass(name);
    } else {
      throw new ClassNotFoundException(name);
    }
  }

  @Override
  public URL getResource(String name) {
    LoadPolicy loadPolicy = getLoadPolicy(name);

    URL url = null;

    if (loadPolicy.selfFirst()) {
      url = super.findResource(name);
    }

    if (url == null && loadPolicy.parentThen()) {
      url = getParent().getResource(name);
    }

    if (url == null && loadPolicy.selfLast()) {
      url = super.findResource(name);
    }

    return url;
  }

  @Override
  public URL findResource(String name) {
    LoadPolicy loadPolicy = getLoadPolicy(name);

    if (loadPolicy.selfFirst() || loadPolicy.selfLast()) {
      return super.findResource(name);
    } else {
      return null;
    }
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    LoadPolicy loadPolicy = getLoadPolicy(name);

    Enumeration<URL>[] urls = new Enumeration[3];

    if (loadPolicy.selfFirst()) {
      urls[0] = super.findResources(name);
    }

    if (loadPolicy.parentThen()) {
      urls[1] = getParent().getResources(name);
    }

    if (loadPolicy.selfLast()) {
      urls[2] = super.findResources(name);
    }

    return new CompoundEnumeration<>(urls);
  }

  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    LoadPolicy loadPolicy = getLoadPolicy(name);

    if (loadPolicy.selfFirst() || loadPolicy.selfLast()) {
      return super.findResources(name);
    } else {
      return null;
    }
  }

  public static final class Builder {
    private final Map<String, LoadPolicy> loadPolicies = new LinkedHashMap<>();
    private List<URL> urls;
    private ClassLoader parent;
    private URLStreamHandlerFactory factory;

    public Builder url(URL url) {
      if (this.urls == null) {
        this.urls = new ArrayList<>();
      }
      this.urls.add(url);
      return this;
    }

    public Builder urls(Iterable<URL> urls) {
      if (this.urls == null) {
        this.urls = new ArrayList<>();
      }
      if (urls instanceof Collection) {
        this.urls.addAll((Collection<URL>) urls);
      } else {
        urls.forEach(this.urls::add);
      }
      return this;
    }

    public Builder urls(URL... urls) {
      if (this.urls == null) {
        this.urls = new ArrayList<>();
      }
      Collections.addAll(this.urls, urls);
      return this;
    }

    public Builder clearUrls() {
      this.urls = null;
      return this;
    }

    public Builder parent(ClassLoader parent) {
      this.parent = parent;
      return this;
    }

    public Builder factory(URLStreamHandlerFactory factory) {
      this.factory = factory;
      return this;
    }

    public Builder loadPolicy(String name, LoadPolicy loadPolicy) {
      loadPolicies.put(name, loadPolicy);
      return this;
    }

    public Builder loadPolicies(Map<String, LoadPolicy> loadPolicies) {
      this.loadPolicies.putAll(loadPolicies);
      return this;
    }

    public Builder clearLoadPolicies() {
      loadPolicies.clear();
      return this;
    }

    public AuroraClassLoader build() {
      return new AuroraClassLoader(
          urls == null ? new URL[0] : urls.toArray(new URL[0]),
          parent,
          factory,
          loadPolicies
      );
    }
  }
}
