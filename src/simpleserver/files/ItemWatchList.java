package simpleserver.files;

import java.util.LinkedList;

import simpleserver.Player;
import simpleserver.Server;


public class ItemWatchList extends FileLoader {
	Server parent;
	static class ItemEntry {
		int id;
		int threshold;
		int[] groups;
		public ItemEntry(int id, int threshold, int[] groups) {
			this.id=id;
			this.threshold = threshold;
			this.groups=groups;
		}
	}
	LinkedList<ItemEntry> items = new LinkedList<ItemEntry>();
	
	@SuppressWarnings("unused")
	private ItemWatchList(){}
	
	public ItemWatchList(Server parent) {
		this.parent=parent;
		this.filename="item-watch-list.txt";
	}
	
	
	public boolean checkCheck(int blockID) {
		for (ItemEntry i: items) {
			if (i.id==blockID) {
				return true;
			}
		}
		return false;
	}
	public boolean checkAllowed(Player p, int blockID, int amt) {
		for (ItemEntry i: items) {
			if (i.id==blockID) {
				if (amt>=i.threshold)
					return Group.contains(i.groups,p);
			}
		}
		return true;
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		items.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		String[] tokens = line.split(":");
		if (tokens.length>=3) 
			items.add(new ItemEntry(Integer.valueOf(tokens[0]),Integer.valueOf(tokens[1]),Group.parseGroups(tokens[2])));
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (ItemEntry i: items) {
        	line+=i.id + ":" + i.threshold + ":";
        	for(int group: i.groups) {
        		line+= group+",";
        	}
        	line+="\r\n";
        }
		return line;
	}
}
