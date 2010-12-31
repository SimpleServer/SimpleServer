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

public abstract class AbstractCommand implements Command {
  private final String name;
  private final String helpText;

  protected AbstractCommand(String name, String helpText) {
    if (name != null) {
      this.helpText = name + "\u00a7f : " + helpText;

      int splitIndex = name.indexOf(" ");
      if (splitIndex != -1) {
        name = name.substring(0, splitIndex);
      }
    }
    else {
      this.helpText = helpText;
    }

    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getHelpText(String prefix) {
    if (name != null) {
      return "\u00a72" + prefix + helpText;
    }
    else {
      return helpText;
    }
  }

  public boolean shouldPassThroughToSMPAPI() {
    return false;
  }

  public boolean shouldPassThroughToConsole() {
    return false;
  }

  public boolean isHidden() {
    return false;
  }

  protected String[] extractArguments(String message) {
    String[] parts = message.split("\\s+");

    // JDK 1.5 Compatibility
    String[] copy = new String[parts.length - 1];
    System.arraycopy(parts, 1, copy, 0, parts.length - 1);

    return copy;
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
}
