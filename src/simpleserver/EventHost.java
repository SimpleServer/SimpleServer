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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import java.util.concurrent.ConcurrentHashMap;

import simpleserver.Server;
import simpleserver.Player;
import simpleserver.Color;

import simpleserver.config.data.Warp;
import simpleserver.config.xml.Event;

public class EventHost {

    public ConcurrentHashMap<Event,Long> events; //stores last time called for each event
    public HashMap<String,String> colors;

    private Server server;
    private ArrayList<Thread> running;

    protected Random rng;

    public void loadEvents() {
      if (running != null)
        for (Thread t : running)
          t.stop();

      this.running = new ArrayList<Thread>();
      this.events = new ConcurrentHashMap<Event, Long>();

      Iterator<Event> it = server.config.events.iterator();
      while (it.hasNext()) {
        Event ev = it.next();
        events.put(ev,(long)0);
      }
    }

    public EventHost(Server s) {
      this.server = s;
      this.rng = new Random();

      colors = new HashMap<String,String>();
      colors.put("BLACK","0");
      colors.put("DARK_BLUE","1");
      colors.put("DARK_GREEN","2");
      colors.put("DARK_CYAN","3");
      colors.put("DARK_RED","4");
      colors.put("PURPLE","5");
      colors.put("GOLD","6");
      colors.put("GRAY","7");
      colors.put("DARK_GRAY","8");
      colors.put("BLUE","9");
      colors.put("GREEN","a");
      colors.put("CYAN","b");
      colors.put("RED","c");
      colors.put("PINK","d");
      colors.put("YELLOW","e");
      colors.put("WHITE","f");

      loadEvents();

      Event autorun = findEvent("onServerStart");
      if (autorun != null)
          execute(autorun, null, true);
    }

    /* Execute given event triggered by given player */
    public void execute(Event e, Player p, boolean forced) {
        //DEBUG
        //System.out.println(p.getName() +"->"+e.name+"@"+System.currentTimeMillis());

        if (e==null) //No event to run :O
          return;

        //If it's a player-triggered event, check trigger conditions
        if (!forced && p != null) {
          if (e.disabled || e.coordinate == null)
            return; //Not triggerable

          if (e.allow != null && !e.allow.contains(p))
            return; //user not allowed for event

          long currtime = System.currentTimeMillis();
          if ( e.interval != 0 && currtime < events.get(e) + 1000*e.interval )
            return; //Event still has timeout
          events.put(e, currtime); //Update last event call time
        }


        //Start top level event in new thread
        RunningEvent rev = new RunningEvent(server,e,p);
        Thread t = new Thread(rev);
        t.start();
        running.add(t);
    }

    public Event findEvent(String name) {
        for(Event f : events.keySet()) {
            if (f.name.equals(name))
              return f;
        }

        return null;
    }

    private class RunningEvent implements Runnable {

        private Server server;
        private Event event; //currently run event
        private Player player; //referred player context (default -> triggering player)

        private int stackdepth = 0; //to limit subroutines

        private String lastline; //line being executed

        private int currline=0; //execution line pointer

        private static final int MAXDEPTH = 5;
        private static final char LOCALSCOPE = '#';
        private static final char PLAYERSCOPE = '@';
        private static final char GLOBALSCOPE = '$';
        private static final char REFERENCEOP = '\'';


        private HashMap<String,String> vars; //local vars
        private ArrayList<String> threadstack; //thread-shared data stack


        //for control structures
        private ArrayList<Integer> ifstack;
        private ArrayList<Integer> whilestack;

        private boolean running = false;

        public RunningEvent(Server s, Event e, Player p) {
          this.server = s;
          this.event  = e;
          this.player = p;

          this.vars = new HashMap<String,String>();
          this.ifstack = new ArrayList<Integer>();
          this.whilestack = new ArrayList<Integer>();
        }

        public RunningEvent(Server s, Event e, Player p, int depth, ArrayList<String> stack) {
          this(s,e,p);
          stackdepth = depth;
          if (stack != null)
            threadstack = stack;
        }

        public void run() {
          if (running)
              return;

          running = true;

          String script = event.script;

          //Top level event without initialized stack -> initialize empty data stack
          if (threadstack == null)
            threadstack = new ArrayList<String>();

          //Split actions by semicola and newlines
          ArrayList<String> actions =
              new ArrayList<String>(java.util.Arrays.asList(script.split("[\n;]")));
          currline = 0;

          while (currline < actions.size()) {
              this.lastline = actions.get(currline);

              ArrayList<String> tokens = parseLine(lastline);
              evaluateVariables(tokens);

              if (tokens.size() == 0) {
                  currline++;
                  continue;
              }

              //run action
              String cmd = tokens.remove(0);
              if (cmd.equals("return"))
                return;
              else if (cmd.equals("rem") || cmd.equals("endif")) {
                currline++;
                continue;
              }
              else if (cmd.equals("print") && tokens.size() > 0)
                System.out.println("L"+String.valueOf(currline)+"@"+event.name+" msg: "+tokens.get(0));
              else if (cmd.equals("sleep") && tokens.size() > 0)
                try {Thread.sleep(Integer.valueOf(tokens.get(0)));} catch(Exception e){}
              else if (cmd.equals("say"))
                say(tokens);
              else if (cmd.equals("give"))
                give(tokens);
              else if (cmd.equals("teleport"))
                teleport(tokens);
              else if (cmd.equals("set"))
                set(tokens);
              else if (cmd.equals("push"))
                push(tokens);
              else if (cmd.equals("run"))
                cmdrun(tokens);
              else if (cmd.equals("if"))
                condition(tokens,actions);
              else if (cmd.equals("else"))
                currline = ifstack.remove(0);
              else if (cmd.equals("while"))
                loop(tokens,actions);
              else if (cmd.equals("endwhile"))
                currline = whilestack.remove(0);
              else
                notifyError("Command not found!");

              currline++;
          }

        }

        private ArrayList<String> parseLine(String line) {
            ArrayList<String> tokens = new ArrayList<String>();

            String currtok = null;
            boolean inString = false;
            for(int i=0; i<line.length(); i++) {
                if (!inString && String.valueOf(line.charAt(i)).matches("\\s")) {
                    if (currtok != null) {
                        tokens.add(currtok);
                        currtok = null;
                    }
                    continue;
                }

                if (currtok == null)
                    currtok = "";

                if (inString && line.charAt(i) == '\\') { //Escape character
                    i++;
                    currtok += line.charAt(i);
                    continue;
                }

                if (line.charAt(i) == '"') {
                    inString = !inString;
                    continue;
                }

                currtok += line.charAt(i);
            }

            if (currtok != null)
                tokens.add(currtok);


            return tokens;
        }

        private void evaluateVariables(ArrayList<String> tokens) {
            for(int i=0; i<tokens.size(); i++) {
              String var = evaluateVar(tokens.get(i));
              if (var!=null)
                tokens.set(i, var);
            }
        }

        private String evaluateVar(String varname) {
            if (varname == null || varname.equals("")) //catch empty string literals
                return "";

            if (varname.charAt(0) == REFERENCEOP) // reference operator -> escape variablename
              return varname.substring(1);

            else if (varname.charAt(0) == LOCALSCOPE) { // local var
              String loc = varname.substring(1);
              //check special vars
              if (loc.equals("PLAYER"))
                return player!=null ? player.getName() : "null";
              else if (loc.equals("EVENT"))
                return event.name;
              else if (loc.equals("THIS"))
                return "$"+event.name;
              else if (loc.equals("VALUE"))
                return event.value;
              else if (loc.equals("COORD"))
                return String.valueOf(event.coordinate);
              else if (loc.equals("CURRTIME"))
                return String.valueOf(System.currentTimeMillis());
              else if (loc.equals("POP"))
                return threadstack.size()==0 ? "null" : threadstack.remove(0);
              else if (server.eventhost.colors.containsKey(loc))
                return "\u00a7"+server.eventhost.colors.get(loc);
              else //normal local variable
                return String.valueOf(vars.get(loc));

            } else if (varname.charAt(0) == PLAYERSCOPE) { // player var
                return String.valueOf(player.vars.get(varname.substring(1)));

            } else if (varname.charAt(0) == GLOBALSCOPE) { // global perm var (event value)
              Event ev = server.eventhost.findEvent(varname.substring(1));
              if (ev == null) {
                notifyError("Event not found!");
                return "null";
              }
              else
                return String.valueOf(ev.value);
            }
            return null; //not a variable
        }

        /*---- Interaction ----*/

        private void say(ArrayList<String> tokens) {
          if (tokens.size() != 2) {
            notifyError("Wrong number of arguments!");
            return;
          }


          Player p = server.findPlayer(tokens.get(0));
          String message = tokens.get(1);

          if (p != null)
            p.addTMessage(message);
          else
            notifyError("Player not found!");
        }

        private void give(ArrayList<String> tokens) {
          if (tokens.size() < 2) {
            notifyError("Wrong number of arguments!");
            return;
          }

          Player p = server.findPlayer(tokens.get(0));
          if (p==null) {
              notifyError("Player not online!");
              return;
          }

          int id = 1;
          if (tokens.get(1).matches("\\d+"))
            id = Integer.valueOf(tokens.get(1));
          else
            id = server.giveAliasList.getItemId(tokens.get(1)).id;

          int amount = 1;
          if (tokens.size() > 2)
            try {
              amount = Integer.valueOf(tokens.get(2));
          } catch (Exception e) {} //invalid amount

          //give items
          p.give(id,amount);
        }

        private void teleport(ArrayList<String> tokens) {
          if (tokens.size() < 2) {
            notifyError("Wrong number of arguments!");
            return;
          }

          Player p = server.findPlayer(tokens.get(0));
          if (p == null) {
            notifyError("Source player not online!");
            return;
          }

          Coordinate c = null;

          String dest = tokens.get(1);

          //Try to find such a player
          Player q = server.findPlayer(dest);
          if (q!=null)
              c = q.position.coordinate();

          //Try to find such an event
          if(c == null && dest.length()>0 && dest.charAt(0)==GLOBALSCOPE) {
              Event evdest = server.eventhost.findEvent(dest.substring(1));
              if (evdest != null)
                  c = evdest.coordinate;
          }

          if (c == null){ //try to find a warp with that name
              String w = server.data.warp.getName(dest);
              if (w!=null) {
                p.teleportSelf(server.data.warp.get(w)); //port to warp, ignore rest
                return;
              }
          }

          //try to parse as coordinate
          if (c==null)
              c = Coordinate.fromString(dest);

          if (c != null)
              p.teleportSelf(c);
          else
              notifyError("Destination not found!");
        }

        /*---- variable & runtime ----*/

        private void set(ArrayList<String> tokens) {
          if (tokens.size() < 2) {
            notifyError("Wrong number of arguments!");
            return;
          }

          String var = tokens.remove(0);
          char scope = var.charAt(0);
          var = var.substring(1);

          String exp = evaluatePostfixExp(tokens);
          if (exp == null)
            return;

          if (scope == LOCALSCOPE)
            vars.put(var, exp);
          else if (scope == PLAYERSCOPE)
            player.vars.put(var, exp);
          else if (scope == GLOBALSCOPE) {
            Event e = server.eventhost.findEvent(var);
            if (e != null)
              e.value = exp;
          } else
            notifyError("Invalid variable reference!");

          //System.out.println("Setting var "+scope+var+" to "+exp); //DEBUG
        }

        private void push(ArrayList<String> tokens) {
          String exp = evaluatePostfixExp(tokens);
          if (exp == null) {
            threadstack.add(0,"null");
            return;
          }

          threadstack.add(0,exp);
        }

        private void cmdrun(ArrayList<String> tokens) {
          if (tokens.size() < 1) {
            notifyError("Wrong number of arguments!");
            return;
          }

          Event e = server.eventhost.findEvent(tokens.get(0));

          if (e == null) {
            notifyError("Event to run not found!");
            return;
          }

          Player p = player;
          if (tokens.size() > 1)
            p = server.findPlayer(tokens.get(1));

          if (stackdepth < MAXDEPTH)
            (new RunningEvent(server,e,p,stackdepth+1,threadstack)).run();
          else
            notifyError("Can not run event - stack level too deep!");
        }

        private void condition(ArrayList<String> tokens, ArrayList<String> actions) {
          String exp = evaluatePostfixExp(tokens);

          if (exp == null)
              return;

          boolean result = toBool(exp);
          int ifcounter = 0;

          boolean haselse = false;
          if (result) {
            for (int i=currline+1; i<actions.size(); i++) {
                ArrayList<String> line = parseLine(actions.get(i));
                if (line.size()==0)
                    continue;
                String cmd = line.get(0);

                if (cmd.equals("if"))
                    ifcounter++;

                if (cmd.equals("else") && ifcounter==0)
                    haselse = true;

                if (cmd.equals("endif")) {
                    if (ifcounter == 0) {
                      if (haselse)
                        ifstack.add(0, i);
                      break;
                    }
                    else
                      ifcounter--;
                }
            }
          } else {
            for (int i=currline+1; i<actions.size(); i++) {
                ArrayList<String> line = parseLine(actions.get(i));
                if (line.size()==0)
                    continue;
                String cmd = line.get(0);

                if (cmd.equals("if"))
                    ifcounter++;

                if (cmd.equals("endif")) {
                   if (ifcounter == 0) { //no else found -> jump here
                     currline = i;
                     break;
                   }
                   ifcounter--;
                }

                if (cmd.equals("else") && ifcounter == 0) { //jump to else part
                    currline = i;
                    break;
                }
            }
          }
        }

        private void loop(ArrayList<String> tokens, ArrayList<String> actions) {
          String exp = evaluatePostfixExp(tokens);

          if (exp == null)
              return;

          boolean result = toBool(exp);

          if (!result) { //jump over while loop
            int whilecounter = 0;
            for (int i=currline+1; i<actions.size(); i++) {
                ArrayList<String> line = parseLine(actions.get(i));
                if (line.size()==0)
                    continue;
                String cmd = line.get(0);

                if (cmd.equals("while"))
                    whilecounter++;

                if (cmd.equals("endwhile")) {
                  if (whilecounter == 0) {
                      currline = i;
                      break;
                  } else
                      whilecounter--;
                }
            }
          } else { //save current line for jump back
              whilestack.add(0, currline-1);
          }
        }

        /*---- Expressions ----*/

        private String evaluatePostfixExp(ArrayList<String> tokens) {
            ArrayList<String> expstack = new ArrayList<String>();

            try {
             while (tokens.size() > 0) {
              /* DEBUG
              for (String a : expstack)
                  System.out.print(a+" ");
              System.out.println("");
              */

              String elem = tokens.remove(0);
              if (elem.equals("not")) {
                boolean b = toBool(expstack.remove(0));
                expstack.add(0, String.valueOf(!b));
              }
              else if (elem.equals("and")) {
                boolean b2 = toBool(expstack.remove(0));
                boolean b1 = toBool(expstack.remove(0));
                expstack.add(0, String.valueOf(b1 && b2));
              }
              else if (elem.equals("or")) {
                boolean b2 = toBool(expstack.remove(0));
                boolean b1 = toBool(expstack.remove(0));
                expstack.add(0, String.valueOf(b1 || b2));
              }
              /* ops on numbers */
              else if (elem.equals("+")) {
                long d2 = toNum(expstack.remove(0));
                long d1 = toNum(expstack.remove(0));
                expstack.add(0, String.valueOf(d1+d2));
              }
              else if (elem.equals("-")) {
                long d2 = toNum(expstack.remove(0));
                long d1 = toNum(expstack.remove(0));
                expstack.add(0, String.valueOf(d1-d2));
              }
              else if (elem.equals("*")) {
                long d2 = toNum(expstack.remove(0));
                long d1 = toNum(expstack.remove(0));
                expstack.add(0, String.valueOf(d1*d2));
              }
              else if (elem.equals("/")) {
                long d2 = toNum(expstack.remove(0));
                long d1 = toNum(expstack.remove(0));
                expstack.add(0, String.valueOf(d1/d2));
              }
              else if (elem.equals("%")) {
                long d2 = toNum(expstack.remove(0));
                long d1 = toNum(expstack.remove(0));
                expstack.add(0, String.valueOf(d1%d2));
              }
              else if (elem.equals("rand")) {
                long d = toNum(expstack.remove(0));
                expstack.add(String.valueOf(rng.nextInt((int)d)));
              }
              else if (elem.equals("eq")) { //works on everything
                String v2 = expstack.remove(0);
                String v1 = expstack.remove(0);
                expstack.add(0, String.valueOf(v1.equals(v2)));
              }
              else if (elem.equals("gt")) {
                long d2 = toNum(expstack.remove(0));
                long d1 = toNum(expstack.remove(0));
                expstack.add(0, String.valueOf(d1>d2));
              }
              else if (elem.equals("lt")) {
                long d2 = toNum(expstack.remove(0));
                long d1 = toNum(expstack.remove(0));
                expstack.add(0, String.valueOf(d1<d2));
              }
              /* ops on strings */
              else if (elem.equals(".")) {
                String s2 = expstack.remove(0);
                String s1 = expstack.remove(0);
                expstack.add(0, s1+s2);
              }
              else if (elem.equals("..")) {
                String s2 = expstack.remove(0);
                String s1 = expstack.remove(0);
                expstack.add(0, s1+" "+s2);
              }
              /* other */
              else if (elem.equals("evalvar")) {
                String s = expstack.remove(0);
                String v = evaluateVar(s);
                if (v!=null)
                    expstack.add(0, v);
                else {
                    notifyError("Warning: Invalid variable: "+s+"!");
                    expstack.add(0, "null");
                }
              }
              else if (elem.equals("int")) {
                String s = expstack.remove(0);
                expstack.add(0, String.valueOf(toNum(s)));

              }
              else if (elem.equals("bool")) {
                String s = expstack.remove(0);
                expstack.add(0, String.valueOf(toBool(s)));
              }
              else /* not op -> push */
                expstack.add(0, elem);
             }
            } catch (Exception e) {
                notifyError("Invalid expression!");
                return null;
            }

            if (expstack.size() != 1) {
                notifyError("Invalid expression!");
                return null;
            }

            return expstack.get(0); //one element left -> correct evaluated result
        }

        private boolean toBool(String bool) {
          if (bool.equals("false") || bool.equals("null") || bool.equals("") || bool.equals("0"))
              return false;
          return true;
        }

        private long toNum(String num) {
          long ret = 0;
          try {
            ret = Long.valueOf(num);
          } catch (Exception e) {
            if (num.equals("true"))
                return 1;
            else
                return 0;
          }

          return ret;
        }

        /*--------*/

        private void notifyError(String err) {
          System.out.println("Error at L"+String.valueOf(currline)+"@"+event.name+": "+lastline);
          System.out.println(err+"\n");
        }

    }
}

