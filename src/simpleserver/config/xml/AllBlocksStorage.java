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
import java.util.Iterator;
import java.util.List;

public class AllBlocksStorage extends Storage {
  public AllBlocks blocks;

  @Override
  void add(XMLTag child) {
    AllBlocks blocks = (AllBlocks) child;
    if (this.blocks == null) {
      this.blocks = blocks;
    } else {
      if (blocks.place != null) {
        this.blocks.place = blocks.place;
      }
      if (blocks.destroy != null) {
        this.blocks.destroy = blocks.destroy;
      }
      if (blocks.use != null) {
        this.blocks.use = blocks.use;
      }
      if (blocks.take != null) {
        this.blocks.take = blocks.take;
      }
      if (blocks.give != null) {
        this.blocks.give = blocks.give;
      }
    }
  }

  @Override
  Iterator<AllBlocks> iterator() {
    List<AllBlocks> list = new ArrayList<AllBlocks>();
    if (blocks != null) {
      list.add(blocks);
    }
    return list.iterator();
  }
}
