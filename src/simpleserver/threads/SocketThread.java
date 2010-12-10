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
