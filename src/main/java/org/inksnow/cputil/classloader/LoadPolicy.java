package org.inksnow.cputil.classloader;

public enum LoadPolicy {
  PARENT_ONLY(false, true, false),
  SELF_ONLY(true, false, false),
  PARENT_THEN_SELF(false, true, true),
  SELF_THEN_PARENT(true, true, false),
  DISABLED(false, false, false);

  private final boolean selfFirst;
  private final boolean parentThen;
  private final boolean selfLast;

  LoadPolicy(boolean selfFirst, boolean parentThen, boolean selfLast) {
    this.selfFirst = selfFirst;
    this.parentThen = parentThen;
    this.selfLast = selfLast;
  }

  public boolean selfFirst() {
    return selfFirst;
  }

  public boolean parentThen() {
    return parentThen;
  }

  public boolean selfLast() {
    return selfLast;
  }
}
