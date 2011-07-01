/*
 * Copyright (c) 2010 SimpleServer authors (see CONTRIBUTORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package simpleserver;

import static simpleserver.lang.Translations.t;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

import simpleserver.config.AuthenticationList;

public class Authenticator {
  private static final int REFRESH_TIME = 120; // online status
  private static final String MINECRAFT_AUTH_URL = "http://50.16.200.224/game/checkserver.jsp";

  public static final int REQUEST_EXPIRATION = 60;
  private static final int REMEMBER_TIME = REQUEST_EXPIRATION;

  private static final short MAX_GUEST_PLAYERS = 50;
  private static final String GUEST_PREFIX = t("Player");

  private final Server server;
  private AuthenticationList auths;

  public boolean isMinecraftUp = true;
  private URL minecraftNet;
  private Timer timer;

  private LinkedList<AuthRequest> authRequests = new LinkedList<AuthRequest>();
  private Hashtable<String, loginBan> loginBans = new Hashtable<String, loginBan>();
  private PriorityQueue<Short> freeGuestNumbers = new PriorityQueue<Short>(MAX_GUEST_PLAYERS);
  private Hashtable<String, String> playerRenames = new Hashtable<String, String>();

  public Authenticator(Server se) {
    server = se;
    auths = server.auths;

    for (int i = 0; i < MAX_GUEST_PLAYERS;) {
      freeGuestNumbers.offer((short) ++i);
    }

    try {
      minecraftNet = new URL(MINECRAFT_AUTH_URL);
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
    }

    if (useCustAuth()) {
      timer = new Timer();
      timer.schedule(new MinecraftOnlineChecker(this), 0, REFRESH_TIME * 1000);
    }

    playerRenames.put("D0l4", "Notch");
  }

  /***** PERMISSIONS *****/
  public boolean vanillaOnlineMode() {
    return !server.options.getBoolean("custAuth") && server.options.getBoolean("onlineMode");
  }

  public boolean useCustAuth() {
    return server.options.getBoolean("onlineMode") && isMinecraftUp;
  }

  public boolean useCustAuth(Player player) {
    return useCustAuth() && !player.isGuest() && !player.usedAuthenticator();
  }

  public boolean allowLogin() {
    return server.options.getBoolean("custAuth");
  }

  public boolean allowRegistration() {
    return server.options.getBoolean("custAuth");
  }

  /***** REGISTRATION *****/

  public void register(Player player, String password) {
    auths.addAuthentication(player.getName(), password);
  }

  public boolean isRegistered(String playerName) {
    return auths.isRegistered(playerName);
  }

  public boolean changePassword(Player player, String oldPassword, String newPassword) {
    if (auths.passwordMatches(player.getName(), oldPassword)) {
      auths.changePassword(player.getName(), newPassword);
      return true;
    }
    return false;
  }

  /***** LOGIN *****/

  public boolean login(Player player, String playerName, String password) {
    // synchronize
    if (auths.passwordMatches(playerName, password)) {
      addLoginRequest(playerName, player.getIPAddress());
      return true;
    }
    return false;
  }

  private void addLoginRequest(String playerName, String IP) {
    authRequests.add(new AuthRequest(playerName, IP));
  }

  public void rememberAuthentication(String playerName, String IP) {
    authRequests.add(new AuthRequest(playerName, IP, false));
  }

  /***** LOGIN REQUEST VALIDATION / COMPLETE LOGIN *****/

  public AuthRequest getAuthRequest(String IP) {
    if (!allowLogin()) {
      return null;
    }
    ListIterator<AuthRequest> requests = authRequests.listIterator();
    AuthRequest res = null;

    while (requests.hasNext()) {
      AuthRequest current = requests.next();
      if (current.IP.equals(IP)) {
        res = current;
        /*if (current.isGuest) {
          releaseGuestName(current.playerName);
        }*/
        requests.remove();
        break;
      }

      if (!current.isValid()) {
        requests.remove();
      }
    }
    return res;
  }

  public boolean completeLogin(AuthRequest req, Player player) {
    // used custAuth or is remembered

    if (req.remember) {
      if (req.isValid()) {
        if (req.isGuest) {
          player.addTMessage(Color.GRAY, "Guestname remembered.");
          player.setGuest(true);
        } else {
          player.addTMessage(Color.GRAY, "Custom Authentication remembered.");
          player.setUsedAuthenticator(true);
          player.setGuest(false);
        }

        return player.setName(req.playerName);
      }
    } else {
      if (req.isValid()) {
        player.addTMessage(Color.GRAY, "Custom Authentication successfully completed.");
        player.setUsedAuthenticator(true);
        player.setGuest(false);
        return player.setName(req.playerName);
      } else {
        player.addTMessage(Color.RED, "Your custom Authentication expired. Please try again.");
      }
    }

    return false;
  }

  private void cleanLoginRequests() {
    ListIterator<AuthRequest> requests = authRequests.listIterator();
    while (requests.hasNext()) {
      AuthRequest current = requests.next();
      if (current.isValid()) {
        break;
      }
      if (current.isGuest) {
        releaseGuestName(current.playerName);
      }

      requests.remove();
    }
  }

  /***** RENAMING *****/
  public String renamePlayer(String name) {
    if (playerRenames.containsKey(name)) {
      return playerRenames.get(name);
    }
    return name;
  }

  /***** GUEST NAMES *****/

  public synchronized String getFreeGuestName() {
    String name;
    if (freeGuestNumbers.peek() == null) {
      name = "Player";
    } else {
      name = buildGuestName(freeGuestNumbers.poll());
    }

    return name;
  }

  public void rememberGuest(String playerName, String IP) {
    authRequests.add(new AuthRequest(playerName, IP, true));
  }

  public synchronized void releaseGuestName(String name) {
    freeGuestNumbers.offer(extractGuestNumber(name));
    // delete name.dat or set to empty
  }

  private short extractGuestNumber(String guestName) {
    return Short.parseShort(guestName.substring(GUEST_PREFIX.length()));
  }

  private String buildGuestName(short guestNumber) {
    return GUEST_PREFIX + guestNumber;
  }

  public boolean isGuestName(String name) {
    if (name.length() < GUEST_PREFIX.length()) {
      return false;
    }
    return name.substring(0, GUEST_PREFIX.length()).equals(GUEST_PREFIX);
  }

  /***** MINECRAFT.NET AUTHENTICATION *****/

  public boolean onlineAuthenticate(Player player) {
    if (useCustAuth()) {
      return true;
    }

    boolean result = false;
    // Send a GET request to minecraft.net

    String urlStr = MINECRAFT_AUTH_URL + String.format("?user=%s&serverId=%s", player.getName(true), player.getConnectionHash());
    try {
      URL url = new URL(urlStr);
      URLConnection conn = url.openConnection();

      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      result = (in.readLine().equals("YES")) ? true : false;
      in.close();

    } catch (MalformedURLException e) {
      System.out.println("[CustAuth] Malformed URL: " + urlStr);
    } catch (Exception e) {
      // seems to be down
      updateMinecraftState();
    }

    return result;
  }

  /***** MINECRAFT ONLINE STATE *****/

  public void updateMinecraftState() {
    boolean before = isMinecraftUp;

    try {
      HttpURLConnection mc = (HttpURLConnection) minecraftNet.openConnection();
      if (mc.getResponseCode() != 200) {
        isMinecraftUp = false;
      } else {
        isMinecraftUp = true;
      }
    } catch (IOException e) {
      // server not reachable
      isMinecraftUp = false;
    }

    if (before != isMinecraftUp) {
      if (!isMinecraftUp) {
        // just went down
        System.out.println("[SimpleServer] Minecraft.net just went down!");
      } else {
        // back online
        System.out.println("[SimpleServer] Minecraft.net is back online!");
      }
    }
  }

  @Override
  public void finalize() {
    timer.cancel();
  }

  public class AuthRequest {
    public String playerName;
    public String IP;
    public long expirationTime;
    public boolean remember = false;
    public boolean isGuest = false;

    public AuthRequest(String playerName, String IP) {
      this.playerName = playerName;
      this.IP = IP;
      expirationTime = System.currentTimeMillis() + (REQUEST_EXPIRATION * 1000);
    }

    public AuthRequest(String playerName, String IP, boolean isGuest) {
      this.playerName = playerName;
      this.IP = IP;
      expirationTime = System.currentTimeMillis() + (REMEMBER_TIME * 1000);
      remember = true;
      this.isGuest = isGuest;
    }

    public boolean isValid() {
      return (expirationTime >= System.currentTimeMillis());
    }
  }

  private class MinecraftOnlineChecker extends TimerTask {

    private Authenticator parent;

    public MinecraftOnlineChecker(Authenticator parent) {
      this.parent = parent;
    }

    @Override
    public void run() {
      parent.updateMinecraftState();
    }
  }

  /***** LOGIN BAN *****/
  public boolean loginBanTimeOver(Player player) {
    return !loginBans.containsKey(player.getName()) ||
        loginBans.get(player.getName()).isBanOver();
  }

  public void banLogin(Player player) {
    if (loginBans.containsKey(player.getName())) {
      loginBans.get(player.getName()).increaseLevel();
    } else {
      loginBans.put(player.getName(), new loginBan());
    }
  }

  public void unbanLogin(Player player) {
    loginBans.remove(player.getName());
  }

  public int leftBanTime(Player player) {
    return loginBans.get(player.getName()).getLeftBanTime();
  }

  static class loginBan {
    int banLevel = 0;
    long endTime;

    public loginBan() {
      increaseLevel();
    }

    public void increaseLevel() {
      setEndTime(++banLevel);
    }

    private void setEndTime(int level) {
      endTime = System.currentTimeMillis() + 1000 * banTime(level);
    }

    public int getLeftBanTime() {
      return (int) (endTime - System.currentTimeMillis()) / 1000;
    }

    public boolean isBanOver() {
      return (getLeftBanTime() <= 0);
    }

    public static int banTime(int level) {
      return (int) Math.pow(2, level) / 10;
    }
  }
}
