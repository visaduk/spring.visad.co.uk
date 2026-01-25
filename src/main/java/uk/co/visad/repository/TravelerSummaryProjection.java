package uk.co.visad.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lightweight projection for listing travelers on the dashboard.
 * Excludes heavy TEXT fields like notes, logins, and invoice JSON.
 */
public interface TravelerSummaryProjection {
    Long getId();

    String getTitle();

    String getFirstName();

    String getLastName();

    String getName(); // Fallback name

    String getPassportNo();

    String getTravelCountry();

    String getVisaType();

    String getVisaCenter();

    String getStatus();

    String getPriority();

    String getPaymentStatus();

    LocalDate getPlannedTravelDate();

    LocalDateTime getCreatedAt();

    LocalDateTime getLastUpdatedAt();

    String getCreatedByUsername();

    String getEmail();

    String getContactNumber();
}
