/*******************************************************************************
 * Copyright (C) 2010 Charles Wagner Jr..
 * spiegalpwns@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
