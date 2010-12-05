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
