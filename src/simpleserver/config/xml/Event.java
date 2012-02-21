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
package simpleserver.config.xml;

import java.util.ArrayList;
import java.util.List;
import simpleserver.Coordinate;

import org.xml.sax.SAXException;

public class Event extends XMLTag implements Comparable<Event>{
  public String name;
  public Coordinate coordinate;
  public int interval = 0;
  public boolean disabled = false;
  public String script = null;
  public Permission allow = null;
  public boolean isbutton = false;

  public String value = "";

  //private ArgumentStorage arguments;

  private static final String NAME = "name";
  private static final String COORDINATE = "coordinate";
  private static final String DIMENSION = "dimension";
  private static final String INTERVAL = "interval";
  private static final String DISABLED = "disabled";
  private static final String VALUE = "value";
  private static final String ALLOW = "allow";
  private static final String ISBUTTON = "isbutton";

  Event() {
    super("event");
  }

  public Event(String name, Coordinate coord) {
    this();
    this.name = name;
    this.coordinate = coord;
  }

  @Override
  void setAttribute(String name, String value) throws SAXException {
    if (name.equals(NAME)) {
      this.name = value;
    } else if (name.equals(COORDINATE)) {
      this.coordinate = Coordinate.fromString(value);
    } else if (name.equals(INTERVAL)) {
      interval = Integer.valueOf(value);
    } else if (name.equals(ALLOW)) {
      allow = new Permission(value);
    } else if (name.equals(DISABLED)) {
      disabled = value != null && !value.equals("false") ? true : false;
    } else if (name.equals(ISBUTTON)) {
      isbutton = value != null && !value.equals("false") ? true : false;
    } else if (name.equals(VALUE)) {
      this.value = value;
    } else if (name.equals(DIMENSION)) {
      this.coordinate = coordinate.setDimension(Coordinate.Dimension.get(value));
    }
  }

  @Override
  void content(String cont) {
      script = (script == null) ? cont : script + cont;
  }

  @Override
  void saveAttributes(AttributeList attributes) {
    attributes.addAttribute(NAME, name);
    if (coordinate!=null)
      attributes.addAttribute(COORDINATE,coordinate.toString());
    if (interval != 0)
      attributes.addAttribute(INTERVAL, interval);
    if (script!=null && !script.equals(""))
      attributes.setValue("script",script);
    if (allow!=null)
      attributes.addAttribute(ALLOW, allow);
    if (disabled)
      attributes.addAttribute(DISABLED,"true");
    if (isbutton)
      attributes.addAttribute(ISBUTTON,"true");
    if (!value.equals(""))
      attributes.addAttribute(VALUE, value);
    if (coordinate.dimension() != Coordinate.Dimension.get("Earth"))
      attributes.addAttribute(DIMENSION, coordinate.dimension().toString());
  }

  public int compareTo(Event ev) {
    return name.compareTo(ev.name);
  }

}
