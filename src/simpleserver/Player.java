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
import java.net.Socket;
import java.util.LinkedList;

import simpleserver.files.Group;
import simpleserver.threads.DelayClose;

public class Player {
	//public SocketThread internal;
	//public SocketThread external;
	public CommandParser parser;
	public Socket intsocket;
	public Socket extsocket;
	Thread t1;
	Thread t2;
	Thread timeout;
	Server parent;
	private String name=null;
	boolean closed=false;
	boolean isKicked=false;
	boolean attemptLock=false;
	boolean destroy=false;
	public String kickMsg=null;
	double x,y,z,stance;
	int uid;
	int group=0;
	Group groupObject=null;
	//test
	double[] warpCoords=null;
	double[] warpCoords2=null;
	boolean isRobot=false;
	int robotPort=0;
	
	
	StreamTunnel serverToClient,clientToServer;
	//StreamDumper serverToClient, clientToServer;
	
	private LinkedList<String> messages= new LinkedList<String>();
	public void sendHome() throws IOException {
		ByteArrayOutputStream cpy = new ByteArrayOutputStream();
		DataOutputStream s = new DataOutputStream(cpy);
		s.writeByte(3);
		s.writeUTF("/home");
		byte[] msgBytes = cpy.toByteArray();
		clientToServer.addPacket(msgBytes);
	}
	public void warp(double[] coords) {
		warpCoords=coords;
		warpCoords2=coords.clone();
	}
	public boolean setName(String name) throws InterruptedException {
		t1.setName(t1.getName()+"-serverToClient-"+name);
		t2.setName(t2.getName()+"-clientToServer-"+name);
		//timeout.setName("timeoutThread-"+name);
		if (name==null) {
			kick("Invalid Name!");
			return false;
		}
		if (name.trim().compareTo("")==0 || name.length()==0 || name.trim().length()==0 || this.name!=null) {
			kick("Invalid Name!");
			return false;
		}
		if (parent.options.useWhitelist) {
			if (!parent.whitelist.isWhitelisted(name)) {
				kick("You are not whitelisted!");
				return false;
			}
		}
		updateGroup(name.trim());
		this.name=name.trim();
		parent.requireBackup=true;
		return true;
	}
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
	public void kick(String msg) {
		kickMsg=msg;
		isKicked=true;
		delayClose();
	}
	public boolean isKicked() {
		return isKicked;
	}
	public void isKicked(boolean b) {
		isKicked=b;
	}
	public boolean isMuted() {
		return parent.mutelist.isMuted(name);
	}
	public String getKickMsg() {
		return kickMsg;
	}
	public boolean hasMessages() {
		if (messages.isEmpty())
			return false;
		return true;
	}
	public String getName() {
		return name;
	}
	public boolean parseCommand(String msg) throws InterruptedException, IOException {
		if (!closed)
			return parser.parse(msg);
		return true;
	}
	public int getGroup() {
		return group;
	}
	private void updateGroup(String name) {
		int nameGroup = parent.members.checkName(name);
		int ipGroup = parent.ipMembers.checkPlayer(this);
		if ((nameGroup==-1 || ipGroup==-1 && parent.options.defaultGroup!=-1) ||
				(nameGroup==-1 && ipGroup==-1 && parent.options.defaultGroup==-1)	
		) {
			group=-1;
			if (parent.groups.groupExists(group))
				groupObject = parent.groups.getGroup(group);
			else
				groupObject=null;
			return;
		}
		
		if (ipGroup>=nameGroup)
			group=ipGroup;
		else 
			group=nameGroup;
		
		if (parent.groups.groupExists(group))
			groupObject = parent.groups.getGroup(group);
		else
			group=0;
	}
	public void updateGroup() {
		updateGroup(this.name);
	}
	public boolean isAdmin() {
		if (groupObject!=null)
			return groupObject.isAdmin();
		return false;
	}
	public String getIPAddress() {
		return extsocket.getInetAddress().getHostAddress();
	}
	public void delayClose() {
		new Thread(new DelayClose(this)).start();
	}
	public Player(Socket inc, Server parent) {
		this.parent = parent;
		parser = new CommandParser(this);
		extsocket = inc;
		if (parent.isRobot(extsocket.getInetAddress().getHostAddress())) {
			System.out.println("[SimpleServer] Robot Heartbeat: " + extsocket.getInetAddress().getHostAddress() + ".");
			isRobot=true;
		}
		if (!isRobot)
			System.out.println("[SimpleServer] IP Connection from " + extsocket.getInetAddress().getHostAddress() + "!");
		try {
			parent.requestTracker.addRequest(extsocket.getInetAddress().getHostAddress());
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if (parent.isIPBanned(extsocket.getInetAddress().getHostAddress())) {
			System.out.println("[SimpleServer] IP " + extsocket.getInetAddress().getHostAddress() + " is banned!");
			kick("Banned IP!");
		}
		try {
			intsocket = new Socket("localhost",parent.options.internalPort);
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			if (parent.options.exitOnFailure)
				System.exit(-1);
			else {
				parent.forceRestart();
			}
		} 
		
		try {
			
			t1=new Thread(serverToClient=new StreamTunnel(intsocket.getInputStream(),extsocket.getOutputStream(),true,this,2048));
			t2=new Thread(clientToServer=new StreamTunnel(extsocket.getInputStream(),intsocket.getOutputStream(),false,this));
			t1.start();
			t2.start();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				close();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		
		
		if (isRobot) {
			parent.addRobotPort(intsocket.getLocalPort());
		}
	}
	
	
	//Public Methods:
	public boolean testTimeout() {
		if (!closed) {
			if (System.currentTimeMillis()-serverToClient.lastRead>StreamTunnel.IDLE_TIME || System.currentTimeMillis()-clientToServer.lastRead>StreamTunnel.IDLE_TIME) {
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
	public boolean isConnected() throws InterruptedException {
		if (closed)
			return false;
		if (!extsocket.isConnected() || !intsocket.isConnected()) {
			parent.notifyClosed(this);
			close();
			return false;
			
		}
		return true;
	}
	public void close() throws InterruptedException {
		//Don't spam the console! : )
		//And don't close if we're already closing!
		if (!isKicked&&parent!=null)
			parent.notifyClosed(this);
		if (!closed) {
			closed=true;
			cleanup();
		}
	}
	public void cleanup() {
		try {
			t1.interrupt();
		}catch(Exception e) {}
		try {
			t2.interrupt();
		}catch(Exception e){}
		try {
			extsocket.close();
		}
		catch (Exception e) {}
		try {
			intsocket.close();
		}
		catch (Exception e) {}
		intsocket=null;
		if (!isRobot && extsocket!=null)
			System.out.println("[SimpleServer] Socket Closed: " + extsocket.getInetAddress().getHostAddress());
		extsocket=null;
		t1=null;
		t2=null;
		clientToServer=null;
		serverToClient=null;
		name=null;
		
	}
	public void reinitialize(Socket inc) {
		extsocket = inc;
		isRobot=false;
		name=null;
		closed=false;
		isKicked=false;
		attemptLock=false;
		destroy=false;
		kickMsg=null;
		x=0;
		y=0;
		z=0;
		stance=0;
		uid=0;
		group=0;
		groupObject=null;
		robotPort=0;
		
		if (parent.isRobot(extsocket.getInetAddress().getHostAddress())) {
			System.out.println("[SimpleServer] Robot Heartbeat: " + extsocket.getInetAddress().getHostAddress() + ".");
			isRobot=true;
		}
		if (!isRobot)
			System.out.println("[SimpleServer] IP Connection from " + extsocket.getInetAddress().getHostAddress() + "!");
		try {
			parent.requestTracker.addRequest(extsocket.getInetAddress().getHostAddress());
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if (parent.isIPBanned(extsocket.getInetAddress().getHostAddress())) {
			System.out.println("[SimpleServer] IP " + extsocket.getInetAddress().getHostAddress() + " is banned!");
			kick("Banned IP!");
		}
		try {
			intsocket = new Socket("localhost",parent.options.internalPort);
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			if (parent.options.exitOnFailure)
				System.exit(-1);
			else {
				parent.forceRestart();
			}
		} 
		
		try {
			
			t1=new Thread(serverToClient=new StreamTunnel(intsocket.getInputStream(),extsocket.getOutputStream(),true,this,2048));
			t2=new Thread(clientToServer=new StreamTunnel(extsocket.getInputStream(),intsocket.getOutputStream(),false,this));
			t1.start();
			t2.start();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				close();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		
		
		if (isRobot) {
			parent.addRobotPort(intsocket.getLocalPort());
		}
	}
	public boolean isRobot() {
		return isRobot;
	}
}
