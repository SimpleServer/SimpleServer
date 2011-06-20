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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

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
    InputStream stream = null;

    try {
      stream = getClass().getResourceAsStream(resourceLocation + "/" + filename);
      byte[] bytes = new byte[stream.available()];
      stream.read(bytes);
      options = new JSONObject(new String(bytes, "UTF-8"));
    } catch (NullPointerException e) {
      loadExternal();
    } catch (JSONException e) {
      System.out.println("[SimpleServer] " + e);
      System.out.println("[SimpleServer] Could not read " + filename);
    } catch (IOException e) {
      System.out.println("[SimpleServer] " + e);
      System.out.println("[SimpleServer] Could not read " + filename);
    } finally {
      if (stream != null) {
        try {
          stream.close();
        } catch (IOException e) {
        }
      }
    }
  }

  protected void loadExternal() {
    File file = new File(resourceLocation + File.separator + filename);
    InputStream stream = null;

    try {
      stream = new FileInputStream(file);
      byte[] bytes = new byte[stream.available()];
      stream.read(bytes);
      options = new JSONObject(new String(bytes, "UTF-8"));
    } catch (NullPointerException e) {
      System.out.println("[SimpleServer] " + e);
      System.out.println("[SimpleServer] Could not find " + filename);
    } catch (JSONException e) {
      System.out.println("[SimpleServer] " + e);
      System.out.println("[SimpleServer] Could not read " + filename);
    } catch (IOException e) {
      System.out.println("[SimpleServer] " + e);
      System.out.println("[SimpleServer] Could not read " + filename);
    } finally {
      if (stream != null) {
        try {
          stream.close();
        } catch (IOException e) {
        }
      }
    }
  }
}
