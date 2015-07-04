package io.jeffrey.swam.workflows;

import io.jeffrey.swam.amazon.HostingRegion;
import io.jeffrey.swam.amazon.Universe;

import java.util.Collections;

import com.amazonaws.services.route53.model.AliasTarget;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.CreateHostedZoneRequest;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.HostedZoneConfig;
import com.amazonaws.services.route53.model.ListHostedZonesRequest;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import com.amazonaws.services.route53.model.ListResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ListResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.BucketWebsiteConfiguration;
import com.amazonaws.services.s3.model.RedirectRule;

/**
 * Experimental workflow for creating a website with Route53 and S3.
 *
 * @author jeffrey
 */
public class CreateWebsiteWorkflow {
  /**
   * How should a bucket be configured? If think about fo.io and www.foo.io, then
   * one of them is a redirect while the other is a real website. In the world of
   * SWAM, we will think that fo.io is the real website whil www.foo.io is the
   * redirect
   *
   * @author jeffrey
   */
  private static enum IdealizedBucketConfig {
    Website, Redirect
  }

  private final Universe universe;

  /**
   * @param universe all things Amazon
   */
  public CreateWebsiteWorkflow(final Universe universe) {
    this.universe = universe;
  }

  /**
   * add the set to the hosted zone
   */
  private void addResourceRecordSet(final ResourceRecordSet set, final HostedZone zone) {
    final Change change = new Change();
    change.setAction(ChangeAction.CREATE);
    change.setResourceRecordSet(set);
    final ChangeResourceRecordSetsRequest request = new ChangeResourceRecordSetsRequest();
    final ChangeBatch batch = new ChangeBatch();
    batch.setChanges(Collections.singleton(change));
    request.setChangeBatch(batch);
    request.setHostedZoneId(zone.getId());
    universe.route53.changeResourceRecordSets(request);
  }

  /**
   * If the website configuration exists, then it is already configured. If it isn't
   * configured, then what ever we desire to be the configuration shall be.
   */
  private IdealizedBucketConfig classify(final String bucket, final IdealizedBucketConfig desired) {
    final BucketWebsiteConfiguration config = universe.s3.getBucketWebsiteConfiguration(bucket);
    if (config == null) {
      return desired;
    }
    // it has already been configured as a redirect, so let's ensure the other is the primary
    if (config.getRedirectAllRequestsTo() != null) {
      return IdealizedBucketConfig.Redirect;
    }
    return IdealizedBucketConfig.Website;
  }

  /**
   * attempt to configure the bucket with a new website configuration
   */
  private void configureBucket(final String bucket, final IdealizedBucketConfig status, final String primaryDomain) {
    final BucketWebsiteConfiguration config = universe.s3.getBucketWebsiteConfiguration(bucket);
    // it already configured, don't touch it
    if (config != null) {
      return;
    }
    final BucketWebsiteConfiguration configuration = new BucketWebsiteConfiguration();
    if (status == IdealizedBucketConfig.Website) {
      configuration.setIndexDocumentSuffix("index.html");
      configuration.setErrorDocument("error.html");
    } else {
      final RedirectRule rule = new RedirectRule();
      rule.setHostName(primaryDomain);
      rule.setHttpRedirectCode("301");
      configuration.setRedirectAllRequestsTo(rule);
    }
    universe.s3.setBucketWebsiteConfiguration(bucket, configuration);
  }

  /**
   * Create a domain using Route53 and S3
   */
  public void create(final String domain, final HostingRegion region) {
    if (region == null) {
      throw new NullPointerException("region is null");
    }
    if (domain == null) {
      throw new NullPointerException("domain is null");
    }

    // step 1: set up DNS
    final HostedZone zone = ensureHostedZoneExists(domain);

    // step 2: set up S3 buckets and make websites
    setupBuckets(domain, region);

    // step 3: link route53 to s3
    linkApex(domain, zone, region);
    linkByCname("www." + domain, zone, region);
  }

  /**
   * Ensure the bucket exists AND owned by us
   */
  private void ensureCriticalBucketsExist(final String bucket, final HostingRegion region) {
    try {
      universe.s3.createBucket(bucket, region.s3BucketLocation);
    } catch (final AmazonS3Exception e) {
      if (e.getErrorCode().equals("BucketAlreadyOwnedByYou")) {
        // this is OK, and good
      } else {
        throw e;
      }
    }
  }

  /**
   * ensure route53 knows about the domain
   */
  private HostedZone ensureHostedZoneExists(final String domain) {
    final HostedZone zone = findHostedZone(domain);
    if (zone == null) {
      final CreateHostedZoneRequest request = new CreateHostedZoneRequest();
      request.setCallerReference(domain + System.currentTimeMillis());
      request.setName(domain + ".");
      request.setHostedZoneConfig(new HostedZoneConfig().withComment(domain + "@" + System.currentTimeMillis()));
      return universe.route53.createHostedZone(request).getHostedZone();
    }
    return zone;
  }

  /**
   * helper: find the hosted zone for the domain
   */
  private HostedZone findHostedZone(final String domain) {
    final ListHostedZonesRequest request = new ListHostedZonesRequest();
    ListHostedZonesResult result;
    while (true) {
      result = universe.route53.listHostedZones(request);
      final String domainWithExtraDot = domain + ".";
      for (final HostedZone zone : result.getHostedZones()) {
        if (zone.getName().equals(domainWithExtraDot)) {
          return zone;
        }
      }
      if (!result.isTruncated()) {
        return null;
      }
      request.setMarker(result.getNextMarker());
    }
  }

  /**
   * Find a ResourceRecordSet with a specific name for a specific type
   */
  private ResourceRecordSet findRR(final String domain, final String type, final HostedZone zone) {
    final ListResourceRecordSetsRequest request = new ListResourceRecordSetsRequest();
    request.setHostedZoneId(zone.getId());
    request.setStartRecordName(domain + ".");
    final ListResourceRecordSetsResult result = universe.route53.listResourceRecordSets(request);
    if (result.isTruncated()) {
      throw new RuntimeException("the hosted zone has too many records, not scanning [TODO]");
    }
    for (final ResourceRecordSet set : result.getResourceRecordSets()) {
      if (set.getName().equals(domain + ".")) {
        if (set.getType().equalsIgnoreCase(type)) {
          return set;
        }
      }
    }
    return null;
  }

  /**
   * link the given domain via the root(apex) record which can be done with route53's alias concept
   */
  private void linkApex(final String domain, final HostedZone zone, final HostingRegion region) {
    ResourceRecordSet set = findRR(domain, "a", zone);
    if (set != null) {
      return;
    }
    set = new ResourceRecordSet();
    set.setName(domain + ".");
    set.setType(RRType.A);
    final AliasTarget target = new AliasTarget();
    target.setDNSName(region.s3Domain + ".");
    target.setHostedZoneId(region.zoneId);
    target.setEvaluateTargetHealth(false);
    set.setAliasTarget(target);
    addResourceRecordSet(set, zone);
  }

  /**
   * link the given domain by a cname (i.e. as a subdomain)
   */
  private void linkByCname(final String domain, final HostedZone zone, final HostingRegion region) {
    ResourceRecordSet set = findRR(domain, "cname", zone);
    if (set != null) {
      return;
    }
    set = new ResourceRecordSet();
    set.setName(domain + ".");
    set.setType(RRType.CNAME);
    set.setTTL(300L);
    final ResourceRecord rr = new ResourceRecord();
    rr.setValue(domain + "." + region.s3Domain + ".");
    set.setResourceRecords(Collections.singleton(rr));
    addResourceRecordSet(set, zone);
  }

  /**
   * ensure the buckets exist and are configured appropriately. If both of the buckets are new
   */
  private void setupBuckets(final String domain, final HostingRegion region) {
    final String wwwDomain = "www." + domain;
    ensureCriticalBucketsExist(domain, region);
    ensureCriticalBucketsExist(wwwDomain, region);
    // attempt to classify www as a redirect
    final IdealizedBucketConfig wwwStatus = classify(wwwDomain, IdealizedBucketConfig.Redirect);
    final IdealizedBucketConfig apexStatus;
    final String primaryDomain;
    if (wwwStatus == IdealizedBucketConfig.Website) {
      apexStatus = classify(domain, IdealizedBucketConfig.Redirect);
      primaryDomain = wwwDomain;
    } else {
      apexStatus = IdealizedBucketConfig.Website;
      primaryDomain = domain;
    }
    configureBucket(domain, apexStatus, primaryDomain);
    configureBucket(wwwDomain, wwwStatus, primaryDomain);
  }
}
