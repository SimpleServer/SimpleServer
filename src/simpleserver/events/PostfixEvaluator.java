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
package simpleserver.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import simpleserver.Player;
import simpleserver.config.GiveAliasList.Item;
import simpleserver.config.xml.Area;

@SuppressWarnings("unused")
public class PostfixEvaluator {
  private RunningEvent e;
  private ArrayList<String> expstack;
  private static HashMap<String, String> ops;

  PostfixEvaluator(RunningEvent context) {
    e = context;
    expstack = new ArrayList<String>();

    if (ops == null) {
      initOps();
    }
  }

  private void initOps() {
    ops = new HashMap<String, String>(); // maps operator token -> method name

    ops.put("not", "not");
    ops.put("and", "and");
    ops.put("or", "or");

    ops.put("+", "add");
    ops.put("-", "sub");
    ops.put("*", "mul");
    ops.put("/", "div");
    ops.put("%", "mod");

    ops.put("gt", "gt");
    ops.put("lt", "lt");
    ops.put("eq", "eq");

    ops.put(".", "strcat");
    ops.put("..", "strcatspace");
    ops.put("isempty", "isempty");
    ops.put("length", "length");

    ops.put("int", "str2num");
    ops.put("bool", "str2bool");
    ops.put("rand", "rand");
    ops.put("currtime", "currtime");
    ops.put("totime", "int2timestr");
    ops.put("evalvar", "evalvar");
    ops.put("getitemalias", "id2alias");
    ops.put("getitemid", "alias2id");

    ops.put("dup", "dup");
    ops.put("drop", "drop");
    ops.put("flip", "flip");

    ops.put("getgroup", "getgroup");
    ops.put("getplayers", "getplayers");
    ops.put("isinarea", "isinarea");
    ops.put("getcoord", "getcoord");

    /* array ops */

    ops.put("Anew", "arraynew");
    ops.put("Acreate", "arraycreate");

    ops.put("Apush", "arraypush");
    ops.put("Apop", "arraypop");
    ops.put("Ainsert", "arrayinsert");
    ops.put("Aremove", "arrayremove");
    ops.put("Asize", "arraysize");
    ops.put("Aisempty", "arrayisempty");
    ops.put("Acontains", "arraycontains");
    ops.put("Aget", "arrayget");
    ops.put("Agetlast", "arraygetlast");
    ops.put("Ajoin", "arrayjoin");
    ops.put("Aexplode", "arrayexplode");

    /* hash ops */
    ops.put("Hnew", "hashnew");
    ops.put("Hcreate", "hashcreate");

    ops.put("Hput", "hashput");
    ops.put("Hget", "hashget");
    ops.put("Hremove", "hashremove");
    ops.put("Hcontainskey", "hashcontainskey");
    ops.put("Hcontainsvalue", "hashcontainsval");
    ops.put("Hisempty", "hashisempty");
    ops.put("Hsize", "hashsize");
    ops.put("Hkeys", "hashkeys");
    ops.put("Hvalues", "hashvalues");

    /* check that all methods really exist... */
    try {
      for (String name : ops.values()) {
        java.lang.reflect.Method m = this.getClass().getDeclaredMethod(name, new Class[] {});
      }
    } catch (NoSuchMethodException e) {
      System.out.println("Error in event interpreter - stack operator method mappings faulty!");
      System.out.println("Please file a bug report!");
    }

  }

  /*---- push + pop helpers ----*/

  private String pop() throws IndexOutOfBoundsException {
    return expstack.remove(0);
  }

  private Long popNum() throws IndexOutOfBoundsException {
    return toNum(expstack.remove(0));

  }

  private Boolean popBool() throws IndexOutOfBoundsException {
    return toBool(expstack.remove(0));
  }

  private ArrayList<String> popArray() throws IndexOutOfBoundsException {
    return toArray(expstack.remove(0));
  }

  private HashMap<String, String> popHash() throws IndexOutOfBoundsException {
    return toHash(expstack.remove(0));
  }

  private void push(String val) {
    expstack.add(0, String.valueOf(val));
  }

  private void push(long val) {
    expstack.add(0, String.valueOf(val));
  }

  private void push(boolean val) {
    expstack.add(0, String.valueOf(val));
  }

  private void push(ArrayList<String> val) {
    expstack.add(0, fromArray(val));
  }

  private void push(HashMap<String, String> val) {
    expstack.add(0, fromHash(val));
  }

  /*---- interface ----*/

  /* evaluate, return all elements left at the end */
  public ArrayList<String> evaluate(ArrayList<String> tokens) {

    try {
      while (tokens.size() > 0) {
        String elem = tokens.remove(0);
        if (elem.charAt(0) == RunningEvent.REFERENCEOP) { // escape->as string
          elem = elem.substring(1);
          push(elem);
          continue;
        }

        if (ops.containsKey(elem)) { // look through methods
          this.getClass().getDeclaredMethod(ops.get(elem), new Class[] {}).invoke(this);
        } else { // no method in hash -> regular value
          push(elem);
        }
      }
    } catch (Exception ex) {
      e.notifyError("Invalid expression!");
      return null;
    }

    ArrayList<String> ret = new ArrayList<String>();
    for (String s : expstack) {
      ret.add(0, s);
    }

    return ret;
  }

  /* evaluate, return top element */
  public String evaluateSingle(ArrayList<String> tokens) {
    tokens = evaluate(tokens);

    if (expstack.size() == 0) {
      e.notifyError("Expression returns no value! Returning null.");
      return null;
    } else if (expstack.size() > 1) {
      e.notifyError("Expression returns multiple values! Returning last.");
    }

    return pop(); // one element left -> correct evaluated result
  }

  /* evaluate stack, use as boolean expression */
  public boolean evaluateCondition(ArrayList<String> tokens) {
    return toBool(evaluateSingle(tokens));
  }

  /*---- Common data type methods ----*/

  public static boolean toBool(String bool) {
    if (bool.equals("false") || bool.equals("null") || bool.equals("") || bool.equals("0")) {
      return false;
    }
    return true;
  }

  public static long toNum(String num) {
    long ret = 0;
    try {
      ret = Long.valueOf(num);
    } catch (Exception e) {
      if (num.equals("true")) {
        return 1;
      } else {
        return 0;
      }
    }

    return ret;
  }

  /* format time string from milliseconds (max. unit=days) */
  public static String fmtTime(long ms) {
    long millis = ms % 1000;
    ms -= millis;
    ms /= 1000;
    long secs = ms % 60;
    ms -= secs;
    ms /= 60;
    long mins = ms % 60;
    ms -= mins;
    ms /= 60;
    long hrs = ms % 24;
    ms -= hrs;
    ms /= 24;
    long days = ms;

    String time = "";
    if (days != 0) {
      time += String.valueOf(days) + "d ";
    }
    if (hrs != 0) {
      time += String.valueOf(hrs) + "h ";
    }
    if (mins != 0) {
      time += String.valueOf(mins) + "m ";
    }
    if (secs != 0) {
      time += String.valueOf(secs) + "s ";
    }
    if (millis != 0) {
      time += String.valueOf(millis) + "ms ";
    }

    return time;
  }

  public static ArrayList<String> toArray(String val) {
    if (val == null || val.length() < 2 || val.charAt(0) != '[' || val.charAt(val.length() - 1) != ']') {
      return new ArrayList<String>(); // not valid array -> return empty one
    }

    ArrayList<String> arr = new ArrayList<String>();
    val = val.substring(1, val.length() - 1);
    for (String s : val.split("(?<!\\\\),")) {
      s = unescape(s, ",");
      if (!s.equals("")) {
        arr.add(s);
      }
    }

    return arr;
  }

  public static String fromArray(ArrayList<String> val) {
    String s = "[";
    while (val.size() != 0) {
      String t = val.remove(0);
      t = escape(t, ",");
      s += t;

      if (val.size() != 0) {
        s += ",";
      }
    }
    s += "]";
    return s;
  }

  public static String fromHash(HashMap<String, String> val) {
    String s = "{";

    if (val.size() > 0) {
      for (String key : val.keySet()) {
        String t = val.get(key);
        key = escape(key, ",:");
        t = escape(t, ",:");
        s += key + ":" + t + ",";
      }
      s = s.substring(0, s.length() - 1);
    }

    s += "}";
    return s;
  }

  public static HashMap<String, String> toHash(String val) {
    if (val == null || val.length() < 2 || val.charAt(0) != '{' || val.charAt(val.length() - 1) != '}') {
      return new HashMap<String, String>(); // not valid hash -> return empty
                                            // one
    }

    HashMap<String, String> ret = new HashMap<String, String>();
    val = val.substring(1, val.length() - 1);
    for (String s : val.split("(?<!\\\\),")) {
      String[] toks = s.split("(?<!\\\\):");
      if (toks.length != 2) {
        return new HashMap<String, String>();
      }
      ret.put(unescape(toks[0], ",:"), unescape(toks[1], ",:"));
    }

    return ret;
  }

  /*---- generic (un)escape for array/hash/etc ----*/
  public static String escape(String str, String chars) {
    String ret = str;
    for (int i = 0; i < chars.length(); i++) {
      String c = chars.substring(i, i + 1);
      ret = ret.replaceAll(c, "\\\\" + c);
    }
    return ret;
  }

  public static String unescape(String str, String chars) {
    String ret = str;
    for (int i = 0; i < chars.length(); i++) {
      String c = chars.substring(i, i + 1);
      ret = ret.replaceAll("\\\\" + c, c);
    }
    return ret;
  }

  /*---- Operators ----*/

  /* boolean ops */

  private void not() {
    push(!popBool());
  }

  private void and() {
    boolean b2 = popBool();
    boolean b1 = popBool();
    push(b1 && b2);
  }

  private void or() {
    boolean b2 = popBool();
    boolean b1 = popBool();
    push(b1 || b2);
  }

  /* number ops */

  private void add() {
    long d2 = popNum();
    long d1 = popNum();
    push(d1 + d2);
  }

  private void sub() {
    long d2 = popNum();
    long d1 = popNum();
    push(d1 - d2);
  }

  private void mul() {
    long d2 = popNum();
    long d1 = popNum();
    push(d1 * d2);
  }

  private void div() {
    long d2 = popNum();
    long d1 = popNum();
    push(d1 / d2);
  }

  private void mod() {
    long d2 = popNum();
    long d1 = popNum();
    push(d1 % d2);
  }

  private void rand() {
    long d = popNum();
    push(e.eventHost.rng.nextInt((int) d));
  }

  /* comparison ops */

  private void gt() {
    long d2 = popNum();
    long d1 = popNum();
    push(d1 > d2);
  }

  private void lt() {
    long d2 = popNum();
    long d1 = popNum();
    push(d1 < d2);
  }

  private void eq() {
    String v2 = pop();
    String v1 = pop();
    push(v1.equals(v2));
  }

  /* string ops */

  private void strcat() {
    String s2 = pop();
    String s1 = pop();
    push(s1 + s2);
  }

  private void strcatspace() {
    String s2 = pop();
    String s1 = pop();
    push(s1 + " " + s2);
  }

  private void isempty() {
    push(pop().equals(""));
  }

  private void length() {
    push(pop().length());
  }

  /* other ops */

  private void str2num() {
    push(popNum());
  }

  private void str2bool() {
    push(popBool());
  }

  private void currtime() {
    push(String.valueOf(System.currentTimeMillis()));
  }

  private void int2timestr() {
    push(fmtTime(popNum()));
  }

  private void evalvar() {
    String s = pop();
    String v = e.evaluateVar(s);
    if (v != null) {
      push(v);
    } else {
      e.notifyError("Warning: Invalid variable: " + s + "! Returning null");
      push("null");
    }
  }

  private void id2alias() {
    String[] id = pop().split(":");
    String alias = "null";
    if (id.length == 1) {
      alias = e.server.giveAliasList.getAlias(Integer.valueOf(id[0]), 0);
    }
    else {
      alias = e.server.giveAliasList.getAlias(Integer.valueOf(id[0]), Integer.valueOf(id[1]));
    }
    push(alias);
  }

  private void alias2id() {
    String alias = pop();
    Item i = e.server.giveAliasList.getItemId(alias);
    if (i != null) {
      String id = String.valueOf(i.id);
      if (i.damage != 0) {
        id += ":" + String.valueOf(i.damage);
      }
      push(id);
    } else {
      push("null");
    }
  }

  private void dup() {
    String s = pop();
    push(s);
    push(s);
  }

  private void drop() {
    pop();
  }

  private void flip() {
    if (expstack.size() < 2) {
      return;
    }

    String b = pop();
    String a = pop();
    push(b);
    push(a);
  }

  private void getgroup() {
    String s = pop();
    Player tmp = e.server.findPlayer(s);
    if (tmp == null) {
      e.notifyError("getgroup: Player not found! Returning group -1");
      push(-1);
    } else {
      push(tmp.getGroupId());
    }
  }

  private void getplayers() {
    ArrayList<String> players = new ArrayList<String>();
    for (Player p : e.server.playerList.getArray()) {
      players.add(p.getName());
    }
    push(players);

  }

  private void isinarea() {
    String area = pop();
    String player = pop();
    Player p = e.server.findPlayer(player);
    if (p == null) {
      e.notifyError("isarea: Player not found!");
      push(false);
    }
    HashSet<Area> areas = new HashSet<Area>(e.server.config.dimensions.areas(p.position()));
    for (Area a : areas) {
      if (a.name.equals(area)) {
        push(true);
        return;
      }
    }
    push(false);
    return;
  }

  private void getcoord() {
    String player = pop();
    Player p = e.server.findPlayer(player);
    if (p == null) {
      e.notifyError("isarea: Player not found!");
      push(false);
    }

    ArrayList<String> c = new ArrayList<String>();
    c.add(String.valueOf(p.position().x()));
    c.add(String.valueOf(p.position().y()));
    c.add(String.valueOf(p.position().z()));
    push(c);
  }

  /* array ops */

  private void arraynew() {
    push(new ArrayList<String>());
  }

  private void arraycreate() {
    long num = popNum();
    ArrayList<String> arr = new ArrayList<String>();
    for (int i = 0; i < num; i++) {
      arr.add(0, pop());
    }

    push(arr);
  }

  private void arraypush() {
    String val = pop();
    ArrayList<String> arr = popArray();
    arr.add(val);
    push(arr);
  }

  private void arraypop() {
    ArrayList<String> arr = popArray();
    if (arr.size() != 0) {
      arr.remove(arr.size() - 1);
      push(arr);

    } else {
      e.notifyError("Popping empty array!");
    }
  }

  private void arrayinsert() {
    String val = pop();
    int index = popNum().intValue();
    ArrayList<String> arr = popArray();

    if (index >= 0 && arr.size() >= index) {
      arr.add(index, val);
    } else {
      e.notifyError("Index out of range!");
    }
    push(arr);
  }

  private void arrayremove() {
    int index = popNum().intValue();
    ArrayList<String> arr = popArray();

    if (index >= 0 && arr.size() > index) {
      arr.remove(index);
    } else {
      e.notifyError("Index out of range!");
    }
    push(arr);
  }

  private void arrayget() {
    int index = popNum().intValue();
    ArrayList<String> arr = popArray();

    if (index >= 0 && arr.size() > index) {
      push(unescape(arr.get(index), ","));
    } else {
      e.notifyError("Index out of range!");
      push("null");
    }
  }

  private void arraycontains() {
    String val = pop();
    ArrayList<String> arr = popArray();
    push(arr.contains(val));
  }

  private void arraygetlast() {
    ArrayList<String> arr = popArray();

    if (arr.size() != 0) {
      push(arr.remove(arr.size() - 1));
    } else {
      e.notifyError("Popping empty array!");
      push("null");
    }
  }

  private void arraysize() {
    ArrayList<String> arr = popArray();
    push(arr.size());
  }

  private void arrayisempty() {
    ArrayList<String> arr = popArray();
    push(arr.size() == 0);
  }

  private void arrayjoin() {
    String delimeter = pop();
    ArrayList<String> arr = popArray();

    String s = "";
    while (arr.size() != 0) {
      s += unescape(arr.remove(0), ",");
      if (arr.size() != 0) {
        s += delimeter;
      }
    }
    push(s);
  }

  private void arrayexplode() {
    ArrayList<String> arr = popArray();
    if (arr.size() > 0) {
      for (String e : arr) {
        push(e);
      }
    } else {
      push("null");
    }
  }

  /* hash ops */

  private void hashnew() {
    push(new HashMap<String, String>());
  }

  private void hashcreate() {
    long num = popNum();
    HashMap<String, String> h = new HashMap<String, String>();

    for (int i = 0; i < num; i++) {
      String val = pop();
      String key = pop();
      if (!key.equals("") && !key.equals("null")) {
        h.put(key, val);
      } else {
        e.notifyError("Invalid hash key! Ignoring hash pair...");
      }
    }

    push(h);
  }

  private void hashput() {
    String val = pop();
    String key = pop();
    HashMap<String, String> h = popHash();

    if (!key.equals("") && !key.equals("null")) {
      h.put(key, val);
    } else {
      e.notifyError("Invalid hash key!");
    }

    push(h);
  }

  private void hashget() {
    String key = pop();
    HashMap<String, String> h = popHash();

    if (!key.equals("") && !key.equals("null")) {
      String val = h.get(key);
      if (val == null) {
        val = "null";
      }
      push(val);
    } else {
      e.notifyError("Invalid hash key!");
      push("null");
    }
  }

  private void hashremove() {
    String key = pop();
    HashMap<String, String> h = popHash();

    if (!key.equals("") && !key.equals("null")) {
      h.remove(key);
    } else {
      e.notifyError("Invalid hash key!");
    }
    push(h);
  }

  private void hashcontainskey() {
    String key = pop();
    HashMap<String, String> h = popHash();

    if (!key.equals("") && !key.equals("null")) {
      push(h.containsKey(key));
    } else {
      e.notifyError("Invalid hash key!");
      push(false);
    }
  }

  private void hashcontainsval() {
    String val = pop();
    HashMap<String, String> h = popHash();
    push(h.containsValue(val));
  }

  private void hashisempty() {
    HashMap<String, String> h = popHash();
    push(h.size() == 0);
  }

  private void hashsize() {
    HashMap<String, String> h = popHash();
    push(h.size());
  }

  private void hashkeys() {
    HashMap<String, String> h = popHash();
    push(new ArrayList<String>(h.keySet()));
  }

  private void hashvalues() {
    HashMap<String, String> h = popHash();
    push(new ArrayList<String>(h.values()));
  }

}
