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
import java.util.Iterator;

public class CommandParser {
	Player parent;
	Server server;
	/*public static final String[] commands = { 
		"ban","banip","unban","unbanip","kick",
		"lock","unlock","tp","warpmeto","warptome",
		"iddqd","listips","kit","kits","mute","unmute",
		"give","giveplayer","setgroup","whitelist",
		"unwhitelist","restart","save","reload","backup"
		};
	*/
	public CommandParser(Player parent) {
		this.parent = parent;
		server = parent.parent;
	}
	public void closing() {
		parent=null;
	}
	public void sendMOTD() {
		//System.out.println(server.getMOTD());
		//parent.addMessage(server.getMOTD());
		//server.runCommand("tell " + parent.getName() + " " + server.getMOTD());
		String rules=server.getMOTD();
		String[] lines = rules.split("\\r?\\n");
		for (int i=0;i<lines.length;i++) {
			parent.addMessage(lines[i]);
			//server.runCommand("tell " + parent.getName() + " " + lines[i]);
		}
	}
	public void sendRules() {
		String rules=server.getRules();
		String[] lines = rules.split("\\r?\\n");
		for (int i=0;i<lines.length;i++) {
			parent.addMessage(lines[i]);
			//server.runCommand("tell " + parent.getName() + " " + lines[i]);
		}
	}
	public void giveItems(String name, int id, int amt) throws InterruptedException {
		while (amt>0) {
			if (amt>64 && amt < 1000) {
				server.runCommand("give " + name + " " + id + " " + 64);
				amt-=64;
			}
			else {
				server.runCommand("give " + name + " " + id + " " + amt);
				amt=0;
			}
		}
	}
	public boolean parse(String msg) throws InterruptedException, IOException {
		String[] tokens = msg.split(" ");
		if (server.options.useSlashes) {
			if (tokens[0].startsWith("/"))
				tokens[0] = "!" + tokens[0].substring(1);
		}
		String cmd = tokens[0].substring(1);
		
		if (tokens.length>=1) {
			/*
			if (tokens[0].compareTo("!kill")==0) {
				parent.kill();
			}
			*/
			if (tokens[0].compareTo("!list")==0 || tokens[0].compareTo("!who")==0) {
				String list = "Connected Players ("+ server.numPlayers() +"): ";
				for (Iterator<Player> itr = PlayerFactory.iterator(); itr.hasNext();) {
					Player i = itr.next();
					if (i!=null)
						if (i.getName()!=null && i.getName()!="" && !i.closed && i.kickMsg==null)
							list+=i.getName() + ", ";
				}
				//server.runCommand("tell " + parent.getName()+ " " + list);
				parent.addMessage(list);
				return true;
			}
			if (tokens[0].compareTo("!listips")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					//server.runCommand("tell " + parent.getName() + " IP Addresses: ");
					parent.addMessage("IP Addresses:");
					for (Iterator<Player> itr = PlayerFactory.iterator(); itr.hasNext();) {
						Player i = itr.next();
						//server.runCommand("tell " + parent.getName() + " " + i.getName() + " " + i.getIPAddress());
						if (i.getName()!=null && i.getName()!="")
							parent.addMessage(i.getName() + " " + i.getIPAddress());
					}
					
				}
				return true;
			}
			if (tokens[0].compareTo("!motd")==0) {
				sendMOTD();
				return true;
			}
			if (tokens[0].compareTo("!rules")==0) {
				sendRules();
				return true;
			}
			if (tokens[0].compareTo("!lock")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (!server.chests.hasLock(parent.getName().toLowerCase())) {
						parent.addMessage("Create a single box chest, and it will be locked to your username!");
						parent.addMessage("You only get ONE locked chest! Release the lock by saying !unlock");
						parent.attemptLock=true;
					}
					else {
						parent.addMessage("You already have a lock! Release the lock by saying !unlock");
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!unlock")==0 || tokens[0].compareTo("!releaselock")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					server.chests.releaseLock(parent.getName().toLowerCase());
					parent.addMessage("Your lock has been released!");
				}
				return true;
			}
			if (tokens[0].compareTo("!iddqd")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					parent.destroy=!parent.destroy;
					if (parent.destroy)
						parent.addMessage("God-Mode Enabled!");
					else
						parent.addMessage("God-Mode Disabled!");
				}
				return true;
			}
			if (tokens[0].compareTo("!rcon")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>1) {
						String rcon = "";
						try {
							rcon = rcon.substring(5+1);
							if (rcon.length()==0||rcon==null)
								rcon="";
						}
						catch (Exception e) { rcon=""; }
						server.runCommand(rcon);
					}
				}
				return true;
			}
			
			
			if (tokens[0].compareTo("!kit")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>=2)
						server.kits.giveKit(parent.getName(), tokens[1]);
				}
				return true;
			}
			
			if (tokens[0].compareTo("!kits")==0) {
				if (server.cmdAllowed(cmd,parent))
					server.kits.listKits(parent);
				return true;
			}
			if (tokens[0].compareTo("!kick")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>=2) {
						//server.runCommand("kick " + tokens[1]);
						String reason="";
						if (tokens.length>2) {
							try {
								reason = msg.substring(5+1+tokens[1].length()+1);
								if (reason.length()==0||reason==null)
									reason="";
							}
							catch (Exception e) { reason=""; }
						}
						if (reason==null)
							reason="";
						if (tokens[1]==null)
							return true;
						String name = server.kick(tokens[1], reason);	
						if (name!=null) {
							server.adminLog.addMessage("Admin " + parent.getName() + " kicked player:\t " + name + "\t(" + reason + ")");
							server.runCommand("say Player " + name + " has been kicked! (" + reason + ")");
						}
						else
							parent.addMessage("No such player online! ("+tokens[1]+")");
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!mute")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>=2) {
						String name = server.findName(tokens[1]);
						if (name==null) 
							name=tokens[1];
						server.mutelist.addName(name);	
						server.adminLog.addMessage("Admin " + parent.getName() + " muted player:\t " + name);
						server.runCommand("say Player " + name + " has been muted!");
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!unmute")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>=2) {
						String name = server.findName(tokens[1]);
						if (name==null) 
							name=tokens[1];
						server.mutelist.removeName(name);	
						server.adminLog.addMessage("Admin " + parent.getName() + " unmuted player:\t " + name);
						server.runCommand("say Player " + name + " has been unmuted!");
					}
				}
				return true;
			}
			
			if (tokens[0].compareTo("!warpmeto")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length==2) {
						if (!tokens[1].equalsIgnoreCase("") && tokens[1] != null) {
							String name = server.findName(tokens[1]);
							if (name!=null) {
								server.runCommand("tp " + parent.getName() + " " + name);
								server.adminLog.addMessage("Admin " + parent.getName() + " teleported:\t " + parent.getName() + "\tto\t" + name);
							}
							else
								parent.addMessage("No such player online! ("+tokens[1]+")");
						}
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!warptome")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length==2) {
						String name = server.findName(tokens[1]);
						if (name!=null) {
							server.runCommand("tp " + name + " " + parent.getName());
							server.adminLog.addMessage("Admin " + parent.getName() + " teleported:\t " + name + "\tto\t" + parent.getName());
						}
						else
							parent.addMessage("No such player online! ("+tokens[1]+")");
					}
				}
				return true;
			}
			/*
			if (tokens[0].compareTo("!warp")==0) {
				if (parent.getRank()>=server.options.useWarpRank) {
					if (tokens.length==2) {
						double[] coords = server.warps.getWarp(tokens[1]);
						if (coords!=null) {
							parent.warp(coords);
						}
						else {
							parent.addMessage("Warp was unsuccessful!");
						}
						//server.runCommand("tp " + tokens[1] + " " + parent.getName());
					}
				}
			}
			if (tokens[0].compareTo("!listwarps")==0) {
				if (parent.getRank()>=server.options.useWarpRank) {
					server.warps.listWarps(parent);
				}
			}
			if (tokens[0].compareTo("!createwarp")==0) {
				if (parent.getRank()>=server.options.createWarpRank) {
					if (tokens.length==2) {
						if (server.warps.makeWarp(tokens[1], parent.x, parent.y, parent.z, parent.stance)) {
							parent.addMessage("Warp creation successful!");
						}
						else {
							parent.addMessage("Warp creation failed!");
						}
						//server.runCommand("tp " + tokens[1] + " " + parent.getName());
					}
				}
			}
			*/
			if (tokens[0].equals("!local") || tokens[0].equals("!l")) {
				if (server.cmdAllowed(cmd,parent)) {
					String chat = msg.substring(tokens[0].length()); 
					int numPlayers = server.localChat(parent,chat);
					if (numPlayers<=0)
						parent.addMessage("§cNobody is around to hear you.");
				}
				return true;
			}
			if (tokens[0].compareTo("!tp")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length==3) {
						String name1 = server.findName(tokens[1]);
						String name2 = server.findName(tokens[2]);
						if (name1!=null && name2!=null) {
							server.runCommand("tp " + name1 + " " + name2);
							parent.addMessage("Teleported " + name1 + " to " + name2 + "!");
							server.adminLog.addMessage("User " + parent.getName() + " teleported:\t " + name1 + "\tto\t" + name2);
						}
						else if (name1==null)
							parent.addMessage("No such player online! ("+tokens[1]+")");
						else
							parent.addMessage("No such player online! ("+tokens[2]+")");
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!ban")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>1) {
						String reason="";
						if (tokens.length>2) {
							try {
								reason = msg.substring(4+1+tokens[1].length()+1);
								if (reason.length()==0||reason==null)
									reason="";
							}
							catch (Exception e) { reason=""; }
						}
						if (reason==null)
							reason="";
						if (tokens[1]==null)
							return true;
						String name = server.findName(tokens[1]);
						if (name!=null) {
							server.runCommand("ban " + name);
							server.kick(name, reason);
							server.runCommand("say Player " + name + " has been banned! (" + reason + ")");
							server.adminLog.addMessage("User " + parent.getName() + " banned player:\t " + name + "\t(" + reason + ")");
						}
						else {
							server.runCommand("ban " + tokens[1]);
							server.runCommand("say Player " + tokens[1] + " has been banned! (" + reason + ")");
							server.adminLog.addMessage("User " + parent.getName() + " banned player:\t " + name + "\t(" + reason + ")");
						}
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!unban")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>1) {
						server.runCommand("pardon " + tokens[1]);
						parent.addMessage("Unbanning " + tokens[1] + "!");
						server.adminLog.addMessage("User " + parent.getName() + " unbanned player:\t " + tokens[1]);
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!banip")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>1) {
						Player p = server.findPlayer(tokens[1]);
						if (p==null) {
							server.ipBans.addBan(tokens[1]);
							parent.addMessage("IP Address " + tokens[1] + " has been banned!");
							server.adminLog.addMessage("User " + parent.getName() + " banned ip:\t " + tokens[1]);
						}
						else {
							server.ipBans.addBan(p.getIPAddress());
							server.kick(p.getName(),"IP Banned!");
							server.runCommand("say Player " + p.getName() + " has been IP banned!");
							server.adminLog.addMessage("User " + parent.getName() + " banned ip:\t " + tokens[1] + "\t(" + p.getName() + ")");
						}
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!unbanip")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>1) {
						if (server.ipBans.removeBan(tokens[1])) {
							//server.runCommand("tell " + parent.getName() + " IP Address " + tokens[1] + " has been unbanned!");
							parent.addMessage("IP Address " + tokens[1] + " has been unbanned!");
							server.adminLog.addMessage("User " + parent.getName() + " unbanned ip:\t " + tokens[1]);
						}
						else {
							//server.runCommand("tell " + parent.getName() + " no IP ban matching " + tokens[1] + "was found!");
							parent.addMessage("No IP ban matching " + tokens[1] + "was found!");
						}
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!give")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>1) {
						int amt=1;
						if (tokens.length>=3) {
							try {
								amt = Integer.valueOf(tokens[2]);
							}
							catch (Exception e) {
								amt=1;
							}
						}
						//server.runCommand("give " + parent.getName() + " " + tokens[1] + " " + amt);
						int id=0;
						try {
							id = Integer.valueOf(tokens[1]);
						}
						catch (Exception e) {
							parent.addMessage("§cInvalid format!");
							return true;
						}
						server.adminLog.addMessage("User " + parent.getName() + " used give:\t " + id + "\t(" + amt + ")");
						giveItems(parent.getName(),id,amt);
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!restart")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					server.adminLog.addMessage("User " + parent.getName() + " attempted a restart!");
					if (server.saveLock.tryAcquire())
						server.saveLock.release();
					else
						parent.addMessage("Server is currently Backing Up/Saving/Restarting...");
					server.forceRestart();
				}
				return true;
			}
			if (tokens[0].compareTo("!setgroup")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>=3) {
					
						int group=0;
						try {
							group =Integer.valueOf(tokens[2]);
						}
						catch (Exception e) {
							parent.addMessage("§cInvalid format!");
							return true;
						}
						if (group>=parent.getGroup()) {
							parent.addMessage("§cYou cannot promote a user to a higher group!");
							return true;
						}
						Player p = server.findPlayer(tokens[1]);
						if (p!=null) {
							if (parent.getGroup()<=p.getGroup()) {
								parent.addMessage("§cYou cannot set the group of this user!");
								return true;
							}
							server.members.setGroup( p.getName(), group);
							//server.runCommand("tell " + parent.getName() + " Player " + tokens[1] + "'s rank was set to " + tokens[2] +"!");
							parent.addMessage("Player " + p.getName() + "'s group was set to " + group +"!");
							server.adminLog.addMessage("User " + parent.getName() + " set player's group:\t " + p.getName() + "\t(" + group + ")");
						}
						else {
							String name=tokens[1];
							if (parent.getGroup()<=server.members.checkName(name)) {
								parent.addMessage("§cYou cannot set the group of this user!");
								return true;
							}
							server.members.setGroup(name, group);
							//server.runCommand("tell " + parent.getName() + " Player " + tokens[1] + "'s rank was set to " + tokens[2] +"!");
							parent.addMessage("Player " + name + "'s group was set to " + group +"!");
							server.adminLog.addMessage("User " + parent.getName() + " set player's group:\t " + name + "\t(" + group + ")");
						}
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!whitelist")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>=2) {
						if (!server.whitelist.isWhitelisted(tokens[1])) {
							server.whitelist.addName(tokens[1]);
							//server.runCommand("tell " + parent.getName() + " Player " + tokens[1] + " was whitelisted!");
							parent.addMessage("Player " + tokens[1] + " was whitelisted!");
							server.adminLog.addMessage("User " + parent.getName() + " whitelisted player:\t " + tokens[1]);
						}
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!unwhitelist")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>=2) {
						if (server.whitelist.removeName(tokens[1])) {
							//server.runCommand("tell " + parent.getName() + " Player " + tokens[1] + " was un-whitelisted!");
							parent.addMessage("Player " + tokens[1] + " was un-whitelisted!");
							server.adminLog.addMessage("User " + parent.getName() + " unwhitelisted player:\t " + tokens[1]);
						}
						else {
							//server.runCommand("tell " + parent.getName() + " No such name: " + tokens[1] + "!");
							parent.addMessage("No such name: " + tokens[1] + "!");
						}
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!giveplayer")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					if (tokens.length>=3) {
						int amt=1;
						if (tokens.length>=4) {
							try {
								amt = Integer.valueOf(tokens[3]);
							}
							catch (Exception e) {
								amt=1;
							}
						}
						int id = 0;
						try {
							id = Integer.valueOf(tokens[2]);
						}
						catch (Exception e) {
							parent.addMessage("§cInvalid format!");
							return true;
						}
						String name = server.findName(tokens[1]);
						if (name!=null) {
							giveItems(tokens[1],id,amt);
							server.adminLog.addMessage("User " + parent.getName() + " used giveplayer:\t " + name + "\t" + id + "\t(" + amt + ")");
						}
						else {
							parent.addMessage("No such player online! ("+tokens[1]+")");
						}
					}
				}
				return true;
			}
			if (tokens[0].compareTo("!commands")==0 || tokens[0].compareTo("!help")==0) {
				String prefix = "!";
				if (server.options.useSlashes)
					prefix="/";
				String line = "Available Commands: "+prefix+"who "+prefix+"motd "+prefix+"rules ";
				line+=server.getCommands(parent);
				parent.addMessage(line);
				return !(server.options.useSMPAPI);
			}
			if (tokens[0].compareTo("!save")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					server.saveAll();
					server.runCommand("save-all");
					parent.addMessage("Resources Saved!");
				}
				return !(server.options.useSMPAPI);
			}
			if (tokens[0].compareTo("!home")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					parent.sendHome();
				}
				return true;
			}
			if (tokens[0].compareTo("!backup")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					parent.addMessage("Forcing backup!");
					server.forceBackup();
				}
				return true;
			}
			if (tokens[0].compareTo("!reload")==0) {
				if (server.cmdAllowed(cmd,parent)) {
					server.loadAll();
					parent.addMessage("Resources Reloaded!");
				}
				return !(server.options.useSMPAPI);
			}
			if (tokens[0].equals("!mods")) {
				return !(server.options.useSMPAPI);
			}
		}
		if (msg.startsWith("/"))
			return false;
		return !(server.options.useSMPAPI);
	}
}
