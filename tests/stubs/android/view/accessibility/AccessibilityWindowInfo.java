package android.view.accessibility;

public final class AccessibilityWindowInfo {
  public static final int TYPE_APPLICATION = 1;
  public static final int TYPE_SYSTEM = 3;
  public static final int TYPE_ACCESSIBILITY_OVERLAY = 4;

  private final int type;
  private final boolean focused;
  private final boolean active;

  public AccessibilityWindowInfo(int type, boolean focused, boolean active) {
    this.type = type;
    this.focused = focused;
    this.active = active;
  }

  public int getType() { return type; }
  public boolean isFocused() { return focused; }
  public boolean isActive() { return active; }
}
