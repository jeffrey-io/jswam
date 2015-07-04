package io.jeffrey.swam;

import io.jeffrey.swam.amazon.Account;
import io.jeffrey.swam.amazon.HostingRegion;
import io.jeffrey.swam.amazon.Universe;
import io.jeffrey.swam.workflows.CreateWebsiteWorkflow;

import java.io.File;

import com.amazonaws.services.route53.AmazonRoute53;

/**
 * Until there is a UI, I will have the CLI
 */
public class Tool {
  public static void main(final String[] args) throws Exception {
    if (args.length != 2) {
      System.err.println("tool missing arguments");
      return;
    }
    if ("create-domain".equals(args[0])) {
      final String home = System.getenv("HOME");
      final Account account = Account.fromPlaintextDisk(new File(home, "root.aws"));
      final Universe universe = new Universe(account);
      final CreateWebsiteWorkflow creator = new CreateWebsiteWorkflow(universe);
      creator.create(args[1], HostingRegion.US_STANDARD);
    }
  }
}
