package org.datamate.pharmacy.shared.config.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * i18n Configuration.
 * Aligned with the dental project's I18nConfiguration to programmatically
 * configure the MessageSource and LocaleResolver instead of using application.yml.
 */
@Configuration
public class I18nConfiguration implements WebMvcConfigurer {

    @Bean
    @Primary
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();

        // Load the messages we just created in resources/messages/messages.properties
        source.setBasenames("classpath:messages/messages");

        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setCacheSeconds(3600); // Cache for 1 hour in production
        source.setFallbackToSystemLocale(false);
        source.setUseCodeAsDefaultMessage(false);

        return source;
    }

    /**
     * Stateless locale resolver - each request is independent.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.ENGLISH);
        return localeResolver;
    }

    /**
     * Allow locale switching via 'lang' query parameter.
     * Example: ?lang=fr
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        registry.addInterceptor(interceptor);
    }
}
