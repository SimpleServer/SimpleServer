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
package simpleserver.export;

import simpleserver.Server;

public class CustAuthExport extends PropertiesExport {
  private Server server;

  public CustAuthExport(Server server) {
    super("custAuthData.txt", 2);
    this.server = server;
    header = "Export of custAuth data in the format: playerName=groupId,pwHash\n" +
        "DO NOT MODIFY THIS FILE!";
  }

  @Override
  protected void populate() {
    for (String playerName : server.data.players.names()) {
      byte[] pwHash = server.data.players.getPwHash(playerName);
      if (pwHash != null) {
        setEntry(playerName, String.valueOf(server.config.players.group(playerName)), hashToHex(pwHash));
      }
    }
  }

  public void addEntry(String playerName, int groupId, byte[] pwHash) {
    setEntry(playerName, String.valueOf(groupId), hashToHex(pwHash));
    save();
  }

  public void updatePw(String playerName, byte[] pwHash) {
    updateEntry(playerName, 1, hashToHex(pwHash));
    save();
  }

  public void updateGroup(String playerName, int groupId) {
    updateEntry(playerName, 0, String.valueOf(groupId));
    save();
  }

  private String hashToHex(byte[] pwHash) {
    StringBuffer hexString = new StringBuffer();
    for (byte element : pwHash) {
      hexString.append(Integer.toHexString(0xFF & element));
    }
    return hexString.toString();
  }
}
