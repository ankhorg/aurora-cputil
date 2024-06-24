package org.inksnow.cputil.db.transform;

import org.inksnow.cputil.transform.Transformer;
import org.objectweb.asm.*;

public class DisableRegisterDriverTransformer implements Transformer {

  @Override
  public byte[] transform(String className, byte[] classFileBuffer) {
    boolean[] foundRegisterCall = {false};

    ClassReader classReader = new ClassReader(classFileBuffer);
    ClassWriter classWriter = new ClassWriter(classReader, 0);
    classReader.accept(new DrdClassVisitor(classWriter, foundRegisterCall), 0);

    if (foundRegisterCall[0]) {
      return classWriter.toByteArray();
    } else {
      return null;
    }
  }

  private static final class DrdClassVisitor extends ClassVisitor {
    private final boolean[] foundRegisterCall;

    public DrdClassVisitor(ClassVisitor cv, boolean[] foundRegisterCalli) {
      super(Opcodes.ASM9, cv);
      this.foundRegisterCall = foundRegisterCalli;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      return new DrdMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions), foundRegisterCall);
    }
  }

  private static final class DrdMethodVisitor extends MethodVisitor {
    private final boolean[] foundRegisterCall;

    public DrdMethodVisitor(MethodVisitor mv, boolean[] foundRegisterCalli) {
      super(Opcodes.ASM9, mv);
      this.foundRegisterCall = foundRegisterCalli;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
      if (opcode == Opcodes.INVOKESTATIC && owner.equals("java/sql/DriverManager") && name.equals("registerDriver")) {
        if (descriptor.equals("(Ljava/sql/Driver;)V")) {
          super.visitInsn(Opcodes.POP);
        } else if (descriptor.equals("(Ljava/sql/Driver;Ljava/sql/DriverAction;)V")) {
          super.visitInsn(Opcodes.POP);
          super.visitInsn(Opcodes.POP);
        } else {
          throw new IllegalStateException("Unexpected descriptor: " + descriptor);
        }
        foundRegisterCall[0] = true;
        return;
      }
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
  }
}
