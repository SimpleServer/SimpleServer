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
import java.net.ServerSocket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import simpleserver.files.AdminLog;
import simpleserver.files.BlockFirewallList;
import simpleserver.files.ChestList;
import simpleserver.files.CommandList;
import simpleserver.files.FileLoader;
import simpleserver.files.GroupList;
import simpleserver.files.IPBanLoader;
import simpleserver.files.IPMemberList;
import simpleserver.files.ItemWatchList;
import simpleserver.files.KitList;
import simpleserver.files.Language;
import simpleserver.files.MOTDLoader;
import simpleserver.files.MemberList;
import simpleserver.files.MuteLoader;
import simpleserver.files.RobotLoader;
import simpleserver.files.RulesLoader;
import simpleserver.files.WhitelistLoader;
import simpleserver.threads.C10TThread;
import simpleserver.threads.ErrorStreamRouter;
import simpleserver.threads.InputStreamRouter;
import simpleserver.threads.MinecraftMonitor;
import simpleserver.threads.PlayerScanner;
import simpleserver.threads.ServerAutoRestart;
import simpleserver.threads.ServerAutoSave;
import simpleserver.threads.ServerBackup;
import simpleserver.threads.SocketThread;




public class Server {
	private static String version = "RC 6.6.6_stable";
	private static String license = "SimpleServer -- Copyright (C) 2010 Charles Wagner Jr.";
	private static String warranty = "This program is licensed under The MIT License.\nSee file LICENSE for details.";
	@SuppressWarnings("unused")
	private static Server server;
	
	boolean open = true;
	boolean debug = false;
	public ServerSocket socket;
	ServerSocket rconSocket;
	
	LinkedList<String> outputLog = new LinkedList<String>();
	
	public Semaphore saveLock = new Semaphore(1);
	public BlockFirewallList blockFirewall;
	//public RankList ranks;
	public GroupList groups;
	public MemberList members;
	public RobotLoader robots;
	public MOTDLoader motd;
	public KitList kits;
	//public WarpList warps;
	public ChestList chests;
	public RulesLoader rules;
	public IPMemberList ipMembers;
	public IPBanLoader ipBans;
	public ItemWatchList itemWatch;
	public WhitelistLoader whitelist;
	public MuteLoader mutelist;
	public Options options;
	public CommandList commands;
	
	public AdminLog adminLog;
	public Thread adminLogThread;
	
	public Language l;
	
	InputStreamRouter input;
	
	Thread backupThread;
	Thread autoSaveThread;
	Thread autoRestartThread;
	Thread playerScannerThread;
	Thread shutDownHook;
	Thread socketThread;
	
	C10TThread c10t;
	Thread c10tThread;
	
	ServerBackup backup;
	ServerAutoSave autosave;
	ServerAutoRestart autoRestart;
	PlayerScanner playerScanner;
	MinecraftMonitor minecraftMonitor;
	RequestTracker requestTracker;
	
	
	
	ForceShutdown forceShutdown;
	ForceRestart forceRestart;
	
	boolean requireBackup=false;
	boolean isSaving=false;
	boolean isRestarting=false;
	boolean waitingForStart=false;
	boolean useDev=false;
	
	LinkedList<FileLoader> resources = new LinkedList<FileLoader>();
	//LinkedList<Player> players = new LinkedList<Player>();
	LinkedList<Rcon> rcons = new LinkedList<Rcon>();
	
	
	//Minecraft Process
	public Process p;
	//Pipe Threads
	Thread t;
	Thread t2;
	Thread t3;
	
	public static void main(String[] args)
    {        
		System.out.println(license);
		System.out.println(warranty);
		System.out.println(">> Starting SimpleServer " + version);
        server = new Server();
    }
	/*
	 * Essential Functions
	 */
	private Server() {
		l = new Language();
		l.load();
		options=new Options(this);
		options.load();
		
		new Thread(new RconServer(this,false)).start();
		
		PlayerFactory.initialize(this, options.maxPlayers);
		
		startMinecraft();
		setShutdownHook();
		initResources();
		loadAll();
		
		backup=new ServerBackup(this);
		backupThread = new Thread(backup);
		backupThread.start();
		
		autosave=new ServerAutoSave(this);
		autoSaveThread = new Thread(autosave);
		autoSaveThread.start();
		
		autoRestart=new ServerAutoRestart(this);
		autoRestartThread=new Thread(autoRestart);
		autoRestartThread.start();
			
		playerScanner=new PlayerScanner(this);
		playerScannerThread=new Thread(playerScanner);
		playerScannerThread.start();
		
		forceShutdown = new ForceShutdown(this);
		forceRestart = new ForceRestart(this);
		
		requestTracker = new RequestTracker(this);
		new Thread(requestTracker).start();
		
        openSocket();
        
        if (options.c10tArgs!=null && options.c10tArgs.trim().length()>0) {
			c10t = new C10TThread(this,options.c10tArgs);
			c10tThread = new Thread(c10t);
			c10tThread.start();
		}
    }
	private void initResources() {
		resources.add(robots = new RobotLoader());
		resources.add(ipMembers = new IPMemberList(this));
		resources.add(chests = new ChestList(this));
		resources.add(commands = new CommandList(this));
		//resources.add(warps = new WarpList(this));
		resources.add(blockFirewall = new BlockFirewallList(this));
		resources.add(itemWatch = new ItemWatchList(this));
		//resources.add(ranks = new RankList(this));
		resources.add(groups = new GroupList(this));
		resources.add(members = new MemberList(this));
		resources.add(motd = new MOTDLoader());
		resources.add(rules = new RulesLoader());
		resources.add(kits = new KitList(this));
		resources.add(ipBans = new IPBanLoader());
		resources.add(whitelist = new WhitelistLoader());
		resources.add(mutelist = new MuteLoader());
		
		
	}
	private void startMinecraft() {
		options.saveMinecraftProperties();
		try {
			int minMemory = 1024;
			if (options.memory<minMemory) {
				minMemory=options.memory;
			}
			String alternateJar = "minecraft_server.jar";
			if (options.alternateJarFile.trim().length()!=0)
				alternateJar=options.alternateJarFile.trim();
			//String[] cmd = {"java", "-Xmx" + options.memory + "M","-Xms" + minMemory + "M", "-jar", alternateJar, "nogui"};
			String cmd = "java " + options.javaArguments + " -Xmx" + options.memory + "M -Xms" + minMemory + "M -jar " + alternateJar + " nogui";
			//String[] cmd = {"cmd", "/C", "java -Xmx" + options.memory + "M -Xms" + minMemory + "M -jar " + alternateJar + " nogui 2>test1.txt 1>test.txt"}; 
			
			p = Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("[SimpleServer] FATAL ERROR: Could not start minecraft_server.jar!");
			System.exit(-1);
		}
		t = new Thread(new ErrorStreamRouter(p.getErrorStream(),this));
		t2 = new Thread(new ErrorStreamRouter(p.getInputStream(),this));
		t.start();
		t2.start();
		
		if (t3==null) {
			input=new InputStreamRouter(System.in,p.getOutputStream(),this);
			t3 = new Thread(input);
			t3.start();
		}
		else {
			input.setOut(p.getOutputStream());
		}
		minecraftMonitor=new MinecraftMonitor(this);
		minecraftMonitor.start();
		
		waitingForStart=true;
		while(waitingForStart) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {}
		}
		
		adminLog = new AdminLog();
		adminLogThread = new Thread(adminLog);
		adminLogThread.start();
	}
	private void stopMinecraft() {
		try {
			runCommand("stop");
		} catch (InterruptedException e1) {	}
		long timer = System.currentTimeMillis();
		boolean stopped=false;
		while (System.currentTimeMillis()-timer<15*1000){
			try {
				p.exitValue();
				stopped=true;
				break;
			}
			catch(Exception e) {}
		}
		if (!stopped)
			p.destroy();
		t.interrupt();
		t2.interrupt();	
		
		adminLogThread.interrupt();
		adminLog=null;
		
	}
	
	public void restart() {
		isRestarting=true;
		open=false;
		try {
			socket.close();
		} catch (Exception e) {}
		
		kickAllPlayers("Server Restarting!");

		
		
		
		minecraftMonitor.interrupt();
		stopMinecraft();
		startMinecraft();
		setShutdownHook();
		
		isRestarting=false;
		openSocket();
	}
	private void kickAllPlayers(String msg) {
		if (msg==null)
			msg="";
		//synchronized(players) {
		for (Iterator<Player> itr = PlayerFactory.iterator(); itr.hasNext(); ) {
			Player p = itr.next();
			p.kick(msg);
			//itr.remove();
		}
		//}
	}
	private void kickAllRcons(String msg) {
		if (msg==null)
			msg="";
		synchronized(rcons) {
			for (Iterator<Rcon> itr = rcons.iterator(); itr.hasNext(); ) {
				Rcon p = itr.next();
				p.kick(msg);
				itr.remove();
			}
		}
	}
	public void stop() {
		saveAll();
		
		isRestarting=true;
		open=false;
		try {
			socket.close();
		} catch (Exception e) {}
		
		kickAllPlayers("Server shutting down!");
		kickAllRcons("Server shutting down!");
		System.out.println("Stopping Server...");
		minecraftMonitor.interrupt();
		stopMinecraft();
		Runtime.getRuntime().removeShutdownHook(shutDownHook);
		System.out.println("Server stopped successfully!");
		System.exit(0);
	}
	public void openSocket() {        
        if (socketThread!=null)
        	socketThread.interrupt();
        open=true;
        socketThread=new Thread(new SocketThread(this));
        socketThread.start();
	}
	public void stopServer() {
		
		new Thread(forceShutdown).start();
	}
	public void restartServer() {
		
		if (!isRestarting) {
			if (saveLock.tryAcquire())
				saveLock.release();
			else
				System.out.println("[SimpleServer] Server is currently Backing Up/Saving...");
			new Thread(forceRestart).start();
		}
		else
			p.destroy();
	}
	public void forceRestart() {
		restartServer();
	}
	private void setShutdownHook() {
		if (shutDownHook!=null)
			Runtime.getRuntime().removeShutdownHook(shutDownHook);
		Runtime.getRuntime().addShutdownHook(shutDownHook=new Thread(new ShutdownHook(p,this)));
		
	}
	public void addRobot(Player p) {
		robots.addRobot(p.extsocket.getInetAddress().getHostAddress());
	}
	public boolean isRobot(String ipAddress) {
		return robots.isRobot(ipAddress);
	}
	public void addRobotPort(int port) {
		robots.addRobotPort(port);
	}
	public void removeRobotPort(int port) {
		robots.removeRobotPort(port);
	}
	public Integer[] getRobotPorts() {
		if (robots!=null)
			return robots.getRobotPorts();
		return null;
	}
	public boolean cmdAllowed(String cmd, Player p) {
		return commands.checkPlayer(cmd, p);
	}
	public String getCommands(Player p) {
		return commands.getCommands(p);
	}
	public int numPlayers() {
		int n=0;
		for (Iterator<Player> itr = PlayerFactory.iterator(); itr.hasNext();) {
			Player i = itr.next();
			if (i!=null) {
				if (i.getName()!=null) {
					n++;
				}
			}
		}
		return n;
	}
	public String getMOTD() {
		return motd.getMOTD();
	}
	public String getRules() {
		return rules.getRules();
	}
	public boolean isIPBanned(String ipAddress) {
		return ipBans.isBanned(ipAddress);
	}
	public void banKickIP(String ipAddress, String reason) throws InterruptedException {
		if (!isIPBanned(ipAddress))
			ipBans.addBan(ipAddress);
		adminLog.addMessage("IP Address " + ipAddress + " was banned:\t " + reason);
		for (Iterator<Player> itr = PlayerFactory.iterator(); itr.hasNext(); ) {
			Player p = itr.next();
			if (p.extsocket.getInetAddress().getHostAddress().equals(ipAddress)) {
				p.kick(reason);
				adminLog.addMessage("Player " + p.getName() + " was ip-banned:\t " + reason);
				//itr.remove();
			}
		}
		/*
		synchronized(players) {
			for (Iterator<Player> itr = players.iterator(); itr.hasNext(); ) {
				Player p = itr.next();
				if (p.extsocket.getInetAddress().getHostAddress().equals(ipAddress)) {
					p.kick(reason);
					adminLog.addMessage("Player " + p.getName() + " was ip-banned:\t " + reason);
					itr.remove();
				}
			}
		}
		*/
	}
	public void banKickIP(String ipAddress) throws InterruptedException {
		banKickIP(ipAddress,"Banned!");
	}
	public void banKick(String name, String msg) throws InterruptedException {
		if (name!=null) {
			runCommand("ban " + name);
			Player p = PlayerFactory.findPlayer(name);
			if (p!=null) {
				adminLog.addMessage("Player " + p.getName() + " was banned:\t " + msg);
				p.kick(msg);
			}
			/*
			synchronized(players) {
				for (Iterator<Player> itr = players.iterator(); itr.hasNext(); ) {
					Player p = itr.next();
					if (p.getName().toLowerCase().equals(name.toLowerCase())) {
						adminLog.addMessage("Player " + p.getName() + " was banned:\t " + msg);
						p.kick(msg);
						itr.remove();
					}
				}
			}
			*/
		}
	}
	public void banKick(String name) throws InterruptedException {
		banKick(name,"Banned!");
	}
	public void runCommand(String msg) throws InterruptedException {
		if (input!=null)
			input.runCommand(msg);
	}
	public void sendToAll(String msg) throws InterruptedException {
		if (input!=null && msg!=null && !msg.equalsIgnoreCase(""))
			input.runCommand("say " + msg);
	}
	public void notifyClosed(Player player) throws InterruptedException {
		//synchronized(players) {
		//	players.remove(player);
		//}
		PlayerFactory.removePlayer(player);
	}
	/*
	public int getRank(String name) throws InterruptedException {
		playerLock.acquire();
		for(Player i: players) {
			if (i.getName()!=null) {
				if (i.getName().toLowerCase().compareTo(name.toLowerCase())==0){
					return i.getRank();
				}
			}
		}
		playerLock.release();
		return options.defaultRank;
	}
	*/
	public void loadAll() {
		for(FileLoader i: resources) {
			i.load();
		}
		l.load();
		options.load();
	}
	public void saveAll() {
		for(FileLoader i: resources) {
			i.save();
		}
		options.save();
	}
	public void forceBackup() {
		requireBackup=true;
		backupThread.interrupt();
	}
	
	public String findName(String prefix) throws InterruptedException {
		//synchronized(players) {
		/*
			for(Player i: players) {
				if (i.getName()!=null) {
					if (i.getName().toLowerCase().startsWith(prefix.toLowerCase().trim())) {
						return i.getName();
					}
				}
			}
		*/
		//}
		Player i = PlayerFactory.findPlayer(prefix);
		if (i!=null)
			return i.getName();
		else
			return null;
	}
	public Player findPlayer(String prefix) throws InterruptedException {
		//synchronized(players) {
		/*
			for(Player i: players) {
				if (i.getName()!=null) {
					if (i.getName().toLowerCase().startsWith(prefix.toLowerCase().trim())) {
						return i;
					}
				}
			}
		*/
		//}
			return PlayerFactory.findPlayer(prefix);
	}
	public Player findPlayerExact(String exact) throws InterruptedException {
		//synchronized(players) {
		/*
			for(Player i: players) {
				if (i.getName()!=null) {
					if (i.getName().equals(exact)) {
						return i;
					}
				}
			}
		*/
		//}
		return PlayerFactory.findPlayerExact(exact);
	}
	public String kick(String userName, String msg) throws InterruptedException {
		synchronized(PlayerFactory.playerLock) {
			Player p = PlayerFactory.findPlayer(userName);
			if (p!=null) {
				p.kick(msg);
				return p.getName();
			}
		}
		return null;
	}
	public String kick(String userName) throws InterruptedException {
		return kick(userName,"");
	}
	
	public void updateGroup(String name) throws InterruptedException {
		//synchronized(players) {
		Player p = PlayerFactory.findPlayer(name);
		if (p!=null) {
			p.updateGroup();
		}
		/*
			for(Player i: players) {
				if (i.getName()!=null) {
					if (i.getName().toLowerCase().compareTo(name.toLowerCase())==0){
						i.updateGroup();
					}
				}
			}
			*/
		//}
	}
	public int localChat(Player p, String msg) {
		String chat = "§7" + p.getName() + " says: " + msg;
		int j=0;
		if (p.getName()==null)
			return 0;
		//synchronized(players) {
			for(Iterator<Player> itr = PlayerFactory.iterator();itr.hasNext();) {
				Player i = itr.next();
				if (i.getName()!=null) {
					if (Math.abs(i.x-p.x)<options.localChatRadius &&Math.abs(i.y-p.y)<options.localChatRadius &&Math.abs(i.z-p.z)<options.localChatRadius) {
						i.addMessage(chat);
						if (p!=i)
							j++;
					}
				}
			}
		//}
		return j;
	}
	public void notifyClosedRcon(Rcon rcon) {
		synchronized(rcons) {
			rcons.remove(rcon);
		}
	}
	public void addOutputLine(String s) {
		synchronized (outputLog) {
			while (outputLog.size()>30)
				outputLog.remove();
			outputLog.add(s);
		}
	}
	public String[] getOutputLog() {
		synchronized (outputLog) {
			String [] a = new String[1];
			return outputLog.toArray(a);
		}
	}
	public boolean isRestarting() {
		return isRestarting;
	}
	public boolean requiresBackup() {
		return requireBackup;
	}
	public void requiresBackup(boolean b) {
		requireBackup=b;
	}
	public void isSaving(boolean b) {
		isSaving=b;
	}
	public void waitingForStart(boolean b) {
		waitingForStart=b;
	}
	public boolean isSaving() {
		return isSaving;
	}
	public boolean isOpen() {
		return open;
	}
	
}
