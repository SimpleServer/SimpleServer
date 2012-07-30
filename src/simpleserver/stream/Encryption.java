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
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.io.CipherOutputStream;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public abstract class Encryption {
  protected SecretKey sharedKey;
  protected byte[] challengeToken;

  public static class ServerEncryption extends Encryption {
    private PublicKey publicKey;

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

    public byte[] encryptChallengeToken() {
      try {
        return encrypt(publicKey, challengeToken, 1);
      } catch (GeneralSecurityException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  public static class ClientEncryption extends Encryption {
    private static KeyPair keyPair;

    public static void generateKeyPair() throws NoSuchAlgorithmException {
      KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
      keyGenerator.initialize(1024);
      keyPair = keyGenerator.generateKeyPair();
    }

    public void setEncryptedSharedKey(byte[] encryptedSharedKey) {
      try {
        sharedKey = decryptSharedKey(encryptedSharedKey, keyPair.getPrivate());
      } catch (GeneralSecurityException e) {
        e.printStackTrace();
      }
    }

    public String getLoginHash(String name) throws NoSuchAlgorithmException, UnsupportedEncodingException {
      return new BigInteger(loginHash(name, keyPair.getPublic(), sharedKey)).toString(16);
    }

    public byte[] getPublicKey() {
      return keyPair.getPublic().getEncoded();
    }

    public boolean checkChallengeToken(byte[] challengeTokenResponse) {
      try {
        return Arrays.equals(encrypt(keyPair.getPrivate(), challengeTokenResponse, 2), challengeToken);
      } catch (GeneralSecurityException e) {
        return false;
      }
    }
  }

  public BufferedBlockCipher getStreamCipher(boolean out) {
    BufferedBlockCipher cipher = new BufferedBlockCipher(new CFBBlockCipher(new AESFastEngine(), 8));
    cipher.init(out, new ParametersWithIV(new KeyParameter(sharedKey.getEncoded()), sharedKey.getEncoded(), 0, 16));
    return cipher;
  }

  public OutputStream encryptedOutputStream(OutputStream stream) {
    try {
      return new CipherOutputStream(stream, getStreamCipher(true));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public InputStream encryptedInputStream(InputStream stream) {
    try {
      return new CipherInputStream(stream, getStreamCipher(false));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public void setChallengeToken(byte[] challangeToken) {
    challengeToken = challangeToken;
  }

  private static PublicKey getPublicKey(byte[] keyBytes) throws GeneralSecurityException {
    return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));
  }

  private static SecretKey generateSharedKey() throws NoSuchAlgorithmException {
    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
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
