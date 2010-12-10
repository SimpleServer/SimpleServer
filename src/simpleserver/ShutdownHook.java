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
