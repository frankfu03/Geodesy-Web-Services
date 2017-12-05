package au.gov.ga.geodesy.domain.service;

import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import au.gov.ga.geodesy.domain.model.CorsSite;
import au.gov.ga.geodesy.domain.model.CorsSiteRepository;
import au.gov.ga.geodesy.domain.model.Setup;
import au.gov.ga.geodesy.domain.model.SetupFactory;
import au.gov.ga.geodesy.domain.model.SetupRepository;
import au.gov.ga.geodesy.domain.model.SetupType;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentConfigurationRepository;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentRepository;
import au.gov.ga.geodesy.domain.model.equipment.HumiditySensorRepository;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLogRepository;

/**
 * Equipment setup service.
 */
@Component
@Transactional("geodesyTransactionManager")
public class SetupService {

    private static final Logger log = LoggerFactory.getLogger(SetupService.class);

    @Autowired
    private SetupFactory setupFactory;

    @Autowired
    private SetupRepository setups;

    @Autowired
    private SiteLogRepository siteLogs;

    @Autowired
    private CorsSiteRepository corsSites;

    @Autowired
    private EquipmentConfigurationRepository equipmentConfigurations;

    @Autowired
    private EquipmentRepository equipment;

    @Autowired
    private HumiditySensorRepository humiditySensors;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Update equipment setups for all CORS sites.
     */
    @Async
    public void updateSetups() {
        StopWatch w = new StopWatch();
        w.start();
        this.deleteSetups();
        this.createSetups();
        w.stop();
        log.info("Updated all setups in " + w.getTotalTimeSeconds() + " seconds.");
    }

    /**
     * Delete equipment setups for all CORS sites.
     */
    public void deleteSetups() {
        setups.deleteAllInBatch();
        equipmentConfigurations.deleteAllInBatch();

        // TODO: Remove in the next release to production, because humidity sensors are
        // no longer created as part of CorsSetups.
        humiditySensors.deleteAllInBatch();

        equipment.deleteAllInBatch();
    }

    /**
     * Create equipment setups for all CORS sites.
     */
    public void createSetups() {
        corsSites.findAll().forEach(site -> {
            createSetups(site);
            entityManager.flush();
            entityManager.clear();
        });
    }

    /**
     * Create equipment setups for given CORS site.
     */
    public void createSetups(CorsSite site) {
        Integer siteId = site.getId();
        String fourCharId = site.getFourCharacterId();
        SiteLog siteLog = siteLogs.findByFourCharacterId(fourCharId);
        HashMap<SetupType, List<Setup>> setupsByType = setupFactory.createSetups(siteId, siteLog);
        setupsByType.forEach((setupType, newSetups) -> {
            List<Setup> oldSetups = setups.findBySiteId(siteId, setupType);
            @SuppressWarnings("unchecked")
            List<Setup> commonSetups = ListUtils.intersection(oldSetups, newSetups);
            newSetups.removeAll(commonSetups);
            oldSetups.removeAll(commonSetups);

            oldSetups.forEach(s -> {
                s.invalidate();
                log.info("Invalidated setup : " + s.getId());
            });

            setups.save(oldSetups);
            setups.save(newSetups);
        });
        log.info("Generated setups for site " + fourCharId);
    }
}

