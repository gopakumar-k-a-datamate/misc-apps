package org.datamate.identity.shared.i18n.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class I18nService {

    private final MessageSource messageSource;

    /**
     * Get a simple message by code.
     *
     * @param code Message code (e.g., "greeting", "patient.welcome")
     * @param args Optional arguments for placeholders
     * @param locale Optional locale (uses current request locale if null)
     * @return Localized message
     */
    public String getMessage(String code, Object[] args, Locale locale) {
        Locale targetLocale = (locale != null) ? locale : LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, targetLocale);
    }

    
    /**
     * Get an ICU-formatted message with plural/gender rules.
     *
     * @param code Message code
     * @param args Map of arguments (e.g., {"count": 5})
     * @return Formatted message
     */
    public String getIcuMessage(String code, Map<String, Object> args) {
        Locale locale = LocaleContextHolder.getLocale();
        String pattern = messageSource.getMessage(code, null, code, locale);
        MessageFormat mf = new MessageFormat(pattern, locale);

        Object[] values = args.values().toArray();
        return mf.format(values);
    }
}
