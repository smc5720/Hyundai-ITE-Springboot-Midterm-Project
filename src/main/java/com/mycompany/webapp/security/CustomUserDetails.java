package com.mycompany.webapp.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class CustomUserDetails extends User{
	private int mno;

	public CustomUserDetails(
			String username,
			String password,
			boolean enabled,
			Collection<? extends GrantedAuthority> authorities,
			int mno) {
		super(username, password, enabled, true, true, true, authorities);
		this.mno = mno;
	}
	
	public int getMno() {
		return mno;
	}
}
