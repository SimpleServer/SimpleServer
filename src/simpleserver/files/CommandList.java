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
package simpleserver.files;

import java.util.LinkedList;

import simpleserver.Player;
import simpleserver.Server;


public class CommandList extends FileLoader {
	public static final String[] commandList = { 
		"ban=3,4","banip=3,4","unban=3,4","unbanip=3,4","kick=2-4",
		"lock=0","unlock=0","tp=3,4","warpmeto=2-4","warptome=2-4",
		"iddqd=3,4","listips=2-4","kit=0","kits=0","mute=2-4","unmute=2-4",
		"give=2-4","giveplayer=3,4","setgroup=3,4","whitelist=2-4","home=-1",
		"unwhitelist=2-4","restart=3,4","save=3,4","reload=3,4","backup=3,4",
		"rcon=4","local=-1"
		};
	public static final String[] synonyms = { "releaselock=unlock","l=local" };
	static class Command {
		String name;
		int[] groups;
		public Command(String name, int[] groups) {
			this.name=name;
			this.groups=groups;
		}
	}
	LinkedList<Command> commands = new LinkedList<Command>();
	Server parent;
	public CommandList(Server parent) {
		this.filename="command-list.txt";
		this.parent=parent;
	}
	private void setDefaults() {
		commands.clear();
		for (int i=0;i<commandList.length;i++) {
			String[] cmd = commandList[i].split("=");
			commands.add(new Command(cmd[0],Group.parseGroups(cmd[1])));
		}
	}

	public boolean checkPlayer(String command, Player p) {
		if (command!=null) {
			for (Command i: commands) {
				if (command.equals(i.name)) {
					return Group.contains(i.groups, p);
				}
			}
		}
		for (String j: synonyms) {
			if (j.startsWith(command)){
				String newCommand = j.split("=")[1];
				return checkPlayer(newCommand, p);
			}
		}
		return false;
	}
	public int[] checkGroups(String command) {
		if (command!=null) {
			for (Command i: commands) {
				if (command.equals(i.name)) {
					return i.groups;
				}
			}
		}
		for (String j: synonyms) {
			if (j.startsWith(command)){
				String newCommand = command.split("=")[1];
				return checkGroups(newCommand);
			}
		}
		return null;
	}
	
	public void setRank(String command, int rank) {
		if (command!=null) {
			for (Command i: commands) {
				if (command.equals(i.name)) {
					i.groups=new int []{rank};
				}
			}
		}
	}
	
	public String getCommands(Player p) {
		String cmds = "";
		String prefix = "!";
		if (parent.options.useSlashes)
			prefix="/";
		if (p!=null) {
			for (Command i: commands) {
				if (Group.contains(i.groups,p)) {
					cmds = cmds + prefix + i.name + " ";
				}
			}
		}
		return cmds;
	}
	
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		setDefaults();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		String[] tokens = line.split("=");
		if (tokens.length>=2) {
			for (Command i: commands) {
				if (tokens[0].equals(i.name)) {
					i.groups=Group.parseGroups(tokens[1]);
				}
			}
		}
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (Command i: commands) {
        	line+=i.name + "=";
        	for(int group: i.groups) {
        		line+= group+",";
        	}
        	line+="\r\n";
        }
		return line;
	}
}
