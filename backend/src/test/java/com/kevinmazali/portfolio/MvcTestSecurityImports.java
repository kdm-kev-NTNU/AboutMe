package com.kevinmazali.portfolio;

import com.kevinmazali.portfolio.config.SecurityConfig;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Standard {@code @WebMvcTest} security wiring: production {@link SecurityConfig}, session JWT
 * beans, and in-memory users. Import this instead of listing the three classes separately.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({SecurityConfig.class, MvcTestSessionAuthConfig.class, MvcTestUserDetailsConfig.class})
public @interface MvcTestSecurityImports {}
