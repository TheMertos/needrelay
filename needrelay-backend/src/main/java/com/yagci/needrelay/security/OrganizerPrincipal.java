package com.yagci.needrelay.security;

import com.yagci.needrelay.domain.OrganizerRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Authenticated organizer principal for Spring Security.
 */
@Getter
public class OrganizerPrincipal implements UserDetails {

	private final UUID id;
	private final String email;
	private final String passwordHash;
	private final OrganizerRole role;
	private final boolean active;

	/**
	 * Creates a principal from organizer fields.
	 *
	 * @param id organizer id
	 * @param email email
	 * @param passwordHash password hash
	 * @param role role
	 * @param active whether account is active
	 */
	public OrganizerPrincipal(UUID id, String email, String passwordHash, OrganizerRole role, boolean active) {
		this.id = id;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.active = active;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return active;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return active;
	}
}
