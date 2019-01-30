/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package playground;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.swing.JOptionPane;

/**
 *
 * @author S53788
 */
public class IshieldLdap {
  
  private Hashtable env = new Hashtable(11);
  private DirContext ctx;
  
  private String ldapURL = "ldaps://idssldap.tm.com.my:636";
//  private String ldapURL = "ldaps://10.54.5.231:636";
  private String ldapAdminPass = "nHQUbG9Z";
  private String usersname;
  private String userspwd;
  private String userDN;
  
  public IshieldLdap(String username, String pasword) throws NamingException{
    
    String keystorePath = "config/ldapCert.ks";
    System.setProperty("javax.net.ssl.keyStore", keystorePath);
    System.setProperty("javax.net.ssl.keyStorePassword", "novabillapps");
    System.setProperty("javax.net.ssl.trustStore", keystorePath);
    System.setProperty("javax.net.ssl.trustStorePassword", "novabillapps");

    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, ldapURL);
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.SECURITY_PRINCIPAL, "cn=novabillviewerldapadmin, ou=serviceAccount, o=Telekom");
    env.put(Context.SECURITY_CREDENTIALS, ldapAdminPass);
    

    System.out.println("Connecting to LDAP...");

    try {
      ctx = new InitialDirContext(env);
      
      System.out.println("Connected to LDAP.");
    } catch (NamingException e) {
      System.err.println("Error connecting to ldap: " + e.getMessage());
      throw e;
    }
    
    usersname = username;
    userspwd = pasword;

  }
  
  public boolean isUserExist() throws NamingException{
    
    SearchControls sctl = new SearchControls();
    sctl.setSearchScope(SearchControls.ONELEVEL_SCOPE);
    String baseDn = "ou=users,o=data";

    String searchFilter = "uid=" + usersname;

    NamingEnumeration<SearchResult> result = ctx.search(baseDn, searchFilter, sctl);
    

    if (result.hasMore()) {
      SearchResult sr = result.next();
      
      Attributes atbs = sr.getAttributes();
      
      System.out.println(atbs.toString());
      
      Attribute atb = atbs.get("acl");
      
      String temp = atb.get().toString();
      userDN = temp;
      
      if(userDN.toLowerCase().contains("cn")){
        while(!userDN.toLowerCase().startsWith("cn")){
          int pos = userDN.indexOf("#");
          if(pos == -1){
            // unrecognised ACL format. hardcode it
            userDN = "cn=" + usersname + "," + baseDn;
            break;
          }
          userDN = userDN.substring(pos + 1);
        }
        
        if(userDN.contains("#")){
          int pos = userDN.indexOf("#");
          userDN = userDN.substring(0, pos);
        }
        
      } else {
        // most likely cannot find the DN. hardcode it instead
        userDN = "cn=" + usersname + "," + baseDn;
      }
      
      return true;
    } else {
      System.err.println("User not found");
    }
    
    
    return false;
  }
  
  public boolean userInGroup() throws NamingException{
    
    SearchControls sctl = new SearchControls();
    sctl.setSearchScope(SearchControls.ONELEVEL_SCOPE);
    String baseDn = "ou=users,o=data";

//    Name baseDn = new LdapName("ou=users,o=data");
    
    String searchFilter = "(&(uid=" + usersname + ")(ValidNOVABILLVIEWER=TRUE))";

    NamingEnumeration<SearchResult> result = ctx.search(baseDn, searchFilter, sctl);
    

    if (result.hasMore()) {
      return true;
    } else {
      System.err.println("User not allowed for Bill Viewer");
    }
    
    return false;
  }

  public boolean authenticate() {
    
    System.out.println("Authenticating " + usersname + " " + userspwd);
    
    System.out.println(userDN);
    Hashtable soloenv = new Hashtable(11);

    soloenv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    soloenv.put(Context.PROVIDER_URL, ldapURL);
    soloenv.put(Context.SECURITY_AUTHENTICATION, "simple");
    soloenv.put(Context.SECURITY_PRINCIPAL, userDN);
    soloenv.put(Context.SECURITY_CREDENTIALS, userspwd);
    
    try {
      ctx.close();
      DirContext skonteks = new InitialDirContext(soloenv);
      skonteks.close();
    } catch (NamingException e) {
      JOptionPane.showMessageDialog(null, e.toString(true), "Error authenticating user", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
      return false;
    }
    
    return true;
    
  }


}
