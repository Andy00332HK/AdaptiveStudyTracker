package com.example.adaptivestudytracker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TaskFormValidator {

    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;

        private ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }

    private static final Set<String> ALLOWED_CATEGORIES = new HashSet<>(Arrays.asList(
            Task.CATEGORY_STUDY,
            Task.CATEGORY_LIFE,
            Task.CATEGORY_SLEEP,
            Task.CATEGORY_EXERCISE
    ));

    private static final Set<String> ALLOWED_REMINDER_FREQUENCIES = new HashSet<>(Arrays.asList(
            Task.REMINDER_NONE,
            Task.REMINDER_DAILY
    ));

    public static ValidationResult validate(String title, String category, long dueTimeMillis,
                                            String reminderFrequency, int importance) {
        String trimmedTitle = title == null ? "" : title.trim();
        if (trimmedTitle.length() < 2 || trimmedTitle.length() > 80) {
            return ValidationResult.error("Title must be 2 to 80 characters.");
        }

        if (!ALLOWED_CATEGORIES.contains(category)) {
            return ValidationResult.error("Please choose a valid category.");
        }

        if (dueTimeMillis <= 0L) {
            return ValidationResult.error("Please choose a valid due time.");
        }

        if (!ALLOWED_REMINDER_FREQUENCIES.contains(reminderFrequency)) {
            return ValidationResult.error("Please choose a valid reminder frequency.");
        }

        if (importance < 1 || importance > 5) {
            return ValidationResult.error("Importance must be between 1 and 5.");
        }

        return ValidationResult.ok();
    }
}

