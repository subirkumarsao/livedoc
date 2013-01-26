package controllers;

import java.util.List;

import javax.persistence.Query;

import models.User;
import play.Play;
import play.db.jpa.JPA;
import play.libs.WS;
import play.libs.WS.WSRequest;
import play.mvc.Controller;

import com.google.gson.JsonElement;

public class Facebook extends Controller{
	
	public static final String authUrl = "https://graph.facebook.com/oauth/access_token";
	
	public static final String graphUrl = "https://graph.facebook.com";
	
	public static void auth(String code) throws Exception {
		
		WSRequest request =  WS.url(authUrl);
		
		request.setParameter("client_id",  Play.configuration.get("facebook_clientId"));
		request.setParameter("redirect_uri", Play.configuration.get("facebook_redirectUrl"));
		request.setParameter("client_secret", Play.configuration.get("facebook_appsecret"));
		request.setParameter("code", code);
		
		String access_token = request.get().getString().split("&")[0].split("=")[1];

		System.out.println(access_token);
		
		request =  WS.url(graphUrl+"/me");
		request.setParameter("access_token", access_token);
		JsonElement element = request.get().getJson();
		String name = element.getAsJsonObject().get("name").getAsString();
		String id = element.getAsJsonObject().get("id").getAsString();
		
		Query query = JPA.em().createQuery("from User where facebookId=:id");
		query.setParameter("id", id);
		
		List<User> users =query.getResultList();
		if(users.isEmpty()){
			User user = new User();
			user.name = name;
			user.facebookId = id;
			user.save();
			session.put("userId", user.id);
		}else{
			session.put("userId", users.get(0).id);
		}
		Application.index();
	}
}
