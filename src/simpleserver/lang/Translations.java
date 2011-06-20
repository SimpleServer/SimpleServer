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

public class Translations {
  private TranslationFile translation;

  private Translations() {
    translation = null;
  }

  private String get(String key) {
    if (translation == null) {
      return key;
    } else {
      return translation.get(key);
    }
  }

  public boolean setLanguage(String languageCode) {
    if (languageCode.equals("en")) {
      translation = null;
      return true;
    } else {
      translation = new TranslationFile(languageCode);

      if (translation.success()) {
        return true;
      } else {
        System.out.println("There's a problem with language '" + languageCode + "'! Using English (en) instead.");
        translation = null;
        return false;
      }
    }
  }

  public void load() {
    if (translation != null) {
      translation.load();
    }
  }

  private static class TranslationsHolder {
    public static final Translations INSTANCE = new Translations();
  }

  public static Translations getInstance() {
    return TranslationsHolder.INSTANCE;
  }

  public static String t(String key) {
    return TranslationsHolder.INSTANCE.get(key);
  }
}
