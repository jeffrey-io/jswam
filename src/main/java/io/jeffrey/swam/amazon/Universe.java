package io.jeffrey.swam.amazon;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53domains.AmazonRoute53Domains;
import com.amazonaws.services.route53domains.AmazonRoute53DomainsClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * Represents the Amazon Universe of the services that we utilize
 * @author jeffrey
 */
public class Universe {
  public final AmazonRoute53        route53;
  public final AmazonS3             s3;
  public final AmazonRoute53Domains domains;

  public Universe(final Account account) {
    route53 = new AmazonRoute53Client(account);
    s3 = new AmazonS3Client(account);
    domains = new AmazonRoute53DomainsClient(account);
  }
}
