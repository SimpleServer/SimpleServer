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
package simpleserver.util;

public class DamerauLevenshtein {
  public static int distance(String a, String b) {
    int n = a.length();
    int m = b.length();
    int d[][] = new int[n + 1][m + 1];

    for (int i = 0; i <= n; i++) {
      d[i][0] = i;
    }

    for (int j = 1; j <= m; j++) {
      d[0][j] = j;
    }

    for (int i = 0; i < n; i++) {
      for (int j = 0; j < m; j++) {
        int k = i + 1;
        int q = j + 1;
        int cost = (a.charAt(i) == b.charAt(j)) ? 0 : 1;
        d[k][q] = Math.min(d[i][q] + 1,
                                   Math.min(d[k][j] + 1,
                                            d[i][j] + cost));
        if (i >= 1 && j >= 1 && a.charAt(i) == b.charAt(j - 1) && a.charAt(i - 1) == b.charAt(j)) {
          d[k][q] = Math.min(d[k][q],
                                     d[i - 1][j - 1] + cost);
        }
      }
    }

    return d[n][m];
  }
}
