package org.inksnow.cputil.db.transform;

import org.inksnow.cputil.transform.Transformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;

public class RedirectSlf4jTransformer implements Transformer {
  public static final String UNMAPPED_SLF4J_PACKAGE_NAME = new String(new char[]{
      'o', 'r', 'g', '.', 's', 'l', 'f', '4', 'j'
  });
  public static final String SLF4J_PACKAGE_NAME = Logger.class.getName().substring(0,
      Logger.class.getName().lastIndexOf('.')
  );

  public static final String UNMAPPED_SLF4J_PACKAGE_INTERNALNAME = UNMAPPED_SLF4J_PACKAGE_NAME.replace('.', '/') + "/";
  public static final String SLF4J_PACKAGE_INTERNALNAME = SLF4J_PACKAGE_NAME.replace('.', '/') + "/";

  private final String targetPackage;

  public RedirectSlf4jTransformer(String targetPackage) {
    this.targetPackage = targetPackage;
  }

  public RedirectSlf4jTransformer() {
    this.targetPackage = SLF4J_PACKAGE_INTERNALNAME;
  }

  @Override
  public byte[] transform(String className, byte[] classFileBuffer) {
    boolean[] foundInvokeSlf4j = {false};

    ClassReader classReader = new ClassReader(classFileBuffer);
    ClassWriter classWriter = new ClassWriter(classReader, 0);
    classReader.accept(new ClassRemapper(classWriter, new Slf4jRemapper(foundInvokeSlf4j)), 0);

    if (foundInvokeSlf4j[0]) {
      return classWriter.toByteArray();
    } else {
      return null;
    }
  }

  private final class Slf4jRemapper extends Remapper {
    private final boolean[] foundInvokeSlf4j;

    public Slf4jRemapper(boolean[] foundInvokeSlf4j) {
      this.foundInvokeSlf4j = foundInvokeSlf4j;
    }

    @Override
    public String map(String internalName) {
      if (internalName.startsWith(UNMAPPED_SLF4J_PACKAGE_INTERNALNAME)) {
        return targetPackage + internalName.substring(UNMAPPED_SLF4J_PACKAGE_INTERNALNAME.length());
      }
      return internalName;
    }
  }
}
