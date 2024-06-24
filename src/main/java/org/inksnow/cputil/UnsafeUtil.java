package org.inksnow.cputil;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class UnsafeUtil {
  private static final Unsafe UNSAFE = createUnsafe();
  private static final MethodHandles.Lookup LOOKUP = createLookup() ;

  private UnsafeUtil() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  private static Unsafe createUnsafe() {
    try {
      return Unsafe.getUnsafe();
    } catch (SecurityException e) {
      //
    }
    try {
      Field field = Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      return (Unsafe) field.get(null);
    } catch (Exception e2) {
      throw new RuntimeException("Unable to acquire Unsafe", e2);
    }
  }

  private static MethodHandles.Lookup createLookup() {
    try {
      Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
      return (MethodHandles.Lookup) UNSAFE.getObject(
          UNSAFE.staticFieldBase(field),
          UNSAFE.staticFieldOffset(field));
    } catch (Exception e) {
      throw new RuntimeException("Unable to acquire MethodHandles.Lookup", e);
    }
  }

  public static Unsafe unsafe() {
    return UNSAFE;
  }

  public static MethodHandles.Lookup lookup() {
    return LOOKUP;
  }

  public static <R> R unsafeThrow(Throwable t) {
    return UnsafeUtil.unsafeThrow0(t);
  }

  @SuppressWarnings("unchecked")
  private static <R, E extends Throwable> R unsafeThrow0(Throwable t) throws E {
    throw (E) t;
  }
}
