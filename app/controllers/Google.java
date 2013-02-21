package controllers;

import java.util.List;

import javax.persistence.Query;

import models.User;
import play.Play;
import play.db.jpa.JPA;
import play.libs.WS;
import play.libs.WS.WSRequest;
import play.mvc.Controller;

import com.google.gson.JsonObject;

public class Google extends Controller{
	
	public static void oauth(String code) throws Exception{
		String authUrl = "http://accounts.google.com/o/oauth2/token";
		String apiUrl = "https://www.googleapis.com/oauth2/v1/userinfo";
		
		WSRequest request =  WS.url(authUrl);
		
		request.setParameter("client_id",  Play.configuration.get("google_clientId"));
		request.setParameter("redirect_uri", Play.configuration.get("google_redirectUrl"));
		request.setParameter("client_secret", Play.configuration.get("google_appsecret"));
		request.setParameter("code", code);
		request.setParameter("grant_type", "authorization_code");
		
		String access_token = request.post().getJson().getAsJsonObject().get("access_token").getAsString();
		
		request = WS.url(apiUrl);
		request.setParameter("access_token", access_token);
		JsonObject obj = request.get().getJson().getAsJsonObject();
		
		String name = obj.get("name").getAsString();
		String id = obj.get("id").getAsString();
		
		Query query = JPA.em().createQuery("from User where googleId=:id");
		query.setParameter("id", id);
		
		List<User> users =query.getResultList();
		if(users.isEmpty()){
			User user = new User();
			user.name = name;
			user.googleId = id;
			user.save();
			session.put("userId", user.id);
		}else{
			session.put("userId", users.get(0).id);
		}
		Application.home(null);
	}

}
