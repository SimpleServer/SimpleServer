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

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import simpleserver.Resource;

public abstract class AbstractConfig implements Resource {
  private static final String resourceLocation = "defaults";
  private static final String folder = "simpleserver";

  private String filename;
  private String header;

  protected AbstractConfig(String filename) {
    this.filename = filename;

    loadHeader();
  }

  public abstract void save();

  public abstract void load();

  protected String getFilename() {
    return filename;
  }

  protected String getHeader() {
    return header;
  }

  protected File getFile() {
    File file = new File(folder + File.separator + filename);
    if (file.exists()) {
      return file;
    }

    new File(folder).mkdir();

    File check = new File(filename);
    if (check.exists()) {
      check.renameTo(file);
    }

    return file;
  }

  protected String readFully(InputStream input) {
    byte[] buffer;
    try {
      DataInputStream dataInput = new DataInputStream(input);
      try {
        buffer = new byte[dataInput.available()];
        dataInput.readFully(buffer);
        return new String(buffer, 0, buffer.length, "UTF-8");
      }
      finally {
        dataInput.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  protected InputStream getResourceStream() {
    return getClass().getResourceAsStream(resourceLocation + "/" + filename);
  }

  private InputStream getHeaderResourceStream() {
    return getClass().getResourceAsStream(resourceLocation + "/" + filename
                                              + "-header");
  }

  private void loadHeader() {
    InputStream headerStream = getHeaderResourceStream();
    try {
      header = readFully(headerStream);
    }
    finally {
      try {
        headerStream.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (header == null) {
      System.out.println("Failed to load default header for " + getFilename());
    }
  }
}
