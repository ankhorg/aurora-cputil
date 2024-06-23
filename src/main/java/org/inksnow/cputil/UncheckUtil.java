package org.inksnow.cputil;

public final class UncheckUtil {
  private UncheckUtil() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static <E extends Throwable> RuntimeException uncheckThrow(Throwable e) throws E {
    throw (E) e;
  }
}
