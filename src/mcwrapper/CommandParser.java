package mcwrapper;

public class CommandParser {
	Player parent;
	Server server;
	public CommandParser(Player parent) {
		this.parent = parent;
		server = parent.parent;
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
	public void giveItems(String name, int id, int amt) {
		while (amt>0) {
			if (amt>64) {
				server.runCommand("give " + name + " " + id + " " + 64);
				amt-=64;
			}
			else {
				server.runCommand("give " + name + " " + id + " " + amt);
				amt=0;
			}
		}
	}
	public void parse(String msg) {
		String[] tokens = msg.split(" ");
		if (tokens.length>=1) {
			/*
			if (tokens[0].compareTo("!kill")==0) {
				parent.kill();
			}
			*/
			if (tokens[0].compareTo("!list")==0 || tokens[0].compareTo("!who")==0) {
				String list = "Connected Players: ";
				for (Player i :server.players) {
					if (i.getName()!=null && i.getName()!="")
						list+=i.getName() + ", ";
				}
				//server.runCommand("tell " + parent.getName()+ " " + list);
				parent.addMessage(list);
			}
			if (tokens[0].compareTo("!listips")==0) {
				if (parent.getRank()>=2) {
					//server.runCommand("tell " + parent.getName() + " IP Addresses: ");
					parent.addMessage("IP Addresses:");
					for (Player i :server.players) {
						//server.runCommand("tell " + parent.getName() + " " + i.getName() + " " + i.getIPAddress());
						if (i.getName()!=null && i.getName()!="")
							parent.addMessage(i.getName() + " " + i.getIPAddress());
					}
					
				}
			}
			if (tokens[0].compareTo("!motd")==0) {
				sendMOTD();
			}
			if (tokens[0].compareTo("!rules")==0) {
				sendRules();
			}
			if (tokens[0].compareTo("!lock")==0) {
				if (!server.chests.hasLock(parent.getName().toLowerCase())) {
					parent.addMessage("Create a 1 box chest, and it will be locked to you!");
					parent.addMessage("You only get ONE locked chest! Release the lock by saying !releaselock");
					parent.attemptLock=true;
				}
				else {
					parent.addMessage("You already have a lock! Release the lock by saying !releaselock");
				}
			}
			if (tokens[0].compareTo("!destroy")==0) {
				if (parent.getRank()>=2) {
					parent.destroy=!parent.destroy;
				}
			}
			if (tokens[0].compareTo("!releaselock")==0 || tokens[0].compareTo("!unlock")==0) {
				server.chests.releaseLock(parent.getName().toLowerCase());
				parent.addMessage("Your lock has been released!");
			}
			
			if (tokens[0].compareTo("!kit")==0) {
				if (tokens.length>=2)
					server.kits.giveKit(parent.getName(), tokens[1]);
			}
			
			if (tokens[0].compareTo("!kits")==0) {
				server.kits.listKits(parent);
			}
			if (tokens[0].compareTo("!kick")==0) {
				if (parent.getRank()>=2) {
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
							return;
						server.kick(tokens[1], reason);	
						server.runCommand("say Player " + tokens[1] + " has been kicked! (" + reason + ")");
					}
				}
			}
			if (tokens[0].compareTo("!mute")==0) {
				if (parent.getRank()>=server.options.muteRank) {
					if (tokens.length>=2) {
						server.mute(tokens[1]);	
						server.runCommand("say Player " + tokens[1] + " has been muted!");
					}
				}
			}
			if (tokens[0].compareTo("!unmute")==0) {
				if (parent.getRank()>=server.options.muteRank) {
					if (tokens.length>=2) {
						server.mute(tokens[1]);	
						server.runCommand("say Player " + tokens[1] + " has been unmuted!");
					}
				}
			}
			
			if (tokens[0].compareTo("!warpmeto")==0) {
				if (parent.getRank()>=server.options.warpPlayerRank) {
					if (tokens.length==2) {
						server.runCommand("tp " + parent.getName() + " " + tokens[1]);
					}
				}
			}
			if (tokens[0].compareTo("!warptome")==0) {
				if (parent.getRank()>=server.options.warpPlayerRank) {
					if (tokens.length==2) {
						server.runCommand("tp " + tokens[1] + " " + parent.getName());
					}
				}
			}
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
					if (tokens.length==2) {
						server.warps.listWarps(parent);
					}
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
			if (tokens[0].compareTo("!tp")==0) {
				if (parent.getRank()>=server.options.teleportRank) {
					if (tokens.length==3) {
						server.runCommand("tp " + tokens[1] + " " + tokens[2]);
					}
				}
			}
			if (tokens[0].compareTo("!ban")==0) {
				if (parent.getRank()>=3) {
					if (tokens.length>1) {
						server.runCommand("ban " + tokens[1]);
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
							return;
						server.kick(tokens[1], reason);
						server.runCommand("say Player " + tokens[1] + " has been banned! (" + reason + ")");
					}
				}
			}
			if (tokens[0].compareTo("!unban")==0) {
				if (parent.getRank()>=3) {
					if (tokens.length>1) {
						server.runCommand("pardon " + tokens[1]);
					}
				}
			}
			if (tokens[0].compareTo("!banip")==0) {
				if (parent.getRank()>=3) {
					if (tokens.length>1) {
						server.ipBans.addBan(tokens[1]);
						//server.runCommand("tell " + parent.getName() + " IP Address " + tokens[1] + " has been banned!");
						parent.addMessage("IP Address " + tokens[1] + " has been banned!");
					}
				}
			}
			if (tokens[0].compareTo("!unbanip")==0) {
				if (parent.getRank()>=3) {
					if (tokens.length>1) {
						if (server.ipBans.removeBan(tokens[1])) {
							//server.runCommand("tell " + parent.getName() + " IP Address " + tokens[1] + " has been unbanned!");
							parent.addMessage("IP Address " + tokens[1] + " has been unbanned!");
						}
						else {
							//server.runCommand("tell " + parent.getName() + " no IP ban matching " + tokens[1] + "was found!");
							parent.addMessage("No IP ban matching " + tokens[1] + "was found!");
						}
					}
				}
			}
			if (tokens[0].compareTo("!give")==0) {
				if (parent.getRank()>=server.options.giveRank) {
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
						int id = Integer.valueOf(tokens[1]);
						giveItems(parent.getName(),id,amt);
					}
				}
			}
			if (tokens[0].compareTo("!restart")==0) {
				if (parent.getRank()>=3) {
					server.forceRestart();
				}
			}
			if (tokens[0].compareTo("!setrank")==0) {
				if (parent.getRank()>=server.options.setRankRank) {
					if (tokens.length>=3) {
						int rank =Integer.valueOf(tokens[2]);
						if (parent.getRank()>server.ranks.checkName(tokens[1]) && rank<parent.getRank()) {
							server.ranks.setRank(tokens[1], rank);
							//server.runCommand("tell " + parent.getName() + " Player " + tokens[1] + "'s rank was set to " + tokens[2] +"!");
							parent.addMessage("Player " + tokens[1] + "'s rank was set to " + rank +"!");
						}
					}
				}
			}
			if (tokens[0].compareTo("!whitelist")==0) {
				if (parent.getRank()>=2) {
					if (tokens.length>=2) {
						if (!server.whitelist.isWhitelisted(tokens[1])) {
							server.whitelist.addName(tokens[1]);
							//server.runCommand("tell " + parent.getName() + " Player " + tokens[1] + " was whitelisted!");
							parent.addMessage("Player " + tokens[1] + " was whitelisted!");
						}
					}
				}
			}
			if (tokens[0].compareTo("!unwhitelist")==0) {
				if (parent.getRank()>=2) {
					if (tokens.length>=2) {
						if (server.whitelist.removeName(tokens[1])) {
							//server.runCommand("tell " + parent.getName() + " Player " + tokens[1] + " was un-whitelisted!");
							parent.addMessage("Player " + tokens[1] + " was un-whitelisted!");
						}
						else {
							//server.runCommand("tell " + parent.getName() + " No such name: " + tokens[1] + "!");
							parent.addMessage("No such name: " + tokens[1] + "!");
						}
					}
				}
			}
			if (tokens[0].compareTo("!giveplayer")==0) {
				if (parent.getRank()>=server.options.givePlayerRank) {
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
						//server.runCommand("give " + tokens[1] + " " + tokens[2] + " " + amt);
						int id = Integer.valueOf(tokens[2]);
						giveItems(tokens[1],id,amt);
					}
				}
			}
			if (tokens[0].compareTo("!commands")==0 || tokens[0].compareTo("!help")==0) {
				String line = "Available Commands: !kits !kit !list !motd !rules !lock !releaselock ";
				switch (parent.getRank()) {
					case 1:
						line+="";
						break;
					case 2:
						line+="!kick !listips !whitelist ";
						break;
					case 3:
						line+= "!ban !banip !unbanip !unban !setrank !save !reload !backup ";
						break;
				}
				if (parent.getRank()>=server.options.giveRank) {
					line+="!give ";
				}
				if (parent.getRank()>=server.options.givePlayerRank) {
					line+="!giveplayer ";
				}
				if (parent.getRank()>=server.options.teleportRank) {
					line+="!tp ";
				}
				if (parent.getRank()>=server.options.warpPlayerRank) {
					line+="!warptome !warpmeto ";
				}
				if (parent.getRank()>=server.options.useWarpRank) {
					line+="!warp !listwarps ";
				}
				if (parent.getRank()>=server.options.createWarpRank) {
					line+="!createwarp ";
				}
				parent.addMessage(line);
			}
			if (tokens[0].compareTo("!save")==0) {
				if (parent.getRank()>=3) {
					server.saveAll();
					server.runCommand("save-all");
				}
			}
			if (tokens[0].compareTo("!backup")==0) {
				if (parent.getRank()>=3) {
					server.runCommand("tell " + parent.getName() + " Forcing backup!");
					server.forceBackup();
				}
			}
			if (tokens[0].compareTo("!reload")==0) {
				if (parent.getRank()>=3) {
					server.loadAll();
				}
			}
		}
	}
}
