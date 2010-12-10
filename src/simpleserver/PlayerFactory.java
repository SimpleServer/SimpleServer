/*******************************************************************************
 * Copyright (C) 2010 Charles Wagner Jr..
 * spiegalpwns@gmail.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
