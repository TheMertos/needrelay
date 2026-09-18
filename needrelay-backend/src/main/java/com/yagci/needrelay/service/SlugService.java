package com.yagci.needrelay.service;

import com.yagci.needrelay.common.UuidV7;
import com.yagci.needrelay.repository.ReliefRequestRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Builds unique public URL slugs from relief request titles.
 */
@Service
public class SlugService {

	private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
	private static final Pattern WHITESPACE = Pattern.compile("[\\s_]+");
	private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

	private final ReliefRequestRepository reliefRequestRepository;

	/**
	 * @param reliefRequestRepository slug uniqueness checks
	 */
	public SlugService(ReliefRequestRepository reliefRequestRepository) {
		this.reliefRequestRepository = reliefRequestRepository;
	}

	/**
	 * Slugifies a title and appends a unique short UUID7 suffix.
	 *
	 * @param title relief request title
	 * @return unique public slug
	 */
	public String createUniqueSlug(String title) {
		String base = slugify(title);
		if (base.isBlank()) {
			base = "relief";
		}
		String candidate = base + "-" + shortSuffix();
		int attempts = 0;
		while (reliefRequestRepository.existsByPublicSlug(candidate) && attempts < 20) {
			candidate = base + "-" + shortSuffix();
			attempts++;
		}
		if (reliefRequestRepository.existsByPublicSlug(candidate)) {
			candidate = base + "-" + UuidV7.generate().toString().replace("-", "");
		}
		return candidate;
	}

	/**
	 * Converts a title into a lowercase hyphenated slug fragment.
	 *
	 * @param title raw title
	 * @return slug base without uniqueness suffix
	 */
	String slugify(String title) {
		String normalized = Normalizer.normalize(title, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		String dashed = WHITESPACE.matcher(normalized.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
		String cleaned = NON_LATIN.matcher(dashed).replaceAll("");
		return MULTI_DASH.matcher(cleaned).replaceAll("-").replaceAll("^-|-$", "");
	}

	/**
	 * Returns an 8-character suffix from a UUID7.
	 *
	 * @return short hex-ish suffix
	 */
	private String shortSuffix() {
		return UuidV7.generate().toString().replace("-", "").substring(0, 8);
	}
}
