package org.inksnow.cputil.classloader;

import org.inksnow.cputil.UnsafeUtil;
import org.inksnow.cputil.transform.Transformer;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.nio.ByteBuffer;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class AuroraClassLoader extends URLClassLoader {
  private static final LoadPolicy DEFAULT_LOAD_POLICY = LoadPolicy.PARENT_THEN_SELF;
  private static final Class<?> UCP_CLASS = getUcpClassImpl();
  private static final Class<?> RESOURCE_CLASS = getResourceClassImpl();

  private final Map<String, LoadPolicy> loadPolicies;
  private final Map<String, List<Transformer>> transformers;

  private final UclAccessor accessor;

  private AuroraClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory, Map<String, LoadPolicy> loadPolicies, Map<String, List<Transformer>> transformers) {
    super(urls, parent, factory);
    this.loadPolicies = loadPolicies;
    this.transformers = transformers;

    this.accessor = new UclAccessor(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  private static Class<?> getUcpClassImpl() {
    try {
      return Class.forName("sun.misc.URLClassPath", false, null);
    } catch (ClassNotFoundException e) {
      // ignore
    }
    try {
      return Class.forName("jdk.internal.loader.URLClassPath", false, null);
    } catch (ClassNotFoundException e) {
      // ignore
    }
    throw new IllegalStateException("Unable to find URLClassPath class");
  }

  private static Class<?> getResourceClassImpl() {
    try {
      return Class.forName("sun.misc.Resource", false, null);
    } catch (ClassNotFoundException e) {
      // ignore
    }
    try {
      return Class.forName("jdk.internal.loader.Resource", false, null);
    } catch (ClassNotFoundException e) {
      // ignore
    }
    throw new IllegalStateException("Unable to find Resource class");
  }

  public LoadPolicy getLoadPolicy(String name) {
    while (name.startsWith("/")) {
      name = name.substring(1);
    }

    while (true) {
      LoadPolicy loadPolicy = loadPolicies.get(name);
      if (loadPolicy != null) {
        return loadPolicy;
      }

      if (name.isEmpty()) {
        break;
      }

      int index = name.lastIndexOf('/');
      name = name.substring(0, (index == -1) ? 0 : index);
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
          clazz = findClassImpl(name);
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
          clazz = findClassImpl(name);
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
      return findClassImpl(name);
    } else {
      throw new ClassNotFoundException(name);
    }
  }

  private Class<?> findClassImpl(String name) throws ClassNotFoundException {
    String path = name.replace('.', '/').concat(".class");
    ResourceAccessor res = accessor.ucp().getResource(path, false);
    if (res == null) {
      throw new ClassNotFoundException(name);
    }
    try {
      return defineClassImpl(name, res);
    } catch (IOException e) {
      throw new ClassNotFoundException(name, e);
    } catch (ClassFormatError e2) {
      if (res.getDataError() != null) {
        e2.addSuppressed(res.getDataError());
      }
      throw e2;
    }
  }

  private byte[] applyTransformers(String name, byte[] bytes) {
    while (name.startsWith("/")) {
      name = name.substring(1);
    }

    while (true) {
      List<Transformer> transformers = this.transformers.get(name);
      if (transformers != null) {
        for (Transformer transformer : transformers) {
          byte[] transformed = transformer.transform(name, bytes);
          if (transformed != null) {
            bytes = transformed;
          }
        }
      }

      if (name.isEmpty()) {
        break;
      }

      int index = name.lastIndexOf('/');
      name = name.substring(0, (index == -1) ? 0 : index);
    }
    return bytes;
  }

  private Class<?> defineClassImpl(String name, ResourceAccessor res) throws IOException {
    int i = name.lastIndexOf('.');
    URL url = res.getCodeSourceURL();
    if (i != -1) {
      String pkgname = name.substring(0, i);
      // Check if package already loaded.
      Manifest man = res.getManifest();
      if (getAndVerifyPackageImpl(pkgname, man, url) == null) {
        try {
          if (man != null) {
            definePackage(pkgname, man, url);
          } else {
            definePackage(pkgname, null, null, null, null, null, null, null);
          }
        } catch (IllegalArgumentException iae) {
          // parallel-capable class loaders: re-verify in case of a
          // race condition
          if (getAndVerifyPackageImpl(pkgname, man, url) == null) {
            // Should never happen
            throw new AssertionError("Cannot find package " +
                pkgname);
          }
        }
      }
    }
    // Now read the class bytes and define the class
    java.nio.ByteBuffer bb = res.getByteBuffer();
    if (bb != null) {
      // Use (direct) ByteBuffer:
      CodeSigner[] signers = res.getCodeSigners();
      CodeSource cs = new CodeSource(url, signers);
      return defineClass0(name, bb, cs);
    } else {
      byte[] b = res.getBytes();
      // must read certificates AFTER reading bytes.
      CodeSigner[] signers = res.getCodeSigners();
      CodeSource cs = new CodeSource(url, signers);
      return defineClass0(name, b, 0, b.length, cs);
    }
  }

  private boolean isSealedImpl(String name, Manifest man) {
    Attributes attr = man.getAttributes(name.replace('.', '/').concat("/"));
    String sealed = null;
    if (attr != null) {
      sealed = attr.getValue(Attributes.Name.SEALED);
    }
    if (sealed == null) {
      if ((attr = man.getMainAttributes()) != null) {
        sealed = attr.getValue(Attributes.Name.SEALED);
      }
    }
    return "true".equalsIgnoreCase(sealed);
  }

  private Package getAndVerifyPackageImpl(String pkgname, Manifest man, URL url) {
    Package pkg = getPackage(pkgname);
    if (pkg != null) {
      // Package found, so check package sealing.
      if (pkg.isSealed()) {
        // Verify that code source URL is the same.
        if (!pkg.isSealed(url)) {
          throw new SecurityException(
              "sealing violation: package " + pkgname + " is sealed");
        }
      } else {
        // Make sure we are not attempting to seal the package
        // at this code source URL.
        if ((man != null) && isSealedImpl(pkgname, man)) {
          throw new SecurityException(
              "sealing violation: can't seal package " + pkgname +
                  ": already loaded");
        }
      }
    }
    return pkg;
  }

  private void definePackageImpl(String pkgname, Manifest man, URL url) {
    if (getAndVerifyPackageImpl(pkgname, man, url) == null) {
      try {
        if (man != null) {
          definePackage(pkgname, man, url);
        } else {
          definePackage(pkgname, null, null, null, null, null, null, null);
        }
      } catch (IllegalArgumentException iae) {
        // parallel-capable class loaders: re-verify in case of a
        // race condition
        if (getAndVerifyPackageImpl(pkgname, man, url) == null) {
          // Should never happen
          throw new AssertionError("Cannot find package " +
              pkgname);
        }
      }
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

  @Override
  public Package definePackage(String name, Manifest man, URL url) {
    return super.definePackage(name, man, url);
  }

  @Override
  public Package definePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) {
    return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
  }

  public Class<?> defineClass0(String name, byte[] b, int off, int len, CodeSource cs) {
    byte[] transformed = applyTransformers(name, Arrays.copyOfRange(b, off, off + len));

    return defineClass(name, transformed, 0, transformed.length, cs);
  }

  public Class<?> defineClass0(String name, byte[] b, int off, int len, ProtectionDomain protectionDomain) {
    byte[] transformed = applyTransformers(name, Arrays.copyOfRange(b, off, off + len));

    return defineClass(name, transformed, 0, transformed.length, protectionDomain);
  }

  public Class<?> defineClass0(String name, ByteBuffer bb, CodeSource cs) {
    byte[] b = new byte[bb.remaining()];
    bb.get(b);
    byte[] transformed = applyTransformers(name, b);

    return defineClass(name, transformed, 0, transformed.length, cs);
  }

  public static final class Builder {
    private final Map<String, LoadPolicy> loadPolicies = new LinkedHashMap<>();
    private final Map<String, List<Transformer>> transformers = new LinkedHashMap<>();
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

    public Builder transformer(String name, Transformer transformer) {
      transformers.computeIfAbsent(name, k -> new ArrayList<>()).add(transformer);
      return this;
    }

    public Builder transformers(String name, List<Transformer> transformers) {
      transformers.forEach(transformer -> this.transformer(name, transformer));
      return this;
    }

    public Builder transformers(Map<String, List<Transformer>> transformers) {
      transformers.forEach(this::transformers);
      return this;
    }

    public Builder clearTransformers() {
      transformers.clear();
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
          loadPolicies,
          transformers
      );
    }
  }

  private static final class UclAccessor {
    private final URLClassLoader ucl;
    private final Object ucp;

    private final UcpAccessor ucpAccessor;

    public UclAccessor(URLClassLoader ucl) {
      this.ucl = ucl;
      this.ucp = getUcpImpl();
      this.ucpAccessor = new UcpAccessor(ucp);
    }

    public URLClassLoader instance() {
      return ucl;
    }

    private Object getUcpImpl() {
      try {
        return UnsafeUtil.lookup().findGetter(URLClassLoader.class, "ucp", UCP_CLASS).invoke(ucl);
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    public UcpAccessor ucp() {
      return ucpAccessor;
    }
  }

  private static final class UcpAccessor {
    private static final MethodHandle GET_RESOURCE_STRING_BOOLEAN = createGetResourceImpl();
    private final Object ucp;

    public UcpAccessor(Object ucp) {
      this.ucp = UCP_CLASS.cast(ucp);
    }

    private static MethodHandle createGetResourceImpl() {
      try {
        return UnsafeUtil.lookup().findVirtual(UCP_CLASS, "getResource", MethodType.methodType(RESOURCE_CLASS, String.class, boolean.class));
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    private Object instance() {
      return ucp;
    }

    public ResourceAccessor getResource(String name, boolean check) {
      try {
        Object resInstance = GET_RESOURCE_STRING_BOOLEAN.invoke(ucp, name, check);
        return resInstance == null ? null : new ResourceAccessor(resInstance);
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }
  }

  private static final class ResourceAccessor {
    private static final MethodHandle GET_CODE_SOURCE_URL = createGetCodeSourceUrlImpl();
    private static final MethodHandle GET_MANIFEST = createGetManifestImpl();
    private static final MethodHandle GET_BYTE_BUFFER = createGetByteBufferImpl();
    private static final MethodHandle GET_CODE_SIGNERS = createGetCodeSignersImpl();
    private static final MethodHandle GET_BYTES = createGetBytesImpl();
    private static final MethodHandle GET_DATA_ERROR = createGetDataErrorImpl();
    private final Object resource;

    public ResourceAccessor(Object resource) {
      this.resource = RESOURCE_CLASS.cast(resource);
    }

    private static MethodHandle createGetCodeSourceUrlImpl() {
      try {
        return UnsafeUtil.lookup().findVirtual(RESOURCE_CLASS, "getCodeSourceURL", MethodType.methodType(URL.class));
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    private static MethodHandle createGetManifestImpl() {
      try {
        return UnsafeUtil.lookup().findVirtual(RESOURCE_CLASS, "getManifest", MethodType.methodType(Manifest.class));
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    private static MethodHandle createGetByteBufferImpl() {
      try {
        return UnsafeUtil.lookup().findVirtual(RESOURCE_CLASS, "getByteBuffer", MethodType.methodType(ByteBuffer.class));
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    private static MethodHandle createGetCodeSignersImpl() {
      try {
        return UnsafeUtil.lookup().findVirtual(RESOURCE_CLASS, "getCodeSigners", MethodType.methodType(CodeSigner[].class));
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    private static MethodHandle createGetBytesImpl() {
      try {
        return UnsafeUtil.lookup().findVirtual(RESOURCE_CLASS, "getBytes", MethodType.methodType(byte[].class));
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    private static MethodHandle createGetDataErrorImpl() {
      try {
        return UnsafeUtil.lookup().findVirtual(RESOURCE_CLASS, "getDataError", MethodType.methodType(Exception.class));
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    private Object instance() {
      return resource;
    }

    public Exception getDataError() {
      try {
        return (Exception) GET_DATA_ERROR.invoke(resource);
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    public URL getCodeSourceURL() {
      try {
        return (URL) GET_CODE_SOURCE_URL.invoke(resource);
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    public ByteBuffer getByteBuffer() {
      try {
        return (ByteBuffer) GET_BYTE_BUFFER.invoke(resource);
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    public Manifest getManifest() {
      try {
        return (Manifest) GET_MANIFEST.invoke(resource);
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    public CodeSigner[] getCodeSigners() {
      try {
        return (CodeSigner[]) GET_CODE_SIGNERS.invoke(resource);
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }

    public byte[] getBytes() {
      try {
        return (byte[]) GET_BYTES.invoke(resource);
      } catch (Throwable e) {
        return UnsafeUtil.unsafeThrow(e);
      }
    }
  }
}
