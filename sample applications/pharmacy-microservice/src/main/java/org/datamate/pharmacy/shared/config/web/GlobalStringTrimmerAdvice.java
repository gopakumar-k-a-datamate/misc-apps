package org.datamate.pharmacy.shared.config.web;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * A global controller advice that applies string trimming to all form data
 * and query parameters handled by Spring's WebDataBinder.
 * Aligned with the dental project's GlobalStringTrimmerAdvice.
 */
@ControllerAdvice
public class GlobalStringTrimmerAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        StringTrimmerEditor stringTrimmer = new StringTrimmerEditor(false);
        binder.registerCustomEditor(String.class, stringTrimmer);
    }
}
