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
package simpleserver.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import simpleserver.Main;
import simpleserver.Server;
import simpleserver.nbt.WorldFile;

/*
 * This class is used to collect usage statistics of all SimpleServer users. To fulfill
 * this task it is necessary to send a short request to our server when starting SimpleServer
 * and then an even shorter one every minute it runs. This only uses a negligible amount of
 * computational power and bandwidth and should not impair your gaming experience in any way.
 * 
 * In order to preserve your privacy the gathered data is stored anonymously which means
 * your server is only identified by your random map seed and the first two bytes of your
 * IP address (if your IP is 135.42.67.166 only 135.42 is stored).
 * 
 * The collected data consists of the following information:
 *   SimpleServer version
 *   Server options: alternativeJar, onlineMode, customAuth, maxPlayers
 *   Server statistics: number of registered players, total playing hours
 *  
 * Your data will not be used in any evil plots to take over the world. Before publishing
 * it in any way your server identifier (see above) is removed to guarantee complete anonymity.
 * 
 * If you are paranoid or just don't want to support the developers of SimpleServer you can turn
 * this feature off by adding the property "disableStatistics" with value "true" to your
 * simpleserver.proeprties file.
 * 
 */

public class Statistics extends Thread {
  private static final int VERSION = 1;
  private static final String URL = "ibotpeaches.com/ss";
  private static final String PATH = "/api/" + VERSION;

  private Server server;
  private boolean run = true;
  private int id;

  public Statistics(Server server) {
    this.server = server;
    this.run();
  }

  private void getSessionId() {
    WorldFile world = null;
    try {
      world = new WorldFile(server);
    } catch (Exception ex) {
    }

    long seed = 0;
    try {
      seed = world.seed();
    } catch (NullPointerException ignored) {

    }

    JSONObject data = new JSONObject();
    JSONObject stats = new JSONObject();
    JSONObject options = new JSONObject();
    try {
      data.put("options", options);
      data.put("stats", stats);
      data.put("version", Main.version);

      options.put("jar", server.options.get("alternateJarFile"));
      options.put("onlineMode", server.config.properties.getBoolean("onlineMode"));
      options.put("custAuth", server.config.properties.getBoolean("custAuth"));
      options.put("maxPlayers", server.config.properties.getInt("maxPlayers"));

      stats.put("registeredPlayers", server.data.players.count());
      stats.put("totalHours", server.data.players.stats.totalHours());

      JSONObject response = getRemoteJSONObject(data, "connect", Long.toHexString(seed));
      id = response.getInt("id");
    } catch (Exception e) {
      run = false;
    }
  }

  private void heartbeat() {
    try {
      getRemoteJSONObject("heartbeat", Integer.toString(id));
    } catch (Exception e) {
    }
  }

  public void halt() {
    run = false;
    interrupt();
  }

  @Override
  public void run() {
    if (server.options.getBoolean("disableStatistics")) {
      return;
    }
    getSessionId();
    while (run) {
      try {
        heartbeat();
        Thread.sleep(60000);
      } catch (InterruptedException e) {
      }
    }
  }

  private static JSONObject getRemoteJSONObject(String... params) throws JSONException, IOException, URISyntaxException {
    return getRemoteJSONObject(null, params);
  }

  private static JSONObject getRemoteJSONObject(JSONObject data, String... params) throws JSONException, IOException, URISyntaxException {
    StringBuilder path = new StringBuilder(PATH);
    for (String param : params) {
      path.append('/');
      path.append(param);
    }

    return new JSONObject(getRemoteContent(new URI("http", URL, path.toString(), null).toURL(), data));
  }

  private static String getRemoteContent(URL url, JSONObject data) throws IOException {
    URLConnection con = url.openConnection();
    if (data != null) {
      String post = "data=" + URLEncoder.encode(data.toString(), "UTF-8");
      con.setDoOutput(true);
      OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
      writer.write(post);
      writer.flush();
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

    StringBuilder content = new StringBuilder();

    while (reader.ready()) {
      content.append(reader.readLine());
    }

    return content.toString();
  }
}
