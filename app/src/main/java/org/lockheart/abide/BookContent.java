package org.lockheart.abide;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.Html;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class BookContent {
  private static final int MAX_ENTRY = 2 * 1024 * 1024;
  private static final int PAGE_CHARS = 7500;
  final File file;
  final List<String> pages;
  final int count;

  private BookContent(File file, List<String> pages, int count) {
    this.file = file;
    this.pages = pages;
    this.count = count;
  }

  BookContent withFile(File saved) { return new BookContent(saved, pages, count); }

  static String format(String name) {
    int dot = name.lastIndexOf('.');
    return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  static BookContent open(File file) throws Exception {
    String format = format(file.getName());
    if ("pdf".equals(format)) {
      try (PdfRenderer pdf = openPdf(file)) {
        int count = pdf.getPageCount();
        if (count < 1) throw new IOException("The PDF contains no pages.");
        return new BookContent(file, null, count);
      } catch (SecurityException e) {
        throw new IOException("This PDF is password protected.", e);
      }
    }
    List<String> pages = new ArrayList<>();
    if ("epub".equals(format)) readEpub(file, pages);
    else if ("mobi".equals(format)) addText(pages, MobiText.read(file));
    else throw new IOException("Choose a PDF, EPUB or MOBI file.");
    if (pages.isEmpty()) throw new IOException("No readable text was found in this book.");
    return new BookContent(file, pages, pages.size());
  }

  Bitmap renderPdfPage(int index, int targetWidth) throws IOException {
    try (PdfRenderer pdf = openPdf(file); PdfRenderer.Page page = pdf.openPage(index)) {
      double scale = Math.min(Math.min(1800, targetWidth) / (double) page.getWidth(), 2400d / page.getHeight());
      Bitmap bitmap = Bitmap.createBitmap(Math.max(1, (int) (page.getWidth() * scale)),
          Math.max(1, (int) (page.getHeight() * scale)), Bitmap.Config.ARGB_8888);
      bitmap.eraseColor(Color.WHITE);
      page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
      return bitmap;
    }
  }

  private static PdfRenderer openPdf(File file) throws IOException {
    ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    try { return new PdfRenderer(fd); }
    catch (IOException | RuntimeException e) { fd.close(); throw e; }
  }

  private static void readEpub(File file, List<String> pages) throws Exception {
    try (ZipFile zip = new ZipFile(file)) {
      String opf = null;
      try (InputStream input = entry(zip, "META-INF/container.xml")) {
        XmlPullParser xml = parser(input);
        for (int event = xml.next(); event != XmlPullParser.END_DOCUMENT; event = xml.next()) {
          if (event == XmlPullParser.START_TAG && "rootfile".equals(xml.getName())) {
            opf = xml.getAttributeValue(null, "full-path");
            break;
          }
        }
      }
      if (opf == null) throw new IOException("The EPUB has no book package.");
      Map<String, String> manifest = new HashMap<>();
      List<String> spine = new ArrayList<>();
      try (InputStream input = entry(zip, opf)) {
        XmlPullParser xml = parser(input);
        for (int event = xml.next(); event != XmlPullParser.END_DOCUMENT; event = xml.next()) {
          if (event != XmlPullParser.START_TAG) continue;
          if ("item".equals(xml.getName())) {
            String type = xml.getAttributeValue(null, "media-type");
            if ("application/xhtml+xml".equals(type) || "text/html".equals(type)) {
              manifest.put(xml.getAttributeValue(null, "id"), xml.getAttributeValue(null, "href"));
            }
          } else if ("itemref".equals(xml.getName()) && !"no".equals(xml.getAttributeValue(null, "linear"))) {
            spine.add(xml.getAttributeValue(null, "idref"));
          }
        }
      }
      int total = 0;
      for (String id : spine) {
        String href = manifest.get(id);
        if (href == null) continue;
        String part = Uri.decode(href.split("[?#]", 2)[0]);
        if (part.contains(":") || part.startsWith("/")) continue;
        Path parent = Paths.get(opf).getParent();
        Path path = (parent == null ? Paths.get(part) : parent.resolve(part)).normalize();
        if (path.startsWith("..") || path.isAbsolute()) continue;
        try (InputStream input = entry(zip, path.toString())) {
          ByteArrayOutputStream bytes = new ByteArrayOutputStream();
          byte[] buffer = new byte[8192];
          for (int n; (n = input.read(buffer)) != -1; ) {
            total += n;
            if (total > MobiText.MAX_TEXT) throw new IOException("The EPUB text exceeds the reader limit.");
            bytes.write(buffer, 0, n);
          }
          addText(pages, bytes.toString("UTF-8"));
        }
      }
    }
  }

  private static InputStream entry(ZipFile zip, String path) throws IOException {
    ZipEntry entry = zip.getEntry(path);
    if (entry == null || entry.isDirectory()) throw new IOException("A required EPUB section is missing.");
    if (entry.getSize() > MAX_ENTRY) throw new IOException("An EPUB section exceeds the reader limit.");
    return new FilterInputStream(zip.getInputStream(entry)) {
      private int count;
      private void check(int n) throws IOException {
        if (n > 0 && (count += n) > MAX_ENTRY) throw new IOException("An EPUB section exceeds the reader limit.");
      }
      @Override public int read() throws IOException {
        int b = super.read();
        check(b == -1 ? 0 : 1);
        return b;
      }
      @Override public int read(byte[] bytes, int offset, int length) throws IOException {
        int n = super.read(bytes, offset, length);
        check(n);
        return n;
      }
    };
  }

  private static XmlPullParser parser(InputStream input) throws Exception {
    XmlPullParser xml = Xml.newPullParser();
    xml.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
    xml.setInput(input, null);
    return xml;
  }

  private static void addText(List<String> pages, String html) {
    String safe = html.replaceAll("(?is)<(head|script|style)\\b[^>]*>.*?</\\1\\s*>", "");
    String text = Html.fromHtml(safe, Html.FROM_HTML_MODE_LEGACY).toString().trim();
    for (int start = 0; start < text.length(); ) {
      int end = Math.min(start + PAGE_CHARS, text.length());
      if (end < text.length()) {
        int boundary = text.lastIndexOf('\n', end);
        if (boundary <= start + PAGE_CHARS / 2) boundary = text.lastIndexOf(' ', end);
        if (boundary > start + PAGE_CHARS / 2) end = boundary + 1;
        if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
      }
      pages.add(text.substring(start, end).trim());
      start = end;
    }
  }
}
