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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

import simpleserver.Server;

public class InputStreamRouter implements Runnable {
	InputStream stream;
	OutputStream out;
	LinkedList<String> queue = new LinkedList<String>();
	Server parent;
	Scanner scan;
	Semaphore outLock = new Semaphore(1);
	
	
	public InputStreamRouter(InputStream in, OutputStream out, Server parent) {
		this.stream = in;
		this.out=out;
		this.parent=parent;
	}
	public boolean runCommand(String msg) throws InterruptedException {
		
		msg +="\r\n";
		msg = parse(msg);
		if (msg==null)
			return true;
		outLock.acquire();
		try {
			out.write(msg.getBytes());
			out.flush();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (!parent.isRestarting()) {
				e.printStackTrace();
				if (parent.options.exitOnFailure)
					System.exit(-1);
				else
					parent.forceRestart();
			}
		}
		outLock.release();
		return true;
	}
	public void setOut(OutputStream out) {
		try {
			outLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.out=out;
		outLock.release();
	}
	private String parse(String line) {
		if (line.startsWith("!reload")) {
			System.out.println("Reloading Resources...");
			parent.loadAll();
			if (parent.options.useSMPAPI)
				return line;
			else
				return "";
		}
		if (line.startsWith("!save")) {
			System.out.println("Saving Resources...");
			parent.saveAll();
			if (parent.options.useSMPAPI)
				return line;
			else
				return "";
		}
		if (line.startsWith("!backup")) {
			parent.forceBackup();
			return "";
		}
		if (line.startsWith("!restart")) {
			parent.restartServer();
			return null;
		}
		if (line.startsWith("stop")) {
			if (!parent.isRestarting()) {
				parent.stopServer();
				return null;
			}
			else {
				return line;
			}
		}
		if (line.startsWith("show")) {
			System.out.println("SimpleServer is licensed under GPL v3.\nSee the file LICENSE for more details.");
		}
		return line;
	}
	public void run() {
		// loop forever...
		scan = new Scanner(stream);
		while(!Thread.interrupted()) {
			String line = scan.nextLine();

			line+="\r\n";
			line = parse(line);
			if (line==null)
				continue;
			try {
				outLock.acquire();
			}
			catch (Exception e) {
				
			}
			try {
				out.write(line.getBytes());
				out.flush();
			}
			catch(Exception e) {
				if (!parent.isRestarting()) {
					e.printStackTrace();
					if (parent.options.exitOnFailure)
						System.exit(-1);
					else
						parent.forceRestart();
				}
			}
			outLock.release();
		}
        try {
			out.close();
		} catch (IOException e) {
			if (!parent.isRestarting())
				e.printStackTrace();
		}
	}
}
