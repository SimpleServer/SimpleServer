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

import java.util.Scanner;

public class Options extends AbstractOptions {

  public Options() {
    super("simpleserver.properties");
  }

  public void set(String option, String value) {
    options.setProperty(option, value);
  }

  @Override
  public void load() {
    super.load();

    String[] fallbackIfEmpty = new String[] { "msgFormat", "msgTitleFormat", "msgForwardFormat", "logMessageFormat" };
    for (String entry : fallbackIfEmpty) {
      if (get(entry).equals("")) {
        set(entry, defaultOptions.getProperty(entry));
      }
    }

    if (getInt("internalPort") == getInt("port")) {
      System.out.println("OH NO! Your 'internalPort' and 'port' properties are the same! Edit simpleserver.properties and change them to different values. 'port' is recommended to be 25565, the default port of minecraft, and will be the port you actually connect to.");
      System.out.println("Press enter to continue...");
      Scanner in = new Scanner(System.in);
      in.nextLine();
      System.exit(0);
    }
  }

  @Override
  protected void missingFile() {
    super.missingFile();

    System.out.println("Properties file not found. Created simpleserver.properties!");
  }

}
