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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

public abstract class AsciiConfig extends AbstractConfig {
  protected AsciiConfig(String filename) {
    super(filename);
  }

  @Override
  public void save() {
    boolean success = false;
    File outFile = getFile();
    try {
      Writer writer = new BufferedWriter(new FileWriter(outFile));

      try {
        writer.write(getHeader());
        writer.write(saveString());
        writer.flush();

        success = true;
      }
      finally {
        writer.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    if (!success) {
      System.out.println("Failed to save " + getFilename() + "!");
    }
  }

  @Override
  public void load() {
    boolean success = false;

    File inFile = getFile();
    try {
      BufferedReader reader = new BufferedReader(new FileReader(inFile));
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.startsWith("#")) {
            loadLine(line);
          }
        }
        success = true;
      }
      finally {
        reader.close();
      }
    }
    catch (FileNotFoundException e) {
      System.out.println(getFilename() + " is missing.  Loading defaults.");
      loadDefaults();

      save();
      success = true;
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    if (!success) {
      System.out.println("Failed to load " + getFilename() + "!");
    }
  }

  protected abstract String saveString();

  protected abstract void loadLine(String line);

  private void loadDefaults() {
    InputStream stream = getResourceStream();
    String defaults;
    try {
      defaults = readFully(stream);
    }
    finally {
      try {
        stream.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (defaults == null) {
      System.out.println("Failed to load defaults for " + getFilename());
      return;
    }

    defaults = defaults.replaceAll("\\s+$", "");
    String[] lines = defaults.split("\n");
    for (String line : lines) {
      if (!line.startsWith("#")) {
        loadLine(line);
      }
    }
  }
}
