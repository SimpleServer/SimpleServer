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

import java.io.IOException;
import java.io.OutputStream;

import simpleserver.thread.SystemInputQueue;

public class InputWrapper implements Wrapper {
  private final SystemInputQueue in;
  private final OutputStream out;
  private final MessageHandler messageHandler;
  private final Wrapper wrapper;

  private volatile boolean run = true;

  public InputWrapper(SystemInputQueue in, OutputStream out,
                      MessageHandler messageHandler) {
    this.in = in;
    this.out = out;
    this.messageHandler = messageHandler;

    wrapper = new Wrapper();
    wrapper.start();
    wrapper.setName("MinecraftInputWrapper");
  }

  public void injectCommand(String command, String arguments) {
    in.appendLine(command + " " + arguments);
  }

  public void stop() {
    run = false;
    wrapper.interrupt();
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
            line = in.nextLine();
          }
          catch (InterruptedException e) {
            continue;
          }

          if (messageHandler.parseCommand(line)) {
            continue;
          }

          line += "\n";
          try {
            out.write(line.getBytes());
            out.flush();
          }
          catch (IOException e) {
            messageHandler.handleError(e);
            break;
          }
        }
      }
      finally {
        try {
          out.close();
        }
        catch (IOException e) {
        }
      }
    }
  }
}
