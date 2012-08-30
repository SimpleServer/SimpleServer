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
package simpleserver.export;

import static simpleserver.util.Util.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import simpleserver.Resource;
import simpleserver.config.SortedProperties;

abstract class PropertiesExport implements Resource {
  private static final String LOCATION = "simpleserver" + File.separator + "export";
  protected SortedProperties properties;

  protected final String filename;
  protected String header;

  protected static final String DELIMITER = ",";
  protected final int valueCount;

  public PropertiesExport(String name, int valueCount) {
    filename = name;
    properties = new SortedProperties();
    this.valueCount = valueCount;
    header = null;
  }

  protected abstract void populate();

  protected File getFile() {
    File file = new File(LOCATION + File.separator + filename);
    if (file.exists()) {
      return file;
    }

    populate();
    new File(LOCATION).mkdir();
    return file;
  }

  public void load() {
    try {
      InputStream stream = new FileInputStream(getFile());
      try {
        properties.load(stream);
      } finally {
        stream.close();
      }
    } catch (FileNotFoundException e) {
      save();
    } catch (IOException e) {
      properties.clear();
      populate();
      save();
    }
  }

  public void save() {
    try {
      OutputStream stream = new FileOutputStream(getFile());
      try {
        properties.store(stream, header);
      } finally {
        stream.close();
      }
    } catch (IOException e) {
      print(e);
      print("Failed to save export " + filename);
    }
  }

  protected void setEntry(String key, String... values) {
    properties.setProperty(key.toLowerCase(), join(DELIMITER, values, valueCount));
  }

  protected void updateEntry(String key, int updateIndex, String updateValue) {
    key = key.toLowerCase();
    String oldEntry = properties.getProperty(key);
    if (oldEntry != null) {
      String[] parts = oldEntry.split(DELIMITER);
      parts[updateIndex] = updateValue;

      properties.setProperty(key, join(DELIMITER, parts, valueCount));
    }
  }

  public static String join(String glue, String[] pieces, int maxPieces) {
    if (pieces.length > maxPieces) {
      String[] reducedPieces = new String[maxPieces];
      System.arraycopy(pieces, 0, reducedPieces, 0, maxPieces);
      pieces = reducedPieces;
    }
    return join(glue, pieces);
  }

  public static String join(String glue, String[] pieces) {
    StringBuilder value = new StringBuilder();
    for (String piece : pieces) {
      value.append(piece.replaceAll(glue, ""));
      value.append(glue);
    }

    if (pieces.length > 0) {
      return value.substring(0, value.length() - 1);
    } else {
      return "";
    }
  }
}
