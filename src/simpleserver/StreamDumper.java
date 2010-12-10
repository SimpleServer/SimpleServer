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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamDumper implements Runnable {
	InputStream in;
	OutputStream out;
	FileOutputStream file;
	String filename;
	boolean debug;
	public long lastRead;
	
	Player parent;
	
	public StreamDumper(InputStream in, OutputStream out, boolean isServerTunnel, Player p) {
		this.in=in;
		this.out=out;
		filename="client.txt";
		if (isServerTunnel)
			filename="server.txt";
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
		this.parent=p;
	}
	public void addPacket(byte[] b) {
	}
	public StreamDumper(InputStream in, OutputStream out, boolean isServerTunnel, Player p, int byteThreshold) {
		this.in=in;
		this.out=out;
		filename="client.txt";
		if (isServerTunnel)
			filename="server.txt";
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
		this.parent=p;
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
				
				lastRead = System.currentTimeMillis();
				
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
			try {
				parent.close();
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return;
		}
		try {in.close();} catch (IOException e1) {}
		try {out.close();} catch (IOException e1) {}
		try {file.close();} catch (IOException e1) {}
		try {
			parent.close();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
