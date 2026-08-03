package com.kapor.user.model;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Stable identifiers for onboarding goals; labels belong to the client. */
public enum LearningGoal {
    OFFICE_COMMUNICATION("office_communication"),
    IT_TERMINOLOGY("it_terminology"),
    INTERVIEW_PREP("interview_prep"),
    WORKPLACE_LIFE("workplace_life");

    private final String value;

    LearningGoal(String value) { this.value = value; }

    public String value() { return value; }

    public static Set<String> normalize(Collection<String> rawGoals) {
        Set<String> normalized = new LinkedHashSet<>();
        if (rawGoals == null) return normalized;
        for (String rawGoal : rawGoals) {
            if (rawGoal == null || rawGoal.isBlank()) continue;
            String candidate = rawGoal.trim().toLowerCase(Locale.ROOT);
            if (candidate.equals(OFFICE_COMMUNICATION.value) || candidate.contains("giao tiếp văn phòng")) {
                normalized.add(OFFICE_COMMUNICATION.value);
            } else if (candidate.equals(IT_TERMINOLOGY.value) || candidate.contains("thuật ngữ it")) {
                normalized.add(IT_TERMINOLOGY.value);
            } else if (candidate.equals(INTERVIEW_PREP.value) || candidate.contains("phỏng vấn")) {
                normalized.add(INTERVIEW_PREP.value);
            } else if (candidate.equals(WORKPLACE_LIFE.value) || candidate.contains("sinh hoạt công sở")) {
                normalized.add(WORKPLACE_LIFE.value);
            }
        }
        return normalized;
    }
}
