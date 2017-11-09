package au.gov.ga.geodesy.domain.service;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import au.gov.ga.geodesy.domain.model.CorsSite;
import au.gov.ga.geodesy.domain.model.CorsSiteRepository;
import au.gov.ga.geodesy.domain.model.Setup;
import au.gov.ga.geodesy.domain.model.SetupFactory;
import au.gov.ga.geodesy.domain.model.SetupRepository;
import au.gov.ga.geodesy.domain.model.SetupType;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentConfigurationRepository;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentRepository;
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

    /**
     * Update equipment setups for all CORS sites.
     */
    @Async
    public void updateSetups() {
        this.deleteSetups();
        this.createSetups();
        log.info("Updated all setups.");
    }

    /**
     * Delete equipment setups for all CORS sites.
     */
    public void deleteSetups() {
        setups.deleteAll();
        equipmentConfigurations.deleteAll();
        equipment.deleteAll();
    }

    /**
     * Create equipment setups for all CORS sites.
     */
    public void createSetups() {
        corsSites.findAll().forEach(this::createSetups);
    }

    /**
     * Create equipment setups for given CORS site.
     */
    public void createSetups(CorsSite site) {
        Integer siteId = site.getId();
        SiteLog siteLog = siteLogs.findByFourCharacterId(site.getFourCharacterId());
        HashMap<SetupType, List<Setup>> setupsByType = setupFactory.createSetups(siteId, siteLog);
        setupsByType.forEach((setupType, newSetups) -> {
            List<Setup> oldSetups = setups.findBySiteId(siteId, setupType);
            @SuppressWarnings("unchecked")
            List<Setup> commonSetups = ListUtils.intersection(oldSetups, newSetups);
            newSetups.removeAll(commonSetups);
            oldSetups.removeAll(commonSetups);

            oldSetups.forEach(s -> {
                    s.invalidate();
                    log.info("Invalidated site : " + s.getId());
                });

            setups.save(oldSetups);
            setups.save(newSetups);
        });
    }
}

