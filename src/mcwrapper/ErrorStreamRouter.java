package mcwrapper;

import java.io.IOException;
import java.io.InputStream;

public class ErrorStreamRouter implements Runnable {
	InputStream stream;
	Server parent;
	public ErrorStreamRouter(InputStream stream, Server parent) {
		this.stream = stream;
		this.parent = parent;
	}
	public void finalize() {
		try {
			stream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void run() {
		
	   
	    
		// TODO Auto-generated method stub
		try
        {
				InputStream in1 = stream;
				byte[] buf = new byte[1024];
				String soFar="";
				// loop forever...
				while(true) {
					Thread.sleep(20);
				    // collect all the bytes waiting on the input stream
					int amt = in1.read(buf);
					for (int i=0;i<amt;i++) {
						soFar+=(char)buf[i];
						if (buf[i]==(byte)'\n' || buf[i]==(byte)'\r') {
							handleLine(soFar);
							soFar="";
						}
					}
				}
		}
        catch (Exception e)
        {
        	//if (!parent.isRestarting)
        		//e.printStackTrace();
        }
        try {
			stream.close();
		} catch (IOException e) {
			//if (!parent.isRestarting)
				//e.printStackTrace();
		}
	}
	private void handleLine(String line) {
		if (line.contains("[INFO] CONSOLE: Save complete.")) {
			parent.isSaving=false;
			parent.runCommand("say Save Complete.");
		}
		System.out.print(line);
	}
}
