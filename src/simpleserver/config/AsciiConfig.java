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
package simpleserver.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import simpleserver.Config;

public abstract class AsciiConfig extends Config {
  private String header = "";

  public AsciiConfig(String filename) {
    super(filename);
  }

  protected abstract String saveString();

  @Override
  public void save() {
    boolean success = false;
    File outFile = getFile();
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));

      try {
        writer.write(header);
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
      System.out.println("Unable to save " + outFile.getName() + "!");
    }
  }

  protected abstract void beforeLoad();

  protected abstract void loadLine(String line);

  @Override
  public void load() {
    beforeLoad();

    header = "";
    boolean readingHeader = true;
    boolean success = false;

    File inFile = getFile();
    try {
      BufferedReader reader = new BufferedReader(new FileReader(inFile));
      try {
        String line;
        while ((line = reader.readLine()) != null) {
          if (readingHeader && line.startsWith("#")) {
            header += line + "\n";
          }
          else {
            readingHeader = false;
            loadLine(line);
          }
        }
        success = true;
      }
      finally {
        reader.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    if (!success) {
      System.out.println("Unable to load " + inFile.getName() + "!");
    }
  }
}
