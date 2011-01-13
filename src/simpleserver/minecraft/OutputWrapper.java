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
package simpleserver.minecraft;

import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class OutputWrapper implements Wrapper {
  private final Scanner scanner;
  private final MessageHandler messageHandler;
  private final Wrapper wrapper;

  private volatile boolean run = true;

  public OutputWrapper(InputStream in, MessageHandler messageHandler,
                       String type) {
    scanner = new Scanner(in);
    this.messageHandler = messageHandler;

    wrapper = new Wrapper();
    wrapper.start();
    wrapper.setName("MinecraftOutputWrapper-" + type);
  }

  public void stop() {
    run = false;
    // This caused the SimpleServer listener to throw an exception and fail
    // sometimes.
    try {
      scanner.close();
    }
    catch (Exception e) {
    }
  }

  public void join() throws InterruptedException {
    wrapper.join();
  }

  private final class Wrapper extends Thread {
    @Override
    public void run() {
      try {
        while (run) {
          String line;
          try {
            line = scanner.nextLine();
          }
          catch (NoSuchElementException e) {
            messageHandler.handleError(e);
            break;
          }
          catch (IllegalStateException e) {
            break;
          }

          messageHandler.handleOutput(line);
        }
      }
      finally {
        try {
          scanner.close();
        }
        catch (IllegalStateException e) {
        }
      }
    }
  }
}
