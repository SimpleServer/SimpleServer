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

public class TimeServerCommand extends TimeCommand implements ServerCommand {
  @Override
  protected void captionedInfo(String caption, String message, Object... args) {
    System.out.println("[SimpleServer] " + caption + ": " + String.format(message, args));
  }

  @Override
  protected void error(String message) {
    System.out.println("[SimpleServer] Error: " + message);
  }

  @Override
  protected void info(String message) {
    System.out.println("[SimpleServer] " + message);
  }

  @Override
  protected void tCaptionedInfo(String caption, String message, Object... args) {
    captionedInfo(caption, message, args);
  }

  @Override
  protected void tError(String message) {
    error(message);
  }

  @Override
  protected void tInfo(String message) {
    info(message);
  }
}
