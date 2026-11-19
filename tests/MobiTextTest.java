package org.lockheart.abide;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

public final class MobiTextTest {
  public static void main(String[] args) throws Exception {
    File fixture = File.createTempFile("stillword-test-", ".mobi");
    try {
      verifyBook(fixture, 1, 0, "<p>quiet</p>".getBytes(StandardCharsets.UTF_8), "<p>quiet</p>");
      verifyBook(fixture, 2, 0, new byte[] {'a', 'b', 'c', (byte) 0x80, 0x18, (byte) 0xd8}, "abcabc X");
      verifyBook(fixture, 2, 1, new byte[] {'h', 'i', (byte) 0x80, 0x10, 0}, "hihih");
      verifyBook(fixture, 1, 2, new byte[] {'x', 9, (byte) 0x82}, "x");
      Files.write(fixture.toPath(), book(1, 2, 0, new byte[] {'x'}, 1));
      rejects(fixture, "DRM");
      Files.write(fixture.toPath(), book(17480, 0, 0, new byte[] {'x'}, 1));
      rejects(fixture, "compression");
      Files.write(fixture.toPath(), book(2, 0, 0, new byte[] {(byte) 0x80, 0x10}, 3));
      rejects(fixture, "incomplete");
      byte[] out = new byte[10];
      int written = MobiText.decode(new byte[] {3, (byte) 0xe6, (byte) 0x97, (byte) 0xa5}, 4, out);
      if (!"日".equals(new String(Arrays.copyOf(out, written), StandardCharsets.UTF_8))) throw new AssertionError();
      System.out.println("PASS: 8 MOBI checks, including compression, trailing data and malformed inputs");
    } finally { fixture.delete(); }
  }

  private static void verifyBook(File file, int compression, int flags, byte[] record, String expected) throws Exception {
    Files.write(file.toPath(), book(compression, 0, flags, record, expected.getBytes(StandardCharsets.UTF_8).length));
    if (!expected.equals(MobiText.read(file))) throw new AssertionError("Incorrect MOBI text");
  }

  private static void rejects(File file, String reason) throws Exception {
    try { MobiText.read(file); }
    catch (IOException e) {
      if (e.getMessage().contains(reason)) return;
      throw e;
    }
    throw new AssertionError("Expected MOBI rejection");
  }

  private static byte[] book(int compression, int drm, int flags, byte[] record, int size) {
    int header = 96, text = header + 248;
    ByteBuffer bytes = ByteBuffer.allocate(text + record.length);
    bytes.position(60);
    bytes.putLong(0x424f4f4b4d4f4249L);
    bytes.position(76);
    bytes.putShort((short) 2).putInt(header).putInt(0).putInt(text).putInt(0);
    bytes.position(header);
    bytes.putShort((short) compression).putShort((short) 0).putInt(size);
    bytes.putShort((short) 1).putShort((short) 4096).putShort((short) drm).putShort((short) 0);
    bytes.putInt(0x4d4f4249).putInt(232).putInt(2).putInt(65001).putInt(0).putInt(6);
    bytes.putShort(header + 242, (short) flags);
    bytes.position(text);
    bytes.put(record);
    return bytes.array();
  }
}
