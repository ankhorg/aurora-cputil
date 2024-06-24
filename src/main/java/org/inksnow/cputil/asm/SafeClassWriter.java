package org.inksnow.cputil.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class SafeClassWriter extends ClassWriter {
  private final ClassLoader classLoader;

  public SafeClassWriter(ClassLoader classLoader, int flags) {
    super(flags);
    this.classLoader = classLoader;
  }

  public SafeClassWriter(ClassLoader classLoader, ClassReader classReader, int flags) {
    super(classReader, flags);
    this.classLoader = classLoader;
  }

  @Override
  protected ClassLoader getClassLoader() {
    return classLoader;
  }
}
