package com.yagci.needrelay.service;

import com.yagci.needrelay.domain.Invite;
import com.yagci.needrelay.repository.InviteRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Periodically delivers invite emails that are queued in the database, so a slow or
 * failing email provider never blocks the request that created the invite.
 */
@Service
@AllArgsConstructor
public class InviteEmailDispatchService {

	private static final Logger log = LoggerFactory.getLogger(InviteEmailDispatchService.class);
	private static final int MAX_ATTEMPTS = 5;

	private final InviteRepository inviteRepository;
	private final EmailService emailService;

	/**
	 * Sends every queued invite email, oldest first. Runs on the scheduler thread, not
	 * the request thread, so this never blocks invite creation.
	 */
	@Scheduled(initialDelay = 5_000, fixedDelay = 5_000)
	public void dispatchPending() {
		for (Invite invite : inviteRepository.findPendingEmail(MAX_ATTEMPTS)) {
			sendOne(invite.getId());
		}
	}

	/**
	 * Sends a single invite's email and records the outcome. Persists explicitly (rather
	 * than relying on transactional dirty checking) since this is called by
	 * {@link #dispatchPending()} within the same bean, where a {@code @Transactional} on
	 * this method would be bypassed by Spring's self-invocation limitation.
	 *
	 * @param inviteId invite id
	 */
	void sendOne(UUID inviteId) {
		Invite invite = inviteRepository.findById(inviteId).orElse(null);
		if (invite == null || invite.getEmail() == null || invite.getEmailSentAt() != null) {
			return;
		}
		try {
			emailService.sendInvite(invite.getEmail(), invite.getToken());
			invite.setEmailSentAt(Instant.now());
			invite.setEmailError(null);
		} catch (Exception ex) {
			invite.setEmailAttempts(invite.getEmailAttempts() + 1);
			invite.setEmailError(ex.getMessage());
			log.error("Failed to email invite {} to {} (attempt {}): {}",
					inviteId, invite.getEmail(), invite.getEmailAttempts(), ex.getMessage());
		}
		inviteRepository.save(invite);
	}
}
