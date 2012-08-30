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

import static simpleserver.util.Util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import simpleserver.util.UnicodeReader;

public class TranslationFile {
  private static final String resourceLocation = "translations";
  protected final String filename;
  protected JSONObject options;
  private final boolean success;

  public TranslationFile(String translationName) {
    filename = translationName + ".json";
    load();

    success = (options != null);
  }

  public boolean success() {
    return success;
  }

  public String get(String key) {
    try {
      return options.getString(key);
    } catch (JSONException e) {
      return key;
    }
  }

  public void load() {
    File file = new File(resourceLocation + File.separator + filename);
    BufferedReader reader = null;

    if (!file.exists()) {
      loadInternal();
      return;
    }

    try {
      reader = new BufferedReader(new UnicodeReader(new FileInputStream(file), "UTF-8"));

      String contents = "";
      String currentLine;
      while ((currentLine = reader.readLine()) != null) {
        contents = contents + currentLine;
      }

      options = new JSONObject(contents);
    } catch (Exception e) {
      print(e);
      print("Could not read " + filename);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
    }
  }

  protected void loadInternal() {
    BufferedReader reader = null;

    try {
      reader = new BufferedReader(new UnicodeReader(getClass().getResourceAsStream(resourceLocation + "/" + filename), "UTF-8"));

      String contents = "";
      String currentLine;
      while ((currentLine = reader.readLine()) != null) {
        contents = contents + currentLine;
      }

      options = new JSONObject(contents);
    } catch (Exception e) {
      print(e);
      print("Could not read " + filename);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
    }
  }
}
