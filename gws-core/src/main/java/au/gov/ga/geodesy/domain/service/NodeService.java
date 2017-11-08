package au.gov.ga.geodesy.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.ComparatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import au.gov.ga.geodesy.domain.model.CorsSite;
import au.gov.ga.geodesy.domain.model.CorsSiteRepository;
import au.gov.ga.geodesy.domain.model.EquipmentInUse;
import au.gov.ga.geodesy.domain.model.Node;
import au.gov.ga.geodesy.domain.model.NodeRepository;
import au.gov.ga.geodesy.domain.model.Setup;
import au.gov.ga.geodesy.domain.model.SetupRepository;
import au.gov.ga.geodesy.domain.model.SetupType;
import au.gov.ga.geodesy.domain.model.equipment.Clock;
import au.gov.ga.geodesy.domain.model.equipment.Equipment;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentRepository;
import au.gov.ga.geodesy.domain.model.equipment.GnssAntenna;
import au.gov.ga.geodesy.domain.model.equipment.GnssReceiver;
import au.gov.ga.geodesy.domain.model.event.Event;
import au.gov.ga.geodesy.domain.model.event.EventPublisher;
import au.gov.ga.geodesy.domain.model.event.EventRepository;
import au.gov.ga.geodesy.domain.model.event.EventSubscriber;
import au.gov.ga.geodesy.domain.model.event.SiteUpdated;
import au.gov.ga.geodesy.domain.model.sitelog.EffectiveDates;

@Component
@Transactional("geodesyTransactionManager")
public class NodeService implements EventSubscriber<SiteUpdated> {

    private static final Logger log = LoggerFactory.getLogger(NodeService.class);

    private final List<Class<? extends Equipment>> significantEquipment =
        new ArrayList<>(Arrays.asList(
            GnssReceiver.class,
            GnssAntenna.class,
            Clock.class));

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private CorsSiteRepository gnssCorsSites;

    @Autowired
    private NodeRepository nodes;

    @Autowired
    private EquipmentRepository equipment;

    @Autowired
    private SetupRepository setups;

    @PostConstruct
    private void subcribe() {
        eventPublisher.subscribe(this);
    }

    public boolean canHandle(Event e) {
        return e instanceof SiteUpdated;
    }

    public void handle(SiteUpdated siteUpdated) {
        log.info("Notified of " + siteUpdated);

        String fourCharId = siteUpdated.getFourCharacterId();
        CorsSite site = gnssCorsSites.findByFourCharacterId(fourCharId);
        EffectiveDates nodePeriod = null;

        setups.findInvalidatedBySiteId(site.getId(), SetupType.CorsSetup)
            .stream()
            .map(s -> nodes.findBySetupId(s.getId()))
            .filter(Objects::nonNull)
            .forEach(n -> {
                n.invalidate();
                nodes.save(n);
                log.info("Invalidated node " + n.getId());
            });

        for (final Setup setup : setups.findBySiteId(site.getId(), SetupType.CorsSetup)) {
            if (nodePeriod != null) {
                Instant nodeEffectiveTo = nodePeriod.getTo();
                if (nodeEffectiveTo == null) {
                    break;
                }
                if (nodeEffectiveTo.compareTo(setup.getEffectivePeriod().getFrom()) == 1) {
                    continue;
                }
            }
            if (isCompleteCorsSetup(setup)) {
                List<EffectiveDates> dates = new ArrayList<>();
                for (Class<? extends Equipment> equipmentType : significantEquipment) {
                    EquipmentInUse inUse = getOneEquipmentInUse(setup, equipmentType);
                    if (inUse != null) {
                        dates.add(inUse.getPeriod());
                    }
                }
                nodePeriod = lcd(dates);
                if (nodes.findBySetupId(setup.getId()) == null) {
                    nodes.save(new Node(site.getId(), nodePeriod, setup.getId()));
                }
            }
        }
        eventPublisher.handled(siteUpdated);
        log.info("Saving nodes: " + fourCharId);
    }

    private Boolean isCompleteCorsSetup(Setup setup) {
        return hasReceiver(setup) && hasAntenna(setup);
    }

    private Boolean hasReceiver(Setup setup) {
        return getOneEquipmentInUse(setup, GnssReceiver.class) != null;
    }

    private Boolean hasAntenna(Setup setup) {
        return getOneEquipmentInUse(setup, GnssAntenna.class) != null;
    }

    @SuppressWarnings("unchecked")
    private EffectiveDates lcd(List<EffectiveDates> es) {
        return new EffectiveDates(
            Collections.max(Lists.transform(es, new Function<EffectiveDates, Instant>() {
                public Instant apply(EffectiveDates dates) {
                    return dates == null ? null : dates.getFrom();
                }
            }), ComparatorUtils.nullLowComparator(ComparatorUtils.NATURAL_COMPARATOR))
            ,
            Collections.min(Lists.transform(es, new Function<EffectiveDates, Instant>() {
                public Instant apply(EffectiveDates dates) {
                    return dates == null ? null : dates.getTo();
                }
            }), ComparatorUtils.nullHighComparator(ComparatorUtils.NATURAL_COMPARATOR)));
    }

    private <T extends Equipment> EquipmentInUse getOneEquipmentInUse(Setup setup, Class<T> equipmentClass) {
        Collection<EquipmentInUse> inUse = getEquipmentInUse(setup, equipmentClass);
        if (inUse.isEmpty()) {
            return null;
        } else if (inUse.size() == 1) {
            return inUse.iterator().next();
        } else {
            System.out.println("Multiple " + equipmentClass.getSimpleName() + " in use!");
            return inUse.iterator().next();
        }
    }

    private <T extends Equipment> Collection<EquipmentInUse> getEquipmentInUse(Setup setup, final Class<T> equipmentClass) {
        return Collections2.filter(setup.getEquipmentInUse(), new Predicate<EquipmentInUse>() {
            public boolean apply(EquipmentInUse equipmentInUse) {
                Equipment e = equipment.findOne(equipmentInUse.getEquipmentId());
                return equipmentClass.isAssignableFrom(e.getClass());
            }
        });
    }
}

