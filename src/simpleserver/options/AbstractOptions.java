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
package simpleserver.options;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import simpleserver.Resource;

public abstract class AbstractOptions implements Resource {
  private static final String resourceLocation = "defaults";

  protected final String filename;

  protected Properties defaultOptions;
  protected Properties options;

  protected AbstractOptions(String filename) {
    this.filename = filename;

    loadDefaults();
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
    }
    catch (NumberFormatException e) {
      String defaultValue = defaultOptions.getProperty(option);
      if (!defaultValue.equals(value)) {
        options.setProperty(option, defaultValue);
        return getInt(option);
      }
      else {
        e.printStackTrace();
        System.out.println("Error: Asked for int value of " + option);
        return Integer.MIN_VALUE;
      }
    }
  }

  public boolean getBoolean(String option) {
    return Boolean.parseBoolean(options.getProperty(option));
  }

  public void load() {
    options = (Properties) defaultOptions.clone();
    File file = new File(filename);

    try {
      InputStream stream = new FileInputStream(file);
      try {
        options.load(stream);
      }
      finally {
        stream.close();
      }
    }
    catch (FileNotFoundException e) {
      missingFile();
    }
    catch (IOException e) {
      e.printStackTrace();
      System.out.println("Could not read " + filename);
    }
  }

  public void save() {
    File file = new File(filename);

    try {
      OutputStream stream = new FileOutputStream(file);
      try {
        options.store(stream, getComment());
      }
      finally {
        stream.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      System.out.println("Could not write " + filename);
    }
  }

  protected void missingFile() {
    save();
  }

  protected String getComment() {
    return null;
  }

  protected void loadDefaults() {
    defaultOptions = new Properties();
    InputStream stream = getClass().getResourceAsStream(resourceLocation + "/"
                                                            + filename);
    try {
      try {
        defaultOptions.load(stream);
      }
      finally {
        stream.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      System.out.println("Could not read default " + filename);
    }

    options = (Properties) defaultOptions.clone();
  }
}
