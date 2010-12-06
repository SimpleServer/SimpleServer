package mcwrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import mcwrapper.files.EOFWriter;

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
	byte[] test;
	
	Player parent;
	
	public StreamTunnel(InputStream in, OutputStream out, boolean isServerTunnel, Player p) {
		this.in=in;
		this.out=out;
		parent=p;
		buf = new byte[BUF_SIZE];
		inGame=false;
		lock = new Semaphore(1);
		debug=false;
		this.isServerTunnel=isServerTunnel;
	}
	public StreamTunnel(InputStream in, OutputStream out, boolean isServerTunnel, Player p, int byteThreshold) {
		this.in=in;
		this.out=out;
		parent=p;
		buf = new byte[BUF_SIZE];
		inGame=false;
		lock = new Semaphore(1);
		debug=false;
		this.isServerTunnel=isServerTunnel;
		BYTE_THRESHOLD= byteThreshold;
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
		    // collect all the bytes waiting on the input stream
			r=0;
			a=0;
			lastRead=System.currentTimeMillis();
		    while (!parent.isClosed() && !Thread.interrupted()) {
		    	lock.acquire();
		    	if (in.available()>0) {
		    		lastRead=System.currentTimeMillis();
		    		int avail = in.available();
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
		    	
		    	if (System.currentTimeMillis()-lastRead>10000) {
		    		System.out.println("Disconnecting + " + parent.getIPAddress() + " due to inactivity.");
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
		    	if (r>0) {
		    		if (r<=a) {
		    			out.write(buf, 0, r);
		    			if (isServerTunnel) {
		    				//Test
		    				if (parent.warpCoords!=null) {
		    					ByteBuffer bb = ByteBuffer.allocate(42);
		    					bb.order(ByteOrder.BIG_ENDIAN);
		    					bb.put((byte)0x0d);
		    					bb.putDouble(parent.warpCoords[0]);
		    					bb.putDouble(parent.warpCoords[1]);
		    					bb.putDouble(parent.warpCoords[3]);
		    					bb.putDouble(parent.warpCoords[2]);
		    					bb.putFloat(0);
		    					bb.putFloat(0);
		    					bb.put((byte) 0);
		    					out.write(bb.array());
		    					parent.warpCoords=null;
		    				}
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
		    				//Stuff for client
		    				while (!packetBuffer.isEmpty()) {
		    					byte[] b = packetBuffer.removeFirst();
		    					out.write(b);
		    				}
		    			}
		    			if (r<a) {
			    			byte[] cpy = new byte[a-r];
			    			System.arraycopy(buf, r, cpy, 0, a-r);
			    			System.arraycopy(cpy, 0, buf, 0, a-r);
		    			}
		    			a-=r;
		    			r=0;
		    		}
		    		else {
		    			out.write(buf, 0, a);
		    			r-=a;
		    			a=0;
		    		}
		    	}
		    	Thread.sleep(20);
		    }
		}
		catch(InterruptedException e1) {
			//We don't care about an Interrupted Exception.
			//Don't even print out to the console that we received the exception.
			//We are only interrupted if we are closing.
			//e1.printStackTrace();
			parent.close();
			try {in.close();} catch (IOException e2) {}
			try {out.close();} catch (IOException e2) {}
			return;
		}
		catch(Exception e) {
			e.printStackTrace();
			parent.close();
			try {in.close();} catch (IOException e2) {}
			try {out.close();} catch (IOException e2) {}
			return;
		}
		parent.close();
		try {in.close();} catch (IOException e2) {}
		try {out.close();} catch (IOException e2) {}
		return;
	}
	private byte[] makePacket(String msg) {
		return makePacket((byte)0x03,msg);
	}
	private byte[] makePacket(byte packetid, String msg) {
		int len = msg.length();
		len+=3;
		byte[] r = new byte[len];
		r[0] = packetid;
		putShort(r,1,(short)msg.length());
		System.arraycopy(msg.getBytes(), 0, r, 3, msg.length());
		return r;
	}
	private void addPacket(byte[] packet) {
		packetBuffer.add(packet);
	}
	static void putShort(byte[] b, int off, short val) {
	    b[off + 1] = (byte) (val >>> 0);
	    b[off + 0] = (byte) (val >>> 8);
	}
	protected void parsePacket(int packetid) throws IOException, InterruptedException {
		int oldCursor=0;
		switch (packetid) {
		
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
						skipBytes(3);
					}
				}
				if (parent.getRank()<0) {
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
				if (parent.parent.chests.hasLock(xC3b,yC3b,zC3b)) {
					if (!parent.parent.chests.ownsLock(xC3b,yC3b,zC3b,parent.getName()) || parent.getName()==null || parent.getRank()<2) {
						removeBytes(r-oldCursor);
						break;
					}
				}
				if (parent.getRank()<0) {
					removeBytes(r-oldCursor);
					break;
				}
				break;
			case 0x06:
				skipBytes(12);
				break;
			case 0x0b:
				if (!isServerTunnel) {
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
				if (!isServerTunnel) {
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
					if (parent.getRank()<0) {
						skipBytes(11);
						removeBytes(12);
						break;
					}
					int status = readByte();
					int xC0e = readInt();
					int yC0e = (int)readByte();
					int zC0e = readInt();
					readByte();
					if (parent.parent.chests.hasLock(xC0e,yC0e,zC0e)&&parent.getRank()<2) {
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
					if (parent.parent.blockFirewall.checkCheck(block)||parent.getRank()<0) {
						if (!parent.parent.blockFirewall.checkAllowed(parent.getName(), block)||parent.getRank()<0) {
							//Remove the packet! : )
							if (!parent.parent.blockFirewall.checkAllowed(parent.getName(), block))
								parent.parent.runCommand("say [ALERT]" + parent.getName() + " tried to create illegal block #" + block + "!");
							skipBytes(10);
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
							if (parent.parent.chests.hasLock(xC0f,yC0f,zC0f)) {
								parent.addMessage("This block is locked already!");
								parent.attemptLock=false;
								break;
							}
							if (!parent.parent.chests.giveLock(parent.getName().toLowerCase(), xC0f, yC0f, zC0f, false)) 	
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
				if (parent.getRank()<0) {
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
			case 0x02:
				if (!isServerTunnel) {
					parent.setName(readString());
				}
				else {
					readString();
				}
				break;
			case 0x03:
				String msg = readString();
				if (!isServerTunnel) {
					if (parent.isMuted() && !msg.startsWith("/") && !msg.startsWith("!")) {
						removeBytes(msg.length()+2+1);
						parent.addMessage("You are muted! You may not send messages to all players.");
						break;
					}
					if (msg.startsWith("/home")) {
						if (parent.getRank()<parent.parent.options.homeCommandRank)
							removeBytes(msg.length()+2+1);
					}
					if (msg.startsWith("!")) {											
						parent.parseCommand(msg);
						//Remove the packet! : )
						removeBytes(msg.length()+2+1);
					}
				}
				break;
			case (byte)0xff:
				readString();
				parent.delayClose();
				break;
			case 0x01:
				readInt();
				readString();
				readString();
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
				new Thread(new EOFWriter(cpy, cpy ,null,"PlayerStream " + parent.getName() + " packetid: " + packetid + " totalsize: " + r + " amt: " + a)).start();
				parent.close();
				return;
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
			if (a+avail>buf.length)
				avail=buf.length-a;
			if (in.available()>0)
				tmp =in.read(buf, a, avail);
			if (tmp>0)
				a+=tmp;
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
			readMore();
		}
		return true;
	}
	protected String readString() throws IOException, InterruptedException {
		ensureRead(2);
		short strLen = readShort();
		ensureRead(strLen);
		byte[] cpy = new byte[strLen];
		System.arraycopy(buf, r, cpy, 0, strLen);
		r+=strLen;
		String str = new String(cpy);
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
			byte[] cpy = new byte[a-r];
			System.arraycopy(buf, r, cpy, 0, a-r);
			System.arraycopy(cpy, 0, buf, r-n, a-r);
		}
		r-=n;
		a-=n;
		lock.release();
	}
	private short bytesToShort(byte[] data) 
	{       
        ByteBuffer bb = ByteBuffer.allocate(2);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.put(data[0]);
		bb.put(data[1]);
		short s = bb.getShort(0);
		return s;
	}
	private int bytesToInt(byte[] data) 
	{       
        ByteBuffer bb = ByteBuffer.allocate(4);
		bb.order(ByteOrder.BIG_ENDIAN);
		bb.put(data[0]);
		bb.put(data[1]);
		bb.put(data[2]);
		bb.put(data[3]);
		int s = bb.getInt(0);
		return s;
	}
	private double bytesToDouble(byte[] data) 
	{       
        ByteBuffer bb = ByteBuffer.allocate(8);
		bb.order(ByteOrder.BIG_ENDIAN);
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
