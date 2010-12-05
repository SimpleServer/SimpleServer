package simpleserver.threads;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import simpleserver.PlayerFactory;
import simpleserver.Server;

public class SocketThread implements Runnable {
	private Server parent;
	public SocketThread(Server parent) {
		this.parent=parent;
	}

	public void run() {
		// TODO Auto-generated method stub
		try {
			if (parent.options.ipAddress.equals("0.0.0.0"))
				parent.socket = new ServerSocket(parent.options.port);
			else
				parent.socket = new ServerSocket(parent.options.port,8,InetAddress.getByName(parent.options.ipAddress));

        } 
        catch (IOException e) {
            System.out.println("Could not listen on port " + parent.options.port +"!\r\nIs it already in use? Exiting application...");
            System.exit(-1);
        }
			
	        
        while (parent.isOpen()) {
        	try {
	        	if (parent.isOpen()) {
	        		PlayerFactory.addPlayer(parent.socket.accept());
	        		//Player p = new Player(parent.socket.accept(),parent);
	        		//synchronized(parent.players) {
	        		//parent.players.add(p);
	        		//}
	        	}
        	}
            catch (IOException e) {
            	if (!parent.isRestarting()) {
            		e.printStackTrace();
            		System.out.println("Accept failed on port " + parent.options.port + "!");
            	}
            }
        }
        try {
        	parent.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if (!parent.isRestarting())
				e.printStackTrace();
		}	
	}
	
}