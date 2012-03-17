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

    ops.put("+", "plus");
    ops.put("-", "minus");
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
    ops.put("getgroup", "getgroup");
  }

  private String pop() throws IndexOutOfBoundsException {
    return expstack.remove(0);
  }

  private Long popNum() throws IndexOutOfBoundsException {
    return toNum(expstack.remove(0));

  }

  private Boolean popBool() throws IndexOutOfBoundsException {
    return toBool(expstack.remove(0));
  }

  private void push(long val) {
    expstack.add(0, String.valueOf(val));
  }

  private void push(boolean val) {
    expstack.add(0, String.valueOf(val));
  }

  private void push(String val) {
    expstack.add(0, val);
  }

  public String evaluate(ArrayList<String> tokens) {

    try {
      while (tokens.size() > 0) {
        /* DEBUG
        for (String a : expstack)
            System.out.print(a+" ");
        System.out.println("");
        */

        String elem = tokens.remove(0);
        if (ops.containsKey(elem)) { // look through methods
          this.getClass().getDeclaredMethod(ops.get(elem), new Class[] {}).invoke(this);
        } else { // no method found -> regular value
          push(elem);
        }
      }
    } catch (Exception ex) {
      e.notifyError("Invalid expression!");
      return null;
    }

    if (expstack.size() != 1) {
      e.notifyError("Invalid expression!");
      return null;
    }

    return pop(); // one element left -> correct evaluated result
  }

  public boolean evaluateCondition(ArrayList<String> tokens) {
    return toBool(evaluate(tokens));
  }

  /*---- Common data type methods ----*/

  private boolean toBool(String bool) {
    if (bool.equals("false") || bool.equals("null") || bool.equals("") || bool.equals("0")) {
      return false;
    }
    return true;
  }

  private long toNum(String num) {
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

  private void plus() {
    long d2 = popNum();
    long d1 = popNum();
    push(d1 + d2);
  }

  private void minus() {
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

  private void getgroup() {
    String s = pop();
    Player tmp = e.server.findPlayer(s);
    if (tmp == null) {
      e.notifyError("Warning: Player not found! Returning group -1");
      push(-1);
    } else {
      push(tmp.getGroupId());
    }
  }
}
