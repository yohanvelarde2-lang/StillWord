package org.lockheart.abide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

final class MobiText {
  static final int MAX_TEXT = 8 * 1024 * 1024;

  static String read(File file) throws IOException {
    try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
      if (input.length() < 94) throw new IOException("This MOBI file is incomplete.");
      input.seek(60);
      if (input.readLong() != 0x424f4f4b4d4f4249L) throw new IOException("This file is not a MOBI book.");
      input.seek(76);
      int count = input.readUnsignedShort();
      if (count < 2 || 78L + 8L * count > input.length()) throw corrupt();
      long[] offsets = new long[count + 1];
      offsets[count] = input.length();
      for (int i = 0; i < count; i++) {
        input.seek(78L + 8L * i);
        offsets[i] = Integer.toUnsignedLong(input.readInt());
        if (offsets[i] >= input.length() || (i > 0 && offsets[i] < offsets[i - 1])) throw corrupt();
      }
      if (offsets[0] < 78L + 8L * count || offsets[1] - offsets[0] < 40) throw corrupt();
      input.seek(offsets[0]);
      int compression = input.readUnsignedShort();
      input.skipBytes(2);
      long length = Integer.toUnsignedLong(input.readInt());
      int records = input.readUnsignedShort();
      input.skipBytes(2);
      if (input.readUnsignedShort() != 0) throw new IOException("This MOBI book is protected by DRM.");
      if (compression != 1 && compression != 2) throw new IOException("This MOBI compression is unsupported. Use a DRM-free EPUB copy.");
      if (length == 0 || length > MAX_TEXT || records < 1 || records >= count) throw corrupt();

      input.seek(offsets[0] + 16);
      if (input.readInt() != 0x4d4f4249) throw corrupt();
      long headerLength = Integer.toUnsignedLong(input.readInt());
      input.skipBytes(4);
      int encoding = input.readInt();
      input.skipBytes(4);
      int version = input.readInt();
      if (version >= 8) throw new IOException("This is a Kindle Format 8 book. Use a DRM-free EPUB copy.");
      int flags = 0;
      if (version >= 5 && headerLength >= 228 && offsets[1] - offsets[0] >= 244) {
        input.seek(offsets[0] + 242);
        flags = input.readUnsignedShort();
      }

      ByteArrayOutputStream text = new ByteArrayOutputStream((int) length);
      for (int i = 1; i <= records && text.size() < length; i++) {
        long size = offsets[i + 1] - offsets[i];
        if (size > 128 * 1024) throw corrupt();
        byte[] data = new byte[(int) size];
        input.seek(offsets[i]);
        input.readFully(data);
        int end = stripTrailing(data, flags);
        if (compression == 1) text.write(data, 0, Math.min(end, (int) length - text.size()));
        else {
          byte[] decoded = new byte[64 * 1024];
          int written = decode(data, end, decoded);
          text.write(decoded, 0, Math.min(written, (int) length - text.size()));
        }
      }
      if (text.size() != length) throw corrupt();
      Charset charset = encoding == 65001 ? StandardCharsets.UTF_8 : Charset.forName("windows-1252");
      return new String(text.toByteArray(), charset);
    }
  }

  private static int stripTrailing(byte[] data, int flags) throws IOException {
    int end = data.length;
    for (int entries = flags >>> 1; entries != 0; entries >>>= 1) {
      if ((entries & 1) == 0) continue;
      int size = 0;
      for (int shift = 0; shift < 28; shift += 7) {
        int position = end - 1 - shift / 7;
        if (position < 0) throw corrupt();
        int value = data[position] & 255;
        size |= (value & 127) << shift;
        if ((value & 128) != 0) break;
      }
      if (size < 1 || size > end) throw corrupt();
      end -= size;
    }
    if ((flags & 1) != 0) {
      if (end == 0) throw corrupt();
      end -= (data[end - 1] & 3) + 1;
    }
    if (end < 0) throw corrupt();
    return end;
  }

  static int decode(byte[] data, int end, byte[] output) throws IOException {
    int written = 0;
    for (int i = 0; i < end; ) {
      int b = data[i++] & 255;
      if (b >= 1 && b <= 8) {
        if (i + b > end || written + b > output.length) throw corrupt();
        System.arraycopy(data, i, output, written, b);
        i += b;
        written += b;
      } else if (b <= 127) {
        if (written == output.length) throw corrupt();
        output[written++] = (byte) b;
      } else if (b >= 192) {
        if (written + 2 > output.length) throw corrupt();
        output[written++] = ' ';
        output[written++] = (byte) (b ^ 128);
      } else {
        if (i == end) throw corrupt();
        int pair = (b << 8) | (data[i++] & 255);
        int distance = (pair >> 3) & 0x7ff;
        int length = (pair & 7) + 3;
        if (distance == 0 || distance > written || written + length > output.length) throw corrupt();
        for (int j = 0; j < length; j++, written++) output[written] = output[written - distance];
      }
    }
    return written;
  }

  private static IOException corrupt() { return new IOException("This MOBI file is incomplete or its text exceeds the reader limit."); }
}
