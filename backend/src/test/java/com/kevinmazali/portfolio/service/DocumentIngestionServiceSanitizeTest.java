package com.kevinmazali.portfolio.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DocumentIngestionServiceSanitizeTest {

	@Test
	void allowsSimpleRelative() {
		assertEquals("doc.pdf", DocumentIngestionService.sanitizeRelativePath("doc.pdf"));
	}

	@Test
	void normalizesSlashesAndTrims() {
		assertEquals("a/b.pdf", DocumentIngestionService.sanitizeRelativePath("  a\\b.pdf  "));
	}

	@Test
	void rejectsAbsolute() {
		assertNull(DocumentIngestionService.sanitizeRelativePath("/etc/passwd"));
	}

	@Test
	void rejectsDotDotSegment() {
		assertNull(DocumentIngestionService.sanitizeRelativePath("foo/../bar.pdf"));
	}

	@Test
	void rejectsBareDotDot() {
		assertNull(DocumentIngestionService.sanitizeRelativePath(".."));
	}
}
