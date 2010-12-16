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
package simpleserver.rcon;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

import simpleserver.Rcon;
import simpleserver.Server;
import simpleserver.StreamTunnel;

public class RconUDP implements Rcon {
  private final int SERVERDATA_AUTH = 3;
  private final int SERVERDATA_EXECCOMMAND = 2;
  private final int SERVERDATA_AUTH_RESPONSE = 2;
  private final int SERVERDATA_RESPONSE_VALUE = 0;
  private final int INT = 4;
  private DatagramPacket current;
  private long lastRead;
  private String name = null;
  private Server parent;
  private boolean closed = false;
  private boolean isKicked = false;
  private boolean auth = true;
  private String kickMsg = null;

  private LinkedList<String> messages = new LinkedList<String>();

  public void addMessage(String msg) {
    synchronized (messages) {
      messages.addLast(msg);
    }
  }

  public String getMessage() {
    synchronized (messages) {
      return messages.removeFirst();
    }
  }

  public boolean hasMessages() {
    if (messages.isEmpty()) {
      return false;
    }
    return true;
  }

  /*
  public boolean parseCommand(String msg) throws InterruptedException, IOException {
    if (!closed)
      return parser.parse(msg);
    return true;
  }
  */
  public void kick(String msg) {
    kickMsg = msg;
    isKicked = true;
    close();
  }

  public String getKickMsg() {
    return kickMsg;
  }

  public boolean isKicked() {
    return isKicked;
  }

  public String getIPAddress() {
    return current.getAddress().getHostAddress();
  }

  public RconUDP(DatagramPacket p, Server parent) throws IOException {
    this.parent = parent;
    // parser = new RconParser(this);
    current = p;

    System.out.println("[SimpleServer] RCON Connection from " + getIPAddress()
        + "!");
    try {
      parent.requestTracker.addRequest(getIPAddress());
    }
    catch (InterruptedException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
    if (parent.isIPBanned(current.getAddress().getHostAddress())) {
      System.out.println("[SimpleServer] IP " + getIPAddress() + " is banned!");
      kick("Banned IP!");
    }
    name = getIPAddress();
    handle(p);
    /*
    try {

      t1=new Thread(rconHandler=new RconHandler(current,this));
      //t2=new Thread(clientToServer=new StreamTunnel(extsocket.getInputStream(),intsocket.getOutputStream(),false,this));
      t1.start();
      //t2.start();
    } catch (Exception e) {
      e.printStackTrace();
      try {
        close();
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }
    }
    try {
      socket.setTcpNoDelay(true);
      socket.setTrafficClass(0x10);

      socket.setPerformancePreferences(1, 2, 0);
    } catch (SocketException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    */

  }

  // Public Methods:
  public boolean testTimeout() {
    if (!closed) {
      if (System.currentTimeMillis() - lastRead > StreamTunnel.IDLE_TIME) {
        /*
        if (!isRobot)
          System.out.println("[SimpleServer] Disconnecting " + getIPAddress() + " due to inactivity.");
        try {
          close();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        */
        return true;
      }
    }
    return false;
  }

  public boolean isClosed() {
    return closed;
  }

  public void close() {
    // Don't spam the console! : )
    // And don't close if we're already closing!
    // if (!isKicked&&parent!=null)
    // parent.notifyClosedRcon(this);
    if (!closed) {
      closed = true;
      cleanup();
    }
  }

  public void cleanup() {

  }

  protected String readString2(ByteBuffer bb) {
    byte b;
    int offset = bb.position();
    int i = 0;
    while (true) {
      b = bb.get();
      System.out.print(b + " ");
      if (b == 0) {
        break;
      }
      i++;
    }
    if (i == 0) {
      return "";
    }
    byte[] string = new byte[i];
    // bb.position(0);
    System.out.println("");
    bb.get(string, offset, i);
    return new String(string);
  }

  private String getConsole() {
    String console = "";
    String[] consolearray = parent.getOutputLog();
    for (String i : consolearray) {
      console += i;
    }
    return console;
  }

  protected String parsePacket(String command) {
    String[] tokens = command.split(" ");
    if (tokens.length > 0) {
      if (tokens[0].equalsIgnoreCase("rcon")) {
        if (tokens.length > 1) {
          int idx = command.indexOf(tokens[1]) + tokens[1].length() + 1;
          parent.runCommand(tokens[1], command.substring(idx));
          return command;
        }
        else {
          return "Error: No Command";
        }
      }
      if (tokens[0].equalsIgnoreCase("help")) {
        if (tokens.length > 1) {
          if (tokens[1].equalsIgnoreCase("get")) {
            return "Resources:\n" + "console    Shows console output\n";
          }
        }
        return "Commands:\n" + "help    Shows this message\n"
            + "rcon    Execute Command\n" + "get    Get a resource";
      }
      if (tokens[0].equalsIgnoreCase("get")) {
        if (tokens.length > 1) {
          if (tokens[1].equalsIgnoreCase("console")) {
            return getConsole();
          }
        }
        return "Error: No Command";
      }
    }
    return "Error: Unrecognized Command";
  }

  protected String auth(String passwd) {
    if (!parent.options.contains("rconPassword")) {
      System.out.println("[SimpleServer] RCON Auth Attempt from "
          + parent.socket.getInetAddress().getHostAddress()
          + "! (rconPassword is blank)");
      return null;
    }
    if (passwd.equals(parent.options.get("rconPassword"))) {
      auth = true;
      return "";
    }
    else {
      System.out.println("[SimpleServer] RCON Authentication Failed from "
          + parent.socket.getInetAddress().getHostAddress() + "!");
      return null;
    }
  }

  private void sendPacket(byte[] bb) throws IOException {
    DatagramSocket s = new DatagramSocket();
    s.connect(current.getAddress(), current.getPort());
    DatagramPacket send = new DatagramPacket(bb, bb.length);
    s.send(send);
  }

  private void junkResponse() throws IOException {
    int packetSize = INT * 2 + 1 + 1;
    ByteBuffer bb;
    bb = ByteBuffer.allocate(INT + packetSize);
    bb.order(ByteOrder.LITTLE_ENDIAN);
    bb.putInt(packetSize);
    bb.putInt(-1);
    bb.putInt(SERVERDATA_RESPONSE_VALUE);
    bb.put((byte) 0);
    bb.put((byte) 0);
    sendPacket(bb.array());

    // out.write(bb.array());
  }

  public void handle(DatagramPacket p) throws IOException {
    lastRead = System.currentTimeMillis();
    ByteBuffer buf = ByteBuffer.wrap(p.getData());

    int packetSize = buf.getInt();
    int requestID = buf.getInt();
    int requestType = buf.getInt();
    String s1 = readString2(buf);
    readString2(buf);

    int responseType = 0;
    String response;
    if (requestType == SERVERDATA_EXECCOMMAND && auth) {
      response = parsePacket(s1);
    }
    else if (requestType == SERVERDATA_AUTH) {
      response = auth(s1);
      responseType = SERVERDATA_AUTH_RESPONSE;
      if (response == null) {
        requestID = -1;
        response = "";
      }
      junkResponse();
    }
    else if (!auth) {
      responseType = SERVERDATA_AUTH_RESPONSE;
      requestID = -1;
      response = "";
    }
    else {
      responseType = SERVERDATA_RESPONSE_VALUE;
      requestID = -1;
      response = "Error";
    }
    ByteBuffer bb;
    ByteBuffer bbSend;

    if (!response.equals("")) {
      bbSend = ByteBuffer.wrap(response.getBytes());
      int i = 0;

      byte[] send = null;
      for (i = 0; i + 4096 < bbSend.capacity(); i += 4096) {
        if (send == null) {
          send = new byte[4098];
        }
        packetSize = INT * 2 + 4096 + 1 + 1;

        bb = ByteBuffer.allocate(4 + packetSize);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(packetSize);
        bb.putInt(requestID);
        bb.putInt(responseType);

        // sendPacket(bb.array());
        bbSend.get(send, i, 4096);
        send[4096] = 0;
        send[4097] = 0;
        bb.put(send);
        sendPacket(bb.array());

      }
      send = new byte[bbSend.capacity() - i + 2];
      packetSize = INT * 2 + (bbSend.capacity() - i) + 1 + 1;
      bb = ByteBuffer.allocate(4 + packetSize);
      bb.order(ByteOrder.LITTLE_ENDIAN);
      bb.putInt(packetSize);
      bb.putInt(requestID);
      bb.putInt(responseType);

      // out.write(bb.array());
      bbSend.get(send, i, bbSend.capacity() - i);
      send[bbSend.capacity() - i] = 0;
      send[bbSend.capacity() - i + 1] = 0;
      bb.put(send);
      sendPacket(bb.array());

    }
    else {
      packetSize = INT * 2 + 1 + 1;
      bb = ByteBuffer.allocate(4 + packetSize);
      bb.order(ByteOrder.LITTLE_ENDIAN);
      bb.putInt(packetSize);
      bb.putInt(requestID);
      bb.putInt(responseType);
      bb.put((byte) 0);
      bb.put((byte) 0);
      sendPacket(bb.array());
    }

  }

  public void handle(Object o) {
    if (o instanceof DatagramPacket) {
      try {
        handle((DatagramPacket) o);
      }
      catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  public String getName() {
    return name;
  }
}
