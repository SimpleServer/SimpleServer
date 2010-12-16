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
package simpleserver.minecraft;

import simpleserver.Server;

public class MessageHandler {
  private Server server;
  public MessageHandler(Server server) {
    this.server = server;
  }
  
  public void handleError(Exception exception) {
    System.out.println("[SimpleServer] Minecraft process stopped unexpectedly! Automatically restarting...");
    server.forceRestart();
  }
  
  public void handleQuit() {
  
  }
  
  public void handleOutput(String line) {
    if (!server.options.getBoolean("debug") && line.contains("\tat")) {
      return;
    }
    
    Integer[] ports = server.getRobotPorts();
    if (ports != null) {
      for (int i = 0; i < ports.length; i++) {
        if (ports[i] != null) {
          if (line.contains(ports[i].toString())) {
            server.removeRobotPort(ports[i]);
            return;
          }
        }
      }
    }
    if (line.contains("[INFO] CONSOLE: Save complete.")) {
      server.isSaving(false);
      server.runCommand("say", server.l.get("SAVE_COMPLETE"));
    }
    /*
    if (line.contains("[INFO] Done!")) {
      server.waitingForStart(false);
    }
    */
    if (line.contains("[SEVERE] Unexpected exception")) {
      server.forceRestart();
    }
    server.addOutputLine(line);
    System.out.println(line);
  }
}
