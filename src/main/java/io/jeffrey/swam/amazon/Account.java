package io.jeffrey.swam.amazon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.amazonaws.auth.AWSCredentials;

/**
 * Represents an account that can talk to AWS
 */
public class Account implements AWSCredentials {

  /**
   * read the account from disk (which is not encrypted)
   */
  public static Account fromPlaintextDisk(final File file) throws Exception {
    final BufferedReader reader = new BufferedReader(new FileReader(file));
    try {
      String accessKey = null;
      String secretKey = null;
      String ln;
      while ((ln = reader.readLine()) != null) {
        ln = ln.trim();
        if (ln.length() == 0 || ln.charAt(0) == '#') {
          continue;
        }
        final String[] parts = ln.split("=");
        if (parts.length == 2) {
          if (keyCheck(parts[0], "access") || keyCheck(parts[0], "akid")) {
            accessKey = parts[1].trim();
          }
          if (keyCheck(parts[0], "secret")) {
            secretKey = parts[1].trim();
          }
        }
      }
      if (accessKey == null || secretKey == null) {
        throw new Exception("file did not contain both the access key and secret key");
      }
      return new Account(accessKey, secretKey);
    } finally {
      reader.close();
    }
  }

  /**
   * verify the given value is a key of the given name (assumes varients)
   */
  private static boolean keyCheck(final String valueToCheck, final String name) {
    final String reduced = valueToCheck.trim();
    if (reduced.equalsIgnoreCase(name)) {
      return true;
    }
    if (reduced.equalsIgnoreCase(name + "key")) {
      return true;
    }
    if (reduced.equalsIgnoreCase(name + "_key")) {
      return true;
    }
    return false;
  }

  public final String accessKey;

  public final String secretKey;

  /**
   * @param accessKey the public key which may be visible on the wire
   * @param secretKey the secret key used to sign requests
   */
  public Account(final String accessKey, final String secretKey) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
  }

  @Override
  public String getAWSAccessKeyId() {
    return accessKey;
  }

  @Override
  public String getAWSSecretKey() {
    return secretKey;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Account[" + accessKey + "]";
  }
}
