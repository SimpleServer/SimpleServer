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
package simpleserver.config;

import static simpleserver.util.Util.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class PropertiesConfig extends AbstractConfig {
  private boolean layered;
  private SortedProperties defaultProperties;

  protected SortedProperties properties;

  protected PropertiesConfig(String filename) {
    this(filename, false);
  }

  protected PropertiesConfig(String filename, boolean layered) {
    super(filename);
    this.layered = layered;

    properties = new SortedProperties();
    loadDefaults();
  }

  @Override
  public void load() {
    if (layered) {
      properties = (SortedProperties) defaultProperties.clone();
    } else {
      properties = new SortedProperties();
    }

    try {
      InputStream stream = new FileInputStream(getFile());
      try {
        properties.load(stream);
      } finally {
        stream.close();
      }
    } catch (FileNotFoundException e) {
      missingFile();
      properties = (SortedProperties) defaultProperties.clone();
      save();
    } catch (IOException e) {
      print(e);
      print("Failed to load " + getFilename());
    }
  }

  protected void missingFile() {
    System.out.println(getFilename() + " is missing.  Loading defaults.");
  }

  @Override
  public void save() {
    try {
      OutputStream stream = new FileOutputStream(getFile());
      try {
        properties.store(stream, getHeader());
      } finally {
        stream.close();
      }
    } catch (IOException e) {
      print(e);
      print("Failed to save " + getFilename());
    }
  }

  private void loadDefaults() {
    InputStream stream = getResourceStream();
    defaultProperties = new SortedProperties();
    try {
      try {
        defaultProperties.load(stream);
      } finally {
        stream.close();
      }
    } catch (IOException e) {
      print(e);
      print("Failed to load defaults for "
          + getFilename());
    }
  }

  @Override
  protected String getHeader() {
    Pattern linePattern = Pattern.compile("^", Pattern.MULTILINE);
    Matcher lineMatcher = linePattern.matcher(super.getHeader());

    return lineMatcher.replaceAll("#").substring(1);
  }
}
