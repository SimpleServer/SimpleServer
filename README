Simple Server RC 6.6
An open source simple server tunnel for minecraft alpha multiplayer servers.
Copyright (c) 2010 Charles Wagner Jr. (spiegalpwns@gmail.com)
See LICENSE for details.

http://www.devfluid.com/simpleserver
http://www.minecraftforum.net/viewtopic.php?f=1012&t=27756
https://github.com/spiegalpwns/SimpleServer

Installation:
1) Download minecraft_server.jar from minecraft.net
2) Unzip all files into the same directory as minecraft_server.jar
3) Run launch.bat or launch.sh to start the server for the first time
4) Press "Enter" at the console (which is informing you that you've started the server for the first time!)
5) Open simpleserver.properties and adjust the values as needed
6) You can now connect to the server using your IP Address (www.whatismyip.com) and the port indicated in simpleserver.properties!

Groups:
	Groups can be added in group-list.txt.
	The format for a group is:
	GroupID#=GroupName,showTitle,isAdmin,colorCode
	So, for example:
	1=Trusted,true,false,f

Group Notation:
	The following text files require "Group Notation"
		kit-list.txt
		command-list.txt
		block-list.txt
	Groups can be denoted with commas (,) or hyphens(-)
	For example:	1,2,3 means to include groups 1, 2, and 3.
			1-3 means the same thing.
			1-3,5 means 1,2,3, and 5.


Guest Mode/Destroy Protection:
	Setting defaultGroup=-1 in simpleserver.properties, or setting someone's group to -1 will enable "Guest Mode"
	Guests are not be able to place blocks, destroy blocks, remove items from chests, or drop items.

Locked Chests:
	A player can say !lock to begin the process to lock a chest. They can then place a single chest block, 
	and it is "locked." Locked chest's contents cannot be viewed or changed by other players unless they
	belong to a group marked with isAdmin=true in group-list.txt.
	A player can release their lock by saying !unlock, and the chest will be available to other players.
	
Local Chat:
	Players can chat with other players in the area by saying !local message or !l message.
	The radius can be changed in simpleserver.properties.
	Note: Radius is somewhat misleading. To save a few CPU cycles, a cube is used instead of a sphere.
	
Commands:
	Command permissions can be set per group in command-list.txt.
	Setting a command to 0 means that it can be used by any player.
	



Console Commands:
Say help in the console for all of the standard minecraft server commands.
Added are:
	!save -- Save all resource files (motd.txt, rules.txt, etc)
	!reload -- Reload all resource files
	!backup -- Force a backup
Player Commands:
	say !commands in game
	Commands are based on rank
	!motd
	!rules
	!lock
	!unlock
	!iddqd
	!local
	!kits
	!kit KIT_NAME
	!banip IPAddress
	!unbanip IPAddress
	!ban NAME REASON
	!unban NAME
	!kick NAME REASON
	!setrank PLAYER RANK
	!giveplayer PLAYER ID# AMT
	!give ID# AMT
	!whitelist NAME
	!unwhitelist NAME
	!help
	!commands
	!who
	!list
	!listips
	!give ID# AMT
	!backup
	!restart
	!reload
	!save
	!mute
	!unmute

Change-log:
RC 6.6.6
	Initial Open Source release!
	Changed ServerBackup.java to make backups worldedit compatible (hopefully).
RC 6.6
	Added new packets required for server version 2.7
RC 6.5_01
	Added a Thread.yield() to the loop for server backups.
	Added javaArguments hidden simpleserver.properties option.
RC 6.5
	Buggy SourceRCON implementation. Set rconPort and rconPassword to use. Remove the password to disable rcon.
		http://developer.valvesoftware.com/wiki/Source_RCON_Protocol
		Say "help" once connected. It's really buggy! Can't guarantee it will be useful (yet).
	Created language.properties file.
	Updated for 0.2.5 minecraft server.
	Some performance-related changes.
	Other stuff I forgot about?
RC 6.4:
	Changed the internal storage of Player objects to a Static Factory design pattern. This should help with memory usage.
	Added packets for 0.2.4 minecraft_server.jar update.
	Fixed (?) !ban cutting off the first letter of the reason.
RC 6.3:
	It is a mystery!
RC 6.2:
	Added a simpleserver.properties setting "guestsCanViewComplex"
		Default value is false, which means guest users (group=-1) cannot view signs or chests.
	Made compatible for minecraft_server.jar version 0.2.2
RC 6.1_01:
	Changed guest mode: 
		Guests are now able to see signs, 
		but are also able to look into and change items inside unlocked chests.
RC 6.1
	Added useSMPAPI to simpleserver.properties to tell SimpleServer to pass unrecognized commands
		and other useful commands to the SMP API. Set to true if you are using the WarpMod
	Added SSWarpMod.jar to the package. This is used in conjunction with SMP API.
		http://www.minecraftforum.net/viewtopic.php?f=1012&t=44394
		Setup instructions are above.
	Fixed some failure detection issues related to minecraft exceptions.
	Fixed the saveLock to only unlock after a full save has been completed.
	Fixed autoBackup/!backup to not try to operate on tmp_chunk.dat
RC 6_06:
	Removed some debug code that was printing to the server console.
	Modified the autoSave routine to more safely release the server lock.
RC 6_05:
	Fixed an issue where backups/log files were not saving with the correct filename.
	Fixed an issue where !home did nothing. !home now corresponds to /home
	Fixed an issue where unknown commands that start with ! were sent to chat.
RC 6_04:
	Fixed an issue with chests where a line of blocks where a locked chest was would be indestructible.
	Fixed an issue with !tp where it would not correctly use prefixed players names.
RC 6_03:
	Added useMsgFormats boolean to simpleserver.properties. Enables/disables SimpleServer message formatting.
	Added msgFormat and msgTitleFormat which sets the string format for player messages that have no title, and a user title, respectively.
		%1$s = Username
		%2$s = Group Title
		%3$s = Group Color
	Changes backup routine so it copies files into /tmp instead of /backups. 
		Backup ZIP files are still copied into /backups.
	Fixed issue with chat messages not appearing on vanilla minecraft_server.jar servers.
RC 6:
	Added ItemWatchList: Bans players who reach a certain threshold amount of an item id.
	Added AdminLog: Logs many admin and server actions.
	Fixed a problem where incorrect arguments to commands like !give would disconnect the user from the server.
	Fixed a bug where -1 was interpreted as [Null,-1] in Group Notation.
	Config files moved to simpleserver folder. 
		First startup of the server will automatically move the files for you
	Generalized Server and Client tunnels to be more efficient and safer.
	Ranks have been changed to groups.
	IP based member list added (for onlineMode=false).
	Server restart refactored to solve most issues and be safer.
	Improved memory usage slightly (more improvements to come in the future).
	Added a monitor thread to watch for unexpected closes of minecraft_server.jar.
	Added exitOnFailure into simpleserver.properties that exits SimpleServer.jar if the minecraft process is unreachable/unrecoverable.
	Added Local Chat (!local) and localChatRadius to simpleserver.properties.
	Moved command permissions to command-list.txt.
	Added !mute and !unmute commands.
	Added !iddqd for admins to quickly destroy blocks (for griefer cleanup).
	Added c10t Integration
		c10tArgs=/path/to/c10t/executable -arg0 -arg1 -arg2 -arg3
		c10tMins=60
		to simpleserver.properties
		Blanking c10tArgs will disable this feature until application is restarted.
		(Tip: This can be used for any mapping program that exits on completion and uses commandline.)
	Added locked chests. Players are currently only able to create a single locked chest using !lock.
	Added alternateJar to simpleserver.properties to load some other jar rather than minecraft_server.jar
	Added -Xmx256m -Xms32m args to launch.sh and launch.bat.
	Fixed an issue where if memory in simpleserver.properties was <1024, the server would not launch correctly.
	Added useSlashes in simpleserver.properties for using /commands instead of !commands. ---Experimental Feature---
	Added robot detection (Detects IP addresses who connect without attempting to login) which trims console spam.
	Added automatic IP Bans to players who attempt to connect more than 30 times in 60 seconds (will be configurable).
	Many more changes that I can't remember. :P
RC5.5:
	Fixed more issues that would result in End of Stream errors. (Should all be gone now)
	!save and !reload now save and reload simpleserver.properties settings
	Automatic Restart, Backup, and Save will activate/deactivate if settings are changed
	Added "Save Complete" message to when saves complete.
	Server shutdowns and restarts send messages to clients
	Fixed issue with !kick and !ban disconnecting the admin/mod
	SimpleServer will now wait until the minecraft server process ends before shutting down.
	Guests (Rank -1) can no longer take items out of chests, put items into chests, or drop items.
		-This feature may still be buggy!
	Recompiled for JRE1.5 compatibility on Mac OS X
		-Send support queries to spiegalpwns@gmail.com SUBJECT: [SimpleServer] MAC OS X
RC5.2:
	Fixed issue with End of Stream errors.
RC5:
	Updated to work with latest Minecraft Server 0.2.0
	autoRestart and autoRestartMins added to simpleserver.properties
	!restart command added to console and admins
	defaultRank added to simpleserver.properties
	Ranks below 0 cannot place or destroy blocks
	Modified server.properties editor so that it does not delete the old file. Instead, it simply overwrites required settings.
	Fixed whitelist
	Fixed client->server tunnel for some situations when the buffer contained over 1024 bytes
	!kick and !ban now allow messages
RC4:
	MANY CHANGES!
	Server->Client communication is now parsed. This makes packet injection safer, without a noticible performance decrease.
	Server message injection now more safe.
	Updated code to properly close streams and sockets.
	Added message for when sockets are closed.
	Server now ends threads correctly.
	Shutdown Hook is now safely closing threads
	autoBackupTime and autoSaveTime properties renamed to autoBackupMins and autoSaveMins respectively.
	Removed "debug" argument for launch.
	Added hidden "debug" properties to simpleserver.properties. Set debug=true to show exceptions and enable dumping.
	Backup thread correctly ends streams.
	Fixed bugs in client->server communication that would give some users an "End of Stream" error.
RC3:
	Server now injects messages to users, instead of wrapping around "tell" command
	Attempted fix to "End of Stream" errors
	Added EOFException dump class. Files will begin with "dump_"
	Added message to !kick and !ban
	Added save-all before backup is run
	MOTD is now multi-line
	Added onlineMode property to simpleserver.properties
	Added a check and message for internalPort and port settings, for when they are the same.
	Added "debug" argument to launch
		java -jar SimpleServer.jar debug
		Will show all exceptions from connections
RC2.1:
	Fixed a small bug with heap sizes >1GB
RC2:
	Whitelisting
	!giveplayer
	Unban Commands
	Console backup command
	!motd now "tell"s rather than "say"s
	Server will no longer do an automatic backup if the server has been empty the entire period between backups
	Various other small fixes?
