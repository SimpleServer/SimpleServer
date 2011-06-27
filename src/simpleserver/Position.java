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

import java.io.DataOutputStream;
import java.io.IOException;

import simpleserver.Coordinate.Dimension;

public class Position {
  public double x;
  public double y;
  public double z;
  public double stance;
  public Dimension dimension;
  public float yaw;
  public float pitch;
  public boolean onGround;

  public Position() {
    dimension = Dimension.EARTH;
    onGround = true;
  }

  public Position(double x, double y, double z, Dimension dimension) {
    this(x, y, z, dimension, 0, 0);
  }

  public Position(double x, double y, double z, Dimension dimension, float yaw, float pitch) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.dimension = dimension;
    this.yaw = yaw;
    this.pitch = pitch;
    onGround = true;
  }

  public void updatePosition(double x, double y, double z, double stance) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.stance = stance;
  }

  public void updateLook(float yaw, float pitch) {
    this.yaw = yaw;
    this.pitch = pitch;
  }

  public void updateDimension(Dimension dimension) {
    this.dimension = dimension;
  }

  public void updateGround(boolean onGround) {
    this.onGround = onGround;
  }

  public void send(DataOutputStream out) throws IOException {
    out.writeByte(0x0d);
    out.writeDouble(x);
    out.writeDouble(y);
    out.writeDouble(stance);
    out.writeDouble(z);
    out.writeFloat(yaw);
    out.writeFloat(pitch);
    out.writeBoolean(onGround);
    out.flush();
  }

  public double x() {
    return x;
  }

  public double y() {
    return y;
  }

  public double z() {
    return z;
  }

  public Dimension dimension() {
    return dimension;
  }

  public double stance() {
    return stance;
  }

  public float yaw() {
    return yaw;
  }
}
