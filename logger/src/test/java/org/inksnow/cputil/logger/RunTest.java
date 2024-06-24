package org.inksnow.cputil.logger;

import org.inksnow.cputil.logger.impl.parent.AuroraParentLogger;
import org.slf4j.Logger;

import java.io.IOException;

public class RunTest {
  public static void main(String[] args) throws IOException {
    Logger logger = new AuroraParentLogger(false).getLogger("Hello");

    logger.info("Hello, world!");
  }
}
