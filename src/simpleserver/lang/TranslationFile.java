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
package simpleserver.lang;

import java.io.IOException;
import java.io.InputStream;

import simpleserver.Resource;
import simpleserver.config.SortedProperties;

public class TranslationFile implements Resource {
  private static final String resourceLocation = "defaults";
  protected final String filename;
  protected SortedProperties options;

  public TranslationFile(String translationName) {
    filename = translationName + ".properties";
    load();
  }

  public boolean contains(String option) {
    String value = options.getProperty(option);
    return value != null && value.trim().length() > 0;
  }

  public String get(String key) {
    return options.getProperty(key);
  }

  public int getInt(String option) {
    String value = options.getProperty(option);
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      System.out.println("Error: Asked for int value of " + option);
      return Integer.MIN_VALUE;
    }
  }

  public boolean getBoolean(String option) {
    return Boolean.parseBoolean(options.getProperty(option));
  }

  public void save() {
    return;
  }

  public void load() {
    options = new SortedProperties();
    InputStream stream = getClass().getResourceAsStream(resourceLocation + "/"
                                                        + filename);
    try {
      try {
        options.load(stream);
      } finally {
        stream.close();
      }
    } catch (IOException e) {
      System.out.println("[SimpleServer] " + e);
      System.out.println("[SimpleServer] Could not read " + filename);
    }
  }
}
