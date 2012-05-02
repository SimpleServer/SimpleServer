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
package simpleserver.stream;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public abstract class Encryption {
  protected PublicKey publicKey;
  protected SecretKey sharedKey;

  public static class ServerEncryption extends Encryption {
    public ServerEncryption() {
      try {
        sharedKey = generateSharedKey();
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      }
    }

    public byte[] getEncryptedSharedKey() {
      try {
        return encrypt(publicKey, sharedKey.getEncoded(), 1);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }

    public void setPublicKey(byte[] keyBytes) {
      try {
        publicKey = getPublicKey(keyBytes);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public static class ClientEncryption extends Encryption {
    private PrivateKey privateKey;

    public ClientEncryption() {
      KeyPair pair;
      try {
        pair = generateKeyPair();
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
        return;
      }
      publicKey = pair.getPublic();
      privateKey = pair.getPrivate();
    }

    public void setEncryptedSharedKey(byte[] encryptedSharedKey) {
      try {
        sharedKey = decryptSharedKey(encryptedSharedKey, privateKey);
      } catch (GeneralSecurityException e) {
        e.printStackTrace();
      }
    }

    public String getLoginHash(String name) throws NoSuchAlgorithmException, UnsupportedEncodingException {
      return new BigInteger(loginHash(name, publicKey, sharedKey)).toString(16);
    }
  }

  public OutputStream encryptedOutputStream(OutputStream stream) {
    try {
      return new CipherOutputStream(stream, getCipher("RC4", sharedKey, 1));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public InputStream encryptedInputStream(InputStream stream) {
    try {
      return new CipherInputStream(stream, getCipher("RC4", sharedKey, 1));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static PublicKey getPublicKey(byte[] keyBytes) throws GeneralSecurityException {
    return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
  }

  private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
    keyGenerator.initialize(1024);
    return keyGenerator.generateKeyPair();
  }

  private static SecretKey generateSharedKey() throws NoSuchAlgorithmException {
    KeyGenerator keyGenerator = KeyGenerator.getInstance("RC4");
    keyGenerator.init(128);
    return keyGenerator.generateKey();
  }

  private static byte[] loginHash(String name, PublicKey publicKey, SecretKey sharedKey) throws NoSuchAlgorithmException, UnsupportedEncodingException {
    return getHash("SHA-1", new byte[][] { name.getBytes("ISO_8859_1"), sharedKey.getEncoded(), publicKey.getEncoded() });
  }

  private static byte[] getHash(String algrithm, byte[][] data) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance(algrithm);
    for (byte[] bytes : data) {
      digest.update(bytes);
    }
    return digest.digest();
  }

  private static Cipher getCipher(String transformation, Key key, int mode) throws GeneralSecurityException {
    Cipher cipher;
    cipher = Cipher.getInstance(transformation);
    cipher.init(mode, key);
    return cipher;
  }

  private static byte[] encrypt(Key key, byte[] data, int opmode) throws GeneralSecurityException {
    return getCipher(key.getAlgorithm(), key, opmode).doFinal(data);
  }

  private static SecretKey decryptSharedKey(byte[] encryptedKey, PrivateKey privateKey) throws GeneralSecurityException {
    return new SecretKeySpec(encrypt(privateKey, encryptedKey, 2), "RC4");
  }

}
