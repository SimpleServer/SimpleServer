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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;

import simpleserver.Resource;
import simpleserver.util.UnicodeReader;

public class ReadFiles implements Resource {
  private final static String FOLDER = "simpleserver";
  private final static String SUBFOLDER = "docs";
  private final static String DOCS_PATH = FOLDER + File.separator + SUBFOLDER;

  private Hashtable<String, String> docs = new Hashtable<String, String>();

  public ReadFiles() {
  }

  public String getText(String documentName) {
    return docs.get(documentName);
  }

  public Set<String> getList() {
    return docs.keySet();
  }

  public void save() {
  }

  private void loadFile(File inFile) {
    boolean success = false;
    String fullName = inFile.getName();
    String docName = fullName.substring(0, fullName.length() - 4);

    try {
      BufferedReader reader = new BufferedReader(new UnicodeReader(new FileInputStream(inFile), "UTF-8"));
      try {
        StringBuilder docText = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          if (!line.startsWith("#")) {
            docText.append(line + "\n");
          }
        }
        docs.put(docName, docText.toString());
        success = true;
      } finally {
        reader.close();
      }
    } catch (FileNotFoundException e) {
      System.out.println("Could not find doc " + docName + "!");
      success = true;
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (!success) {
      System.out.println("Failed to load doc " + docName + "!");
    }
  }

  public void load() {
    docs.clear();

    File docsFolder = new File(DOCS_PATH);
    if (!docsFolder.exists()) {
      docsFolder.mkdirs();
      return;
    }

    for (File f : docsFolder.listFiles(new docsFilter())) {
      loadFile(f);
    }

  }

  class docsFilter implements FilenameFilter {
    public boolean accept(File paramFile, String paramString) {
      return paramString.toLowerCase().endsWith(".txt");
    }
  }

}
