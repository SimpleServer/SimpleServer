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

  private static final int MAX_GUEST_PLAYERS = 20;
  private static final String GUEST_PREFIX = t("Player");

  private final Server server;
  private AuthenticationList auths;

  public boolean isMinecraftUp = true;
  private URL minecraftNet;
  private Timer timer;

  private LinkedList<LoginRequest> loginRequests = new LinkedList<LoginRequest>();
  // private LindedList<Guests>
  private PriorityQueue<Integer> freeGuestNumbers = new PriorityQueue<Integer>(MAX_GUEST_PLAYERS);

  public Authenticator(Server simpleserver) {
    server = simpleserver;
    auths = server.auths;

    for (int i = 0; i < MAX_GUEST_PLAYERS; i++) {
      freeGuestNumbers.offer(i + 1);
    }

    try {
      minecraftNet = new URL(MINECRAFT_AUTH_URL);
    } catch (MalformedURLException e1) {
      e1.printStackTrace();
    }

    if (server.options.getBoolean("onlineMode")) {
      timer = new Timer();
      timer.schedule(new MinecraftOnlineChecker(this), 0, REFRESH_TIME * 1000);
    }
  }

  /***** PERMISSIONS *****/

  public boolean allowLogin() {
    return server.options.getBoolean("custAuth") && (!isMinecraftUp || !server.options.getBoolean("onlineMode"));
  }

  public boolean allowRegistration() {
    return server.options.getBoolean("custAuth") && (isMinecraftUp || !server.options.getBoolean("onlineMode"));
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
    loginRequests.add(new LoginRequest(playerName, IP));
  }

  public void rememberAuthentication(String playerName, String IP) {
    loginRequests.add(new LoginRequest(playerName, IP, false));
  }

  /***** LOGIN REQUEST VALIDATION / COMPLETE LOGIN *****/

  public LoginRequest getLoginRequest(String IP) {
    ListIterator<LoginRequest> requests = loginRequests.listIterator();
    LoginRequest res = null;

    while (requests.hasNext()) {
      LoginRequest current = requests.next();
      if (current.IP.equals(IP)) {
        res = current;
        if (current.isGuest) {
          releaseGuestName(current.playerName);
        }
        requests.remove();
        break;
      }

      if (!current.isValid()) {
        requests.remove();
      }
    }
    return res;
  }

  public boolean completeLogin(LoginRequest req, Player player) {
    // used custAuth or is remembered

    if (req.remember) {
      if (req.isValid()) {
        if (req.isGuest) {
          player.addTMessage(Color.GRAY, "Guestname remembered.");
          player.setGuest(true);
        } else {
          player.addTMessage(Color.GRAY, "Custom Authentication remembered.");
          player.setUsedAuthenticator(true);
        }

        return player.setName(req.playerName);
      }
    } else {
      if (req.isValid()) {
        player.addTMessage(Color.GRAY, "Custom Authentication successfully completed.");
        player.setUsedAuthenticator(true);

        return player.setName(req.playerName);
      } else {
        player.addTMessage(Color.RED, "Your custom Authentication expired. Please try again.");
      }
    }

    return false;
  }

  private void cleanLoginRequests() {
    ListIterator<LoginRequest> requests = loginRequests.listIterator();
    while (requests.hasNext()) {
      LoginRequest current = requests.next();
      if (current.isValid()) {
        break;
      }
      if (current.isGuest) {
        releaseGuestName(current.playerName);
      }

      requests.remove();
    }
  }

  /***** GUEST NAMES *****/

  public String getFreeGuestName(String IP) {
    String name = null;
    LoginRequest req = getLoginRequest(IP);
    if (req != null && req.isValid() && server.playerList.findPlayerExact(req.playerName) != null) {
      return req.playerName;
    } else {

      name = buildGuestName(freeGuestNumbers.poll());
    }

    return name;
  }

  public void rememberGuest(String playerName, String IP) {
    loginRequests.add(new LoginRequest(playerName, IP, true));
  }

  public void releaseGuestName(String name) {
    freeGuestNumbers.offer(extractGuestNumber(name));
  }

  private int extractGuestNumber(String guestName) {
    return Integer.parseInt(guestName.substring(GUEST_PREFIX.length()));
  }

  private String buildGuestName(int guestNumber) {
    return GUEST_PREFIX + guestNumber;
  }

  /***** MINECRAFT.NET AUTHENTICATION *****/

  public boolean onlineAuthenticate(Player player) {
    if (!server.options.getBoolean("custAuth") || !server.options.getBoolean("onlineMode")) {
      return true;
    }
    if (!isMinecraftUp) {
      return true;
    }

    boolean result = false;
    // Send a GET request to minecraft.net
    try {

      String urlStr = MINECRAFT_AUTH_URL + "?user=" + player.getName() + "&serverId=" + player.getConnectionHash();

      URL url = new URL(urlStr);
      URLConnection conn = url.openConnection();

      BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

      result = (in.readLine().equals("YES")) ? true : false;
      // System.out.println("onlineAuthentcation: " + result);

      in.close();
    } catch (Exception e) {
      // seems to be down
      e.printStackTrace();
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
        server.runCommand("say", t("Minecraft.net just went down!"));
      } else {
        // back online
        server.runCommand("say", t("minecraft.net is back online!"));
      }
    }

    String o = "[SimpleServer] Minecraft.net is ";
    o += (isMinecraftUp) ? "up!" : "down!";
    System.out.println(o);
  }

  @Override
  protected void finalize() {
    timer.cancel();
  }

  public class LoginRequest {
    public String playerName;
    public String IP;
    public long expirationTime;
    public boolean remember = false;
    public boolean isGuest = false;

    public LoginRequest(String playerName, String IP) {
      this.playerName = playerName;
      this.IP = IP;
      expirationTime = System.currentTimeMillis() + (REQUEST_EXPIRATION * 1000);
    }

    public LoginRequest(String playerName, String IP, boolean isGuest) {
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
}
