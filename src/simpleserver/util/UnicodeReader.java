/*
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package simpleserver.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

public class UnicodeReader extends Reader {
  private static final int BOM_SIZE = 4;
  private static LinkedHashMap<String, byte[]> BOMS;

  private PushbackInputStream pushbackReader;
  private InputStreamReader reader;
  private String encoding;

  public UnicodeReader(InputStream in) {
    this(in, "UTF-8");
  }

  public UnicodeReader(InputStream in, String encoding) {
    pushbackReader = new PushbackInputStream(in, BOM_SIZE);
    this.encoding = encoding;

    BOMS = new LinkedHashMap<String, byte[]>(5);
    BOMS.put("UTF-8", new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
    BOMS.put("UTF-32BE", new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF });
    BOMS.put("UTF-32LE", new byte[] { (byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00 });
    BOMS.put("UTF-16BE", new byte[] { (byte) 0xFE, (byte) 0xFF });
    BOMS.put("UTF-16LE", new byte[] { (byte) 0xFF, (byte) 0xFE });
  }

  public String getEncoding() {
    try {
      return reader.getEncoding();
    } catch (NullPointerException e) {
      return null;
    }
  }

  protected void init() throws IOException {
    if (reader != null) {
      return;
    }

    processBOM();
  }

  protected void processBOM() throws IOException {

    byte[] bom = new byte[BOM_SIZE];
    int read = pushbackReader.read(bom, 0, BOM_SIZE);
    int unread = 0;

    Set<String> encodings = BOMS.keySet();
    Iterator<String> itr = encodings.iterator();
    while (itr.hasNext()) {
      String currentEncoding = itr.next();
      byte[] currentBOM = BOMS.get(currentEncoding);
      if (arrayStartsWith(bom, currentBOM)) {
        encoding = currentEncoding;
        unread = currentBOM.length;
        break;
      }
    }

    if (unread <= 4) {
      pushbackReader.unread(bom, unread, read - unread);
    }
    if (encoding == null) {
      reader = new InputStreamReader(pushbackReader);
    } else {
      reader = new InputStreamReader(pushbackReader, encoding);
    }
  }

  protected boolean arrayStartsWith(byte[] in, byte[] needleBytes) {
    int pos = 0;
    boolean found = true;

    if (in.length < needleBytes.length) {
      return false;
    }

    for (byte c : needleBytes) {
      if (c != in[pos++]) {
        found = false;
        break;
      }
    }
    return found;
  }

  @Override
  public void close() throws IOException {
    if (reader != null) {
      reader.close();
    }
  }

  @Override
  public int read(char[] buffer, int offset, int length) throws IOException {
    init();
    return reader.read(buffer, offset, length);
  }
}
