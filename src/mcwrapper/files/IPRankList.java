package mcwrapper.files;

import java.util.LinkedList;

import mcwrapper.Player;
import mcwrapper.Server;

public class IPRankList extends FileLoader {
	class Rank {
		String ip;
		int rank;
		public Rank(String ip, int rank) {
			this.ip=ip;
			this.rank=rank;
		}
	}
	LinkedList<Rank> ranks = new LinkedList<Rank>();
	Server parent;
	public IPRankList(Server parent) {
		this.filename="ip-rank-list.txt";
		this.parent=parent;
	}
	public int checkPlayer(Player p) {
		if (p!=null) {
			String ip = p.extsocket.getInetAddress().getHostAddress();
			for (Rank i: ranks) {
				if (ip.startsWith(i.ip)) {
					
					return i.rank;
				}
			}
		}
		return parent.options.defaultRank;
	}
	public void setRank(Player p, int rank) {
		if (p!=null) {
			String ip = p.extsocket.getInetAddress().getHostAddress();
			for (Rank i: ranks) {
				if (i.ip.compareTo(ip.trim())==0) {
					i.rank=rank;
					save();
					return;
				}
			}
			ranks.add(new Rank(ip.trim(),rank));
			
			save();
		}
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
        	line+=i.ip + "=" + i.rank + "\r\n";
        }
		return line;
	}
}
