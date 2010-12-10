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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import simpleserver.files.EOFWriter;


public class StreamTunnel implements Runnable {
	protected final int BUF_SIZE = 8192;
	protected int BYTE_THRESHOLD = 32;
	protected byte[] buf;
	protected int r;
	protected int a;
	protected long lastRead;
	protected InputStream in;
	protected OutputStream out;
	protected boolean inGame;
	protected Semaphore lock;
	protected boolean debug;
	protected boolean isServerTunnel;
	protected LinkedList<byte[]> packetBuffer=new LinkedList<byte[]>();
	protected LinkedList<byte[]> history = new LinkedList<byte[]>();
	public static final int IDLE_TIME=30*1000;
	protected ByteBuffer bb = ByteBuffer.allocate(128);
	
	protected int motionCounter=0;
	
	private Player parent;
	private Server server;
	
	public StreamTunnel(InputStream in, OutputStream out, boolean isServerTunnel, Player p) {
		this.in=in;
		this.out=out;
		parent=p;
		server=p.parent;
		buf = new byte[BUF_SIZE];
		inGame=false;
		lock = new Semaphore(1);
		debug=false;
		this.isServerTunnel=isServerTunnel;
		bb.order(ByteOrder.BIG_ENDIAN);
	}
	public StreamTunnel(InputStream in, OutputStream out, boolean isServerTunnel, Player p, int byteThreshold) {
		this.in=in;
		this.out=out;
		parent=p;
		server=p.parent;
		buf = new byte[BUF_SIZE];
		inGame=false;
		lock = new Semaphore(1);
		debug=false;
		this.isServerTunnel=isServerTunnel;
		BYTE_THRESHOLD= byteThreshold;
		bb.order(ByteOrder.BIG_ENDIAN);
	}
	public void setByteThreshold(int n) {
		if (n>BUF_SIZE)
			BYTE_THRESHOLD=BUF_SIZE;
		if (n<=0)
			BYTE_THRESHOLD=32;
		BYTE_THRESHOLD=n;
	}
	
	
	public void run() {
		int packetid=0;
		try {
			r=0;
			a=0;
			lastRead=System.currentTimeMillis();
		    while (!parent.isClosed() && !Thread.interrupted()) {
		    	lock.acquire();
		    	int avail = 0;
		    	try {
		    		avail = in.available();
		    	}
		    	catch (IOException ee) {
		    		break;
		    	}
		    	if (avail>0) {
		    		lastRead=System.currentTimeMillis();
		    		if (avail>buf.length-a) {
		    			avail = buf.length-a;
		    		}
		    		if (a<buf.length){ 
			    		int read = in.read(buf, a,avail);
			    		if (read>0) 
			    			a += read;
		    		}
		    	}
		    	lock.release();
		    	
		    	if (System.currentTimeMillis()-lastRead>IDLE_TIME) {
		    		if (!parent.isRobot)
		    			System.out.println("[SimpleServer] Disconnecting " + parent.getIPAddress() + " due to inactivity.");
		    		try {in.close();} catch (IOException e1) {}
					try {out.close();} catch (IOException e1) {}
		    		parent.close();
		    	}
		    	if (r==0 && a > 0) {
		    		while (r<BYTE_THRESHOLD && r<a) {
		    			packetid=readByte();
						if (debug) {
							//System.out.println("pid: " + packetid + " size: " + r + " amt: " + a);
				    	}
						parsePacket(packetid);
		    		}
		    	}
		    	while (history.size()>64) {
		    		history.removeFirst();
		    	}
		    	if (r>0) {
		    		if (r<=a) {
		    			out.write(buf, 0, r);
		    			byte[] hist = new byte[r];
		    			System.arraycopy(buf, 0, hist, 0, r);
		    			history.addLast(hist);
		    			while (!packetBuffer.isEmpty()) {
	    					byte[] b = packetBuffer.removeFirst();
	    					out.write(b);
	    					//printStream(b);
	    				}
		    			if (isServerTunnel) {
		    				//Messages
					        if (parent.isKicked()) {
								out.write(makePacket((byte)0xff,parent.getKickMsg()));
								parent.delayClose();
								break;
							}
							while(parent.hasMessages()) {
								byte[] m = makePacket(parent.getMessage());
								out.write(m, 0, m.length);
							}
		    			}
		    			else {
		    				if (parent.isKicked()) {
								out.write(makePacket((byte)0xff,parent.getKickMsg()));
								parent.delayClose();
								break;
							}
		    			}
		    			if (r<a) {
			    			//byte[] cpy = new byte[a-r];
			    			System.arraycopy(buf, r, buf, 0, a-r);
			    			//System.arraycopy(cpy, 0, buf, 0, a-r);
		    			}
		    			a-=r;
		    			r=0;
		    		}
		    		else {
		    			out.write(buf, 0, a);
		    			byte[] hist = new byte[a];
		    			System.arraycopy(buf, 0, hist, 0, a);
		    			history.addLast(hist);
		    			r-=a;
		    			a=0;
		    		}
		    	}
		    	if (parent.isClosed()) {
		    		throw new InterruptedException();
		    	}
		    	Thread.sleep(20);
		    }
		}
		catch(InterruptedException e1) {
			//We don't care about an Interrupted Exception.
			//Don't even print out to the console that we received the exception.
			//We are only interrupted if we are closing.
			//e1.printStackTrace();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		try {
			if (!parent.isClosed())
				parent.close();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		history.clear();
		history=null;
		try {in.close();} catch (IOException e2) {}
		try {out.close();} catch (IOException e2) {}
		in=null;
		out=null;
		parent=null;
		lock=null;
	}
	private byte[] makePacket(String msg) throws IOException {
		return makePacket((byte)0x03,msg);
	}
	private byte[] makePacket(byte packetid, String msg) throws IOException {
		ByteArrayOutputStream cpy = new ByteArrayOutputStream();
		DataOutputStream s = new DataOutputStream(cpy);
		s.writeByte(packetid);
		s.writeUTF(msg);
		byte[] msgBytes = cpy.toByteArray();
		s.close();
		return msgBytes;
	}
	public void addPacket(byte[] packet) {
		packetBuffer.add(packet);
	}
	static void putShort(byte[] b, int off, short val) {
	    b[off + 1] = (byte) (val >>> 0);
	    b[off + 0] = (byte) (val >>> 8);
	}
	protected void parsePacket(int packetid) throws IOException, InterruptedException {
		int oldCursor=0;

		switch (packetid) {
			/* 0.2.7 Update */
			case 0x3c:
				skipBytes(28);
				skipBytes(readInt()*3);
				break;
			/* 0.2.5 Update */
			case 0x08:
				skipBytes(1);
				break;
			case 0x09:
				break;
			case 0x26:
				skipBytes(5);
				break;
			case 0x07:
				//skipBytes(8);
				skipBytes(9);
				break;
			/* old */
			case 0x0a:
				skipBytes(1);
				if (!inGame&&!isServerTunnel) {
					parent.parser.sendMOTD();
					inGame=true;
				}
				break;
			case 0x04:
				skipBytes(8);
				break;
			//Format:
			//int, short
			//Then, each entry in the array is either s or sbs
			//it will be just s if it is -1
			case 0x05:
				oldCursor = r-1;
				readInt();
				short sizeOfArray=  readShort();
				for(int pst=0;pst<sizeOfArray;pst++) {
					short s = readShort();
					if (s!=-1) {
						if (server.itemWatch.checkCheck(s)&& parent.getName()!=null) {
							byte amtOfItem = readByte();
							if (!server.itemWatch.checkAllowed(parent, s, amtOfItem)&& parent.getName()!=null) {
								server.adminLog.addMessage("ItemWatchList banned player:\t" + parent.getName());
								server.banKick(parent.getName());
							}
							skipBytes(2);
						}
						else
							skipBytes(3);
					}
				}
				if (parent.getGroup()<0) {
					removeBytes(r-oldCursor);
					break;
				}
				//printStream(buf,i-2,size+4);
				break;
			case 0x3b:
				oldCursor = r-1;
				int xC3b = readInt();
				int yC3b = (int)readShort();
				int zC3b = readInt();
				int arraySize= readShort();
				skipBytes(arraySize);
				if (server.chests.hasLock(xC3b,yC3b,zC3b)) {
					if (!parent.isAdmin()) {
						if (!server.chests.ownsLock(xC3b,yC3b,zC3b,parent.getName()) || parent.getName()==null) {
							removeBytes(r-oldCursor);
							break;
						}
					}
				}
				
				if (parent.getGroup()<0 && !server.options.guestsCanViewComplex) {
					removeBytes(r-oldCursor);
					break;
				}
				
				break;
			case 0x06:
				skipBytes(12);
				break;
			case 0x0b:
				if (!isServerTunnel)
					motionCounter++;
				if (!isServerTunnel && motionCounter%8==0) {
					parent.x=readDouble();
					parent.y=readDouble();
					parent.stance=readDouble();
					parent.z=readDouble();
					skipBytes(1);
				}
				else
					skipBytes(33);
				break;
			case 0x0c:
				skipBytes(9);
				break;
			case 0x0d:
				if (!isServerTunnel)
					motionCounter++;
				if (!isServerTunnel && motionCounter%8==0) {
					parent.x=readDouble();
					parent.y=readDouble();
					parent.stance=readDouble();
					parent.z=readDouble();
					skipBytes(9);
				}
				else
					skipBytes(41);
				break;
			case 0x0e:
				
				if (!isServerTunnel) {
					if (parent.getGroup()<0) {
						skipBytes(11);
						removeBytes(12);
						break;
					}
					@SuppressWarnings("unused")
					int status = readByte();
					int xC0e = readInt();
					int yC0e = (int)readByte();
					int zC0e = readInt();
					readByte();
					if (server.chests.hasLock(xC0e,yC0e,zC0e)&&!parent.isAdmin()) {
						removeBytes(12);
						break;
					}
					if (parent.destroy) {
						byte[] cpyPacket = new byte[12];
						byte[] cpyPacket2 = new byte[12];
						System.arraycopy(buf, r-12, cpyPacket, 0, 12);
						cpyPacket[1] = 1;
						cpyPacket2=cpyPacket.clone();
						cpyPacket2[1]=3;
						addPacket(cpyPacket);
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket.clone());
						addPacket(cpyPacket2);
					}
				}
				else
					skipBytes(11);
				break;
			//CHECK THIS
			case 0x0f:
				short block=readShort();
				if (!isServerTunnel) {
					if (server.blockFirewall.checkCheck(block)||parent.getGroup()<0) {
						boolean allowed=server.blockFirewall.checkAllowed(parent, block);
						if (!allowed||parent.getGroup()<0) {
							//Remove the packet! : )
							int coord_x = readInt();
							if (!allowed && coord_x!=-1) {
								//printStream(buf);
								server.sendToAll(String.format(server.l.get("BAD_BLOCK"), parent.getName(),Short.toString(block)));
								//server.runCommand("say [ALERT]" + parent.getName() + " tried to create illegal block #" + block + "!");
							}
							skipBytes(6);
							removeBytes(13);
							break;
						}
					}
					if (block==54) {
						//Check if lock is ready and allowed
						if (parent.attemptLock) {
							//calculate coordinates
							int xC0f = readInt();
							int yC0f = (int)readByte();
							int zC0f = readInt();
							int dir = readByte();
							switch (dir) {
							case 0:
								yC0f--;break;
							case 1:
								yC0f++;break;
							case 2:
								zC0f--;break;
							case 3:
								zC0f++;break;
							case 4:
								xC0f--;break;
							case 5:
								xC0f++;break;
							}
							//create chest entry
							if (server.chests.hasLock(xC0f,yC0f,zC0f)) {
								parent.addMessage("This block is locked already!");
								parent.attemptLock=false;
								break;
							}
							if (!server.chests.giveLock(parent.getName().toLowerCase(), xC0f, yC0f, zC0f, false)) 	
								parent.addMessage("You already have a lock, or this block is locked already!");
							else
								parent.addMessage("Your locked chest is created! Do not add another chest to it!");
							parent.attemptLock=false;
						}
						else 
							skipBytes(10);
					}
					else
						skipBytes(10);
				}
				else 
					skipBytes(10);
				break;
			case 0x10:
				skipBytes(6);
				break;
			case 0x11:
				skipBytes(5);
				break;
			case 0x12:
				skipBytes(5);
				break;
			case 0x15:
				skipBytes(22);
				if (parent.getGroup()<0) {
					removeBytes(23);
					break;
				}
				break;
			case 0x16:
				skipBytes(8);
				break;
			case 0x17:
				skipBytes(17);
				break;
			case 0x18:
				skipBytes(19);
				break;
			case 0x1D:
				skipBytes(4);
				break;
			case 0x1E:
				skipBytes(4);
				break;
			case 0x1F:
				skipBytes(7);
				break;
			case 0x20:
				skipBytes(6);
				break;
			case 0x21:
				skipBytes(9);
				break;
			case 0x22:
				skipBytes(18);
				break;
			case 0x32:
				skipBytes(9);
				break;
			case 0x35:
				skipBytes(11);
				break;
			case 0x1c:
				skipBytes(10);
				break;
			case 0x27:
				skipBytes(8);
				break;
			case 0x02:
				oldCursor = r-1;
				if (!isServerTunnel) {
					if (!parent.setName(readString()))
						removeBytes(r-oldCursor);
				}
				else {
					readString();
				}
				break;
			case 0x03:
				oldCursor = r-1;
				String msg = readString();
				if (isServerTunnel && server.options.useMsgFormats) {
					if (msg.startsWith("<")) {
						try {
							String nameTok = msg.substring(1);
							if (nameTok.startsWith("§"))
								nameTok = msg.substring(3);
							int sidx = nameTok.indexOf("§");
							if (sidx>0)
								nameTok = nameTok.substring(0,sidx);
							else {
								sidx = nameTok.indexOf(">");
								nameTok = nameTok.substring(0,sidx);
							}
							Player p = server.findPlayerExact(nameTok);
							
							if (p!=null) {
								int idx = msg.indexOf(">");
								msg = msg.substring(idx+1);
								if (server.options.msgFormat.equals("") || server.options.msgFormat==null) {
									server.options.msgFormat="<§%3$s%1$s>§f";
								}
								if (server.options.msgTitleFormat.equals("") || server.options.msgTitleFormat==null) {
									server.options.msgTitleFormat="<§%3$s[%2$s]%1$s>§f";
								}
								
								String color = "f";
								String title = "";
								String format = server.options.msgFormat;
								if (p.groupObject!=null) {
									color = p.groupObject.getColor();
									if (p.groupObject.showTitle()) {
										title = p.groupObject.getName();
										format = server.options.msgTitleFormat;
									}
								}
								msg = String.format(format, p.getName(),title,color) + msg;
								parent.addMessage(msg);
							}
							removeBytes(r-oldCursor);
						}
						catch (Exception e) {
							System.out.println("[SimpleServer] There is an error in your msgFormat/msgTitleFormat settings!");
						}
					}
				}
				
				

				if (!isServerTunnel) {
					if (parent.isMuted() && !msg.startsWith("/") && !msg.startsWith("!")) {
						removeBytes(msg.length()+2+1);
						parent.addMessage("You are muted! You may not send messages to all players.");
						break;
					}
					if (msg.equalsIgnoreCase("/home")) {
						if (!server.cmdAllowed("home", parent))
							removeBytes(msg.length()+2+1);
						else
							break;
					}
					
					if (msg.startsWith("!") || (server.options.useSlashes && msg.startsWith("/"))) {											
						boolean remove = parent.parseCommand(msg);
						//Remove the packet! : )
						if (remove)
							removeBytes(msg.length()+2+1);
					}
				}
				break;
			case (byte)0xff:
				String discMsg = readString();
				if (discMsg.startsWith("Took too long")) {
					server.addRobot(parent);
				}
				parent.delayClose();
				break;
			case 0x01:
				readInt();
				readString();
				readString();
				readInt();
				readInt();
				readByte();
				break;
			case (byte)0x14:
				readInt();
				readString();
				skipBytes(16);
				break;
			//THIS DOESNT WORK :(
			case (byte)0x34:
				skipBytes(8);
				short chunkSize34 = readShort();
				skipBytes(chunkSize34*4);
				break;
			case (byte)0x33:
				skipBytes(13);
				int chunkSize33 = readInt();
				skipBytes(chunkSize33);
				break;
			case 0:
				break;
			default:
				byte[] cpy = new byte[a];
				System.arraycopy(buf, 0, cpy, 0, a);
				String streamType="PlayerStream";
				if (isServerTunnel)
					streamType="ServerStream";
				new Thread(new EOFWriter(cpy, history ,null,streamType + " " + parent.getName() + " packetid: " + packetid + " totalsize: " + r + " amt: " + a)).start();
				throw new InterruptedException();
		}
	}
	
	private void printStream(byte[] stream) {
		for(int i=0;i<stream.length;i++) {
			System.out.print(Byte.toString(stream[i]) + " ");
		}
		System.out.println();
	}
	
	private int readMore() throws InterruptedException, IOException {
		int tmp=0;
		int avail;
		try {
			lock.acquire();
			avail = in.available();	
			if (avail==0)
				Thread.sleep(20);
			if (a+avail>buf.length)
				avail=buf.length-a;
			if (avail>0)
				tmp =in.read(buf, a, avail);
			if (tmp>0) {
				a+=tmp;
				lastRead=System.currentTimeMillis();
			}
			lock.release();
			return a;
		} catch (IOException e) {
			lock.release();
			throw e;
		} catch (InterruptedException e) {
			lock.release();
			throw e;
		}
		
	}
	private int readMore(int num) throws InterruptedException, IOException {
		int tmp=0;
		try {
			lock.acquire();
			tmp =in.read(buf, a, num);
			if (tmp>0) {
				a+=tmp;
				lastRead=System.currentTimeMillis();
			}
			lock.release();
			return a;
		} catch (IOException e) {
			lock.release();
			throw e;
		} catch (InterruptedException e) {
			lock.release();
			throw e;
		}
		
	}
	protected boolean ensureRead(int n) throws InterruptedException, IOException {
		
		if (r+n>BUF_SIZE)
			return false;
		if (a>=r+n)
			return true;
		while(a<r+n) {
			if (parent.isClosed() || Thread.interrupted())
				throw new InterruptedException();
			//readMore();
			//System.out.println((r+n) + " " + a);
			readMore((r+n)-a);
		}
		return true;
	}
	protected String readString() throws IOException, InterruptedException {
		ensureRead(2);
		byte[] strLenBytes = readBytes(2);
		short strLen = bytesToShort(strLenBytes);
		ensureRead(strLen);
		byte[] cpy = new byte[strLen+3];
		System.arraycopy(buf, r-3, cpy, 0, strLen+3);
		r+=strLen;
		DataInputStream s = new DataInputStream(new ByteArrayInputStream(cpy));
		s.readByte();
		String str = s.readUTF();
		s.close();
		return str;
	}
	protected int readInt() throws IOException, InterruptedException {
		ensureRead(4);
		byte[] cpy = new byte[4];
		cpy[0] = buf[r];
		cpy[1] = buf[r+1];
		cpy[2] = buf[r+2];
		cpy[3] = buf[r+3];
		r+=4;
		return bytesToInt(cpy);
	}
	protected double readDouble() throws IOException, InterruptedException {
		ensureRead(8);
		byte[] cpy = new byte[8];
		cpy[0] = buf[r];
		cpy[1] = buf[r+1];
		cpy[2] = buf[r+2];
		cpy[3] = buf[r+3];
		cpy[4] = buf[r+4];
		cpy[5] = buf[r+5];
		cpy[6] = buf[r+6];
		cpy[7] = buf[r+7];
		r+=8;
		return bytesToDouble(cpy);
	}
	protected short readShort() throws IOException, InterruptedException {
		ensureRead(2);
		byte[] cpy = new byte[2];
		cpy[0] = buf[r];
		cpy[1] = buf[r+1];
		r+=2;
		return bytesToShort(cpy);
	}
	protected byte[] readBytes(int n) throws IOException, InterruptedException {
		ensureRead(n);
		byte[] cpy = new byte[n];
		System.arraycopy(buf, r, cpy, 0, n);
		r+=n;
		return cpy;
	}
	protected byte readByte() throws IOException, InterruptedException {
		ensureRead(1);
		return buf[r++];
	}
	protected void skipBytes(int n) {
		r+=n;
	}
	protected void removeBytes(int n) throws IOException, InterruptedException {
		ensureRead(0);
		lock.acquire();
		if (a-r!=0) {
			//byte[] cpy = new byte[a-r];
			System.arraycopy(buf, r, buf, r-n, a-r);
			//System.arraycopy(cpy, 0, buf, r-n, a-r);
		}
		r-=n;
		a-=n;
		lock.release();
	}
	private short bytesToShort(byte[] data) 
	{       
		bb.clear();
		bb.put(data[0]);
		bb.put(data[1]);
		short s = bb.getShort(0);
		return s;
	}
	private int bytesToInt(byte[] data) 
	{       
		bb.clear();
		bb.put(data[0]);
		bb.put(data[1]);
		bb.put(data[2]);
		bb.put(data[3]);
		int s = bb.getInt(0);
		return s;
	}
	private double bytesToDouble(byte[] data) 
	{       
		bb.clear();
		bb.put(data[0]);
		bb.put(data[1]);
		bb.put(data[2]);
		bb.put(data[3]);
		bb.put(data[4]);
		bb.put(data[5]);
		bb.put(data[6]);
		bb.put(data[7]);
		double s = bb.getDouble(0);
		return s;
	}
	

}
