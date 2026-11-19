package org.lockheart.abide;

import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;

final class ForegroundWindow {
  static AccessibilityWindowInfo select(List<AccessibilityWindowInfo> windows, boolean guardHasFocus) {
    AccessibilityWindowInfo topApp = null;
    AccessibilityWindowInfo activeApp = null;
    for (AccessibilityWindowInfo window : windows) {
      if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
      if (window.isFocused()) return window;
      if (topApp == null) topApp = window;
      if (activeApp == null && window.isActive()) activeApp = window;
    }
    if (activeApp != null) return activeApp;

    // The guard itself takes focus. Android reports the app below it in layer order.
    return guardHasFocus ? topApp : null;
  }
}
