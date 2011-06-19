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
package simpleserver.bot;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import simpleserver.Server;
import simpleserver.Player.LocalAddressFactory;

public class Bot {
  private static final LocalAddressFactory addressFactory = new LocalAddressFactory();

  private static final int VERSION = 13;

  private String name;
  private Server server;
  private boolean connected;
  private boolean receiving;

  private Socket socket;
  private DataInputStream in;
  protected DataOutputStream out;

  private Timer timer;
  private ReentrantLock writeLock;

  public Bot(Server server, String name) throws UnknownHostException, IOException {
    this.name = name;
    this.server = server;

    try {
      InetAddress localAddress = InetAddress.getByName(addressFactory.getNextAddress());
      socket = new Socket(InetAddress.getByName(null), server.options.getInt("internalPort"), localAddress, 0);
    } catch (Exception e) {
      socket = new Socket(InetAddress.getByName(null), server.options.getInt("internalPort"));
    }
    in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

    writeLock = new ReentrantLock();

    connected = receiving = true;
    new Tunneler().start();
    timer = new Timer();
    timer.schedule(new KeepAlive(), 0, 30 * 1000);

    handshake();
  }

  private void handshake() throws IOException {
    out.writeByte(2);
    write(name);
    out.flush();
  }

  public void logout() throws IOException {
    connected = receiving = false;
    out.writeByte(0xff);
    write("");
    out.flush();
    die();
  }

  protected void login() throws IOException {
    out.writeByte(1);
    out.writeInt(VERSION);
    write(name);
    out.writeLong(0);
    out.writeByte(0);
  }

  protected void ready() throws IOException {
    receiving = false;
  }

  protected void handlePacket(byte packetId) throws IOException {
    switch (packetId) {
      case 0x2:
        readUTF16();
        login();
        break;
      case 0x1:
        in.readInt();
        readUTF16();
        in.readLong();
        in.readByte();
        ready();
        break;
    }
  }

  protected String write(String s) throws IOException {
    byte[] bytes = s.getBytes("UTF-16");
    if (s.length() == 0) {
      out.write((byte) 0x00);
      out.write((byte) 0x00);
      return s;
    }
    bytes[0] = (byte) ((s.length() >> 8) & 0xFF);
    bytes[1] = (byte) ((s.length() & 0xFF));
    for (byte b : bytes) {
      out.write(b);
    }
    return s;
  }

  private String readUTF16() throws IOException {
    short length = in.readShort();
    byte[] bytes = new byte[length * 2 + 2];
    for (short i = 0; i < length * 2; i++) {
      bytes[i + 2] = in.readByte();
    }
    bytes[0] = (byte) 0xfffffffe;
    bytes[1] = (byte) 0xffffffff;
    return new String(bytes, "UTF-16");
  }

  protected void die() {
    timer.cancel();
    connected = receiving = false;
    try {
      socket.close();
    } catch (IOException e) {
    }
  }

  protected void error() {
    die();
    System.out.print("[SimpleServer] Bot " + name + " died");
  }

  private final class Tunneler extends Thread {
    @Override
    public void run() {
      while (receiving) {
        writeLock.lock();
        try {
          handlePacket(in.readByte());
          out.flush();
        } catch (IOException e) {
          error();
        } finally {
          writeLock.unlock();
        }
      }
    }
  }

  private final class KeepAlive extends TimerTask {
    @Override
    public void run() {
      if (!receiving) {
        try {
          logout();
        } catch (IOException e) {
          error();
        }
      } else if (connected) {
        writeLock.lock();
        try {
          out.write((byte) 0);
        } catch (IOException e) {
          error();
        } finally {
          writeLock.unlock();
        }
      }
    }
  }
}
