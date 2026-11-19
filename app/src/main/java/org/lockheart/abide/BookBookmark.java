package org.lockheart.abide;

import android.content.SharedPreferences;

final class BookBookmark {
  final int page;
  final float scroll;

  BookBookmark(int page, float scroll) {
    this.page = page;
    this.scroll = Float.isNaN(scroll) ? 0 : Math.max(0, Math.min(1, scroll));
  }

  static BookBookmark load(SharedPreferences prefs, String bookId) {
    return new BookBookmark(prefs.getInt("book-page-" + bookId, 0),
        prefs.getFloat("book-scroll-" + bookId, 0));
  }

  void save(SharedPreferences prefs, String bookId) {
    prefs.edit().putInt("book-page-" + bookId, page)
        .putFloat("book-scroll-" + bookId, scroll).apply();
  }
}
