package mcwrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import mcwrapper.files.BlockFirewallList;
import mcwrapper.files.FileLoader;
import mcwrapper.files.IPBanLoader;
import mcwrapper.files.IPRankList;
import mcwrapper.files.KitList;
import mcwrapper.files.MOTDLoader;
import mcwrapper.files.MuteLoader;
import mcwrapper.files.RankList;
import mcwrapper.files.ChestList;
import mcwrapper.files.RulesLoader;
import mcwrapper.files.WarpList;
import mcwrapper.files.WhitelistLoader;



public class Server extends Thread {
	
	@SuppressWarnings("unused")
	private static Server server;
	
	boolean open = true;
	boolean debug = false;
	ServerSocket socket;
	
	Semaphore saveLock = new Semaphore(1);
	public BlockFirewallList blockFirewall;
	public RankList ranks;
	public MOTDLoader motd;
	public KitList kits;
	public WarpList warps;
	public ChestList chests;
	public RulesLoader rules;
	public IPRankList ipRanks;
	public IPBanLoader ipBans;
	public WhitelistLoader whitelist;
	public MuteLoader mutelist;
	public Options options;
	
	InputStreamRouter input;
	
	Thread backupThread;
	Thread autoSaveThread;
	Thread autoRestartThread;
	Thread playerScannerThread;
	
	ServerBackup backup;
	ServerAutoSave autosave;
	ServerAutoRestart autoRestart;
	PlayerScanner playerScanner;
	RequestTracker requestTracker;
	boolean requireBackup=false;
	boolean isSaving=false;
	boolean isRestarting=false;
	boolean useDev=false;
	
	LinkedList<FileLoader> resources = new LinkedList<FileLoader>();
	LinkedList<Player> players = new LinkedList<Player>();
	Process p;
	
	public static void main(String[] args)
    {        
		
        server = new Server();
    }
	
	public int numPlayers() {
		int n=0;
		for (Player i: players) {
			if (i.getName()!=null) {
				n++;
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
	public void banKickIP(String ipAddress) {
		if (!isIPBanned(ipAddress))
			ipBans.addBan(ipAddress);
		for (Player p: players) {
			if (p.extsocket.getInetAddress().getHostAddress().equals(ipAddress))
				p.kick("Banned: Too many connections!");
		}
	}
	public void runCommand(String msg) {
		input.runCommand(msg);
	}
	public void notifyClosed(Player player) {
		players.remove(player);
	}
	public void stopServer() {
		server.start();
	}
	public void loadAll() {
		for(FileLoader i: resources) {
			i.load();
		}
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
	public boolean isMuted(String name) {
		if (name!=null)
			return mutelist.isMuted(name);
		return true;
	}
	public void mute(String userName) {
		if (userName!=null)
			mutelist.addName(userName);
	}
	public void unmute(String userName) {
		if (userName!=null)
			mutelist.removeName(userName);
	}
	public void kick(String userName, String msg) {
		for(Player i: players) {
			if (i.getName()!=null) {
				if (i.getName().toLowerCase().compareTo(userName.toLowerCase())==0) {
					i.kick(msg);
				}
			}
		}
	}
	public void kick(String userName) {
		kick(userName,"");
	}
	Thread socketThread;
	public void openSocket() {
		System.out.println(">> Starting SimpleServer RC 5.9");
        open=true;
        if (socketThread!=null)
        	socketThread.interrupt();
        socketThread=new Thread(new SocketThread(this));
        socketThread.start();
	}
	public void forceRestart() {
		new Thread(new ForceRestart(this)).start();
	}
	public void setRank(String name) {
		for(Player i: players) {
			if (i.getName()!=null) {
				if (i.getName().toLowerCase().compareTo(name.toLowerCase())==0){
					i.setRank();
				}
			}
		}
	}
	public void restart() {
		isRestarting=true;
		open=false;
		for (Player i: players) {
			if (!i.isClosed())
				i.kick("Server Restarting!");
			else
				players.remove(i);
		}
		
		try {
			socket.close();
		} catch (Exception e) {}
		
		runCommand("stop");
		while (true){
			try {
				p.waitFor();
				break;
			} catch (InterruptedException e) {}
		}
		closePipes();
		
		startMinecraft();
		setShutdownHook();
		
		isRestarting=false;
		openSocket();
		return;
	}
	Thread t;
	Thread t2;
	Thread t3;
	private void getPipes() {
		if (t!=null)
			t.interrupt();
		if (t2!=null)
			t2.interrupt();
		t = new Thread(new ErrorStreamRouter(p.getErrorStream(),this));
		t2 = new Thread(new ErrorStreamRouter(p.getInputStream(),this));
		
		if (input==null) {
			input=new InputStreamRouter(System.in,p.getOutputStream(),this);
			t3 = new Thread(input);
			t3.start();
		}
		else {
			input.out = p.getOutputStream();
		}
		t.start();
		t2.start();
		
	}
	
	Thread shutDownHook;
	private void setShutdownHook() {
		if (shutDownHook!=null)
			Runtime.getRuntime().removeShutdownHook(shutDownHook);
		Runtime.getRuntime().addShutdownHook(shutDownHook=new Thread(new ShutdownHook(p,this)));
		
	}
	private Server() {
		options=new Options();
		options.load();
		options.saveMinecraftProperties();
		startProcess();
		getPipes();

		addShutdownHook();
		resources.add(ipRanks = new IPRankList(this));
		resources.add(chests = new ChestList(this));
		resources.add(warps = new WarpList(this));
		resources.add(blockFirewall = new BlockFirewallList(this));
		resources.add(ranks = new RankList(this));
		resources.add(motd = new MOTDLoader());
		resources.add(rules = new RulesLoader());
		resources.add(kits = new KitList(this));
		resources.add(ipBans = new IPBanLoader());
		resources.add(whitelist = new WhitelistLoader());
		resources.add(mutelist = new MuteLoader());
		loadAll();
		
		backup=new ServerBackup(this);
		backupThread = new Thread(backup);
		//if (options.autoBackup)
			backupThread.start();
		autosave=new ServerAutoSave(this);
		autoSaveThread = new Thread(autosave);
		//if (options.autoSave)
			autoSaveThread.start();
		autoRestart=new ServerAutoRestart(this);
		autoRestartThread=new Thread(autoRestart);
		//if (options.autoRestart)
			autoRestartThread.start();
			
		playerScanner=new PlayerScanner(this);
		playerScannerThread=new Thread(playerScanner);
		playerScannerThread.start();
		
		requestTracker = new RequestTracker(this);
		new Thread(requestTracker).start();
        openSocket();
    }

	@Override
	public void run() {
		try {
			saveLock.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		isRestarting=true;
		open=false;
		for (Player i: players) {
			i.kick("Server Shutting Down!");
		}
		socketThread.interrupt();
		try {
			socket.close();
		} catch (Exception e) {}
		System.out.println("Stopping Server...");
		backupThread.interrupt();
		autoSaveThread.interrupt();
		autoRestartThread.interrupt();
		playerScannerThread.interrupt();
		saveAll();
		runCommand("stop");
		long startShutdown = System.currentTimeMillis();
		while (System.currentTimeMillis()-startShutdown<10000){
			try {
				p.exitValue();
				break;
			} catch (Exception e) {}
		}
		try {
			Runtime.getRuntime().removeShutdownHook(shutDownHook);
		}
		catch (Exception e) {e.printStackTrace();}
		System.out.println("Server stopped successfully!");
		System.exit(0);
	}

	
}
