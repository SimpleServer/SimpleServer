package simpleserver.files;

import java.util.LinkedList;

import simpleserver.Player;
import simpleserver.Server;


public class IPMemberList extends FileLoader {
	static class Member {
		String ip;
		int group;
		public Member(String ip, int group) {
			this.ip=ip;
			this.group=group;
		}
	}
	LinkedList<Member> members = new LinkedList<Member>();
	Server parent;
	public IPMemberList(Server parent) {
		this.filename="ip-member-list.txt";
		this.parent=parent;
	}
	public int checkPlayer(Player p) {
		if (p!=null) {
			String ip = p.extsocket.getInetAddress().getHostAddress();
			for (Member i: members) {
				if (ip.startsWith(i.ip)) {
					return i.group;
				}
			}
		}
		return parent.options.defaultGroup;
	}
	public void setRank(Player p, int group) {
		if (p!=null) {
			String ip = p.extsocket.getInetAddress().getHostAddress();
			for (Member i: members) {
				if (i.ip.compareTo(ip.trim())==0) {
					i.group=group;
					save();
					return;
				}
			}
			members.add(new Member(ip.trim(),group));
			
			save();
		}
	}
	
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		members.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		String[] tokens = line.split("=");
		if (tokens.length>=2) 
			members.add(new Member(tokens[0],Integer.valueOf(tokens[1])));
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (Member i: members) {
        	line+=i.ip + "=" + i.group + "\r\n";
        }
		return line;
	}
}
