package org.lockheart.abide;

import android.content.SharedPreferences;

public final class BookBookmarkTest {
  public static void main(String[] args) {
    SharedPreferences prefs = new SharedPreferences();
    prefs.edit().putInt("book-page-first.epub", 8).apply();
    expect(prefs, "first.epub", 8, 0);
    expect(prefs, "second.pdf", 0, 0);

    new BookBookmark(8, 0.75f).save(prefs, "first.epub");
    new BookBookmark(12, 0.25f).save(prefs, "second.pdf");
    expect(prefs, "first.epub", 8, 0.75f);
    expect(prefs, "second.pdf", 12, 0.25f);

    new BookBookmark(9, 0).save(prefs, "first.epub");
    expect(prefs, "first.epub", 9, 0);
    expect(prefs, "second.pdf", 12, 0.25f);
    System.out.println("PASS: 6 per-book bookmark and upgrade checks");
  }

  private static void expect(SharedPreferences prefs, String id, int page, float scroll) {
    BookBookmark bookmark = BookBookmark.load(prefs, id);
    if (bookmark.page != page || bookmark.scroll != scroll) throw new AssertionError(id);
  }
}
