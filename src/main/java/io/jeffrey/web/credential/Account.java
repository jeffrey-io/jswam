package io.jeffrey.web.credential;

import com.amazonaws.auth.AWSCredentials;

public class Account {

  public final String accessKey;
  public final String secretKey;
  
  public Account(final String accessKey, final String secretKey) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
  }
  
  AWSCredentials forAWS() {
    return new AWSCredentials() {
      
      public String getAWSSecretKey() {
        return secretKey;
      }
      
      public String getAWSAccessKeyId() {
        return accessKey;
      }
    };
  }
}
