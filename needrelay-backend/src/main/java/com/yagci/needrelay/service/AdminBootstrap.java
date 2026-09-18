package com.yagci.needrelay.service;

import com.yagci.needrelay.config.NeedRelayProperties;
import com.yagci.needrelay.domain.Organizer;
import com.yagci.needrelay.domain.OrganizerRole;
import com.yagci.needrelay.repository.OrganizerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the bootstrap ADMIN organizer when the database has none.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final OrganizerRepository organizerRepository;
	private final PasswordEncoder passwordEncoder;
	private final NeedRelayProperties properties;

	/**
	 * @param organizerRepository organizer persistence
	 * @param passwordEncoder password hasher
	 * @param properties admin bootstrap settings
	 */
	public AdminBootstrap(
			OrganizerRepository organizerRepository,
			PasswordEncoder passwordEncoder,
			NeedRelayProperties properties) {
		this.organizerRepository = organizerRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	/**
	 * Seeds an ADMIN account from {@code needrelay.admin.*} when organizer count is zero.
	 *
	 * @param args startup args (unused)
	 */
	@Override
	public void run(ApplicationArguments args) {
		if (organizerRepository.count() > 0) {
			return;
		}
		NeedRelayProperties.Admin admin = properties.getAdmin();
		Organizer organizer = new Organizer();
		organizer.setEmail(admin.getEmail().trim().toLowerCase());
		organizer.setPasswordHash(passwordEncoder.encode(admin.getPassword()));
		organizer.setDisplayName(admin.getDisplayName());
		organizer.setRole(OrganizerRole.ADMIN);
		organizer.setActive(true);
		organizerRepository.save(organizer);
		log.info("Bootstrap ADMIN organizer created for email {}", organizer.getEmail());
	}
}
