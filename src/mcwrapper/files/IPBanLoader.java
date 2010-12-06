package mcwrapper.files;

import java.util.LinkedList;

public class IPBanLoader extends FileLoader {
	LinkedList<String> bans = new LinkedList<String>();
	public IPBanLoader() {
		this.filename="ip-ban-list.txt";
	}
	public void addBan(String ipAddress) {
		bans.add(ipAddress);
		save();
	}
	public boolean removeBan(String ipAddress) {
		for (String i: bans) {
			if (i.compareTo(ipAddress)==0) {
				bans.remove(i);
				save();
				return true;
			}
		}
		return false;
	}
	public boolean isBanned(String ipAddress) {
		for (String i: bans) {
			if (ipAddress.startsWith(i) && i.length()!=0) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected void beforeLoad() {
		// TODO Auto-generated method stub
		bans.clear();
	}
	@Override
	protected void loadLine(String line) {
		// TODO Auto-generated method stub
		if (line!=null && line!="") 
			bans.add(line);
	}
	@Override
	protected String saveString() {
		// TODO Auto-generated method stub
		String line="";
		for (String i: bans) {
        	line+=i + "\r\n";
        }
		return line;
	}
}
