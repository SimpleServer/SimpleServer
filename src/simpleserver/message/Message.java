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
package simpleserver.message;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import org.apache.commons.lang.StringUtils;
import simpleserver.Color;
import simpleserver.Translate;

import java.lang.reflect.Type;
import java.util.Collection;

public class Message {

  private Color color;
  private Translate translate;
  private String using;

  private boolean italic = false;

  private Gson gson = new GsonBuilder().create();
  private JsonObject jsonObject = new JsonObject();

  public Message(Color color, Translate translate, String message) {
    this.color = color;
    this.translate = translate;
    this.using = message;
  }

  public Message(String message) {
    this.using = message;
  }

  public Message() {

  }

  public void setItalic(boolean val) {
    this.italic = val;
  }

  public String buildMessage() {
    if (color != null) {
      jsonObject.addProperty("color", color.toColorString());
    }
    jsonObject.addProperty("italic", italic);
    jsonObject.addProperty("translate", translate.toString());
    jsonObject.addProperty("using", using);
    return getJson();
  }

  public String buildMessage(boolean old_style) {
    if (old_style) {
      jsonObject.addProperty("text", StringUtils.trim(using));
      return getJson();
    }  else {
      return buildMessage();
    }
  }

  public MessagePacket decodeMessage(String msg) {
    MessagePacket messagePacket;
    try {
      messagePacket = gson.fromJson(msg, MessagePacket.class);
    } catch (JsonParseException ex) {
      messagePacket = null;
    }
    return messagePacket;
  }

  public ServerList decodeServerList(String msg) {
    ServerList serverList;
    try {
      serverList = gson.fromJson(msg, ServerList.class);
    } catch(JsonParseException ex) {
      serverList = null;
    }
    return serverList;
  }

  public String encodeServerList(ServerList serverList) {
    return gson.toJson(serverList);
  }

  private String getJson() {
    return gson.toJson(jsonObject);
  }
}

