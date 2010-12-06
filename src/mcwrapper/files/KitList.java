package mcwrapper.files;

import java.util.LinkedList;

import mcwrapper.Player;
import mcwrapper.Server;

public class KitList extends FileLoader {
	Server parent;
	class Kit {
		String name;
		int rank;
		LinkedList<Entry> items = new LinkedList<Entry>();
		class Entry {
			int id;
			int amt;
			public Entry(int id, int amt) {
				this.id=id;
				this.amt=amt;
			}
		}
		public Kit(String name, int rank) {
			this.name=name;
			this.rank=rank;
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
	public void giveKit(String name, String kitName) {
		Kit kit=null;
		for (Kit i: kits) {
			if (kitName.toLowerCase().compareTo(i.name.toLowerCase())==0) {
				if (parent.ranks.checkName(name)>=i.rank)
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
	public void giveItems(String name, int id, int amt) {
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
		int rank=parent.ranks.checkName(p.getName());
		String line = "Allowed kits: ";
		for (Kit i: kits) {
			if (i.rank<=rank) {
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
        		current = new Kit(tokens2[0],Integer.valueOf(tokens2[1]));
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
        	line+=i.name + ":" + i.rank + ",";
        	for (KitList.Kit.Entry j: i.items) {
        		line+=j.id + ":" + j.amt + ",";
        	}
        	line+="\r\n";
        }
		return line;
	}
}
