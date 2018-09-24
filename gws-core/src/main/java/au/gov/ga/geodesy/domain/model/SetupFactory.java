package au.gov.ga.geodesy.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import au.gov.ga.geodesy.domain.model.equipment.Equipment;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentConfiguration;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentConfigurationRepository;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentFactory;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentRepository;
import au.gov.ga.geodesy.domain.model.sitelog.EffectiveDates;
import au.gov.ga.geodesy.domain.model.sitelog.EquipmentLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;

/**
 * Setups factory
 */
@Component
public class SetupFactory {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentConfigurationRepository configurationRepository;

    @Autowired
    private EquipmentFactory equipmentFactory;

    /**
     * Extract setups from site log. Setup types are defined in
     * {@code SetupType}.
     *
     * @param siteId site to associate with the new setups
     * @param siteLog site log from which to extract setups
     * @return list of setups hashed by setup type
     *
     * @see SetupType
     */
    public HashMap<SetupType, List<Setup>> createSetups(Integer siteId, SiteLog siteLog) {
        HashMap<SetupType, List<Setup>> setups = new HashMap<>();
        for (SetupType setupType : SetupType.values()) {
            setups.put(setupType, this.createSetups(siteId, siteLog, setupType));
        }
        return setups;
    }

    private List<Setup> createSetups(Integer siteId, SiteLog siteLog, SetupType setupType) {
        @SuppressWarnings("unchecked")
        Comparator<Instant> fromC = ComparatorUtils.nullLowComparator(ComparatorUtils.NATURAL_COMPARATOR);
        SortedSet<Instant> datesOfChange = new TreeSet<>(fromC);

        List<EquipmentLogItem<?>> equipmentLogItems = (List<EquipmentLogItem<?>>) siteLog.getEquipmentLogItems()
            .stream()
            .filter(logItem -> {
                for (Class<?> equipmentType : setupType.equipmentTypes) {
                    if (equipmentType.isAssignableFrom(logItem.getClass())) {
                        return true;
                    }
                }
                return false;
            })
            .filter(logItem -> logItem.getDateDeleted() == null)
            .collect(Collectors.toList());

        for (EquipmentLogItem<?> logItem : equipmentLogItems) {
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
                setups.add(new Setup(siteId, setupType, new EffectiveDates(d, null)));
            } else {
                Iterator<Instant> i = datesOfChange.iterator();
                Iterator<Instant> j = datesOfChange.iterator();
                j.next();
                Setup s = null;
                do {
                    Instant from = i.next();
                    Instant to = j.next();
                    s = new Setup(siteId, setupType, new EffectiveDates(from, to));
                    setups.add(s);
                }
                while (j.hasNext());
                EffectiveDates lastPeriod = s.getEffectivePeriod();
                if (lastPeriod.getTo() != null) {
                    s = new Setup(siteId, setupType, new EffectiveDates(lastPeriod.getTo(), null));
                    setups.add(s);
                }
            }
        }
        for (EquipmentLogItem<?> logItem : equipmentLogItems) {
            addEquipment(logItem, setups);
        }
        return setups;
    }

    private void addEquipment(EquipmentLogItem<?> logItem, List<Setup> setups) {
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

    private void addEquipment(EquipmentLogItem<?> logItem, Setup s) {
        Pair<? extends Equipment, ? extends EquipmentConfiguration> ec = equipmentFactory.create(logItem);
        Equipment equipment = ec.getLeft();
        EquipmentConfiguration config = ec.getRight();
        equipmentRepository.save(equipment);
        configurationRepository.saveAndFlush(config); // TODO: must flush to get the id, is there another way?
        EquipmentInUse inUse = new EquipmentInUse(equipment.getId(), config.getId(), logItem.getEffectiveDates());
        s.getEquipmentInUse().add(inUse);
    }
}

