package io.jeffrey.swam.amazon;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.Region;

/**
 * Since there are many attributes related to a region, this combines them all
 *
 * @author jeffrey
 */
public enum HostingRegion {
  US_WEST_2("Z3BJ6K6RIION7M", "s3-website-us-west-2.amazonaws.com", Regions.US_WEST_2, Region.US_West_2), //
  US_STANDARD("Z3AQBSTGFYJSTF", "s3-website-us-east-1.amazonaws.com", Regions.US_EAST_1, Region.US_Standard);

  public final String  zoneId;
  public final String  s3Domain;
  public final Regions region;
  public final Region  s3BucketLocation;

  private HostingRegion(final String zoneId, final String s3Domain, final Regions region, final Region s3BucketLocation) {
    this.zoneId = zoneId;
    this.s3Domain = s3Domain;
    this.region = region;
    this.s3BucketLocation = s3BucketLocation;
  }
}
