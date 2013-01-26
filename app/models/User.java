package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;

import play.db.jpa.Model;

@Entity
public class User extends Model{

	public String name;
	
	public String googleId;
	
	public String facebookId;
	
	public String twitterId;
	
	@OneToMany
	public List<Document> documents;
	
}
