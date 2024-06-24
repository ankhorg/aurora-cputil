package org.inksnow.cputil.db.h2;

import org.inksnow.cputil.classloader.LoadPolicy;
import org.inksnow.cputil.db.Database;
import org.inksnow.cputil.db.transform.DisableRegisterDriverTransformer;
import org.inksnow.cputil.transform.Transformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractH2Database implements Database {
  private static final String DRIVER_CLASS_NAME = "org.h2.Driver";
  private static final Map<String, LoadPolicy> LOAD_POLICIES = createLoadPolicies();
  private static final Map<String, List<Transformer>> TRANSFORMERS = createTransformers();

  private static Map<String, LoadPolicy> createLoadPolicies() {
    Map<String, LoadPolicy> loadPolicies = new HashMap<>();
    loadPolicies.put("org/h2", LoadPolicy.SELF_ONLY);
    return Collections.unmodifiableMap(loadPolicies);
  }

  private static Map<String, List<Transformer>> createTransformers() {
    Map<String, List<Transformer>> transformers = new HashMap<>();
    transformers.put("org/h2/Driver", Collections.singletonList(new DisableRegisterDriverTransformer()));
    return Collections.unmodifiableMap(transformers);
  }

  @Override
  public String driverClassName() {
    return DRIVER_CLASS_NAME;
  }

  @Override
  public Map<String, LoadPolicy> loadPolicies() {
    return LOAD_POLICIES;
  }

  @Override
  public Map<String, List<Transformer>> transformers() {
    return TRANSFORMERS;
  }
}
