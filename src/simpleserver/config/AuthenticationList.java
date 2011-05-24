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
package simpleserver.config;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthenticationList extends PropertiesConfig {
  
  public AuthenticationList() {
    super("authentication-list.txt");
  }
  
  public Boolean isRegistered(String userName) {
    return properties.containsKey(userName.toLowerCase());
  }
  
  public Boolean passwordMatches(String userName, String password){
    return getSHA(password).equals(getPasswordHash(userName));
  }
  
  public void changePassword(String userName, String newPassword) {
    addAuthentication(userName, newPassword);
  }
  
  public void addAuthentication(String userName, String password) {
    String pwHash = getSHA(password);
    
    properties.setProperty(userName.toLowerCase(), pwHash);
    save();
  }
  
  private String getPasswordHash(String userName) {
    return (String) properties.get(userName.toLowerCase());
  }
  
  private String getSHA(String s){
    // returns SHA-256 Hash of a String
    
    StringBuffer hex = new StringBuffer();
    try {
      
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.reset();
      md.update(s.getBytes());
      byte[] encrypted = md.digest();
      
      // convert to hex
      for (int i = 0; i < encrypted.length; i++ ) {
        // includes the case when the hex is only one char and prepends a 0 (by adding 0x100)
        hex.append(Integer.toString((encrypted[i] & 0xff) + 0x100, 16).substring(1)); 
      }

    } catch(NoSuchAlgorithmException nsae){
      System.out.println("Attention: Seems like MessageDigest is missing in your Java installation...");
      return s;         // proceed without encryption
    }
    
    return hex.toString();
  }
}
