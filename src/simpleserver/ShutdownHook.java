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

import java.io.IOException;
import java.io.OutputStream;


public class ShutdownHook implements Runnable {
	Process mc;
	OutputStream stream;
	Server parent;
	public ShutdownHook(Process minecraftServer, Server parent) {
		mc =minecraftServer;
		this.parent=parent;
		stream = mc.getOutputStream();
	}
	public void finalize() {
		mc=null;
		try {
			stream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		stream=null;
		parent=null;
	}

	public void run() {
		// TODO Auto-generated method stub
		System.out.println("Shutdown Hook Enabled");
		//parent.options.save();
		//parent.saveAll();
		stream = parent.p.getOutputStream();
		String cmd = "stop" + "\r\n";
		try {
			stream.write(cmd.getBytes());
			stream.flush();
			stream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		try {
			this.finalize();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
	}
	
}
