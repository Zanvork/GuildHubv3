package com.zanvork.guildhubv3.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.security.core.GrantedAuthority;

/**
 *
 * @author zanvork
 */

@Data
@EqualsAndHashCode(exclude="users")
@ToString(exclude="users")
@JsonIgnoreProperties("users")
@Entity
@Table(name = "role")
public class Role implements GrantedAuthority {
    public static final String 
            ROLE_USER   =   "ROLE_USER",
            ROLE_ADMIN  =   "ROLE_ADMIN";
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private long id;

	@NotEmpty
	private String name;

	@JsonIgnore
	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "roles")
	private Set<User> users = new HashSet<>();

    public Role(){
        
    }
    public Role(String name){
        this.name   =   name;
    }
	@Override
	public String getAuthority() {
		return name;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<User> getUsers() {
		return users;
	}

	public void setUsers(Set<User> users) {
		this.users = users;
	}

}