package mcwrapper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Scanner;

public class InputStreamRouter implements Runnable {
	InputStream stream;
	OutputStream out;
	LinkedList<String> queue = new LinkedList<String>();
	Server parent;
	BufferedReader scan;
	
	public InputStreamRouter(InputStream in, OutputStream out, Server parent) {
		this.stream = in;
		this.out=out;
		this.parent=parent;
	}
	public boolean runCommand(String msg) {
		try {
			msg +="\r\n";
			out.write(msg.getBytes());
			out.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	public void finalize() {
		//scan.close();
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		try
        {
			// loop forever...
			scan = new BufferedReader(new InputStreamReader(stream));
			while(true) {
				
					String line = scan.readLine() + "\r\n";
				
					if (line.startsWith("!reload")) {
						System.out.println("Reloading Resources...");
						parent.loadAll();
						continue;
					}
					if (line.startsWith("!save")) {
						System.out.println("Saving Resources...");
						parent.saveAll();
						continue;
					}
					if (line.startsWith("!backup")) {
						parent.forceBackup();
						continue;
					}
					if (line.startsWith("!restart")) {
						parent.restart();
						continue;
					}
					if (line.startsWith("stop")) {
						parent.stopServer();
						return;
					}
					out.write(line.getBytes());
					out.flush();
			}
		}
        catch (Exception e)
        {
        	//if (parent.options.debug&&!parent.isRestarting)
        		e.printStackTrace();
        }
        //scan.close();
        try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
