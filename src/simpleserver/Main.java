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
package simpleserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Main {
  private static final String license = "SimpleServer -- Copyright (C) 2011 SimpleServer authors (see CONTRIBUTORS)";
  private static final String warranty = "This program is licensed under The MIT License.\nSee file LICENSE for details.";
  private static final String baseVersion = "8.1.1";
  private static final String version;

  static {
    String extendedVersion = baseVersion;

    String commitversion = getVersionString("VERSION");
    if (!commitversion.equals("")) {
      extendedVersion += "-" + commitversion;
    }

    version = extendedVersion;
  }

  private static String getVersionString(String file) {
    InputStream input = Main.class.getResourceAsStream(file);
    String retversion = "";

    if (input != null) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      try {
        try {
          retversion = reader.readLine();
        } finally {
          reader.close();
        }
      } catch (IOException e) {
        System.out.println("[SimpleServer] " + e);
        System.out.println("[SimpleServer] Warning, jar may be corrupted!");
      }
    }

    return retversion;
  }

  public static void main(String[] args) {
    if (args.length > 0) {
      for (String x : args) {
        if (x.equals("--version") || x.equals("-v")) {
          System.out.println(version);
          return;
        }
      }
    }

    System.out.println(license);
    System.out.println(warranty);
    System.out.println(">> Starting SimpleServer " + version);

    new Server();
  }
}
