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

import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage: java -jar NBT.jar file [command] [arguments]");
      System.out.println("\nCommands:");
      System.out.println("\tread [key]");
      System.out.println("\tset key value");
      // System.out.println("\tadd key type [value]");
      return;
    }

    NBTFile file = null;

    try {
      try {
        file = new GZipNBTFile(args[0]);
      } catch (IOException g) {
        file = new NBTFile(args[0]);
      }
    } catch (FileNotFoundException e) {
      System.out.println("Error: No such file or dictionary");
    } catch (Exception e) {
      System.out.println("Error: " + e + " (" + e.getMessage() + ")");
    }

    if (file == null) {
      return;
    }

    if (args.length <= 1) {
      System.out.println(file);
      return;
    }

    String command = args[1].toLowerCase();
    if (command.equals("read")) {
      NBTag tag = file.root();
      if (args.length >= 3) {
        tag = tryGetTag(args[2], tag);
      }
      if (tag != null) {
        System.out.println(tag);
      }
    } else if (command.equals("set")) {
      if (args.length >= 4) {
        NBTag tag = tryGetTag(args[2], file.root());
        if (tag == null) {
          return;
        }
        String value = args[3];
        try {
          tag.set(value);
        } catch (NumberFormatException e) {
          System.out.println("Error: Wrong format (" + e.getMessage() + ")");
          return;
        }
        try {
          file.save(args[0]);
        } catch (IOException e) {
          System.out.println("Error: File couldn't be saved (" + e.getMessage() + ")");
        }
      } else {
        System.out.println("Error: Wrong number of arguments");
      }
    }
  }

  private static NBTag tryGetTag(String path, NBTag root) {
    try {
      return getTag(path, root);
    } catch (NoSuchKeyException e) {
      System.out.println("Error: " + e.getMessage());
      return null;
    } catch (NoContainerException e) {
      System.out.println("Error: " + e.getMessage());
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  public static NBTag getTag(String path, NBTag root) throws NoSuchKeyException, NoContainerException {
    String[] keys = path.split("/|\\.");
    NBTag current = root;
    for (String name : keys) {
      switch (current.type()) {
        case COMPOUND:
          NBTCompound comp = (NBTCompound) current;
          if (!comp.containsKey(name)) {
            throw new NoSuchKeyException(name);
          }
          current = comp.get(name);
          break;
        case LIST:
          NBTList list = (NBTList) current;
          int key = Integer.valueOf(name);
          if (key >= list.size()) {
            throw new NoSuchKeyException(Integer.toString(key));
          }
          current = list.get(key);
          break;
        default:
          throw new NoContainerException(current);
      }
    }
    return current;
  }

  public static class NoSuchKeyException extends Exception {
    private static final long serialVersionUID = 1L;
    public String key;

    public NoSuchKeyException(String key) {
      super("No such key: \"" + key + "\"");
      this.key = key;
    }
  }

  public static class NoContainerException extends Exception {
    private static final long serialVersionUID = 1L;
    public NBTag tag;

    public NoContainerException(NBTag tag) {
      super("Tag \"" + tag.name + "\" is not a container (" + tag.getClass().getSimpleName() + ")");
      this.tag = tag;
    }
  }
}
