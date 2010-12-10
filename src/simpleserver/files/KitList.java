/*******************************************************************************
 * Copyright (C) 2010 Charles Wagner Jr..
 * spiegalpwns@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package simpleserver.files;

import java.util.LinkedList;

import simpleserver.Player;
import simpleserver.Server;


public class KitList extends FileLoader {
	Server parent;
	static class Kit {
		String name;
		int[] groups;
		LinkedList<Entry> items = new LinkedList<Entry>();
		class Entry {
			int id;
			int amt;
			public Entry(int id, int amt) {
				this.id=id;
				this.amt=amt;
			}
		}
		public Kit(String name, int[] groups) {
			this.name=name;
			this.groups=groups;
		}
		public void addItem(int id, int amt) {
			items.add(new Entry(id,amt));
		}
	}
	LinkedList<Kit> kits = new LinkedList<Kit>();
	@SuppressWarnings("unused")
	private KitList() {}
	public KitList(Server parent) {
		this.parent=parent;
		this.filename="kit-list.txt";
	}
	public void giveKit(String name, String kitName) throws InterruptedException {
		Kit kit=null;
		for (Kit i: kits) {
			if (kitName.toLowerCase().compareTo(i.name.toLowerCase())==0) {
				if (Group.contains(i.groups, parent.findPlayer(name)))
					kit=i;
			}
		}
		if (kit!=null) {
			for (KitList.Kit.Entry i: kit.items) {
				//parent.runCommand("give " + name + " " + i.id + " " + i.amt);
				giveItems(name,i.id,i.amt);
			}
		}
	}
	public void giveItems(String name, int id, int amt) throws InterruptedException {
		while (amt>0) {
			if (amt>64) {
				parent.runCommand("give " + name + " " + id + " " + 64);
				amt-=64;
			}
			else {
				parent.runCommand("give " + name + " " + id + " " + amt);
				amt=0;
			}
		}
	}
	public void listKits(Player p) {
		String line = "Allowed kits: ";
		for (Kit i: kits) {
			if (Group.contains(i.groups, p)) {
				line+=i.name+",";
			}
		}
		p.addMessage(line);
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		kits.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		Kit current=null;
		String[] tokens = line.split(",");
        for(int i=0;i<tokens.length;i++) {
        	if (i==0) {
        		String[] tokens2 = tokens[i].split(":");
        		current = new Kit(tokens2[0],Group.parseGroups(tokens2[1],";"));
        		kits.add(current);
        	}
        	else{ 
        		String[] tokens2 = tokens[i].split(":");
        		current.addItem(Integer.valueOf(tokens2[0]), Integer.valueOf(tokens2[1]));
        	}
        }
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (Kit i: kits) {
        	line+=i.name + ":";
        	for (int k: i.groups){ 
        		line+=k + ";";
        	}
        	line+= ",";
        	for (KitList.Kit.Entry j: i.items) {
        		line+=j.id + ":" + j.amt + ",";
        	}
        	line+="\r\n";
        }
		return line;
	}
}
