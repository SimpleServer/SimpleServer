package simpleserver;

import java.net.Socket;
import java.util.Iterator;

public class PlayerFactory {
	
	private static Server parent;
	private static Player[] players;
	public static Object playerLock = new Object();
	
	static class PFIterator implements Iterator<Player> {
		private int i = 0;
		private int next = -1;
		
		public boolean hasNext() {
			for (int j=i;j<PlayerFactory.players.length;j++) {
				if (PlayerFactory.players[j]!=null) {
					if (PlayerFactory.players[j].intsocket!=null) {
						next = j;
						return true;
					}
				}
			}
			return false;
		}

		
		public Player next() {
			if (next>=0) {
				i=next+1;
				return PlayerFactory.players[next];
			}
			for (int j=i;j<PlayerFactory.players.length;j++) {
				if (PlayerFactory.players[j]!=null) {
					if (PlayerFactory.players[j].intsocket!=null) {
						i=j+1;
						return PlayerFactory.players[j];
					}
				}
			}
			return null;
		}

		
		public void remove() {
			if (i>0) {
				PlayerFactory.removePlayer(PlayerFactory.players[i-1]);
			}
		}
		
	}
	public static Iterator<Player> iterator() {
		return new PFIterator();
	}
	public static void initialize(Server s, int maxPlayers) {
		parent = s;
		players = new Player[maxPlayers*2];
	}
	public static Player findPlayer(String name) {
		for(Player i: players) {
			if (i!=null) {
				if (i.getName()!=null && i.extsocket!=null && i.isClosed()==false) {
					if (i.getName().toLowerCase().startsWith(name.toLowerCase())){
						return i;
					}
				}
			}
		}
		return null;
	}
	public static Player findPlayerExact(String name) {
		for(Player i: players) {
			if (i!=null) {
				if (i.getName()!=null && i.extsocket!=null && i.isClosed()==false) {
					if (i.getName().equalsIgnoreCase(name)){
						return i;
					}
				}
			}
		}
		return null;
	}
	public static void removePlayer(Player p) {
		if (p!=null)
			if (p.intsocket!=null)
				p.cleanup();
	}
	public static Player addPlayer(Socket sock) {
		for (int i=0;i<players.length;i++) {
			if (players[i]==null) {
				players[i] = new Player(sock, parent);
				return players[i];
			}
			if (players[i].intsocket==null) {
				players[i].reinitialize(sock);
				return players[i];
			}
		}
		return null;
	}
}
