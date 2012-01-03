/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.example.dataencryption.helper;

import java.io.*;
import java.security.KeyStore;

import javax.crypto.*;

import org.jppf.utils.FileUtils;
import org.jppf.utils.base64.*;

/**
 * This class provides helper methods to provide a cipher and its parameters,
 * and create and perform operations on a keystore.
 * @author Laurent Cohen
 */
public final class Helper
{
  /**
   * The keystore password.
   * This variable will be assigned the password value in clear,
   * after it has been read from a file and decoded from Base64 encoding.
   */
  private static char[] some_chars = null;

  /**
   * Instantiation of this class is not permitted.
   */
  private Helper()
  {
  }

  /**
   * Main entry point, creates the keystore.
   * The keystore is then included in the jar file generated by the script.<br/>
   * The keystore password, passed as argument, is encoded in Base64 form, then stored
   * into a file that is also included in the jar file. This ensures that no password
   * in clear is ever deployed.
   * @param args the first argument must be the keystore password in clear.
   */
  public static void main(final String...args)
  {
    try
    {
      generateKeyStore(args[0]);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Generate a keystore with a default password.
   * @param pwd default keystore password
   * @throws Exception if any error occurs.
   */
  private static void generateKeyStore(final String pwd) throws Exception
  {
    byte[] passwordBytes = pwd.getBytes();
    // encode the password in Base64
    byte[] encodedBytes = Base64Encoding.encodeBytesToBytes(passwordBytes);
    // store the encoded password to a file
    FileOutputStream fos = new FileOutputStream(getPasswordFilename());
    fos.write(encodedBytes);
    fos.flush();
    fos.close();
    char[] password = pwd.toCharArray();
    KeyStore ks = KeyStore.getInstance(getProvider());
    // create an empty keystore
    ks.load(null, password);
    // generate the initial secret key
    KeyGenerator gen = KeyGenerator.getInstance(getAlgorithm());
    SecretKey key = gen.generateKey();
    // save the key in the keystore
    KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(key);
    ks.setEntry(getKeyAlias(), skEntry, new KeyStore.PasswordProtection(password));
    // save the keystore to a file
    fos = new FileOutputStream(getKeystoreFilename());
    ks.store(fos, password);
    fos.close();
  }

  /**
   * Get the keystore password.
   * @return the password as a char[].
   */
  public static char[] getPassword()
  {
    if (some_chars == null)
    {
      try
      {
        String path = getKeystoreFolder() + getPasswordFilename();
        InputStream is = Helper.class.getClassLoader().getResourceAsStream(path);
        // read the encoded password
        byte[] encodedBytes = FileUtils.getInputStreamAsByte(is);
        // decode the password from Base64
        byte[] passwordBytes = Base64Decoding.decode(encodedBytes);
        some_chars = new String(passwordBytes).toCharArray();
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
    return some_chars;
  }

  /**
   * Get the password file name.
   * @return the password file name.
   */
  public static String getPasswordFilename()
  {
    return "password.pwd";
  }

  /**
   * Get the keystore file name.
   * @return the keystore file name.
   */
  public static String getKeystoreFilename()
  {
    return "keystore.ks";
  }

  /**
   * The folder in which the keystore and password file will be in the jar file.
   * @return the folder name as a string.
   */
  public static String getKeystoreFolder()
  {
    return "org/jppf/example/dataencryption/helper/";
  }

  /**
   * Get the key alias.
   * @return the key alias.
   */
  public static String getKeyAlias()
  {
    return "secretKeyAlias";
  }

  /**
   * Get the cryptographic provider, or keystore type.
   * @return the provider name.
   */
  public static String getProvider()
  {
    // jceks is the only ootb provider that allows storing a secret key
    return "jceks";
  }

  /**
   * Get the name of the cryptographic algorithm used to generate secret keys.
   * @return the algorithm name as a string.
   */
  public static String getAlgorithm()
  {
    return "DES";
  }

  /**
   * Get the name of the cryptographic transformation used when encrypting or decrypting data.
   * @return the transformation as a string.
   */
  public static String getTransformation()
  {
    return "DES/ECB/PKCS5Padding";
  }
}
