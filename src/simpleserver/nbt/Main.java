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
      System.out.println("\tremove key");
      System.out.println("\tadd key [name type] [value]");
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
      read(args, file);
    } else if (command.equals("set")) {
      set(args, file);
    } else if (command.equals("remove")) {
      remove(args, file);
    } else if (command.equals("add")) {
      add(args, file);
    }
  }

  private static void read(String[] args, NBTFile file) {
    NBTag tag = file.root();
    if (args.length >= 3) {
      tag = tryGetTag(args[2], tag);
    }
    if (tag != null) {
      System.out.println(tag);
    }
  }

  private static void set(String[] args, NBTFile file) {
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
      trySave(file, args[0]);
    } else {
      System.out.println("Error: Wrong number of arguments");
    }
  }

  @SuppressWarnings("unchecked")
  private static void remove(String[] args, NBTFile file) {
    if (args.length >= 3) {
      String path = args[2];
      int split = Math.max(path.lastIndexOf('.'), path.lastIndexOf('/'));
      NBTag tag;
      String name;
      if (split > 0) {
        tag = tryGetTag(path.substring(0, split), file.root());
        if (tag == null) {
          return;
        }
        name = path.substring(split + 1);
      } else {
        tag = file.root();
        name = path;
      }
      switch (tag.type()) {
        case COMPOUND:
          if (((NBTCompound) tag).containsKey(name)) {
            ((NBTCompound) tag).remove(name);
          } else {
            System.out.println(new NoSuchKeyException(path));
          }
          break;
        case LIST:
          int index;
          try {
            index = Integer.valueOf(name);
          } catch (NumberFormatException e) {
            System.out.println(new NoSuchKeyException(path));
            return;
          }
          if (((NBTList<NBTag>) tag).size() > index) {
            ((NBTList<NBTag>) tag).remove(((NBTList<NBTag>) tag).get(index));
          } else {
            System.out.println(new NoSuchKeyException(path));
          }
          break;
        default:
          System.out.println(new NoContainerException(tag));
          return;
      }
      trySave(file, args[0]);
    } else {
      System.out.println("Error: Wrong number of arguments");
    }
  }

  @SuppressWarnings("unchecked")
  private static void add(String[] args, NBTFile file) {
    NBTag newTag = null;
    if (args.length >= 3) {
      NBTag tag = tryGetTag(args[2], file.root());
      if (tag == null) {
        return;
      }
      int index = 3;
      if (tag.type() == NBT.COMPOUND) {
        if (args.length >= 5) {
          String typeString = args[4].toLowerCase();
          if (typeString.equals("list")) {
            System.out.println("Error: Use list.<type> for list tags (e.g. list.string)");
            return;
          } else if (typeString.startsWith("list.")) {
            NBT type = NBT.get(args[4].substring(5));
            if (type == null || type == NBT.END || type == NBT.LIST) {
              System.out.println("Error: Unknown type");
              return;
            }
            newTag = new NBTList<NBTag>(type);
          } else {
            NBT type = NBT.get(args[4]);
            if (type == null || type == NBT.END) {
              System.out.println("Error: Unknown type");
              return;
            }
            try {
              newTag = type.getInstance();
            } catch (Exception e) {
              System.out.println("Error: " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")");
              return;
            }
          }
          newTag.rename(args[3]);
          ((NBTCompound) tag).put(newTag);
          index = 5;
        } else {
          System.out.println("Error: Wrong number of arguments");
        }
      } else if (tag.type() == NBT.LIST) {
        NBTList<NBTag> list = (NBTList<NBTag>) tag;
        try {
          newTag = list.listType().getInstance();
        } catch (Exception e) {
          System.out.println("Error: " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")");
          return;
        }
        list.add(newTag);
      } else {
        System.out.println("Error: Can't add tags to " + tag.getClass().getSimpleName());
        return;
      }
      if (args.length > index) {
        String value = args[index];
        try {
          newTag.set(value);
        } catch (NumberFormatException e) {
          System.out.println("Error: Wrong format (" + e.getMessage() + ")");
          return;
        }
      }
      trySave(file, args[0]);
    } else {
      System.out.println("Error: Wrong number of arguments");
    }
  }

  private static void trySave(NBTFile file, String filename) {
    try {
      file.save(filename);
    } catch (IOException e) {
      System.out.println("Error: File couldn't be saved (" + e.getMessage() + ")");
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
          NBTList<NBTag> list = (NBTList<NBTag>) current;
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
