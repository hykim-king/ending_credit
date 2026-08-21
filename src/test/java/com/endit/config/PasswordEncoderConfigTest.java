package com.endit.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.endit.domain.MemberVO;

class PasswordEncoderConfigTest {

	@Test
	void passwordEncoderUsesOneWayHash() {
		PasswordEncoder encoder = new PasswordEncoderConfig().passwordEncoder();
		String rawPassword = "plain-password";

		String encodedPassword = encoder.encode(rawPassword);

		assertFalse(encodedPassword.equals(rawPassword));
		assertTrue(encoder.matches(rawPassword, encodedPassword));
	}

	@Test
	void memberToStringDoesNotExposePassword() {
		MemberVO member = new MemberVO();
		member.setPassword("secret-password");

		assertFalse(member.toString().contains("secret-password"));
	}
}
