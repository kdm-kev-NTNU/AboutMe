package com.kevinmazali.portfolio.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidatorTest {

	@Test
	void isValidQuestionRejectsNullBlankAndTooLong() {
		assertFalse(InputValidator.isValidQuestion(null));
		assertFalse(InputValidator.isValidQuestion(""));
		assertFalse(InputValidator.isValidQuestion("   "));
		assertFalse(InputValidator.isValidQuestion("a".repeat(3001)));
	}

	@Test
	void isValidQuestionRejectsScriptAndProtocolPatterns() {
		assertFalse(InputValidator.isValidQuestion("What is <script>alert(1)</script>?"));
		assertFalse(InputValidator.isValidQuestion("Click javascript:void(0)"));
		assertFalse(InputValidator.isValidQuestion("data:text/html,<p>x</p>"));
		assertFalse(InputValidator.isValidQuestion("vbscript:evil"));
	}

	@Test
	void isValidQuestionAcceptsLettersNumbersPunctuationSymbolsAndWhitespace() {
		assertTrue(InputValidator.isValidQuestion("Hello, world 123, «test»?"));
		assertTrue(InputValidator.isValidQuestion("2+2?"));
		assertTrue(InputValidator.isValidQuestion("Cost is ~€10 (or $11)."));
	}

	@Test
	void isValidQuestionRejectsUnsafeCharacters() {
		assertFalse(InputValidator.isValidQuestion("What about\tcontrol?"));
	}

	@Test
	void isValidRequesterIdRejectsNullBlankAndTooLong() {
		assertFalse(InputValidator.isValidRequesterId(null));
		assertFalse(InputValidator.isValidRequesterId(""));
		assertFalse(InputValidator.isValidRequesterId("x".repeat(101)));
	}

	@Test
	void isValidRequesterIdAcceptsSafeString() {
		assertTrue(InputValidator.isValidRequesterId("user-42"));
	}

	@Test
	void sanitizeStringHandlesNullAndControlChars() {
		assertNull(InputValidator.sanitizeString(null));
		assertEquals("a b", InputValidator.sanitizeString("a\u0000b"));
		assertEquals("a b", InputValidator.sanitizeString("a  \n  b"));
	}

	@Test
	void isValidQuestionRespectsCustomMaxLength() {
		assertFalse(InputValidator.isValidQuestion("12345", 4));
		assertTrue(InputValidator.isValidQuestion("1234", 4));
	}

	@Test
	void isValidFeedbackMessageRejectsBlockedPatterns() {
		assertFalse(InputValidator.isValidFeedbackMessage("see <script"));
		assertFalse(InputValidator.isValidFeedbackMessage("ok!", 2));
	}
}
