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
package simpleserver.telnet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

import simpleserver.Server;

public class TelnetHandler implements Runnable {
  private BufferedReader in;
  private BufferedWriter out;

  private Socket s;
  private TelnetTCP parent;
  private Server server;
  private boolean authorized = false;

  static final int IDLE_TIME = 60*1000;

  public TelnetHandler(Socket s, TelnetTCP r, Server server) throws IOException {
    in = new BufferedReader(new InputStreamReader(s.getInputStream()));
    out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
    this.s = s;
    parent = r;
    this.server = server;
  }

  public void run() {
    String line;

    try {
      s.setSoTimeout(IDLE_TIME);

      output("~> Welcome to SimpleServer remote administration! <~\n\n");
      output("Password: ");

      String tpass = server.options.get("telnetPassword");
      String pwd = in.readLine();
      if (tpass != null && !tpass.equals("") && tpass.equals(pwd)) {
        authorized = true;
      }

      while (authorized && !parent.isClosed() && !Thread.interrupted()) {
        output("> ");
        line = in.readLine();
        parent.lastRead = System.currentTimeMillis();
        if (line == null || line.equals("exit")) {
            output("Bye!\n");
            parent.close();
            break;
        }

        parseLine(line);
      }
    }
    catch (InterruptedException e1) {
    }
    catch (SocketTimeoutException e) {
      
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    try {
      in.close();
      out.close();
      s.close();
    }
    catch (IOException e) {
    }
    in = null;
    out = null;
    parent = null;
  }

  private void output(String text) {
    try {
      out.write(text,0,text.length());
      out.flush();
    } catch(IOException e) {
    }
  }

  private void parseLine(String command) throws IOException, InterruptedException {
    String[] tokens = command.split("\\s");

    if (tokens.length == 0)
        return;



    if (tokens[0].equals("help")) {
      output("Commands:\nhelp\nexit\nshow-console\nany valid server commands (no checking!)\n");
      return;
    }
    
    if (!authorized) {
      output("Error: You are not authenticated!\n");
      return;
    }

    if (tokens[0].equals("show-console")) {
      String console = "";
      String[] consolearray = server.getOutputLog();
      for (String i : consolearray) {
        console += i + "\n";
      }
      output(console);
      return;
    }

    String rest = "";
    if (tokens.length > 1) {
      int idx = command.indexOf(tokens[0]) + tokens[0].length() + 1;
      rest = command.substring(idx);
    }
    server.runCommand(tokens[0], rest);
  }

}
