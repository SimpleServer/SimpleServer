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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Scanner;

import simpleserver.files.CommandList;

public class Options {

	
	
	public int port=25565;
	public int rconPort=25500;
	public int internalPort=25566;
	public int memory=1024;
	public int maxPlayers=16;
	
	public boolean exitOnFailure=true;
	public boolean guestsCanViewComplex=false;
	
	public boolean autoSave=true;
	public int autoSaveMins=5;
	public boolean autoBackup=true;
	public int autoBackupMins=30;
	public int keepBackupHours=2;
	public boolean autoRestart=false;
	public int autoRestartMins=240;
	
	public int c10tMins=60;
	public String c10tArgs="";
	public String rconPassword="";
	public String ipAddress="0.0.0.0";
	public String javaArguments="";
	
	public int createWarpRank=1;
	public int useWarpRank=1;
	public int warpPlayerRank=3;
	public int teleportRank=3;
	public int homeCommandRank=0;
	public int giveRank=1;
	public int givePlayerRank=2;
	public int setRankRank=3;
	public int muteRank=2;
	
	
	public int defaultGroup=0;	
	public int localChatRadius=30;
	
	
	public boolean useWhitelist=false;
	public boolean useSlashes=false;
	public boolean onlineMode=true;
	public boolean debug=false;
	public boolean experimental=false;
	public String levelName="world";
	public String alternateJarFile="";
	
	public boolean useSMPAPI=false;
	
	public boolean useMsgFormats=true;
	public String msgFormat="";
	public String msgTitleFormat="";
	
	Properties optionsLoader;
	Properties serverOptions;
	Server parent;
	
	public Options(Server s) {
		optionsLoader=new Properties();
		parent=s;
	}
	public void save() {
		save(false);
	}
	public void save(boolean createNew) {
		try {
			File file = new File("simpleserver.properties");
			if (!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream w = new FileOutputStream(file);
			optionsLoader.setProperty("port", Integer.toString(port));
			optionsLoader.setProperty("rconPort", Integer.toString(rconPort));
			optionsLoader.setProperty("memory", Integer.toString(memory));
			optionsLoader.setProperty("internalPort", Integer.toString(internalPort));
			
			optionsLoader.setProperty("autoSaveMins", Integer.toString(autoSaveMins));
			optionsLoader.setProperty("autoBackupMins", Integer.toString(autoBackupMins));
			optionsLoader.setProperty("autoRestartMins", Integer.toString(autoRestartMins));
			optionsLoader.setProperty("keepBackupHours", Integer.toString(keepBackupHours));
			optionsLoader.setProperty("maxPlayers", Integer.toString(maxPlayers));
			
			
			
			/*
			optionsLoader.setProperty("createWarpRank", Integer.toString(createWarpRank));
			optionsLoader.setProperty("useWarpRank", Integer.toString(useWarpRank));
			*/
			/*
			optionsLoader.setProperty("giveRank", Integer.toString(giveRank));
			optionsLoader.setProperty("muteRank", Integer.toString(muteRank));
			optionsLoader.setProperty("givePlayerRank", Integer.toString(givePlayerRank));
			optionsLoader.setProperty("setRankRank", Integer.toString(setRankRank));
			
			optionsLoader.setProperty("teleportRank", Integer.toString(teleportRank));
			optionsLoader.setProperty("warpPlayerRank", Integer.toString(warpPlayerRank));
			optionsLoader.setProperty("homeCommandRank", Integer.toString(homeCommandRank));
			*/
			optionsLoader.setProperty("defaultGroup", Integer.toString(defaultGroup));
			optionsLoader.setProperty("autoBackup", Boolean.toString(autoBackup));
			optionsLoader.setProperty("autoSave", Boolean.toString(autoSave));
			optionsLoader.setProperty("autoRestart", Boolean.toString(autoRestart));
			optionsLoader.setProperty("useWhitelist", Boolean.toString(useWhitelist));
			optionsLoader.setProperty("useSlashes", Boolean.toString(useSlashes));
			optionsLoader.setProperty("useSMPAPI", Boolean.toString(useSMPAPI));
			optionsLoader.setProperty("onlineMode", Boolean.toString(onlineMode));
			optionsLoader.setProperty("exitOnFailure", Boolean.toString(exitOnFailure));
			optionsLoader.setProperty("guestsCanViewComplex", Boolean.toString(guestsCanViewComplex));
			optionsLoader.setProperty("levelName", levelName);
			optionsLoader.setProperty("rconPassword", rconPassword);
			optionsLoader.setProperty("alternateJarFile", alternateJarFile);
			optionsLoader.setProperty("c10tArgs", c10tArgs);
			optionsLoader.setProperty("c10tMins", Integer.toString(c10tMins));
			optionsLoader.setProperty("localChatRadius", Integer.toString(localChatRadius));
			
			optionsLoader.setProperty("msgFormat", msgFormat);
			optionsLoader.setProperty("msgTitleFormat", msgTitleFormat);
			optionsLoader.setProperty("useMsgFormats", Boolean.toString(useMsgFormats));
			
			optionsLoader.store(w, "");
			if (createNew) { 
				System.out.println("Properties file not found! Created simpleserver.properties! Adjust values and then start the server again!");
				System.out.println("Press enter to continue...");
				Scanner in = new Scanner(System.in);
				in.nextLine();
				w.close();
				System.exit(0);
			}
			w.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not write the properties file!");
		}
		
	}
	private void conversion() {
		System.out.println("The Properties file format has changed! Command ranks are now set in command-list.txt!");
		System.out.println("Your previous settings for commands have been saved, and cleared from simpleserver.properties!");
		System.out.println("Press enter to continue...");
		CommandList cmds = new CommandList(parent);
		cmds.load();
		cmds.setRank("warptome", warpPlayerRank);
		cmds.setRank("warpmeto", warpPlayerRank);
		cmds.setRank("tp", teleportRank);
		cmds.setRank("home", homeCommandRank);
		cmds.setRank("give", giveRank);
		cmds.setRank("giveplayer", givePlayerRank);
		cmds.setRank("mute", muteRank);
		cmds.setRank("unmute", muteRank);
		cmds.setRank("setrank", setRankRank);
		optionsLoader.remove("warpPlayerRank");
		optionsLoader.remove("teleportRank");
		optionsLoader.remove("homeCommandRank");
		optionsLoader.remove("giveRank");
		optionsLoader.remove("givePlayerRank");
		optionsLoader.remove("muteRank");
		optionsLoader.remove("setRankRank");
		optionsLoader.remove("useWarpRank");
		optionsLoader.remove("createWarpRank");
		File f = new File("simpleserver.properties");
		f.delete();
		cmds.save();
		save();
		Scanner in = new Scanner(System.in);
		in.nextLine();
		System.exit(0);
	}
	
	public void saveMinecraftProperties() {
		try {
			File file = new File("server.properties");
			serverOptions = new Properties();
			if (!file.exists()) {
				file.createNewFile();
			}
			else {
				FileInputStream r = new FileInputStream(file);
				serverOptions.load(r);
				r.close();
			}
			FileOutputStream w = new FileOutputStream(file);
			serverOptions.setProperty("online-mode", Boolean.toString(onlineMode));
			serverOptions.setProperty("server-ip", "127.0.0.1");
			serverOptions.setProperty("server-port", Integer.toString(internalPort));
			serverOptions.setProperty("max-players", Integer.toString(maxPlayers));
			serverOptions.setProperty("level-name", levelName);
			serverOptions.store(w, "Generated by SimpleServer\r\nDO NOT EDIT THIS FILE!");
			w.close();
			
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not write Minecraft properties file!");
		}
	}
	
	public void load() {
		try {
			File file = new File("simpleserver.properties");
			if (!file.exists()) {
				file.createNewFile();
				save(true);
				return;
			}
			FileInputStream r = new FileInputStream(file);
			if (r==null || r.available()==0) {
				file.createNewFile();
				save(true);
				return;
			}
			
			
			optionsLoader.load(r);
			
			if (optionsLoader.getProperty("rconPassword")!=null)
				rconPassword = optionsLoader.getProperty("rconPassword");
			
			if (optionsLoader.getProperty("port")!=null)
				port = Integer.valueOf(optionsLoader.getProperty("port"));
			if (optionsLoader.getProperty("rconPort")!=null)
				rconPort = Integer.valueOf(optionsLoader.getProperty("rconPort"));
			if (optionsLoader.getProperty("memory")!=null)
				memory = Integer.valueOf(optionsLoader.getProperty("memory"));
			if (optionsLoader.getProperty("internalPort")!=null)
				internalPort = Integer.valueOf(optionsLoader.getProperty("internalPort"));
			if (optionsLoader.getProperty("autoSaveMins")!=null)
				autoSaveMins = Integer.valueOf(optionsLoader.getProperty("autoSaveMins"));
			if (optionsLoader.getProperty("autoBackupMins")!=null)
				autoBackupMins = Integer.valueOf(optionsLoader.getProperty("autoBackupMins"));
			if (optionsLoader.getProperty("autoRestartMins")!=null)
				autoRestartMins = Integer.valueOf(optionsLoader.getProperty("autoRestartMins"));
			if (optionsLoader.getProperty("keepBackupHours")!=null)
				keepBackupHours = Integer.valueOf(optionsLoader.getProperty("keepBackupHours"));
			if (optionsLoader.getProperty("maxPlayers")!=null)
				maxPlayers = Integer.valueOf(optionsLoader.getProperty("maxPlayers"));
			
			if (optionsLoader.getProperty("guestsCanViewComplex")!=null)
				guestsCanViewComplex = Boolean.valueOf(optionsLoader.getProperty("guestsCanViewComplex"));
			
			if (optionsLoader.getProperty("defaultGroup")!=null)
				defaultGroup = Integer.valueOf(optionsLoader.getProperty("defaultGroup"));
			if (optionsLoader.getProperty("autoBackup")!=null)
				autoBackup = Boolean.valueOf(optionsLoader.getProperty("autoBackup"));
			if (optionsLoader.getProperty("autoSave")!=null)
				autoSave = Boolean.valueOf(optionsLoader.getProperty("autoSave"));
			if (optionsLoader.getProperty("autoRestart")!=null)
				autoRestart = Boolean.valueOf(optionsLoader.getProperty("autoRestart"));
			if (optionsLoader.getProperty("useWhitelist")!=null)
				useWhitelist = Boolean.valueOf(optionsLoader.getProperty("useWhitelist"));
			if (optionsLoader.getProperty("useSlashes")!=null)
				useSlashes = Boolean.valueOf(optionsLoader.getProperty("useSlashes"));
			if (optionsLoader.getProperty("onlineMode")!=null)
				onlineMode = Boolean.valueOf(optionsLoader.getProperty("onlineMode"));
			if (optionsLoader.getProperty("debug")!=null)
				debug = Boolean.valueOf(optionsLoader.getProperty("debug"));
			if (optionsLoader.getProperty("useSMPAPI")!=null)
				useSMPAPI = Boolean.valueOf(optionsLoader.getProperty("useSMPAPI"));
			if (optionsLoader.getProperty("experimental")!=null)
				experimental = Boolean.valueOf(optionsLoader.getProperty("experimental"));
			if (optionsLoader.getProperty("exitOnFailure")!=null)
				experimental = Boolean.valueOf(optionsLoader.getProperty("exitOnFailure"));
			if (optionsLoader.getProperty("levelName")!=null)
				levelName = optionsLoader.getProperty("levelName");
			if (optionsLoader.getProperty("ipAddress")!=null)
				ipAddress = optionsLoader.getProperty("ipAddress");
			if (optionsLoader.getProperty("alternateJarFile")!=null)
				alternateJarFile = optionsLoader.getProperty("alternateJarFile");
			if (optionsLoader.getProperty("c10tArgs")!=null)
				c10tArgs = optionsLoader.getProperty("c10tArgs");
			if (optionsLoader.getProperty("c10tMins")!=null)
				c10tMins = Integer.valueOf(optionsLoader.getProperty("c10tMins"));
			if (optionsLoader.getProperty("localChatRadius")!=null)
				localChatRadius = Integer.valueOf(optionsLoader.getProperty("localChatRadius"));
			if (optionsLoader.getProperty("useMsgFormats")!=null)
				useMsgFormats = Boolean.valueOf(optionsLoader.getProperty("useMsgFormats"));
			if (optionsLoader.getProperty("msgFormat")!=null)
				msgFormat = optionsLoader.getProperty("msgFormat");
			if (optionsLoader.getProperty("msgTitleFormat")!=null)
				msgTitleFormat = optionsLoader.getProperty("msgTitleFormat");
			if (optionsLoader.getProperty("javaArguments")!=null)
				javaArguments = optionsLoader.getProperty("javaArguments");
			/*
			if (optionsLoader.getProperty("useWarpRank")!=null)
				useWarpRank = Integer.valueOf(optionsLoader.getProperty("useWarpRank"));
			if (optionsLoader.getProperty("createWarpRank")!=null)
				createWarpRank = Integer.valueOf(optionsLoader.getProperty("createWarpRank"));
			*/			

			if (optionsLoader.getProperty("muteRank")!=null ||
					optionsLoader.getProperty("warpPlayerRank")!=null ||
					optionsLoader.getProperty("teleportRank")!=null ||
					optionsLoader.getProperty("giveRank")!=null ||
					optionsLoader.getProperty("homeCommandRank")!=null ||
					optionsLoader.getProperty("givePlayerRank")!=null ||
					optionsLoader.getProperty("setRankRank")!=null ||
					optionsLoader.getProperty("useWarpRank")!=null ||
					optionsLoader.getProperty("createWarpRank")!=null) {
				if (optionsLoader.getProperty("muteRank")!=null)
					muteRank = Integer.valueOf(optionsLoader.getProperty("muteRank"));
				if (optionsLoader.getProperty("warpPlayerRank")!=null)
					warpPlayerRank = Integer.valueOf(optionsLoader.getProperty("warpPlayerRank"));
				if (optionsLoader.getProperty("teleportRank")!=null)
					teleportRank = Integer.valueOf(optionsLoader.getProperty("teleportRank"));
				if (optionsLoader.getProperty("giveRank")!=null)
					giveRank = Integer.valueOf(optionsLoader.getProperty("giveRank"));
				if (optionsLoader.getProperty("homeCommandRank")!=null)
					homeCommandRank = Integer.valueOf(optionsLoader.getProperty("homeCommandRank"));
				if (optionsLoader.getProperty("givePlayerRank")!=null)
					givePlayerRank = Integer.valueOf(optionsLoader.getProperty("givePlayerRank"));
				if (optionsLoader.getProperty("setRankRank")!=null)
					setRankRank = Integer.valueOf(optionsLoader.getProperty("setRankRank"));
				if (optionsLoader.getProperty("useWarpRank")!=null)
					useWarpRank = Integer.valueOf(optionsLoader.getProperty("useWarpRank"));
				if (optionsLoader.getProperty("createWarpRank")!=null)
					createWarpRank = Integer.valueOf(optionsLoader.getProperty("createWarpRank"));
				r.close();
				conversion();
			}
			
			
			if (autoRestartMins<5) {
				//autoRestartMins=5;
			}
			if (internalPort == port) {
				System.out.println("OH NO! Your 'internalPort' and 'port' properties are the same! Edit simpleserver.properties and change them to different values. 'port' is recommended to be 25565, the default port of minecraft, and will be the port you actually connect to.");
				System.out.println("Press enter to continue...");
				Scanner in = new Scanner(System.in);
				in.nextLine();
				r.close();
				System.exit(0);
			}
			r.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("Could not read the properties file!");
		}
	}

}
