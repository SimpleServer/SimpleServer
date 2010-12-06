package mcwrapper;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;

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
	private String name;
	boolean closed=false;
	boolean isKicked=false;
	boolean attemptLock=false;
	boolean destroy=false;
	public String kickMsg=null;
	double x,y,z,stance;
	int uid;
	int rank=0;
	//test
	double[] warpCoords=null;
	
	StreamTunnel serverToClient,clientToServer;
	
	private LinkedList<String> messages= new LinkedList<String>();

	public void warp(double[] coords) {
		warpCoords=coords;
	}
	public void setName(String name) {
		if (name==null)
			kick("Invalid Name!");
		if (name.trim().compareTo("")==0 || name.length()==0 || name.trim().length()==0 || this.name!=null) {
			kick("Invalid Name!");
		}
		if (parent.options.useWhitelist) {
			if (!parent.whitelist.isWhitelisted(name)) {
				close();
			}
		}
		
		this.name=name.trim();
		setRank();
		parent.requireBackup=true;
	}
	public void addMessage(String msg) {
		messages.addLast(msg);
	}
	public String getMessage() {
		
		return messages.removeFirst();
	}
	public void kick(String msg) {
		kickMsg=msg;
		isKicked=true;
		delayClose();
	}
	public boolean isKicked() {
		return isKicked;
	}
	public boolean isMuted() {
		return parent.isMuted(name);
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
	public void parseCommand(String msg) {
		parser.parse(msg);
	}
	public int getRank() {
		return rank;
	}
	public void setRank() {
		int nameRank = parent.ranks.checkName(name);
		int ipRank = parent.ipRanks.checkPlayer(this);
		if (ipRank>=nameRank)
			rank=ipRank;
		else 
			rank=nameRank;
		
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
		
		System.out.println("[SimpleServer] IP Connection from " + extsocket.getInetAddress().getHostAddress() + "!");
		parent.requestTracker.addRequest(extsocket.getInetAddress().getHostAddress());
		if (parent.isIPBanned(extsocket.getInetAddress().getHostAddress())) {
			System.out.println("[SimpleServer] IP " + extsocket.getInetAddress().getHostAddress() + " is banned!");
			kick("Banned IP!");
		}
		
		try {
			intsocket = new Socket("localhost",parent.options.internalPort);
			if (!parent.options.debug) {
				t1=new Thread(serverToClient=new StreamTunnel(intsocket.getInputStream(),extsocket.getOutputStream(),true,this,2048));
				t2=new Thread(clientToServer=new StreamTunnel(extsocket.getInputStream(),intsocket.getOutputStream(),false,this));
			}
			else {
				t1=new Thread(new StreamDumper(intsocket.getInputStream(),extsocket.getOutputStream(),this,"extsocket.txt"));
				t2=new Thread(new StreamDumper(extsocket.getInputStream(),intsocket.getOutputStream(),this,"intsocket.txt"));
			}
			t1.start();
			t2.start();
			//timeout = new Thread(new TimeoutDetect(this,proxy,t2));
			//timeout.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			close();
		}
		/*
		try {
			extsocket.setTcpNoDelay(true);
			intsocket.setTcpNoDelay(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			parent.notifyClosed(this);
			if (parent.debug)
				e.printStackTrace();
		}
		*/
	}
	
	
	//Public Methods:
	public void testTimeout() {
		if (System.currentTimeMillis()-serverToClient.lastRead>10*1000 || System.currentTimeMillis()-clientToServer.lastRead>10*1000) {
			System.out.println("[SimpleServer] Disconnecting + " + getIPAddress() + " due to inactivity.");
			close();
			return;
		}
	}
	public boolean isClosed() {
		return closed;
	}
	public boolean isConnected() {
		if (!extsocket.isConnected() || !intsocket.isConnected()) {
			parent.notifyClosed(this);
			close();
			return false;
			
		}
		return true;
	}
	public void finalize() {
		try {
			intsocket.close();
		} catch (IOException e) {}
		try {
			extsocket.close();
		} catch (IOException e) {}
		parent.notifyClosed(this);
		t1.interrupt();
		t2.interrupt();
	}
	public void close() {
		//Don't spam the console! : )
		//And don't close if we're already closing!
		parent.notifyClosed(this);
		if (!closed) {
			closed=true;
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
			System.out.println("[SimpleServer] Socket Closed: " + extsocket.getInetAddress().getHostAddress());
		}
	}
}
