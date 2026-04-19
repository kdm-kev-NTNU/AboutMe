package com.kevinmazali.portfolio.service;

import ai.philterd.phileas.model.configuration.PhileasConfiguration;
import ai.philterd.phileas.model.enums.FilterType;
import ai.philterd.phileas.model.enums.MimeType;
import ai.philterd.phileas.model.objects.Span;
import ai.philterd.phileas.model.policy.Identifiers;
import ai.philterd.phileas.model.policy.Policy;
import ai.philterd.phileas.model.policy.filters.EmailAddress;
import ai.philterd.phileas.model.policy.filters.FirstName;
import ai.philterd.phileas.model.policy.filters.PhoneNumber;
import ai.philterd.phileas.model.policy.filters.Surname;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.FirstNameFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.dynamic.SurnameFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.EmailAddressFilterStrategy;
import ai.philterd.phileas.model.policy.filters.strategies.rules.PhoneNumberFilterStrategy;
import ai.philterd.phileas.model.responses.FilterResponse;
import ai.philterd.phileas.services.PhileasFilterService;
import com.kevinmazali.portfolio.config.SanitizerProperties;
import com.kevinmazali.portfolio.model.SanitizeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PII detection and redaction powered by Phileas.
 * <p>
 * Detects person names (first + surname via dictionary/census data),
 * email addresses, and phone numbers. Replaces each detected span
 * with a redaction marker such as {@code {{{REDACTED-email-address}}}}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "portfolio.sanitizer.enabled", havingValue = "true", matchIfMissing = true)
public class PiiSanitizerService {

    private final PhileasFilterService filterService;
    private final Policy policy;

    public PiiSanitizerService(PhileasConfiguration phileasConfiguration, SanitizerProperties props) throws IOException {
        this.filterService = new PhileasFilterService(phileasConfiguration, null);
        this.policy = buildPolicy();
        log.info("PII sanitizer initialized (Phileas); ph-eye NER endpoint: {}",
                props.getPhEyeUrl().isBlank() ? "disabled" : props.getPhEyeUrl());
    }

    /**
     * Detects and redacts PII in the given text.
     *
     * @return result with sanitized text and detection stats
     */
    public SanitizeResult sanitize(String text) {
        if (text == null || text.isBlank()) {
            return new SanitizeResult(text, 0, Map.of(), "PASS");
        }

        try {
            FilterResponse response = filterService.filter(
                    policy, "document-sanitize", "doc", text, MimeType.TEXT_PLAIN);

            List<Span> spans = response.getExplanation().appliedSpans();
            int detected = spans.size();

            Map<String, Integer> typeCount = new HashMap<>();
            for (Span span : spans) {
                FilterType ft = span.getFilterType();
                String type = ft != null ? ft.name() : "UNKNOWN";
                typeCount.merge(type, 1, Integer::sum);
            }

            String status = detected == 0 ? "PASS" : "REVIEW";

            log.debug("PII sanitization: {} entities detected ({})", detected, typeCount);
            return new SanitizeResult(response.getFilteredText(), detected, typeCount, status);
        } catch (Exception e) {
            log.warn("PII sanitization failed, returning original text: {}", e.getMessage());
            return new SanitizeResult(text, 0, Map.of(), "FAIL");
        }
    }

    private Policy buildPolicy() {
        EmailAddressFilterStrategy emailStrategy = new EmailAddressFilterStrategy();
        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setEmailAddressFilterStrategies(List.of(emailStrategy));

        PhoneNumberFilterStrategy phoneStrategy = new PhoneNumberFilterStrategy();
        PhoneNumber phoneNumber = new PhoneNumber();
        phoneNumber.setPhoneNumberFilterStrategies(List.of(phoneStrategy));

        FirstNameFilterStrategy firstNameStrategy = new FirstNameFilterStrategy();
        FirstName firstName = new FirstName();
        firstName.setFirstNameFilterStrategies(List.of(firstNameStrategy));

        SurnameFilterStrategy surnameStrategy = new SurnameFilterStrategy();
        Surname surname = new Surname();
        surname.setSurnameFilterStrategies(List.of(surnameStrategy));

        Identifiers identifiers = new Identifiers();
        identifiers.setEmailAddress(emailAddress);
        identifiers.setPhoneNumber(phoneNumber);
        identifiers.setFirstName(firstName);
        identifiers.setSurname(surname);

        Policy p = new Policy();
        p.setIdentifiers(identifiers);
        return p;
    }
}
