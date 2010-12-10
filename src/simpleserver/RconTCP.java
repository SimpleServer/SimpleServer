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
package simpleserver;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

import simpleserver.files.Group;

public class RconTCP implements Rcon {
	protected final int SERVERDATA_AUTH=3;
	protected final int SERVERDATA_EXECCOMMAND=2;
	protected final int SERVERDATA_AUTH_RESPONSE = 2;
	protected final int SERVERDATA_RESPONSE_VALUE = 0;
	protected final int INT = 4;
	protected final int BB_DEFAULT = 128;
	//public SocketThread internal;
	//public SocketThread external;
	//public RconParser parser;
	public Socket current;
	protected long lastRead;
	Thread t1;
	String name=null;
	Thread timeout;
	Server parent;
	//private String name=null;
	boolean closed=false;
	boolean isKicked=false;
	boolean auth=true;
	//boolean attemptLock=false;
	//boolean destroy=false;
	public String kickMsg=null;
	//double x,y,z,stance;
	//int uid;
	//int group=0;
	//Group groupObject=null;
	//test
	//double[] warpCoords=null;
	//double[] warpCoords2=null;
	//boolean isRobot=false;
	//int robotPort=0;
	
	
	//RconHandler rconHandler;
	
	private LinkedList<String> messages= new LinkedList<String>();
	

	public void addMessage(String msg) {
		synchronized(messages) {
			messages.addLast(msg);
		}
	}
	public String getMessage() {
		synchronized(messages) {
			return messages.removeFirst();
		}
	}

	public boolean hasMessages() {
		if (messages.isEmpty())
			return false;
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
		kickMsg=msg;
		isKicked=true;
		close();
	}
	
	public String getKickMsg() {
		return kickMsg;
	}
	public boolean isKicked() {
		return isKicked;
	}
	
	public String getIPAddress() {
		return current.getInetAddress().getHostAddress();
	}

	public RconTCP(Socket p, Server parent) throws IOException {
		
		this.parent = parent;
		//parser = new RconParser(this);
		current = p;

		System.out.println("[SimpleServer] RCON Connection from " + getIPAddress() + "!");
		try {
			parent.requestTracker.addRequest(getIPAddress());
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if (parent.isIPBanned(getIPAddress())) {
			System.out.println("[SimpleServer] IP " + getIPAddress() + " is banned!");
			kick("Banned IP!");
		}
		this.name = getIPAddress();
		
		lastRead = System.currentTimeMillis();
		t1 = new Thread() {
			public void run() {
				if (testTimeout()) {
					close();
					return;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					return;
				}
			}
		};
		t1.start();
		
		
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
	
	
	//Public Methods:
	public boolean testTimeout() {
		if (!closed) {
			if (System.currentTimeMillis()-lastRead>RconHandler.IDLE_TIME) {
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
		//Don't spam the console! : )
		//And don't close if we're already closing!
		//if (!isKicked&&parent!=null)
			//parent.notifyClosedRcon(this);
		if (!closed) {
			closed=true;
			auth=false;
			if (!isKicked)
				parent.notifyClosedRcon(this);
		}
	}
	public void cleanup() {

	}
	/*
	
	protected String readString2(ByteBuffer bb) {
		byte b;
		int offset = bb.position();
		int i=0;
		while(true) {
			b = bb.get();
			System.out.print(b + " ");
			if (b==0)
				break;
			i++;
		}
		if (i==0)
			return "";
		byte[] string = new byte[i];
		//bb.position(0);
		System.out.println("");
		bb.get(string, offset, i);
		return new String(string);
	}
	private String getConsole() {
		String console = "";
		String[] consolearray = parent.getOutputLog();
		for (String i: consolearray) {
			console += i;
		}
		return console;
	}
	protected String parsePacket(String command) {
		String[] tokens = command.split(" ");
		if (tokens.length>0) {
			if (tokens[0].equalsIgnoreCase("rcon")) {
				if (tokens.length>1) {
					int idx = command.indexOf(tokens[1]);
					try {
						parent.runCommand(command.substring(idx));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return command;
				}
				else
					return "Error: No Command";
			}
			if (tokens[0].equalsIgnoreCase("help")) {
				if (tokens.length>1) {
					if (tokens[1].equalsIgnoreCase("get")) {
						return "Resources:\r\n" +
						"console		Shows console output\r\n";
					}
				}
				return "Commands:\r\n" +
					"help		Shows this message\r\n" +
					"rcon		Execute Command\r\n" +
					"get		Get a resource";
			}
			if (tokens[0].equalsIgnoreCase("get")) {
				if (tokens.length>1) {
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
		if (parent.options.rconPassword.equals("")) {
			System.out.println("[SimpleServer] RCON Auth Attempt from " + parent.socket.getInetAddress().getHostAddress() + "! (rconPassword is blank)");
			return null;
		}
		if (passwd.equals(parent.options.rconPassword)) {
			auth=true;
			return "";
		}
		else {
			System.out.println("[SimpleServer] RCON Authentication Failed from " + parent.socket.getInetAddress().getHostAddress() + "!");
			return null;
		}
	}
	
	private void sendPacket(byte[] bb) throws IOException {
		DatagramSocket s = new DatagramSocket();
		s.connect(current.getAddress(),current.getPort());
		DatagramPacket send = new DatagramPacket(bb,bb.length);
		s.send(send);
	}

	private void junkResponse() throws IOException {
		int packetSize= INT * 2 + 1 + 1;
		ByteBuffer bb;
		bb = ByteBuffer.allocate(INT + packetSize);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(packetSize);
		bb.putInt(-1);
		bb.putInt(SERVERDATA_RESPONSE_VALUE);
		bb.put((byte) 0);
		bb.put((byte) 0);
		sendPacket(bb.array());
		
		//out.write(bb.array());
	}
	*/
	public void handle(Socket p) throws IOException {
		new Thread(new RconHandler(p,this)).start();

    }
	
	public void handle(Object o) {
		if (o instanceof Socket)
			try {
				handle((Socket)o);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	public String getName() {
		return name;
	}
}
