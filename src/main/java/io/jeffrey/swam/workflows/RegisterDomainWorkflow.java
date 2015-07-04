package io.jeffrey.swam.workflows;

import io.jeffrey.swam.WorkflowStatusLog;
import io.jeffrey.swam.amazon.Universe;

import com.amazonaws.services.route53domains.model.ContactDetail;
import com.amazonaws.services.route53domains.model.DomainSummary;
import com.amazonaws.services.route53domains.model.ListDomainsRequest;
import com.amazonaws.services.route53domains.model.ListDomainsResult;
import com.amazonaws.services.route53domains.model.RegisterDomainRequest;

/**
 * Registers a domain
 *
 * @author jeffrey
 */
public class RegisterDomainWorkflow {
  private final WorkflowStatusLog log;
  private final Universe universe;

  /**
   * @param universe all things Amazon
   */
  public RegisterDomainWorkflow(final WorkflowStatusLog log, final Universe universe) {
    this.log = log;
    this.universe = universe;
  }

  /**
   * @return whether or not the domain exists
   */
  private boolean domainExists(final String domain) {
    final ListDomainsRequest request = new ListDomainsRequest();
    final ListDomainsResult result = universe.domains.listDomains(request);
    if (result.getNextPageMarker() != null) {
      throw new RuntimeException("have a new page marker");
    }
    for (final DomainSummary domainSummary : result.getDomains()) {
      if (domainSummary.getDomainName().equals(domain)) {
        return true;
      }
    }
    return false;
  }

  /**
   * register a domain
   */
  public void register(final String domain, final ContactDetail techContact, final ContactDetail adminContact, final ContactDetail registrantContact) {
    log.log("register", "domain=", domain);
    if (domainExists(domain)) {
      log.log("register", "domain exists");
      return;
    }

    log.log("register", "attempting to register");
    final RegisterDomainRequest request = new RegisterDomainRequest();
    request.setDurationInYears(1);
    request.setDomainName(domain);
    request.setTechContact(techContact);
    request.setRegistrantContact(registrantContact);
    request.setAdminContact(adminContact);
    request.setPrivacyProtectAdminContact(true);
    request.setPrivacyProtectRegistrantContact(true);
    request.setPrivacyProtectTechContact(true);
    universe.domains.registerDomain(request);
  }
}
