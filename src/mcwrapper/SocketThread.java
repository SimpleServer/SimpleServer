package mcwrapper;

import java.io.IOException;
import java.net.ServerSocket;

public class SocketThread implements Runnable {
	private Server parent;
	public SocketThread(Server parent) {
		this.parent=parent;
	}

	public void run() {
		// TODO Auto-generated method stub
		try {
            parent.socket = new ServerSocket(parent.options.port);
        } 
        catch (IOException e) {
            System.out.println("Could not listen on port " + parent.options.port +"!\r\nIs it already in use? Exiting application...");
            System.exit(-1);
        }
			
	        
        while (parent.open) {
        	try {
	        	if (parent.open) {
	        		parent.players.add(new Player(parent.socket.accept(),parent));
	        	}
        	}
            catch (IOException e) {
            	if (!parent.isRestarting) {
            		e.printStackTrace();
            		System.out.println("Accept failed on port " + parent.options.port + "!");
            	}
                //System.exit(-1);
            }
        }
        try {
        	parent.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if (!parent.isRestarting)
				e.printStackTrace();
		}
		if (!parent.isRestarting) {
			System.out.println("Server is shutting down! Good-bye!");
			System.exit(0);
		}
		
	}
	
}