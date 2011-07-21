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
package simpleserver.command;

import static simpleserver.lang.Translations.t;

import java.util.Collection;

import simpleserver.Color;
import simpleserver.CommandParser;

public abstract class AbstractCommand implements Command {
  protected final String name;
  protected final String commandCode;
  protected String helpText;
  protected CommandParser parser;

  protected AbstractCommand(String name, String commandCode) {
    if (name != null) {
      helpText = name + Color.WHITE + " : " + t(commandCode);

      int splitIndex = name.indexOf(" ");
      if (splitIndex != -1) {
        name = name.substring(0, splitIndex);
      }
    } else {
      helpText = t(commandCode);
    }

    this.name = name;
    this.commandCode = commandCode;
  }

  public String getName() {
    return name;
  }

  public String getHelpText(String prefix) {
    if (name != null) {
      return Color.DARK_GREEN + prefix + helpText;
    } else {
      return helpText;
    }
  }

  public boolean shouldPassThroughToConsole() {
    return false;
  }

  protected String[] extractArguments(String message, int startOffset) {
    startOffset++;
    String[] parts = message.split("\\s+");

    String[] copy = new String[parts.length - startOffset];
    System.arraycopy(parts, startOffset, copy, 0, parts.length - startOffset);

    return copy;
  }

  protected String[] extractArguments(String message) {
    return extractArguments(message, 0);
  }

  protected String extractArgument(String message, int startOffset) {
    int argumentIndex = 0;
    for (int c = 0; c <= startOffset; ++c) {
      argumentIndex = message.indexOf(" ", argumentIndex) + 1;

      if (argumentIndex == 0) {
        return null;
      }
    }

    return message.substring(argumentIndex);
  }

  protected String extractArgument(String message) {
    return extractArgument(message, 0);
  }

  protected String commandPrefix() {
    if (parser != null) {
      return parser.commandPrefix();
    } else {
      return "!";
    }
  }

  protected static String join(Collection<String> list) {
    return join(list, "");
  }

  protected static String join(Collection<String> list, String prefix) {
    StringBuilder string = new StringBuilder();
    for (String part : list) {
      string.append(prefix);
      string.append(part);
      string.append(", ");
    }
    if (string.length() > 0) {
      string.delete(string.length() - 2, string.length() - 1);
    }
    return string.toString();
  }

  public void reloadText() {
    if (name != null) {
      helpText = name + Color.WHITE + " : " + t(commandCode);
    } else {
      helpText = t(commandCode);
    }
  }

  public void setParser(CommandParser parser) {
    this.parser = parser;
  }
}
