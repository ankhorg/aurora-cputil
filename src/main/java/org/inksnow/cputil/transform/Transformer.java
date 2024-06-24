package org.inksnow.cputil.transform;

public interface Transformer {
  byte[] transform(String className, byte[] classFileBuffer);
}
