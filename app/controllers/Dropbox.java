package controllers;

import models.oauthclient.Credentials;
import play.modules.oauthclient.ICredentials;
import play.modules.oauthclient.OAuthClient;
import play.mvc.Controller;

public class Dropbox extends Controller{
	
	public static ICredentials creds = new Credentials();

	public static OAuthClient client = new OAuthClient(
			"https://api.dropbox.com/1/oauth/request_token",
			"https://api.dropbox.com/1/oauth/access_token",
			"https://www.dropbox.com/1/oauth/authorize",
			"kil3i4sf1ux3bhd",
			"fmhpu3qmdq1q6j7");
	
	public static void auth() throws Exception {
		
    	ICredentials creds = new Credentials();
    	client.authenticate(creds, "http://localhost:9000/dropboxAuth");
	}
	
	public static void apptoken(String uid,String oauth_token) throws Exception {
		
    	Dropbox.client.retrieveAccessToken(creds, oauth_token);
    	String token = creds.getToken();
    	System.out.println("Dropbox Token:"+token);
    	System.out.println("Dropbox Secret:"+creds.getSecret());
    	session.put("dropbox_token", token);
		Application.index();
	}
	
}
