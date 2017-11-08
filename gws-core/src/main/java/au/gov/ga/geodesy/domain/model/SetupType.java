package au.gov.ga.geodesy.domain.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import au.gov.ga.geodesy.domain.model.sitelog.EquipmentLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.FrequencyStandardLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.GnssAntennaLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.GnssReceiverLogItem;

/**
 * List of equipment setup types.
 */
public enum SetupType {

    CorsSetup(
        GnssAntennaLogItem.class,
        GnssReceiverLogItem.class,
        FrequencyStandardLogItem.class
    ),

    GnssAntennaSetup(GnssAntennaLogItem.class);

    public final List<Class<? extends EquipmentLogItem<?>>> equipmentTypes;

    @SafeVarargs
    private SetupType(Class<? extends EquipmentLogItem<?>>... equipmentTypes) {
        this.equipmentTypes = Collections.unmodifiableList(Arrays.asList(equipmentTypes));
    }
}
