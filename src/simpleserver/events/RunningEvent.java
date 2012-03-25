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

import simpleserver.Coordinate;
import simpleserver.Player;
import simpleserver.Server;
import simpleserver.command.CommandFeedback;
import simpleserver.command.InvalidCommand;
import simpleserver.command.ServerCommand;
import simpleserver.config.xml.Event;

class RunningEvent extends Thread implements Runnable {

  /**
   * 
   */
  public boolean stopping = false; // indicates that the thread should stop

  protected EventHost eventHost;
  protected Server server;
  protected Event event; // currently run event
  protected Player player; // referred player context
  // (default -> triggering player)
  private String threadname = null; // null = just subroutine

  private static final int MAXDEPTH = 5;
  private static final char LOCALSCOPE = '#';
  private static final char PLAYERSCOPE = '@';
  private static final char GLOBALSCOPE = '$';
  private static final char REFERENCEOP = '\'';

  private HashMap<String, String> vars; // local vars
  private ArrayList<String> threadstack; // thread-shared data stack

  // for control structures
  private int stackdepth = 0; // to limit subroutines
  private String lastline; // line being executed
  private int currline = 0; // execution line pointer

  private ArrayList<Integer> ifstack;
  private ArrayList<Integer> whilestack;

  private boolean running = false;

  public RunningEvent(EventHost h, String n, Event e, Player p) {
    eventHost = h;
    server = h.server;
    event = e;
    player = p;
    threadname = n;

    // Top level event without initialized stack -> initialize empty data stack
    threadstack = new ArrayList<String>();

    vars = new HashMap<String, String>();
    ifstack = new ArrayList<Integer>();
    whilestack = new ArrayList<Integer>();
  }

  /* init as subroutine */
  public RunningEvent(EventHost h, String n, Event e, Player p, int depth, ArrayList<String> stack) {
    this(h, n, e, p);
    stackdepth = depth;
    if (stack != null) {
      threadstack = stack;
    }
  }

  @Override
  public void run() {
    if (running) { // prevent running the same object twice
      return;
    }
    running = true;

    String script = event.script;
    if (script == null)
    {
      return; // no script data present
    }

    // Split actions by semicola and newlines
    ArrayList<String> actions =
        new ArrayList<String>(java.util.Arrays.asList(script.split("[\n;]")));
    currline = 0;

    while (currline < actions.size()) {
      if (stopping) {
        break;
      }

      lastline = actions.get(currline);

      ArrayList<String> tokens = parseLine(lastline);
      evaluateVariables(tokens);

      if (tokens.size() == 0) {
        currline++;
        continue;
      }

      // run action
      String cmd = tokens.remove(0);
      if (cmd.equals("return")) {
        return;
      } else if (cmd.equals("rem") || cmd.equals("endif")) {
        currline++;
        continue;
      }
      else if (cmd.equals("print") && tokens.size() > 0) {
        System.out.println("L" + String.valueOf(currline) + "@" + event.name + " msg: " + tokens.get(0));
      } else if (cmd.equals("sleep") && tokens.size() > 0) {
        try {
          Thread.sleep(Integer.valueOf(tokens.get(0)));
        } catch (InterruptedException e) {
          if (stopping) {
            break;
          }
        }
      } else if (cmd.equals("say")) {
        say(tokens);
      } else if (cmd.equals("broadcast") && tokens.size() > 0) {
        server.runCommand("say", tokens.get(0));
      } else if (cmd.equals("give")) {
        give(tokens);
      } else if (cmd.equals("teleport")) {
        teleport(tokens);
      } else if (cmd.equals("set")) {
        set(tokens);
      } else if (cmd.equals("push")) {
        push(tokens);
      } else if (cmd.equals("run")) {
        cmdrun(tokens, false);
      } else if (cmd.equals("launch")) {
        cmdrun(tokens, true);
      } else if (cmd.equals("if")) {
        condition(tokens, actions);
      } else if (cmd.equals("else")) {
        currline = ifstack.remove(0);
      } else if (cmd.equals("while")) {
        loop(tokens, actions);
      } else if (cmd.equals("endwhile")) {
        currline = whilestack.remove(0);
      } else if (cmd.equals("execsvrcmd")) {
        execsvrcmd(tokens);
      } else if (cmd.equals("execcmd")) {
        execcmd(tokens);
      } else {
        notifyError("Command not found!");
      }

      currline++;
    }

    // finished -> remove itself from the running thread list
    if (threadname != null) {
      eventHost.running.remove(threadname);
    }

  }

  private ArrayList<String> parseLine(String line) {
    ArrayList<String> tokens = new ArrayList<String>();

    String currtok = null;
    boolean inString = false;
    for (int i = 0; i < line.length(); i++) {
      if (!inString && String.valueOf(line.charAt(i)).matches("\\s")) {
        if (currtok != null) {
          tokens.add(currtok);
          currtok = null;
        }
        continue;
      }

      if (currtok == null) {
        currtok = "";
      }

      if (inString && line.charAt(i) == '\\') { // Escape character
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

    if (currtok != null) {
      tokens.add(currtok);
    }

    return tokens;
  }

  private void evaluateVariables(ArrayList<String> tokens) {
    for (int i = 0; i < tokens.size(); i++) {
      String var = evaluateVar(tokens.get(i));
      if (var != null) {
        tokens.set(i, var);
      }
    }
  }

  protected String evaluateVar(String varname) {
    if (varname == null || varname.equals("")) {
      return "";
    }

    if (varname.charAt(0) == REFERENCEOP) {
      return varname.substring(1);
    } else if (varname.charAt(0) == LOCALSCOPE) { // local var
      String loc = varname.substring(1);
      // check special vars
      if (loc.equals("PLAYER")) {
        return player != null ? player.getName() : "null";
      } else if (loc.equals("EVENT")) {
        return event.name;
      } else if (loc.equals("THIS")) {
        return "$" + event.name;
      } else if (loc.equals("VALUE")) {
        return event.value;
      } else if (loc.equals("COORD")) {
        return String.valueOf(event.coordinate);
      } else if (loc.equals("CURRTIME")) {
        return String.valueOf(System.currentTimeMillis());
      } else if (loc.equals("POP")) {
        return threadstack.size() == 0 ? "null" : threadstack.remove(0);
      } else if (loc.equals("TOP")) {
        return threadstack.size() == 0 ? "null" : threadstack.get(0);
      } else if (eventHost.colors.containsKey(loc)) {
        return "\u00a7" + eventHost.colors.get(loc);
      } else {
        return String.valueOf(vars.get(loc));
      }

    } else if (varname.charAt(0) == PLAYERSCOPE) { // player var
      return String.valueOf(player.vars.get(varname.substring(1)));

    } else if (varname.charAt(0) == GLOBALSCOPE) { // global perm var (event
                                                   // value)
      Event ev = eventHost.findEvent(varname.substring(1));
      if (ev == null) {
        notifyError("Event not found!");
        return "null";
      } else {
        return String.valueOf(ev.value);
      }
    }
    return null; // not a variable
  }

  /*---- Interaction ----*/

  private void say(ArrayList<String> tokens) {
    if (tokens.size() != 2) {
      notifyError("Wrong number of arguments!");
      return;
    }

    Player p = server.findPlayer(tokens.get(0));
    String message = tokens.get(1);

    if (p != null) {
      p.addTMessage(message);
    } else {
      notifyError("Player not found!");
    }
  }

  private void give(ArrayList<String> tokens) {
    if (tokens.size() < 2) {
      notifyError("Wrong number of arguments!");
      return;
    }

    Player p = server.findPlayer(tokens.get(0));
    if (p == null) {
      notifyError("Player not online!");
      return;
    }

    int id = 1;
    if (tokens.get(1).matches("\\d+")) {
      id = Integer.valueOf(tokens.get(1));
    } else {
      id = server.giveAliasList.getItemId(tokens.get(1)).id;
    }

    int amount = 1;
    if (tokens.size() > 2)
    {
      try {
        amount = Integer.valueOf(tokens.get(2));
      } catch (Exception e) {
      } // invalid amount
    }

    // give items
    p.give(id, amount);
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

    // Try to find such a player
    Player q = server.findPlayer(dest);
    if (q != null) {
      c = q.position.coordinate();
    }

    // Try to find such an event
    if (c == null && dest.length() > 0 && dest.charAt(0) == GLOBALSCOPE) {
      Event evdest = eventHost.findEvent(dest.substring(1));
      if (evdest != null) {
        c = evdest.coordinate;
      }
    }

    if (c == null) { // try to find a warp with that name
      String w = server.data.warp.getName(dest);
      if (w != null) {
        p.teleportSelf(server.data.warp.get(w)); // port to warp, ignore rest
        return;
      }
    }

    // try to parse as coordinate
    if (c == null) {
      c = Coordinate.fromString(dest);
    }

    if (c != null) {
      p.teleportSelf(c);
    } else {
      notifyError("Destination not found!");
    }
  }

  private void execcmd(ArrayList<String> tokens) {
    if (tokens.size() == 0) {
      notifyError("Wrong number of arguments!");
      return; // no command to execute
    }

    String message = server.options.getBoolean("useSlashes") ? "/" : "!";
    for (String token : tokens) {
      message += token + " ";
    }
    message = message.substring(0, message.length() - 1);

    // execute the server command, overriding the player permissions
    player.parseCommand(message, true);
  }

  private void execsvrcmd(ArrayList<String> tokens) {
    if (tokens.size() == 0) {
      notifyError("Wrong number of arguments!");
      return; // no command to execute
    }

    String cmd = tokens.get(0);
    String cmdline = "";
    for (String t : tokens) {
      cmdline += t + " ";
    }
    cmdline = cmdline.substring(0, cmdline.length() - 1);

    ServerCommand command = server.getCommandParser().getServerCommand(cmd);

    if ((command != null) && !(command instanceof InvalidCommand)) {
      command.execute(server, cmdline, feedback);
    }
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

    String exp = new PostfixEvaluator(this).evaluate(tokens);

    if (exp == null) {
      return;
    }

    if (scope == LOCALSCOPE) {
      vars.put(var, exp);
    } else if (scope == PLAYERSCOPE) {
      player.vars.put(var, exp);
    } else if (scope == GLOBALSCOPE) {
      Event e = eventHost.findEvent(var);
      if (e != null) {
        e.value = exp;
      }
    } else {
      notifyError("Invalid variable reference!");
    }

    // System.out.println("Setting var "+scope+var+" to "+exp); //DEBUG
  }

  private void push(ArrayList<String> tokens) {
    String exp = new PostfixEvaluator(this).evaluate(tokens);

    if (exp == null) {
      threadstack.add(0, "null");
      return;
    }

    threadstack.add(0, exp);
  }

  private void cmdrun(ArrayList<String> tokens, boolean newThread) {
    if (tokens.size() < 1) {
      notifyError("Wrong number of arguments!");
      return;
    }

    Event e = eventHost.findEvent(tokens.get(0));

    if (e == null) {
      notifyError("Event to run not found!");
      return;
    }

    Player p = player;
    if (tokens.size() > 1) {
      p = server.findPlayer(tokens.get(1));
    }

    if (!newThread) {
      if (stackdepth < MAXDEPTH) {
        (new RunningEvent(eventHost, null, e, p, stackdepth + 1, threadstack)).run();
      } else {
        notifyError("Can not run event - stack level too deep!");
      }
    } else { // run in a new thread (passing threadstack copy!)
      @SuppressWarnings("unchecked")
      ArrayList<String> clone = (ArrayList<String>) threadstack.clone();
      eventHost.execute(e, p, true, clone);
    }
  }

  private void condition(ArrayList<String> tokens, ArrayList<String> actions) {
    boolean result = new PostfixEvaluator(this).evaluateCondition(tokens);

    int ifcounter = 0;

    boolean hasElse = false;
    if (result) {
      for (int i = currline + 1; i < actions.size(); i++) {
        ArrayList<String> line = parseLine(actions.get(i));
        if (line.size() == 0) {
          continue;
        }
        String cmd = line.get(0);

        if (cmd.equals("if")) {
          ifcounter++;
        }

        if (cmd.equals("else") && ifcounter == 0) {
          hasElse = true;
        }

        if (cmd.equals("endif")) {
          if (ifcounter == 0) {
            if (hasElse) {
              ifstack.add(0, i);
            }
            break;
          } else {
            ifcounter--;
          }
        }
      }
    } else {
      for (int i = currline + 1; i < actions.size(); i++) {
        ArrayList<String> line = parseLine(actions.get(i));
        if (line.size() == 0) {
          continue;
        }
        String cmd = line.get(0);

        if (cmd.equals("if")) {
          ifcounter++;
        }

        if (cmd.equals("endif")) {
          if (ifcounter == 0) { // no else found -> jump here
            currline = i;
            break;
          }
          ifcounter--;
        }

        if (cmd.equals("else") && ifcounter == 0) { // jump to else part
          currline = i;
          break;
        }
      }
    }
  }

  private void loop(ArrayList<String> tokens, ArrayList<String> actions) {
    boolean result = new PostfixEvaluator(this).evaluateCondition(tokens);

    if (!result) { // jump over while loop
      int whilecounter = 0;
      for (int i = currline + 1; i < actions.size(); i++) {
        ArrayList<String> line = parseLine(actions.get(i));
        if (line.size() == 0) {
          continue;
        }
        String cmd = line.get(0);

        if (cmd.equals("while")) {
          whilecounter++;
        }

        if (cmd.equals("endwhile")) {
          if (whilecounter == 0) {
            currline = i;
            break;
          } else {
            whilecounter--;
          }
        }
      }
    } else { // save current line for jump back
      whilestack.add(0, currline - 1);
    }
  }

  /*--------*/

  protected void notifyError(String err) {
    System.out.println("Error at L" + String.valueOf(currline) + "@" + event.name + ": " + lastline);
    System.out.println(err + "\n");
  }

  private CommandFeedback feedback = new CommandFeedback() {
    public void send(String message, Object... args) {
      System.out.println("[SimpleServer] " + String.format(message, args));
    }
  };
}
