package org.inksnow.cputil.classloader;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public final class CompoundEnumeration<E> implements Enumeration<E> {
  private final Enumeration<E>[] enums;
  private int index;

  public CompoundEnumeration(Enumeration<E>[] enums) {
    this.enums = enums;
  }

  private boolean next() {
    while (index < enums.length) {
      if (enums[index] != null && enums[index].hasMoreElements()) {
        return true;
      }
      index++;
    }
    return false;
  }

  public boolean hasMoreElements() {
    return next();
  }

  public E nextElement() {
    if (!next()) {
      throw new NoSuchElementException();
    }
    return enums[index].nextElement();
  }
}