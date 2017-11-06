package au.gov.ga.geodesy.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import au.gov.ga.geodesy.domain.model.CorsSite;
import au.gov.ga.geodesy.domain.model.CorsSiteRepository;
import au.gov.ga.geodesy.domain.model.EquipmentInUse;
import au.gov.ga.geodesy.domain.model.Monument;
import au.gov.ga.geodesy.domain.model.Setup;
import au.gov.ga.geodesy.domain.model.SetupRepository;
import au.gov.ga.geodesy.domain.model.equipment.Equipment;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentConfiguration;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentConfigurationRepository;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentFactory;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentRepository;
import au.gov.ga.geodesy.domain.model.event.Event;
import au.gov.ga.geodesy.domain.model.event.EventPublisher;
import au.gov.ga.geodesy.domain.model.event.EventSubscriber;
import au.gov.ga.geodesy.domain.model.event.SiteLogReceived;
import au.gov.ga.geodesy.domain.model.event.SiteUpdated;
import au.gov.ga.geodesy.domain.model.sitelog.EffectiveDates;
import au.gov.ga.geodesy.domain.model.sitelog.EquipmentLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.SiteIdentification;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLogRepository;

@Component
@Transactional("geodesyTransactionManager")
public class CorsSiteService implements EventSubscriber<SiteLogReceived> {

    private static final Logger log = LoggerFactory.getLogger(CorsSiteService.class);
    private static final String gnssCorsSetupName = "GNSS CORS Setup";

    @Autowired
    private SiteLogRepository siteLogs;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentConfigurationRepository configurationRepository;

    @Autowired
    private CorsSiteRepository corsSites;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private SetupRepository setups;

    @Autowired
    private EquipmentFactory equipmentFactory;

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

        CorsSite corsSite = corsSites.findByFourCharacterId(fourCharacterId);
        if (corsSite == null) {
            corsSite = new CorsSite(fourCharacterId);
        }
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

        List<Setup> newSetups = getSetups(corsSite.getId(), siteLog);
        List<Setup> oldSetups = setups.findBySiteId(corsSite.getId());
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

        eventPublisher.handled(siteLogReceived);
        eventPublisher.publish(new SiteUpdated(fourCharacterId));

        log.info("Saving site: " + fourCharacterId);
    }

    private List<Setup> getSetups(Integer siteId, SiteLog siteLog) {
        @SuppressWarnings("unchecked")
        Comparator<Instant> fromC = ComparatorUtils.nullLowComparator(ComparatorUtils.NATURAL_COMPARATOR);
        SortedSet<Instant> datesOfChange = new TreeSet<>(fromC);

        List<EquipmentLogItem> equipmentLogItems = siteLog.getEquipmentLogItems()
            .stream()
            .filter(logItem -> logItem.getDateDeleted() == null)
            .collect(Collectors.toList());

        for (EquipmentLogItem logItem : equipmentLogItems) {
            EffectiveDates dates = logItem.getEffectiveDates();
            if (dates != null) {
                if (dates.getFrom() != null) {
                    datesOfChange.add(dates.getFrom());
                }
                if (dates.getTo() != null) {
                    datesOfChange.add(dates.getTo());
                }
            }
        }
        List<Setup> setups = new ArrayList<>();

        if (datesOfChange.size() > 0) {
            if (datesOfChange.size() == 1) {
                Instant d = datesOfChange.iterator().next();
                setups.add(new Setup(siteId, gnssCorsSetupName, new EffectiveDates(d, null)));
            } else {
                Iterator<Instant> i = datesOfChange.iterator();
                Iterator<Instant> j = datesOfChange.iterator();
                j.next();
                Setup s = null;
                do {
                    Instant from = i.next();
                    Instant to = j.next();
                    s = new Setup(siteId, gnssCorsSetupName, new EffectiveDates(from, to));
                    setups.add(s);
                }
                while (j.hasNext());
                EffectiveDates lastPeriod = s.getEffectivePeriod();
                if (lastPeriod.getTo() != null) {
                    s = new Setup(siteId, gnssCorsSetupName, new EffectiveDates(lastPeriod.getTo(), null));
                    setups.add(s);
                }
            }
        }
        for (EquipmentLogItem logItem : equipmentLogItems) {
            addEquipment(logItem, setups);
        }
        return setups;
    }

    private void addEquipment(EquipmentLogItem logItem, List<Setup> setups) {
        EffectiveDates period = logItem.getEffectiveDates();
        Instant equipmentFrom = null;
        Instant equipmentTo = null;

        int i = 0;
        int j = setups.size();

        if (period != null) {
            equipmentFrom = period.getFrom();
            equipmentTo = period.getTo();
            if (equipmentFrom != null && equipmentFrom.equals(equipmentTo)) {
                // equipment log entries with empty periods are corrections
                return;
            }
            if (equipmentFrom == null && equipmentTo == null) {
                setups.forEach(s -> addEquipment(logItem, s));
                return;
            }
            // TODO: why is this commented out?
            /* while (!setups.get(i).getEffectivePeriod().getFrom().equals(equipmentFrom)) { */
            /*     i++; */
            /* } */
            while (true) {
                EffectiveDates setupPeriod = setups.get(i).getEffectivePeriod();
                if (setupPeriod != null && setupPeriod.getFrom() != null
                        && setupPeriod.getFrom().equals(equipmentFrom)) {
                    break;
                }
                i++;
            }
            j = i;
            if (equipmentTo == null) {
                j = setups.size();
            } else {
                Instant setupTo = null;
                do {
                    setupTo = setups.get(j++).getEffectivePeriod().getTo();
                } while (setupTo != null && !setupTo.equals(equipmentTo));
            }
        }
        for (int k = i; k < j; k++) {
            addEquipment(logItem, setups.get(k));
        }
    }

    private void addEquipment(EquipmentLogItem logItem, Setup s) {
        Pair<? extends Equipment, ? extends EquipmentConfiguration> ec = equipmentFactory.create(logItem);
        Equipment equipment = ec.getLeft();
        EquipmentConfiguration config = ec.getRight();
        equipmentRepository.save(equipment);
        configurationRepository.saveAndFlush(config); // TODO: must flush to get the id, is there another way?
        EquipmentInUse inUse = new EquipmentInUse(equipment.getId(), config.getId(), logItem.getEffectiveDates());
        s.getEquipmentInUse().add(inUse);
    }
}

