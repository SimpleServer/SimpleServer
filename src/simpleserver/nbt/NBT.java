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
package simpleserver.nbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NBT {

  private NBTRoot root;

  NBT() {

  }

  NBT(String filename) throws FileNotFoundException {
    this(new FileInputStream(filename));
  }

  NBT(InputStream input) {
    load(input);
  }

  NBTCompound root() {
    return root;
  }

  private void load(InputStream input) {
    try {
      root = new NBTRoot(new DataInputStream(new GZIPInputStream(input)));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void save(String filename) {
    try {
      DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(filename)));
      root.save(out);
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public String toString() {
    return root.toString();
  }

  public static void main(String[] args) {
    if (args.length >= 1) {
      try {
        System.out.println(new NBT(args[0]));
      } catch (FileNotFoundException e) {
        System.out.println("Error: No such file or dictionary");
      }
    } else {
      System.out.println("Usage: java -jar NBT.jar file");
    }
  }

}
