package au.gov.ga.geodesy.domain.model;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Ad-hoc methods for querying and updating Setups.
 */
public interface SetupRepositoryCustom {

    /**
     * Find the Setup currently in effect at a Site.
     */
    Setup findCurrentBySiteId(Integer siteId, SetupType type);

    /**
     * Find all Setups at a Site for a period of interest.
     */
    Page<Setup> findBySiteIdAndPeriod(Integer siteId, SetupType type, Instant periodStart, Instant periodEnd, Pageable pageRequest);

    /**
     * Delete all Setups and associated EquipmentInUse objects.
     */
    void deleteAllInBatch();
}
