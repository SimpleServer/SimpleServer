/*******************************************************************************
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
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
package simpleserver.command;

public abstract class AbstractCommand implements Command {
  private String name;

  protected AbstractCommand(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String[] getAliases() {
    return new String[] {};
  }

  /**
   * @return true if command should be passed-through to SMP API also
   */
  public boolean passThrough() {
    return false;
  }

  public boolean isHidden() {
    return false;
  }

  protected String[] extractArguments(String message) {
    String[] parts = message.split("\\s+");
    // return Arrays.copyOfRange(parts, 1, parts.length);
    // JDK 1.5 Compatibility
    String[] cpy = new String[parts.length - 1];
    System.arraycopy(parts, 1, cpy, 0, parts.length - 1);
    return cpy;
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
