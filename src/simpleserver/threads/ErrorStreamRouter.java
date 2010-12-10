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

import simpleserver.Server;

public class ErrorStreamRouter implements Runnable {
	InputStream stream;
	Server parent;
	public ErrorStreamRouter(InputStream stream, Server parent) {
		this.stream = stream;
		this.parent = parent;
	}
	public void run() {
		
	   
	    
		// TODO Auto-generated method stub
		try
        {
				InputStream in1 = stream;
				byte[] buf = new byte[1024];
				String soFar="";
				// loop forever...
				while(!Thread.interrupted()) {
				    // collect all the bytes waiting on the input stream
					int amt = in1.read(buf);
					for (int i=0;i<amt;i++) {
						soFar+=(char)buf[i];
						if (buf[i]==(byte)'\n') {
							handleLine(soFar);
							soFar="";
						}
					}
				}
		}
        catch (Exception e)
        {
        	if (!parent.isRestarting())
        		e.printStackTrace();
        }
        try {
			stream.close();
		} catch (IOException e) {
			if (!parent.isRestarting())
				e.printStackTrace();
		}
	}
	private void handleLine(String line) throws InterruptedException {
		if (!parent.options.debug){ 
			if (line.contains("\tat")) {
				return;
			}
		}
		Integer[] ports = parent.getRobotPorts();
		if (ports!=null) {
			for(int i=0;i<ports.length;i++) {
				if (ports[i]!=null) {
					if (line.contains(ports[i].toString())) {
						parent.removeRobotPort(ports[i]);
						return;
					}
				}
			}
		}
		if (line.contains("[INFO] CONSOLE: Save complete.")) {
			parent.isSaving(false);
			//parent.runCommand("say Save Complete.");
			parent.sendToAll(parent.l.get("SAVE_COMPLETE"));
		}
		if (line.contains("[INFO] Done!")) {
			parent.waitingForStart(false);
		}
		if (line.contains("[SEVERE] Unexpected exception")) {
			parent.forceRestart();
		}
		parent.addOutputLine(line);
		System.out.print(line);
	}
}
