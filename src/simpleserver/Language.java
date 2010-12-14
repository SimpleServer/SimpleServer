/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package simpleserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class Language {
  private static final String resourceLocation = "defaults";
  private static final String filename = "language.properties";
  private Properties translations;

  public Language() {
    loadDefaults();
  }

  public String get(String key) {
    return translations.getProperty(key);
  }

  public void load() {
    loadDefaults();
    File file = new File(filename);

    try {
      InputStream stream = new FileInputStream(file);
      try {
        translations.load(stream);
      }
      finally {
        stream.close();
      }
    }
    catch (FileNotFoundException e) {
      save();
    }
    catch (IOException e) {
      e.printStackTrace();
      System.out.println("Could not read language properties!");
    }
  }

  private void save() {
    File file = new File(filename);

    try {
      OutputStream stream = new FileOutputStream(file);
      try {
        translations.store(stream, null);
      }
      finally {
        stream.close();
      }
    }
    catch (FileNotFoundException e) {
      save();
    }
    catch (IOException e) {
      e.printStackTrace();
      System.out.println("Could not write language properties!");
    }
  }

  private void loadDefaults() {
    translations = new Properties();
    InputStream stream = getClass().getResourceAsStream(resourceLocation + "/"
                                                            + filename);
    try {
      try {
        translations.load(stream);
      }
      finally {
        stream.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
      System.out.println("Could not read default language properties!");
    }
  }
}
