package org.inksnow.cputil.logger.impl.parent;

import org.inksnow.cputil.UnsafeUtil;
import org.inksnow.cputil.asm.SafeClassWriter;
import org.inksnow.cputil.classloader.AuroraClassLoader;
import org.inksnow.cputil.logger.AuroraLogger;
import org.objectweb.asm.*;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.function.Function;

public class AuroraParentLogger implements ILoggerFactory {
  public static final boolean DEFAULT_ENABLE_MARKER = false;
  public static final String DEFAULT_PARENT_PACKAGE_NAME = new String(new char[]{
      'o', 'r', 'g', '.', 's', 'l', 'f', '4', 'j'
  });

  public static final String SLF4J_PACKAGE_NAME = AuroraLogger.SLF4J_PACKAGE_NAME;
  public static final String SLF4J_INTERNAL_NAME = SLF4J_PACKAGE_NAME.replace('.', '/');

  private static final String LOGGER_PROXY_CLASS_NAME = SLF4J_PACKAGE_NAME + ".generated.LoggerProxy";
  private static final String LOGGER_PROXY_INTERNAL_NAME = LOGGER_PROXY_CLASS_NAME.replace('.', '/');

  private static final String LOGGER_PROXY_FACTORY_CLASS_NAME = SLF4J_PACKAGE_NAME + ".generated.LoggerProxyFactory";
  private static final String LOGGER_PROXY_FACTORY_INTERNAL_NAME = LOGGER_PROXY_FACTORY_CLASS_NAME.replace('.', '/');

  private final String parentPackageName;
  private final String parentInternalName;

  private final boolean enableMarker;
  private final AuroraClassLoader classLoader;
  private final Function<String, Logger> loggerFunction;

  public AuroraParentLogger() {
    this(DEFAULT_ENABLE_MARKER, DEFAULT_PARENT_PACKAGE_NAME);
  }

  public AuroraParentLogger(boolean enableMarker) {
    this(enableMarker, DEFAULT_PARENT_PACKAGE_NAME);
  }

  public AuroraParentLogger(String parentPackageName) {
    this(DEFAULT_ENABLE_MARKER, parentPackageName);
  }

  public AuroraParentLogger(boolean enableMarker, String parentPackageName) {
    this.enableMarker = enableMarker;
    this.parentPackageName = parentPackageName;
    this.parentInternalName = parentPackageName.replace('.', '/');
    this.classLoader = AuroraClassLoader.builder()
        .parent(Logger.class.getClassLoader())
        .build();

    byte[] loggerProxyClassBytes = generateLoggerProxy(classLoader);
    Class<?> loggerClass = classLoader.defineClass0(LOGGER_PROXY_CLASS_NAME, loggerProxyClassBytes, 0, loggerProxyClassBytes.length, (ProtectionDomain) null);

    byte[] loggerProxyFactoryClassBytes = generateLoggerFactory(classLoader);
    Class<?> loggerFactoryClass = classLoader.defineClass0(LOGGER_PROXY_FACTORY_CLASS_NAME, loggerProxyFactoryClassBytes, 0, loggerProxyFactoryClassBytes.length, (ProtectionDomain) null);
    try {
      this.loggerFunction = (Function) loggerFactoryClass.getConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw UnsafeUtil.<RuntimeException>unsafeThrow(e);
    }
  }

  @Override
  public Logger getLogger(String name) {
    return loggerFunction.apply(name);
  }

  public boolean isAvailable() {
    if (parentPackageName.equals(SLF4J_PACKAGE_NAME)) {
      return false;
    }
    try {
      Class.forName(parentPackageName + ".Logger", false, Logger.class.getClassLoader());
    } catch (ClassNotFoundException e) {
      return false;
    }
    return true;
  }

  private byte[] generateLoggerProxy(AuroraClassLoader classLoader) {
    String parentLoggerName = parentInternalName + "/Logger";

    ClassWriter cw = new SafeClassWriter(classLoader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, LOGGER_PROXY_INTERNAL_NAME, null, "java/lang/Object",
        new String[]{Logger.class.getName().replace('.', '/')});

    {
      FieldVisitor fv = cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "delegate", "L" + parentLoggerName + ";", null, null);
      fv.visitEnd();
    }

    {
      MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(L" + parentLoggerName + ";)V", null, null);
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitVarInsn(Opcodes.ALOAD, 1);
      mv.visitFieldInsn(Opcodes.PUTFIELD, LOGGER_PROXY_INTERNAL_NAME, "delegate", "L" + parentLoggerName + ";");
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

    for (Method method : Logger.class.getDeclaredMethods()) {
      if ((method.getModifiers() & Opcodes.ACC_STATIC) != 0) {
        continue;
      }
      Type methodType = Type.getType(method);
      Type[] argumentTypes = methodType.getArgumentTypes();
      Type returnType = methodType.getReturnType();

      MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), methodType.getDescriptor(), null, null);
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitFieldInsn(Opcodes.GETFIELD, LOGGER_PROXY_INTERNAL_NAME, "delegate", "L" + parentLoggerName + ";");

      StringBuilder methodDescriptor = new StringBuilder("(");
      int variableIndex = 1;
      for (Type argumentType : argumentTypes) {

        if (argumentType.getSort() == Type.OBJECT && argumentType.getInternalName().equals(SLF4J_INTERNAL_NAME + "/Marker")) {
          if (enableMarker) {
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, SLF4J_INTERNAL_NAME + "/Marker", "getName", "()Ljava/lang/String;", true);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, parentInternalName + "/MarkerFactory", "getMarker", "(Ljava/lang/String;)L" + parentInternalName + "/Marker;", false);
            methodDescriptor.append("L").append(parentInternalName).append("/Marker;");
          } else {
            variableIndex++;
          }
        } else {
          mv.visitVarInsn(argumentType.getOpcode(Opcodes.ILOAD), variableIndex);
          variableIndex += argumentType.getSize();
          methodDescriptor.append(argumentType.getDescriptor());
        }
      }
      methodDescriptor.append(")").append(returnType.getDescriptor());

      mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, parentLoggerName, method.getName(), methodDescriptor.toString(), true);
      mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));

      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

    cw.visitEnd();
    return cw.toByteArray();
  }

  private byte[] generateLoggerFactory(AuroraClassLoader classLoader) {
    ClassWriter cw = new SafeClassWriter(classLoader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, LOGGER_PROXY_FACTORY_INTERNAL_NAME, null, "java/lang/Object",
        new String[]{"java/util/function/Function"});

    {
      MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

    {
      MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
      mv.visitCode();
      mv.visitTypeInsn(Opcodes.NEW, LOGGER_PROXY_INTERNAL_NAME);
      mv.visitInsn(Opcodes.DUP);
      mv.visitVarInsn(Opcodes.ALOAD, 1);
      mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, parentInternalName + "/LoggerFactory", "getLogger", "(Ljava/lang/String;)L" + parentInternalName + "/Logger;", false);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, LOGGER_PROXY_INTERNAL_NAME, "<init>", "(L" + parentInternalName + "/Logger;)V", false);
      mv.visitInsn(Opcodes.ARETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }

    cw.visitEnd();
    return cw.toByteArray();
  }
}
