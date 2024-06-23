package org.inksnow.cputil.db.h2;

import org.inksnow.cputil.classloader.LoadPolicy;
import org.inksnow.cputil.db.Database;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractH2Database implements Database {
  private static final String DRIVER_CLASS_NAME = "org.h2.Driver";
  private static final Map<String, LoadPolicy> LOAD_POLICIES = createLoadPolicies();

  private static Map<String, LoadPolicy> createLoadPolicies() {
    Map<String, LoadPolicy> loadPolicies = new HashMap<>();
    loadPolicies.put("org/h2", LoadPolicy.SELF_ONLY);
    return Collections.unmodifiableMap(loadPolicies);
  }

  @Override
  public String driverClassName() {
    return DRIVER_CLASS_NAME;
  }

  @Override
  public Map<String, LoadPolicy> loadPolicies() {
    return LOAD_POLICIES;
  }
}
