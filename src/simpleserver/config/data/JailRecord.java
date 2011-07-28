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
package simpleserver.config.data;

import simpleserver.nbt.NBTByte;
import simpleserver.nbt.NBTCompound;
import simpleserver.nbt.NBTLong;

public class JailRecord {
  private final static String JAIL = "jail";

  private final PlayerData playerData;

  JailRecord(PlayerData playerData) {
    this.playerData = playerData;
  }

  public Record get(String playerName) {
    NBTCompound player = playerData.get(playerName);
    if (!player.containsKey(JAIL)) {
      Record record = new Record(playerName);
      set(playerName, record);
    }
    return new Record(player.getCompound(JAIL), playerName);
  }

  public void remove(String playerName) {
    NBTCompound player = playerData.get(playerName);
    if (player.containsKey(JAIL)) {
      player.remove(JAIL);
    }
  }

  private void set(String playerName, Record record) {
    NBTCompound player = playerData.get(playerName);
    player.put(record.tag());
  }

  public enum JailState {
    RELEASED((byte) 0), LOCKED((byte) 1);

    private final byte type;

    JailState(byte type) {
      this.type = type;
    }

    public byte getType() {
      return type;
    }
  }

  public class Record {
    private final static String STATE = "state";
    private final static String JAILED_UNTIL = "jailedUntil";
    private final static String WILL_GET_RELEASED = "willGetReleased";

    private final String playerName;

    private JailState state = JailState.RELEASED;
    private long jailedUntil = 0;
    private boolean willGetReleased = true;

    Record(String playerName) {
      this.playerName = playerName;
    }

    Record(NBTCompound tag, String playerName) {
      this.playerName = playerName;

      state = (tag.getByte(STATE).get().equals((byte) 1)) ? JailState.LOCKED : JailState.RELEASED;
      jailedUntil = tag.getLong(JAILED_UNTIL).get();
      willGetReleased = tag.getByte(WILL_GET_RELEASED).get().equals((byte) 1);

      if (state == JailState.LOCKED) {
        if (willGetReleased) {
          if (System.currentTimeMillis() >= jailedUntil) {
            unjail();
          }
        }
      }
    }

    public void jail(long jailLenghtMillis) {
      state = JailState.LOCKED;

      willGetReleased = (jailLenghtMillis != 0);
      jailedUntil = System.currentTimeMillis() + jailLenghtMillis;

      set(playerName, this);
    }

    public void unjail() {
      state = JailState.RELEASED;
      jailedUntil = 0;
      willGetReleased = true;
      set(playerName, this);
    }

    public JailState getIsJailed() {
      return state;
    }

    public NBTCompound tag() {
      NBTCompound tag = new NBTCompound(JAIL);
      NBTByte isJailedValue = new NBTByte(STATE, state.getType());
      tag.put(isJailedValue);
      NBTLong jailedUntilValue = new NBTLong(JAILED_UNTIL, jailedUntil);
      tag.put(jailedUntilValue);
      NBTByte getsReleasedValue = new NBTByte(WILL_GET_RELEASED, willGetReleased ? (byte) 1 : (byte) 0);
      tag.put(getsReleasedValue);
      return tag;
    }
  }
}
