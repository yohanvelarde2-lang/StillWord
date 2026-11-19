package org.lockheart.abide;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

final class BookFiles {
  private static final long MAX_FILE = 200L * 1024 * 1024;
  private final File staged;
  private final File library;

  BookFiles(Context context) {
    staged = new File(context.getFilesDir(), "book-imports");
    library = new File(context.getFilesDir(), "download-library");
  }

  File importFile(ContentResolver resolver, Uri uri) throws IOException {
    String name = displayName(resolver, uri);
    String format = BookContent.format(name);
    if (!"pdf".equals(format) && !"epub".equals(format) && !"mobi".equals(format)) {
      throw new IOException("Choose a file ending in .pdf, .epub or .mobi.");
    }
    String title = name.substring(0, name.lastIndexOf('.'))
        .replace('/', '_').replace('\\', '_').replaceAll("[\\p{Cntrl}\\p{Cf}]", "_").trim();
    if (title.isEmpty()) title = "Untitled";
    if (title.length() > 45) {
      int end = Character.isHighSurrogate(title.charAt(44)) ? 44 : 45;
      title = title.substring(0, end);
    }

    ensureDirectory(staged);
    File[] oldImports = staged.listFiles(file -> file.isFile()
        && file.lastModified() < System.currentTimeMillis() - 7L * Policy.DAY);
    if (oldImports != null) for (File old : oldImports) old.delete();
    File dst = new File(staged, UUID.randomUUID() + "_" + title + "." + format);
    boolean complete = false;
    try {
      try (InputStream in = resolver.openInputStream(uri); FileOutputStream out = new FileOutputStream(dst)) {
        if (in == null) throw new IOException("Android could not open the selected file.");
        byte[] buffer = new byte[64 * 1024];
        long total = 0;
        for (int n; (n = in.read(buffer)) != -1; ) {
          if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
          total += n;
          if (total > MAX_FILE) throw new IOException("Choose a book smaller than 200 MiB.");
          out.write(buffer, 0, n);
        }
        out.getFD().sync();
      }
      complete = true;
    } finally {
      if (!complete) dst.delete();
    }
    return dst;
  }

  private static String displayName(ContentResolver resolver, Uri uri) {
    try (Cursor cursor = resolver.query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
      if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getString(0);
    }
    String name = uri.getLastPathSegment();
    return name == null ? "" : name;
  }

  File save(File file) throws IOException {
    ensureDirectory(library);
    File dst = new File(library, file.getName());
    if (!file.renameTo(dst)) throw new IOException("Could not save to Download Library.");
    return dst;
  }

  File[] list() {
    File[] entries = library.listFiles(File::isFile);
    if (entries == null) return new File[0];
    Arrays.sort(entries, Comparator.comparing(file -> title(file).toLowerCase(Locale.ROOT)));
    return entries;
  }

  File restore(String name, boolean saved) {
    if (name == null || !name.equals(new File(name).getName())) return null;
    File file = new File(saved ? library : staged, name);
    return file.isFile() ? file : null;
  }

  static String title(File file) {
    String name = file.getName();
    return name.substring(name.indexOf('_') + 1);
  }

  private static void ensureDirectory(File folder) throws IOException {
    if (!folder.isDirectory() && !folder.mkdirs() && !folder.isDirectory()) {
      throw new IOException("Could not create the book folder.");
    }
  }
}
