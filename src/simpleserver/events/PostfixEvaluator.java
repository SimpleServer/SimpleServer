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

import simpleserver.Player;

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
    ops.put("rand", "rand");

    ops.put("gt", "gt");
    ops.put("lt", "lt");
    ops.put("eq", "eq");

    ops.put(".", "strcat");
    ops.put("..", "strcatspace");
    ops.put("isempty", "isempty");
    ops.put("length", "length");

    ops.put("int", "str2num");
    ops.put("bool", "str2bool");
    ops.put("evalvar", "evalvar");

    ops.put("dup", "dup");
    ops.put("drop", "drop");
    ops.put("flip", "flip");
    ops.put("rotr", "rotr");
    ops.put("rotl", "rotl");

    ops.put("getgroup", "getgroup");

    /* array ops */

    ops.put("Anew", "arraynew");
    ops.put("Acreate", "arraycreate");

    ops.put("Apush", "arraypush");
    ops.put("Apop", "arraypop");
    ops.put("Ainsert", "arrayinsert");
    ops.put("Aremove", "arrayremove");
    ops.put("Asize", "arraysize");
    ops.put("Aisempty", "arrayisempty");

    ops.put("Aget", "arrayget");
    ops.put("Agetlast", "arraygetlast");
    ops.put("Ajoin", "arrayjoin");

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

  private void push(String val) {
    expstack.add(0, val);
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

  /*---- interface ----*/

  /* evaluate, return all elements left at the end */
  public ArrayList<String> evaluate(ArrayList<String> tokens) {

    try {
      while (tokens.size() > 0) {
        String elem = tokens.remove(0);
        if (ops.containsKey(elem)) { // look through methods
          this.getClass().getDeclaredMethod(ops.get(elem), new Class[] {}).invoke(this);
        } else { // no method in hash -> regular value
          push(elem);
        }
      }
    } catch (NoSuchMethodException ex) {
      e.notifyError("Error in interpreter - no matching method found!");
      System.out.println("Method not found! Please file a bug report!");
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

  public static ArrayList<String> toArray(String val) {
    if (val == null || val.length() < 2 || val.charAt(0) != '[' || val.charAt(val.length() - 1) != ']') {
      return new ArrayList<String>(); // not valid array -> return empty one
    }

    ArrayList<String> arr = new ArrayList<String>();
    val = val.substring(1, val.length() - 1);
    for (String s : val.split("(?<!\\\\),")) {
      s = unescape(s, ",");
      arr.add(s);
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

  private void rotr() {
    if (expstack.size() == 0) {
      return;
    }
    expstack.add(0, pop());
  }

  private void rotl() {
    if (expstack.size() == 0) {
      return;
    }
    push(expstack.remove(0));
  }

  private void getgroup() {
    String s = pop();
    Player tmp = e.server.findPlayer(s);
    if (tmp == null) {
      e.notifyError("Player not found! Returning group -1");
      push(-1);
    } else {
      push(tmp.getGroupId());
    }
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
}
