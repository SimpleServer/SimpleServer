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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

public class Authenticator {
  private static final int REFRESH_TIME = 120; // online status
  private static final String MINECRAFT_AUTH_URL = "http://session.minecraft.net/game/checkserver.jsp";

  public static final int REQUEST_EXPIRATION = 60;
  private static final int REMEMBER_TIME = REQUEST_EXPIRATION;

  private static final short MAX_GUEST_PLAYERS = 50;
  private static final String GUEST_PREFIX = t("Player");

  private final Server server;

  public boolean isMinecraftUp = true;
  private URL minecraftNet;
  private MessageDigest shaMD;
  private Timer timer;

  private LinkedList<AuthRequest> authRequests = new LinkedList<AuthRequest>();
  private Hashtable<String, loginBan> loginBans = new Hashtable<String, loginBan>();
  private PriorityQueue<Short> freeGuestNumbers = new PriorityQueue<Short>(MAX_GUEST_PLAYERS);

  public Authenticator(Server se) {
    server = se;

    for (int i = 0; i < MAX_GUEST_PLAYERS;) {
      freeGuestNumbers.offer((short) ++i);
    }

    try {
      minecraftNet = new URL(MINECRAFT_AUTH_URL);
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
    }

    try {
      shaMD = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException nsae) {
      System.out.println("Attention: Seems like MessageDigest is missing in your Java installation...");
    }

    if (useCustAuth()) {
      timer = new Timer();
      timer.schedule(new MinecraftOnlineChecker(this), 0, REFRESH_TIME * 1000);
    }
  }

  /***** PERMISSIONS *****/
  public boolean vanillaOnlineMode() {
    return false;
  }

  public boolean useCustAuth() {
    return server.config.properties.getBoolean("onlineMode") && isMinecraftUp;
  }

  public boolean useCustAuth(Player player) {
    return useCustAuth() && !player.isGuest() && !player.usedAuthenticator();
  }

  public boolean allowGuestJoin() {
    return server.config.properties.getBoolean("custAuth") || !server.config.properties.getBoolean("onlineMode");
  }

  public boolean allowLogin() {
    return server.config.properties.getBoolean("custAuth");
  }

  public boolean allowRegistration() {
    return server.config.properties.getBoolean("custAuth");
  }

  /***** REGISTRATION *****/

  public void register(String playerName, String password) {
    byte[] pwHash = generateHash(password, playerName);
    server.data.players.setPw(playerName, pwHash);
    server.data.players.setRealName(playerName);
    server.data.save();

    if (server.options.getBoolean("enableCustAuthExport")) {
      Integer groupId = server.config.players.group(playerName);
      if (groupId == null) {
        groupId = server.config.properties.getInt("defaultGroup");
      }
      server.custAuthExport.addEntry(playerName, groupId, pwHash);
    }
  }

  public boolean isRegistered(String playerName) {
    return server.data.players.getPwHash(playerName) != null;
  }

  public boolean changePassword(Player player, String oldPassword, String newPassword) {
    String playerName = player.getName();
    if (passwordMatches(playerName, oldPassword)) {
      byte[] pwHash = generateHash(newPassword, playerName);
      server.data.players.setPw(playerName, pwHash);
      server.data.save();

      if (server.options.getBoolean("enableCustAuthExport")) {
        server.custAuthExport.updatePw(playerName, pwHash);
      }
      return true;
    }
    return false;
  }

  /***** LOGIN *****/

  public boolean login(Player player, String playerName, String password) {
    if (passwordMatches(playerName, password)) {
      addLoginRequest(playerName, player.getIPAddress());
      return true;
    }
    return false;
  }

  private boolean passwordMatches(String playerName, String password) {
    return Arrays.equals(generateHash(password, playerName), getPasswordHash(playerName));
  }

  private String getRealPlayerName(String playerName) {
    return server.data.players.getRealName(playerName);
  }

  private void addLoginRequest(String playerName, String IP) {
    authRequests.add(new AuthRequest(playerName, IP));
  }

  public void rememberAuthentication(String playerName, String IP) {
    authRequests.add(new AuthRequest(playerName, IP, false));
  }

  /***** PW HASHING *****/

  private byte[] getPasswordHash(String playerName) {
    return server.data.players.getPwHash(playerName);
  }

  private byte[] generateHash(String pw, String playerName) {
    byte[] salt = getSHA(playerName.toLowerCase().getBytes());
    byte[] pwArray = pw.getBytes();
    byte[] toHash = new byte[salt.length + pwArray.length];
    System.arraycopy(pwArray, 0, toHash, 0, pwArray.length);
    System.arraycopy(salt, 0, toHash, pwArray.length, salt.length);
    return getSHA(toHash);
  }

  private byte[] getSHA(byte[] s) {
    // returns SHA-256 Hash of a String

    shaMD.reset();
    shaMD.update(s);
    byte[] encrypted = shaMD.digest();

    return encrypted;
  }

  /***** LOGIN REQUEST VALIDATION / COMPLETE LOGIN *****/

  public synchronized AuthRequest getAuthRequest(String IP) {
    if (!allowLogin()) {
      return null;
    }
    ListIterator<AuthRequest> requests = authRequests.listIterator();
    AuthRequest res = null;

    while (requests.hasNext()) {
      AuthRequest current = requests.next();
      if (current.IP.equals(IP)) {
        res = current;
        requests.remove();
        break;
      }

      if (!current.isValid()) {
        if (current.isGuest) {
          releaseGuestName(current.playerName);
        }
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
      if (!current.isValid()) {
        if (current.isGuest) {
          releaseGuestName(current.playerName);
        }
        requests.remove();
      }
    }
  }

  /***** RENAMING *****/
  public String renamePlayer(String name) {
    return server.data.players.getRenameName(name);
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

    File dat = new File(server.options.get("levelName") + File.separator + "players" + File.separator + name + ".dat");
    server.bots.trash(dat);
  }

  private static short extractGuestNumber(String guestName) {
    return Short.parseShort(guestName.substring(GUEST_PREFIX.length()));
  }

  private static String buildGuestName(short guestNumber) {
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
    if (!useCustAuth(player)) {
      return true;
    }

    boolean result = false;
    // Send a GET request to minecraft.net

    String urlStr;
    try {
      urlStr = MINECRAFT_AUTH_URL + String.format("?user=%s&serverId=%s", player.getName(true), player.getLoginHash());
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
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
      System.out.println("[CustAuth] Could not reach authentication url: " + urlStr);
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
    try {
      timer.cancel();
    } catch (Exception e) {
    }
    cleanLoginRequests();
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
      return (int) Math.pow(2, level);
    }
  }

  public class AuthRequest {
    public String playerName;
    public String IP;
    public long expirationTime;
    public boolean remember = false;
    public boolean isGuest = false;

    public AuthRequest(String playerName, String IP) {
      this.playerName = getRealPlayerName(playerName);
      this.IP = IP;
      expirationTime = System.currentTimeMillis() + (REQUEST_EXPIRATION * 1000);
    }

    public AuthRequest(String playerName, String IP, boolean isGuest) {
      this.playerName = getRealPlayerName(playerName);
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
}
