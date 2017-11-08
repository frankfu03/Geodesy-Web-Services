package au.gov.ga.geodesy.domain.service;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import au.gov.ga.geodesy.domain.model.CorsSite;
import au.gov.ga.geodesy.domain.model.CorsSiteRepository;
import au.gov.ga.geodesy.domain.model.Monument;
import au.gov.ga.geodesy.domain.model.Setup;
import au.gov.ga.geodesy.domain.model.SetupFactory;
import au.gov.ga.geodesy.domain.model.SetupRepository;
import au.gov.ga.geodesy.domain.model.SetupType;
import au.gov.ga.geodesy.domain.model.event.Event;
import au.gov.ga.geodesy.domain.model.event.EventPublisher;
import au.gov.ga.geodesy.domain.model.event.EventSubscriber;
import au.gov.ga.geodesy.domain.model.event.SiteLogReceived;
import au.gov.ga.geodesy.domain.model.event.SiteUpdated;
import au.gov.ga.geodesy.domain.model.sitelog.SiteIdentification;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLogRepository;

@Component
@Transactional("geodesyTransactionManager")
public class CorsSiteService implements EventSubscriber<SiteLogReceived> {

    private static final Logger log = LoggerFactory.getLogger(CorsSiteService.class);

    @Autowired
    private SiteLogRepository siteLogs;

    @Autowired
    private CorsSiteRepository corsSites;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private SetupRepository setups;

    @Autowired
    private SetupFactory setupFactory;

    @PostConstruct
    private void subscribe() {
        eventPublisher.subscribe(this);
    }

    public boolean canHandle(Event e) {
        return e instanceof SiteLogReceived;
    }

    public void handle(SiteLogReceived siteLogReceived) {
        log.info("Notified of " + siteLogReceived);

        String fourCharacterId = siteLogReceived.getFourCharacterId();
        SiteLog siteLog = siteLogs.findByFourCharacterId(fourCharacterId);

        CorsSite corsSite = Optional.ofNullable(corsSites.findByFourCharacterId(fourCharacterId))
            .orElse(new CorsSite(fourCharacterId));

        corsSite.setName(siteLog.getSiteIdentification().getSiteName());

        SiteIdentification siteId = siteLog.getSiteIdentification();
        corsSite.setDomesNumber(siteId.getIersDOMESNumber());
        corsSite.setDateInstalled(siteId.getDateInstalled());

        Monument monument = new Monument();
        monument.setDescription(siteId.getMonumentDescription());
        monument.setHeight(siteId.getHeightOfMonument());
        monument.setFoundation(siteId.getMonumentFoundation());
        monument.setMarkerDescription(siteId.getMarkerDescription());
        corsSite.setMonument(monument);

        corsSites.saveAndFlush(corsSite);

        HashMap<SetupType, List<Setup>> setupsByType = setupFactory.createSetups(corsSite.getId(), siteLog);
        setupsByType.forEach((setupType, newSetups) -> {
            List<Setup> oldSetups = setups.findBySiteId(corsSite.getId(), setupType);
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

        eventPublisher.handled(siteLogReceived);
        eventPublisher.publish(new SiteUpdated(fourCharacterId));

        log.info("Saving site: " + fourCharacterId);
    }
}

