package mcwrapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamDumper implements Runnable {
	InputStream in;
	OutputStream out;
	FileOutputStream file;
	boolean debug;
	
	Player parent;
	
	public StreamDumper(InputStream in, OutputStream out, Player parent, String filename) {
		this.in=in;
		this.out=out;
		File f = new File(filename);
		if (!f.exists())
			try {
				f.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		try {
			file = new FileOutputStream(filename);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.parent=parent;
	}
	public static final int unsignedIntToLong(byte[] b) 
	{
	    int l = 0;
	    l |= b[0] & 0xFF;
	    l <<= 8;
	    l |= b[1] & 0xFF;
	    l <<= 8;
	    l |= b[2] & 0xFF;
	    l <<= 8;
	    l |= b[3] & 0xFF;
	    return l;
	}
	int bytesToShort(byte[] data) 
	{       
	        //int value = data[1];
	        int value=(((short)data[0])<<8) | (((short)data[1])&0xff);
	        //value = (value << 8) | data[0];
	        //System.out.println("DATA:" + value);
	        return value;
	}
	void printStream(byte[] stream, int len) {
		for(int i=0;i<len;i++) {
			System.out.print(stream[i] + " ");
		}
		System.out.println();
	}

	
	int skipbytes=0;
	int size=0;
	byte[] cpy = new byte[2];
	byte[] buf = new byte[1024];
	int timer=0;
	public void finalize() {
		try {
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void run() {
		// TODO Auto-generated method stub
		try {
			int avail = in.available();
			
			boolean dontsleep=false;
		    while (!parent.isClosed()) {
		    	//if (parent.isKilled()) {
		    	//	continue;
		    	//}
		    	avail = in.available();
				int amt = avail;
				if (amt > buf.length) {
					amt = buf.length;
				}
				if (amt>0)
					amt = in.read(buf, 0, amt);
				
				
				
				if (amt>0) {
					file.write(buf,0,amt);
					out.write(buf, 0, amt);
			        out.flush();
				}
		        
		        if (!dontsleep)
		        	Thread.sleep(20);
		        else
		        	dontsleep=false;
	    	}
			    
		}
		catch (InterruptedException e) {
			//We don't care about an Interrupted Exception.
			//Don't even print out to the console that we received the exception.
			//We are only interrupted if we are closing.
			try {in.close();} catch (IOException e1) {}
			try {out.close();} catch (IOException e1) {}
			try {file.close();} catch (IOException e1) {}
			return;
		}
		catch (Exception e) {
			try {in.close();} catch (IOException e1) {}
			try {out.close();} catch (IOException e1) {}
			try {file.close();} catch (IOException e1) {}
			parent.close();
			return;
		}
		try {in.close();} catch (IOException e1) {}
		try {out.close();} catch (IOException e1) {}
		try {file.close();} catch (IOException e1) {}
		parent.close();
	}
	/*
	private void print(byte[] buf, int amt) {
		byte[] conv;
		for(int i=0;i<amt;i++) {
			if (reading) {
				if (waitingfor>0) {
					if (i+waitingfor<=amt) {
						System.arraycopy(buf, i, current, 0, waitingfor);
						waitingfor=0;
						state++;
						continue;
					}
					else {
						System.arraycopy(buf, i, current, cursor, (amt-i));
						cursor=amt-i;
						continue;
					}
				}
				switch(packetid) {
					case 0:
						reading=false;
						state=0;
						current=data;
						i--;
						break;
					case 1: 
						
						switch(state) {
							case 0:
								current=data;
								waitingfor=4;
								i--;
								break;
							case 1:
								current=data2;
								conv = new byte[2];
								System.arraycopy(buf, i, conv, 0, 2);
								waitingfor=bytesToShort(conv);
								i++;
								break;
							case 2:
								current=data3;
								conv = new byte[2];
								System.arraycopy(buf, i, conv, 0, 2);
								waitingfor=bytesToShort(conv);
								i++;
								break;
							case 3:
								current=data;
								state=0;
								reading=false;
								System.out.println(new String(data) + " " + new String(data2) + " " + new String(data3));
								i--;
								break;
						}
						break;
					case 2: 
						
						switch(state) {
							case 0:
								current=data;
								conv = new byte[2];
								System.arraycopy(buf, i, conv, 0, 2);
								waitingfor=bytesToShort(conv);
								i++;
								break;
							case 1:
								current=data;
								state=0;
								reading=false;
								System.out.println(new String(data));
								i--;
								break;
						}
						break;
					default: 
						reading=false;
						state=0;
						current=data;
						i--;
						break;

				}
			}
			else {
				
				packetid=buf[i];
				System.out.println(packetid);
				reading=true;
			}
		}
	}
	*/
}
