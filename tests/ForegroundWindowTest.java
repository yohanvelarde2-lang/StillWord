package org.lockheart.abide;

import android.view.accessibility.AccessibilityWindowInfo;
import java.util.Arrays;
import static android.view.accessibility.AccessibilityWindowInfo.*;

public final class ForegroundWindowTest {
  public static void main(String[] args) {
    AccessibilityWindowInfo overview = new AccessibilityWindowInfo(TYPE_SYSTEM, true, true);
    AccessibilityWindowInfo guard = new AccessibilityWindowInfo(TYPE_ACCESSIBILITY_OVERLAY, true, true);
    AccessibilityWindowInfo background = new AccessibilityWindowInfo(TYPE_APPLICATION, false, false);
    AccessibilityWindowInfo resumed = new AccessibilityWindowInfo(TYPE_APPLICATION, true, false);
    AccessibilityWindowInfo active = new AccessibilityWindowInfo(TYPE_APPLICATION, false, true);
    expect(null, false, overview, background);
    expect(resumed, false, overview, active, resumed);
    expect(active, false, overview, active);
    expect(background, true, guard, background);
    expect(resumed, true, guard, background, resumed);
    expect(null, false, overview);
    expect(null, true, guard);
    System.out.println("PASS: 7 foreground window selection checks");
  }

  private static void expect(AccessibilityWindowInfo expected, boolean guardHasFocus,
      AccessibilityWindowInfo... windows) {
    if (ForegroundWindow.select(Arrays.asList(windows), guardHasFocus) != expected) {
      throw new AssertionError("Unexpected foreground window");
    }
  }
}
