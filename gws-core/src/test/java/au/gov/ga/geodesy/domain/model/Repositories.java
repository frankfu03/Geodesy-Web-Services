package au.gov.ga.geodesy.domain.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import au.gov.ga.geodesy.domain.model.sitelog.SiteLogRepository;
import au.gov.ga.geodesy.domain.service.SetupService;

/**
 * Utilities that affect all repositories.
 */
@Configurable
public class Repositories {

    private static final Logger log = LoggerFactory.getLogger(Repositories.class);

    @Autowired
    private SiteLogRepository siteLogs;

    @Autowired
    private CorsSiteRepository sites;

    @Autowired
    private SetupService setupService;

    @Autowired
    private NodeRepository nodes;

    /**
     * Delete all persistent entitites.
     */
    public void deleteAll() {
        siteLogs.deleteAll();
        nodes.deleteAll();
        setupService.deleteSetups();
        sites.deleteAll();
        log.info("Deleted all data");
    }
}
