package io.jeffrey.swam;

import io.jeffrey.swam.amazon.Account;
import io.jeffrey.swam.amazon.ContactDetailsSerialization;
import io.jeffrey.swam.amazon.HostingRegion;
import io.jeffrey.swam.amazon.Universe;
import io.jeffrey.swam.workflows.CreateWebsiteWorkflow;
import io.jeffrey.swam.workflows.RegisterDomainWorkflow;

import java.io.File;

import com.amazonaws.services.route53domains.model.ContactDetail;

/**
 * Until there is a UI, I will have the CLI
 */
public class Tool {
  
  public static class StdErrWorkflowLog implements WorkflowStatusLog {

    private long started;
    
    public StdErrWorkflowLog() {
      this.started = System.currentTimeMillis();
    }
    
    private String age() {
      long delta = System.currentTimeMillis() - started;
      return delta + "ms";
    }
    
    @Override
    public void log(String... lineParts) {
      System.err.println("LOG[" + age() + "] : 6" + String.join(" ", lineParts));
    }
    
  }
  
  public static void main(final String[] args) throws Exception {
    final String home = System.getenv("HOME");
    final Account account = Account.fromPlaintextDisk(new File(home, "root.aws"));
    final Universe universe = new Universe(account);
    if (args.length != 2) {
      System.err.println("tool missing arguments");
      return;
    }
    if ("register-domain".equals(args[0])) {
      final ContactDetail contact = ContactDetailsSerialization.load(new File(home, "contact.aws.domain"));
      final RegisterDomainWorkflow register = new RegisterDomainWorkflow(new StdErrWorkflowLog(), universe);
      register.register(args[1], contact, contact, contact);
    }
    if ("setup-domain".equals(args[0])) {
      final CreateWebsiteWorkflow creator = new CreateWebsiteWorkflow(new StdErrWorkflowLog(), universe);
      creator.setupDomain(args[1], HostingRegion.US_STANDARD);
    }
  }
}
