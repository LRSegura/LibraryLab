package common.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lending.model.Loan;
import lombok.Getter;
import membership.model.Member;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Centralized configuration for the library application.
 * <p>
 * Uses MicroProfile Config to externalize business parameters that were
 * previously hardcoded in domain entities. Values are read from
 * META-INF/microprofile-config.properties by default, but can be overridden
 * via system properties or environment variables following the MicroProfile
 * Config ordinal priority:
 * <ol>
 *     <li>System properties (ordinal 400)</li>
 *     <li>Environment variables (ordinal 300)</li>
 *     <li>microprofile-config.properties (ordinal 100)</li>
 * </ol>
 * <p>
 * The {@code defaultValue} on each {@code @ConfigProperty} ensures the
 * application works even without the properties file, using the same
 * defaults that were previously defined as constants in the domain entities.
 */
@Getter
@ApplicationScoped
public class LibraryConfig {

    // -------------------------------------------------------------------------
    // Lending configuration
    // -------------------------------------------------------------------------

    /**
     * Number of days for a new loan period.
     */
    private final int defaultLoanDays;

    /**
     * Maximum number of times a loan can be renewed.
     */
    private final int maxRenewals;

    /**
     * Number of days added on each loan renewal.
     */
    private final int renewalDays;

    // -------------------------------------------------------------------------
    // Membership configuration
    // -------------------------------------------------------------------------

    /**
     * Default maximum number of concurrent loans for a new member.
     */
    private final int defaultMaxLoans;

    /**
     * Default membership duration in years for new members.
     */
    private final int defaultMembershipYears;

    // CDI proxy no-arg constructor
    public LibraryConfig() {
        this.defaultLoanDays = Loan.DEFAULT_LOAN_DAYS;
        this.maxRenewals = Loan.DEFAULT_MAX_RENEWALS;
        this.renewalDays = Loan.DEFAULT_LOAN_DAYS;
        this.defaultMaxLoans = Member.DEFAULT_MAX_LOANS;
        this.defaultMembershipYears = Member.DEFAULT_MEMBERSHIP_YEARS;
    }

    @Inject
    public LibraryConfig(
            @ConfigProperty(name = "library.lending.default-loan-days", defaultValue = "14") int defaultLoanDays,
            @ConfigProperty(name = "library.lending.max-renewals", defaultValue = "2") int maxRenewals,
            @ConfigProperty(name = "library.lending.renewal-days", defaultValue = "14") int renewalDays,
            @ConfigProperty(name = "library.membership.default-max-loans", defaultValue = "5") int defaultMaxLoans,
            @ConfigProperty(name = "library.membership.default-duration-years", defaultValue = "1") int defaultMembershipYears) {
        this.defaultLoanDays = defaultLoanDays;
        this.maxRenewals = maxRenewals;
        this.renewalDays = renewalDays;
        this.defaultMaxLoans = defaultMaxLoans;
        this.defaultMembershipYears = defaultMembershipYears;
    }
}