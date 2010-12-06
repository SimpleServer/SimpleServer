package mcwrapper.files;

import java.util.LinkedList;

import mcwrapper.Server;

public class BlockFirewallList extends FileLoader {
	Server parent;
	class BlockEntry {
		int id;
		int rank;
		public BlockEntry(int id, int rank) {
			this.id=id;
			this.rank=rank;
		}
	}
	LinkedList<BlockEntry> blocks = new LinkedList<BlockEntry>();
	@SuppressWarnings("unused")
	private BlockFirewallList(){}
	public BlockFirewallList(Server parent) {
		this.parent=parent;
		this.filename="block-list.txt";
	}
	public boolean checkCheck(int blockID) {
		for (BlockEntry i: blocks) {
			if (i.id==blockID) {
				return true;
			}
		}
		return false;
	}
	public boolean checkAllowed(String name, int blockID) {
		int blockRank=0;
		for (BlockEntry i: blocks) {
			if (i.id==blockID) {
				blockRank=i.rank;
			}
		}
		if (parent.ranks.checkName(name)<blockRank) 
			return false;
		return true;
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		blocks.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		String[] tokens = line.split(":");
		if (tokens.length>=2) 
			blocks.add(new BlockEntry(Integer.valueOf(tokens[0]),Integer.valueOf(tokens[1])));
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (BlockEntry i: blocks) {
        	line+=i.id + ":" + i.rank + "\r\n";
        }
		return line;
	}
}
