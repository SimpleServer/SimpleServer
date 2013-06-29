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


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import simpleserver.Color;
import simpleserver.Translate;


public class Message {

  private Color color;
  private Translate translate;
  private String using;

  private boolean italic = false;

  private Gson gson = new Gson();
  private JsonObject jsonObject = new JsonObject();

  public Message(Color color, Translate translate, String message) {
    this.color = color;
    this.translate = translate;
    this.using = message;
  }

  public Message(String message) {
    this.using = message;
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

  private String getJson() {
    return gson.toJson(jsonObject);
  }
}
