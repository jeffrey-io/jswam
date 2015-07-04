package io.jeffrey.swam.amazon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.amazonaws.services.route53domains.model.ContactDetail;

/**
 * Makes it easier to work with ContactDetail
 * 
 * @author jeffrey
 */
public class ContactDetailsSerialization {
  private static boolean is(final String key, final String name) {
    if (key.equalsIgnoreCase(name)) {
      return true;
    }
    if (key.equalsIgnoreCase(name.replaceAll("_", ""))) {
      return true;
    }
    return false;
  }

  private static void writeKey(StringBuilder sb, String key, String value) {
    if (value == null)
      return;
    sb.append(key);
    sb.append("=");
    sb.append(value);
    sb.append("\n");
  }

  public static String serialize(ContactDetail contact) {
    StringBuilder sb = new StringBuilder();
    writeKey(sb, "first_name", contact.getFirstName());
    writeKey(sb, "last_name", contact.getLastName());
    writeKey(sb, "address1", contact.getAddressLine1());
    writeKey(sb, "address2", contact.getAddressLine2());
    writeKey(sb, "city", contact.getCity());
    writeKey(sb, "state", contact.getState());
    writeKey(sb, "zipcode", contact.getZipCode());
    writeKey(sb, "type", contact.getContactType());
    writeKey(sb, "country_code", contact.getCountryCode());
    writeKey(sb, "email", contact.getEmail());
    writeKey(sb, "fax", contact.getFax());
    writeKey(sb, "phone", contact.getPhoneNumber());
    writeKey(sb, "organization_name", contact.getOrganizationName());
    return sb.toString();
  }

  public static ContactDetail parse(BufferedReader reader) throws Exception {
    ContactDetail contact = new ContactDetail();
    String ln;
    while ((ln = reader.readLine()) != null) {
      ln = ln.trim();
      if (ln.length() == 0 || ln.charAt(0) == '#') {
        continue;
      }
      final String[] parts = ln.split("=");
      if (parts.length == 2) {
        if (is(parts[0], "address1")) {
          contact.setAddressLine1(parts[1].trim());
        } else if (is(parts[0], "address2")) {
          contact.setAddressLine2(parts[1].trim());
        } else if (is(parts[0], "city")) {
          contact.setCity(parts[1].trim());
        } else if (is(parts[0], "type")) {
          contact.setContactType(parts[1].trim());
        } else if (is(parts[0], "country_code")) {
          contact.setCountryCode(parts[1].trim());
        } else if (is(parts[0], "email")) {
          contact.setEmail(parts[1].trim());
        } else if (is(parts[0], "first_name")) {
          contact.setFirstName(parts[1].trim());
        } else if (is(parts[0], "last_name")) {
          contact.setLastName(parts[1].trim());
        } else if (is(parts[0], "fax")) {
          contact.setFax(parts[1].trim());
        } else if (is(parts[0], "phone")) {
          contact.setPhoneNumber(parts[1].trim());
        } else if (is(parts[0], "state")) {
          contact.setState(parts[1].trim());
        } else if (is(parts[0], "zipcode")) {
          contact.setZipCode(parts[1].trim());
        } else if (is(parts[0], "organization_name")) {
          contact.setOrganizationName(parts[1].trim());
        }
      }
    }
    return contact;
  }

  public static ContactDetail load(final File file) throws Exception {
    final BufferedReader reader = new BufferedReader(new FileReader(file));
    try {
      return parse(reader);
    } finally {
      reader.close();
    }
  }
}
