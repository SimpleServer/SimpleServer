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
