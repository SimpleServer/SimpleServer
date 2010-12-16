/*******************************************************************************
 * Open Source Initiative OSI - The MIT License:Licensing
 * The MIT License
 * Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
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
package simpleserver.threads;

import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SystemInputQueue {
  private final BlockingQueue<String> queue;
  private final Scanner scanner;
  
  public SystemInputQueue() {
    queue = new LinkedBlockingQueue<String>();
    scanner = new Scanner(System.in);
    
    new Reader().start();
  }
  
  public String nextLine() throws InterruptedException {
    return queue.take();
  }
  
  public void appendLine(String line) {
    queue.add(line);
  }
  
  public void stop() {
    scanner.close();
  }
  
  private final class Reader extends Thread {
    public void run() {
      while (true) {
        String line;
        try {
          line = scanner.nextLine();
        }
        catch (IllegalStateException e) {
          break;
        }
        
        try {
          queue.put(line);
        }
        catch (InterruptedException e) {
          continue;
        }
      }
    }
  }
}
