package com.example.adaptivestudytracker;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskFormValidatorTest {

    @Test
    public void validate_acceptsValidTask() {
        TaskFormValidator.ValidationResult result = TaskFormValidator.validate(
                "Study chapter 3",
                Task.CATEGORY_STUDY,
                System.currentTimeMillis() + 60_000,
                Task.REMINDER_DAILY,
                4
        );

        assertTrue(result.isValid);
    }

    @Test
    public void validate_rejectsTooShortTitle() {
        TaskFormValidator.ValidationResult result = TaskFormValidator.validate(
                "A",
                Task.CATEGORY_STUDY,
                System.currentTimeMillis() + 60_000,
                Task.REMINDER_NONE,
                3
        );

        assertFalse(result.isValid);
    }

    @Test
    public void validate_rejectsInvalidCategory() {
        TaskFormValidator.ValidationResult result = TaskFormValidator.validate(
                "Task title",
                "other",
                System.currentTimeMillis() + 60_000,
                Task.REMINDER_NONE,
                3
        );

        assertFalse(result.isValid);
    }

    @Test
    public void validate_rejectsInvalidImportance() {
        TaskFormValidator.ValidationResult result = TaskFormValidator.validate(
                "Task title",
                Task.CATEGORY_SLEEP,
                System.currentTimeMillis() + 60_000,
                Task.REMINDER_DAILY,
                9
        );

        assertFalse(result.isValid);
    }

    @Test
    public void validate_rejectsNonDailyReminderFrequencies() {
        TaskFormValidator.ValidationResult result = TaskFormValidator.validate(
                "Task title",
                Task.CATEGORY_STUDY,
                System.currentTimeMillis() + 60_000,
                Task.REMINDER_WEEKLY,
                3
        );

        assertFalse(result.isValid);
    }
}

