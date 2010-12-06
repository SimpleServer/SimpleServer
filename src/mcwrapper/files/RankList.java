package mcwrapper.files;

import java.util.LinkedList;

import mcwrapper.Server;

public class RankList extends FileLoader {
	class Rank {
		String name;
		int rank;
		public Rank(String name, int rank) {
			this.name=name;
			this.rank=rank;
		}
	}
	LinkedList<Rank> ranks = new LinkedList<Rank>();
	Server parent;
	public RankList(Server parent) {
		this.filename="rank-list.txt";
		this.parent=parent;
	}
	public int checkName(String name) {
		if (name!=null) {
			for (Rank i: ranks) {
				if (name.toLowerCase().trim().compareTo(i.name.toLowerCase().trim())==0) {
					return i.rank;
				}
			}
		}
		return parent.options.defaultRank;
	}
	public void setRank(String name, int rank) {
		for (Rank i: ranks) {
			if (name.toLowerCase().trim().compareTo(i.name.toLowerCase().trim())==0) {
				i.rank=rank;
				save();
				return;
			}
		}
		ranks.add(new Rank(name,rank));
		parent.setRank(name);
		save();
	}
	
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		ranks.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		String[] tokens = line.split("=");
		if (tokens.length>=2) 
			ranks.add(new Rank(tokens[0],Integer.valueOf(tokens[1])));
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (Rank i: ranks) {
        	line+=i.name + "=" + i.rank + "\r\n";
        }
		return line;
	}
}
