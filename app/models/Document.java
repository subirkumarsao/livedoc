package models;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class Document extends Model{

	public String name;

	public int version;
	
}
