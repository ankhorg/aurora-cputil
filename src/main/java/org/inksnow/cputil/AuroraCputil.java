package org.inksnow.cputil;

public final class AuroraCputil {
  private static ClassLoader contextClassLoader = AuroraCputil.class.getClassLoader();

  private AuroraCputil() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static void contextClassLoader(ClassLoader classLoader) {
    contextClassLoader = classLoader;
  }

  public static ClassLoader contextClassLoader() {
    return contextClassLoader;
  }
}
