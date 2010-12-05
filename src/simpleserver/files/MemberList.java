package simpleserver.files;

import java.util.LinkedList;

import simpleserver.Server;


public class MemberList extends FileLoader {
	static class Member {
		String name;
		int group;
		public Member(String name, int group) {
			this.name=name;
			this.group=group;
		}
	}
	LinkedList<Member> members = new LinkedList<Member>();
	Server parent;
	public MemberList(Server parent) {
		this.filename="member-list.txt";
		this.parent=parent;
	}
	public int checkName(String name) {
		if (name!=null) {
			for (Member i: members) {
				if (name.toLowerCase().trim().compareTo(i.name.toLowerCase().trim())==0) {
					return i.group;
				}
			}
		}
		return parent.options.defaultGroup;
	}
	public void setGroup(String name, int group) throws InterruptedException {
		if (group>0&&!parent.groups.groupExists(group))
			return;
		for (Member i: members) {
			if (name.toLowerCase().trim().compareTo(i.name.toLowerCase().trim())==0) {
				
				i.group=group;
				parent.updateGroup(name);
				save();
				return;
			}
		}
		members.add(new Member(name,group));
		parent.updateGroup(name);
		save();
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
        	line+=i.name + "=" + i.group + "\r\n";
        }
		return line;
	}
}
