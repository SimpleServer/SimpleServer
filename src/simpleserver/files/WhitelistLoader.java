package simpleserver.files;

import java.util.LinkedList;

public class WhitelistLoader extends FileLoader {
	LinkedList<String> users = new LinkedList<String>();
	public WhitelistLoader() {
		this.filename="white-list.txt";
	}
	public boolean isWhitelisted(String name) {
		for (String i: users) {
			if (name.toLowerCase().trim().equals(i.toLowerCase().trim())) {
				return true;
			}
		}
		return false;
	}
	public void addName(String name) {
		users.add(name);
		save();
	}
	public boolean removeName(String name) {
		for (String i: users) {
			if (name.toLowerCase().trim().compareTo(i.toLowerCase().trim())==0) {
				users.remove(i);
				save();
				return true;
			}
		}
		return false;	
	}
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		users.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		users.add(line);
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (String i: users) {
        	line+=i + "\r\n";
        }
		return line;
	}
}
