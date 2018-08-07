package au.gov.ga.geodesy.support.mapper.orika.geodesyml;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.opengis.metadata.citation.ResponsibleParty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import au.gov.ga.geodesy.domain.model.sitelog.CollocationInformationLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.DifferentialFromMarker;
import au.gov.ga.geodesy.domain.model.sitelog.EffectiveDates;
import au.gov.ga.geodesy.domain.model.sitelog.FormInformation;
import au.gov.ga.geodesy.domain.model.sitelog.FrequencyStandardLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.GnssAntennaLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.GnssReceiverLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.HumiditySensorLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.LocalEpisodicEffectLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.LogItem;
import au.gov.ga.geodesy.domain.model.sitelog.MoreInformation;
import au.gov.ga.geodesy.domain.model.sitelog.MultipathSourceLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.OtherInstrumentationLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.PressureSensorLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.RadioInterferenceLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.SensorEquipmentLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.SignalObstructionLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.SiteIdentification;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLocation;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;
import au.gov.ga.geodesy.domain.model.sitelog.SurveyedLocalTieLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.TemperatureSensorLogItem;
import au.gov.ga.geodesy.domain.model.sitelog.WaterVaporSensorLogItem;
import au.gov.ga.geodesy.support.gml.LogItemPropertyType;
import au.gov.ga.geodesy.support.java.util.Iso;
import au.gov.xml.icsm.geodesyml.v_0_4.BaseSensorEquipmentType;
import au.gov.xml.icsm.geodesyml.v_0_4.CollocationInformationType;
import au.gov.xml.icsm.geodesyml.v_0_4.FormInformationType;
import au.gov.xml.icsm.geodesyml.v_0_4.FrequencyStandardType;
import au.gov.xml.icsm.geodesyml.v_0_4.GnssAntennaPropertyType;
import au.gov.xml.icsm.geodesyml.v_0_4.GnssAntennaType;
import au.gov.xml.icsm.geodesyml.v_0_4.GnssReceiverType;
import au.gov.xml.icsm.geodesyml.v_0_4.HumiditySensorType;
import au.gov.xml.icsm.geodesyml.v_0_4.IgsAntennaModelCodeType;
import au.gov.xml.icsm.geodesyml.v_0_4.IgsRadomeModelCodeType;
import au.gov.xml.icsm.geodesyml.v_0_4.IgsReceiverModelCodeType;
import au.gov.xml.icsm.geodesyml.v_0_4.LocalEpisodicEffectPropertyType;
import au.gov.xml.icsm.geodesyml.v_0_4.LocalEpisodicEffectType;
import au.gov.xml.icsm.geodesyml.v_0_4.MoreInformationType;
import au.gov.xml.icsm.geodesyml.v_0_4.MultipathSourcePropertyType;
import au.gov.xml.icsm.geodesyml.v_0_4.MultipathSourceType;
import au.gov.xml.icsm.geodesyml.v_0_4.OtherInstrumentationType;
import au.gov.xml.icsm.geodesyml.v_0_4.PressureSensorType;
import au.gov.xml.icsm.geodesyml.v_0_4.RadioInterferencePropertyType;
import au.gov.xml.icsm.geodesyml.v_0_4.RadioInterferenceType;
import au.gov.xml.icsm.geodesyml.v_0_4.SignalObstructionPropertyType;
import au.gov.xml.icsm.geodesyml.v_0_4.SignalObstructionType;
import au.gov.xml.icsm.geodesyml.v_0_4.SiteIdentificationType;
import au.gov.xml.icsm.geodesyml.v_0_4.SiteLocationType;
import au.gov.xml.icsm.geodesyml.v_0_4.SiteLogType;
import au.gov.xml.icsm.geodesyml.v_0_4.SurveyedLocalTieType;
import au.gov.xml.icsm.geodesyml.v_0_4.TemperatureSensorType;
import au.gov.xml.icsm.geodesyml.v_0_4.WaterVaporSensorType;

import ma.glasnost.orika.CustomMapper;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.converter.BidirectionalConverter;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.converter.builtin.PassThroughConverter;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.Type;
import ma.glasnost.orika.metadata.TypeFactory;

import net.opengis.gml.v_3_2_1.AbstractGMLType;
import net.opengis.gml.v_3_2_1.CodeType;
import net.opengis.gml.v_3_2_1.TimeIndeterminateValueType;
import net.opengis.gml.v_3_2_1.TimePeriodType;
import net.opengis.gml.v_3_2_1.TimePositionType;
import net.opengis.iso19139.gmd.v_20070417.CIResponsiblePartyType;

/**
 * Reversible mapping between GeodesyML SiteLogType DTO and
 * SiteLog site log entity.
 */
@Component
public class GenericMapper {

    private MapperFacade mapper;

    @Autowired
    private FormInformationMapper formInformationMapper;

    @Autowired
    private SiteIdentificationMapper siteIdentificationMapper;

    @Autowired
    private ResponsiblePartiesMapper responsiblePartiesMapper;

    @Autowired
    private GnssReceiverMapper gnssReceiverMapper;

    @Autowired
    private GnssAntennaMapper gnssAntennaMapper;

    @Autowired
    private FrequencyStandardMapper frequencyStandardMapper;

    @Autowired
    private SurveyedLocalTieMapper surveyedLocalTieMapper;

    @Autowired
    private LocalEpisodicEffectMapper localEpisodicEffectMapper;

    @Autowired
    private HumiditySensorMapper humiditySensorMapper;

    @Autowired
    private PressureSensorMapper pressureSensorMapper;

    @Autowired
    private TemperatureSensorMapper temperatureSensorMapper;

    @Autowired
    private WaterVaporSensorMapper waterVaporSensorMapper;

    @Autowired
    private RadioInterferenceMapper radioInterferenceMapper;

    @Autowired
    private OtherInstrumentationMapper otherInstrumentationMapper;

    @Autowired
    private SignalObstructionMapper signalObstructionMapper;

    @Autowired
    private MultipathSourceMapper multipathSourceMapper;

    @Autowired
    private CollocationInformationMapper collocationInformationMapper;

    @Autowired
    private MoreInformationMapper moreInformationMapper;


    @PostConstruct
    public void init() {
        MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

        ConverterFactory converters = mapperFactory.getConverterFactory();
        mapperFactory.getConverterFactory().registerConverter(new PassThroughConverter(Instant.class));
        converters.registerConverter(new InstantToTimePositionConverter());
        converters.registerConverter("validTimeConverter", new JAXBElementConverter<TimePeriodType, EffectiveDates>() {});

        mapperFactory.classMap(TimePeriodType.class, EffectiveDates.class)
            .fieldMap("beginPosition", "from").mapNulls(false).add()
            .fieldMap("endPosition", "to").mapNulls(false).add()
            .customize(new CustomMapper<TimePeriodType, EffectiveDates>() {

                @Override
                public void mapBtoA(EffectiveDates dates, TimePeriodType period, MappingContext ctx) {
                    if (dates.getFrom() == null) {
                        TimePositionType begin = new TimePositionType();
                        begin.setIndeterminatePosition(TimeIndeterminateValueType.UNKNOWN);
                        period.setBeginPosition(begin);
                    }
                    if (dates.getTo() == null) {
                        TimePositionType end = new TimePositionType();
                        end.setIndeterminatePosition(TimeIndeterminateValueType.UNKNOWN);
                        period.setEndPosition(end);
                    }
                }

            })
            .register();

        // Form Information
        mapperFactory.classMap(FormInformationType.class, FormInformation.class)
                .byDefault()
                .register();

       // Site Identification
       mapperFactory.classMap(SiteIdentificationType.class, SiteIdentification.class)
            .field("fourCharacterID", "fourCharacterId")
            .fieldMap("monumentDescription", "monumentDescription").converter("monumentDescription").add()
            .field("heightOfTheMonument", "heightOfMonument")
            .fieldMap("geologicCharacteristic", "geologicCharacteristic").converter("geologicCharacteristic").add()
            .fieldMap("faultZonesNearby", "faultZonesNearby").converter("faultZonesNearby").add()
            .byDefault()
            .register();

        converters.registerConverter("monumentDescription", new StringToCodeTypeConverter("eGeodesy/monumentDescription") {});
        converters.registerConverter("geologicCharacteristic", new StringToCodeTypeConverter("eGeodesy/geologicCharacteristic") {});
        converters.registerConverter("faultZonesNearby", new StringToCodeTypeConverter("eGeodesy/faultZonesNearby") {});

        // Collocation Information
        mapperFactory.classMap(CollocationInformationType.class, CollocationInformationLogItem.class)
                .fieldMap("instrumentationType", "instrumentType").converter("instrumentTypeConverter").add()
                .fieldMap("status", "status").converter("statusTypeConverter").add()
                .fieldMap("validTime.abstractTimePrimitive", "effectiveDates").converter("validTimeConverter").add()
                .byDefault()
                .register();


        converters.registerConverter("instrumentTypeConverter", new StringToCodeTypeConverter("eGeodesy/instrumentType") {});
        converters.registerConverter("statusTypeConverter", new StringToCodeTypeConverter("eGeodesy/status") {});

        // GNSS Receiver
        mapperFactory.classMap(GnssReceiverLogItem.class, GnssReceiverType.class)
            .fieldMap("type", "igsModelCode").converter("receiverTypeConverter").add()
            .field("serialNumber", "manufacturerSerialNumber")
            .fieldMap("satelliteSystem", "satelliteSystem").converter("satelliteSystemConverter").add()
            .fieldMap("dateRemoved", "dateRemoved").mapNulls(false).add()
            .byDefault()
            .customize(new CustomMapper<GnssReceiverLogItem, GnssReceiverType>() {

                @Override
                public void mapAtoB(GnssReceiverLogItem receiverLogItem, GnssReceiverType receiverType, MappingContext ctx) {
                    if (receiverLogItem.getDateInstalled() == null) {
                        TimePositionType dateInstalled = new TimePositionType();
                        dateInstalled.setIndeterminatePosition(TimeIndeterminateValueType.UNKNOWN);
                        receiverType.setDateInstalled(dateInstalled);
                    }
                    if (receiverLogItem.getDateRemoved() == null) {
                        TimePositionType dateRemoved = new TimePositionType();
                        dateRemoved.setIndeterminatePosition(TimeIndeterminateValueType.UNKNOWN);
                        receiverType.setDateRemoved(dateRemoved);
                    }
                }
            })
            .register();

        converters.registerConverter("receiverTypeConverter", new StringToCodeListValueConverter<IgsReceiverModelCodeType>(
            "https://igscb.jpl.nasa.gov/igscb/station/general/rcvr_ant.tab",
            "http://xml.gov.au/icsm/geodesyml/codelists/antenna-receiver-codelists.xml#GeodesyML_GNSSReceiverTypeCode"
        ));
        converters.registerConverter("satelliteSystemConverter",
                new StringToListConverter<CodeType>(
                    new StringToCodeTypeConverter("eGeodesy/satelliteSystem"), TypeFactory.valueOf(CodeType.class)
                )
            );

        // GNSS Antenna
        mapperFactory.classMap(GnssAntennaLogItem.class, GnssAntennaType.class)
            .fieldMap("type", "igsModelCode").converter("antennaTypeConverter").add()
            .field("serialNumber", "manufacturerSerialNumber")

            .fieldMap("antennaRadomeType", "antennaRadomeType").converter("codeWithAuthorityTypeConverter").add()
            .fieldMap("antennaReferencePoint", "antennaReferencePoint").converter("antennaReferencePointConverter").add()
            .byDefault()
            .customize(new CustomMapper<GnssAntennaLogItem, GnssAntennaType>() {

                @Override
                public void mapAtoB(GnssAntennaLogItem antennaLogItem, GnssAntennaType antennaType, MappingContext ctx) {
                    if (antennaLogItem.getDateInstalled() == null) {
                        TimePositionType dateInstalled = new TimePositionType();
                        dateInstalled.setIndeterminatePosition(TimeIndeterminateValueType.UNKNOWN);
                        antennaType.setDateInstalled(dateInstalled);
                    }
                    if (antennaLogItem.getDateRemoved() == null) {
                        TimePositionType dateRemoved = new TimePositionType();
                        dateRemoved.setIndeterminatePosition(TimeIndeterminateValueType.UNKNOWN);
                        antennaType.setDateRemoved(dateRemoved);
                    }
                    antennaType.setMarkerArpUpEcc(this.roundArpEcc(antennaType.getMarkerArpUpEcc()));
                    antennaType.setMarkerArpNorthEcc(this.roundArpEcc(antennaType.getMarkerArpNorthEcc()));
                    antennaType.setMarkerArpEastEcc(this.roundArpEcc(antennaType.getMarkerArpEastEcc()));
                }

                @Override
                public void mapBtoA(GnssAntennaType antennaType, GnssAntennaLogItem antennaLogItem, MappingContext ctx) {
                    antennaLogItem.setMarkerArpUpEcc(this.roundArpEcc(antennaLogItem.getMarkerArpUpEcc()));
                    antennaLogItem.setMarkerArpNorthEcc(this.roundArpEcc(antennaLogItem.getMarkerArpNorthEcc()));
                    antennaLogItem.setMarkerArpEastEcc(this.roundArpEcc(antennaLogItem.getMarkerArpEastEcc()));
                }

                private Double roundArpEcc(Double x) {
                    return this.round(x, 4);
                }

                private Double round(Double x, int scale) {
                    return BigDecimal.valueOf(x).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
            })
            .register();

        converters.registerConverter("antennaTypeConverter", new StringToCodeListValueConverter<IgsAntennaModelCodeType>(
                "https://igscb.jpl.nasa.gov/igscb/station/general/rcvr_ant.tab",
                "http://xml.gov.au/icsm/geodesyml/codelists/antenna-receiver-codelists.xml#GeodesyML_GNSSAntennaTypeCode"
        ));
        converters.registerConverter("antennaReferencePointConverter", new StringToCodeTypeConverter("eGeodesy/antennaReferencePoint") {});
        converters.registerConverter("codeWithAuthorityTypeConverter", new StringToCodeWithAuthorityTypeConverter<IgsRadomeModelCodeType>("eGeodesy/antennaRadomeType"));

        // Frequency Standard
        mapperFactory.classMap(FrequencyStandardType.class, FrequencyStandardLogItem.class)
            .fieldMap("standardType", "type").converter("frequencyStandardTypeConverter").add()
            .fieldMap("validTime.abstractTimePrimitive", "effectiveDates").converter("validTimeConverter").add()
            .byDefault()
            .register();

        converters.registerConverter("frequencyStandardTypeConverter", new StringToCodeTypeConverter("eGeodesy/frequencyStandardType") {});

        mapperFactory.classMap(SiteLogType.class, SiteLog.class)
            .fieldMap("siteIdentification", "siteIdentification").converter("siteIdentification").add()
            .fieldMap("siteLocation", "siteLocation").converter("siteLocation").add()
            .fieldMap("gnssReceivers", "gnssReceivers").converter("gnssReceivers").add()
            .fieldMap("gnssAntennas", "gnssAntennas").converter("gnssAntennas").add()
            .fieldMap("frequencyStandards", "frequencyStandards").converter("frequencyStandards").add()
            .fieldMap("humiditySensors", "humiditySensors").converter("humiditySensors").add()
            .fieldMap("pressureSensors", "pressureSensors").converter("pressureSensors").add()
            .fieldMap("temperatureSensors", "temperatureSensors").converter("temperatureSensors").add()
            .fieldMap("waterVaporSensors", "waterVaporSensors").converter("waterVaporSensors").add()
            .fieldMap("otherInstrumentation", "otherInstrumentationLogItem").converter("otherInstrumentation").add()
            .fieldMap("signalObstructions", "signalObstructionLogItems").converter("signalObstructions").add()
            .fieldMap("multipathSources", "multipathSourceLogItems").converter("multipathSources").add()
            .fieldMap("localEpisodicEffects", "localEpisodicEffectLogItems").converter("localEpisodicEffects").add()
            .fieldMap("radioInterferences", "radioInterferences").converter("radioInterferences").add()
            .fieldMap("moreInformation", "moreInformation").converter("moreInformation").add()
            .fieldMap("formInformation", "formInformation").converter("formInformation").add()
            .fieldMap("collocationInformation", "collocationInformation").converter("collocationInformation").add()
            .fieldMap("surveyedLocalTies", "surveyedLocalTies").converter("surveyedLocalTies").add()
            .customize(responsiblePartiesMapper)
            .register();

        // Surveyed Local Tie
        mapperFactory.classMap(SurveyedLocalTieType.class, SurveyedLocalTieLogItem.class)
                .field("tiedMarkerCDPNumber", "tiedMarkerCdpNumber")
                .field("tiedMarkerDOMESNumber", "tiedMarkerDomesNumber")
                .field("differentialComponentsGNSSMarkerToTiedMonumentITRS", "differentialFromMarker")
                .field("localSiteTiesAccuracy", "localSiteTieAccuracy")
                .field("dateMeasured", "dateMeasured")
                .byDefault()
                .customize(new CustomMapper<SurveyedLocalTieType, SurveyedLocalTieLogItem>() {

                    @Override
                    public void mapBtoA(SurveyedLocalTieLogItem logItem, SurveyedLocalTieType logItemDto, MappingContext ctx) {
                        if (logItem.getDifferentialFromMarker() != null) {
                            DifferentialFromMarker diff = logItem.getDifferentialFromMarker();
                            if (diff.getDx() == null && diff.getDy() == null && diff.getDz() == null) {
                                logItemDto.setDifferentialComponentsGNSSMarkerToTiedMonumentITRS(null);
                            }
                        }
                        if (logItem.getDateMeasured() == null) {
                            logItemDto.setDateMeasured(new TimePositionType()
                                    .withIndeterminatePosition(TimeIndeterminateValueType.UNKNOWN));
                        }
                    }
                })
                .register();

        mapperFactory.classMap(SurveyedLocalTieType.DifferentialComponentsGNSSMarkerToTiedMonumentITRS.class, DifferentialFromMarker.class)
                .field("dx", "dx")
                .field("dy", "dy")
                .field("dz", "dz")
                .register();

        // Local Episodic Effect
		mapperFactory.classMap(LocalEpisodicEffectType.class, LocalEpisodicEffectLogItem.class)
				.fieldMap("validTime.abstractTimePrimitive", "effectiveDates").converter("validTimeConverter").add()
                .byDefault()
                .register();

        // Met Sensors
        CustomMapper<BaseSensorEquipmentType, SensorEquipmentLogItem> indeterminateCalibrationDateMapper = new CustomMapper<BaseSensorEquipmentType, SensorEquipmentLogItem>() {

            @Override
            public void mapBtoA(SensorEquipmentLogItem sensorLogItem, BaseSensorEquipmentType sensorLogItemDto, MappingContext ctx) { 
                if (sensorLogItem.getCalibrationDate() == null) {
                    sensorLogItemDto.setCalibrationDate(new TimePositionType()
                        .withIndeterminatePosition(TimeIndeterminateValueType.UNKNOWN)
                    );
                }
            }
        };

        mapperFactory.classMap(BaseSensorEquipmentType.class, SensorEquipmentLogItem.class)
                .fieldMap("validTime.abstractTimePrimitive", "effectiveDates").converter("validTimeConverter").add()
                .byDefault()
                .exclude("type")
                .customize(indeterminateCalibrationDateMapper)
                .register();


        // Humidity Sensor
        mapperFactory.classMap(HumiditySensorType.class, HumiditySensorLogItem.class)
                .use(BaseSensorEquipmentType.class, SensorEquipmentLogItem.class)
                .fieldMap("type", "type").converter("humiditySensorTypeConverter").add()
                .byDefault()
                .register();

        converters.registerConverter("humiditySensorTypeConverter", new StringToCodeTypeConverter("eGeodesy/type") {});

        // Pressure Sensor
        mapperFactory.classMap(PressureSensorType.class, PressureSensorLogItem.class)
                .use(BaseSensorEquipmentType.class, SensorEquipmentLogItem.class)
                .fieldMap("type", "type").converter("pressureSensorTypeConverter").add()
                .byDefault()
                .register();

        converters.registerConverter("pressureSensorTypeConverter", new StringToCodeTypeConverter("eGeodesy/type") {});

        // Temperature Sensor
        mapperFactory.classMap(TemperatureSensorType.class, TemperatureSensorLogItem.class)
                .use(BaseSensorEquipmentType.class, SensorEquipmentLogItem.class)
                .fieldMap("type", "type").converter("temperatureSensorTypeConverter").add()
                .byDefault()
                .register();

        converters.registerConverter("temperatureSensorTypeConverter", new StringToCodeTypeConverter("eGeodesy/type") {});

        // Water Vapor Sensor
        mapperFactory.classMap(WaterVaporSensorType.class, WaterVaporSensorLogItem.class)
                .use(BaseSensorEquipmentType.class, SensorEquipmentLogItem.class)
                .fieldMap("type", "type").converter("waterVaporSensorTypeConverter").add()
                .byDefault()
                .register();

        converters.registerConverter("waterVaporSensorTypeConverter", new StringToCodeTypeConverter("eGeodesy/type") {});

        // Other Instrumentation
        mapperFactory.classMap(OtherInstrumentationType.class, OtherInstrumentationLogItem.class)
                .fieldMap("validTime.abstractTimePrimitive", "effectiveDates").converter("validTimeConverter").add()
                .byDefault()
                .register();

        // Radio Interference
        mapperFactory.classMap(RadioInterferenceType.class, RadioInterferenceLogItem.class)
                .fieldMap("validTime.abstractTimePrimitive", "effectiveDates").converter("validTimeConverter").add()
                .byDefault()
                .register();

        // Signal Obstruction
        mapperFactory.classMap(SignalObstructionType.class, SignalObstructionLogItem.class)
                .fieldMap("validTime.abstractTimePrimitive", "effectiveDates").converter("validTimeConverter").add()
                .byDefault()
                .register();

        // Multipath Source
        mapperFactory.classMap(MultipathSourceType.class, MultipathSourceLogItem.class)
                .fieldMap("validTime.abstractTimePrimitive", "effectiveDates").converter("validTimeConverter").add()
                .byDefault()
                .register();
        
        // More Information
        mapperFactory.classMap(MoreInformationType.class, MoreInformation.class)
                .fieldBToA("primaryDataCenter", "dataCenter[0]")
                .fieldBToA("secondaryDataCenter", "dataCenter[1]")
                .customize(new CustomMapper<MoreInformationType, MoreInformation>() {
                       @Override
                       public void mapAtoB(MoreInformationType infoType, MoreInformation info, MappingContext ctx) {
                           if (infoType.getDataCenter().size() >= 1) {
                               info.setPrimaryDataCenter(infoType.getDataCenter().get(0));
                           }
                           if (infoType.getDataCenter().size() >= 2) {
                               info.setSecondaryDataCenter(infoType.getDataCenter().get(1));
                           }
                       }
                })
                .fieldMap("DOI", "doi").converter("doiCodeTypeConverter").add()
                .byDefault()
                .register();

        converters.registerConverter("doiCodeTypeConverter", new StringToCodeTypeConverter("eGeodesy/doi") {});

        // ********
        converters.registerConverter("siteIdentification",
                new IsoConverter<SiteIdentificationType, SiteIdentification>(siteIdentificationMapper) {});

        converters.registerConverter("siteLocation",
                new IsoConverter<SiteLocationType, SiteLocation>(new SiteLocationMapper()) {});

        converters.registerConverter("gnssReceivers",
                new BidirectionalConverterWrapper<List<LogItemPropertyType>, Set<GnssReceiverLogItem>>(
                        logItemsConverter(gnssReceiverMapper)
                ) {}
        );

        converters.registerConverter("frequencyStandards",
                new BidirectionalConverterWrapper<List<LogItemPropertyType>, Set<FrequencyStandardLogItem>>(
                        logItemsConverter(frequencyStandardMapper)
                ) {}
        );

        converters.registerConverter("humiditySensors",
                new BidirectionalConverterWrapper<List<LogItemPropertyType>, Set<HumiditySensorLogItem>>(
                        logItemsConverter(humiditySensorMapper)
                ) {}
        );

        converters.registerConverter("pressureSensors",
                new BidirectionalConverterWrapper<List<LogItemPropertyType>, Set<PressureSensorLogItem>>(
                        logItemsConverter(pressureSensorMapper)
                ) {}
        );

        converters.registerConverter("temperatureSensors",
                new BidirectionalConverterWrapper<List<LogItemPropertyType>, Set<TemperatureSensorLogItem>>(
                        logItemsConverter(temperatureSensorMapper)
                ) {}
        );

        converters.registerConverter("waterVaporSensors",
                new BidirectionalConverterWrapper<List<LogItemPropertyType>, Set<WaterVaporSensorLogItem>>(
                        logItemsConverter(waterVaporSensorMapper)
                ) {}
        );

        converters.registerConverter("otherInstrumentation",
                new BidirectionalConverterWrapper<List<LogItemPropertyType>, Set<OtherInstrumentationLogItem>>(
                        logItemsConverter(otherInstrumentationMapper)
                ) {}
        );

        converters.registerConverter("signalObstructions",
                new BidirectionalConverterWrapper<List<SignalObstructionPropertyType>, Set<SignalObstructionLogItem>>(
                        logItemsConverter(signalObstructionMapper)
                ) {}
        );

        converters.registerConverter("multipathSources",
                new BidirectionalConverterWrapper<List<MultipathSourcePropertyType>, Set<MultipathSourceLogItem>>(
                        logItemsConverter(multipathSourceMapper)
                ) {}
        );


        converters.registerConverter("localEpisodicEffects",
                new BidirectionalConverterWrapper<List<LocalEpisodicEffectPropertyType>, Set<LocalEpisodicEffectLogItem>>(
                        logItemsConverter(localEpisodicEffectMapper)
                ) {}
        );

        converters.registerConverter("radioInterferences",
                new BidirectionalConverterWrapper<List<RadioInterferencePropertyType>, Set<RadioInterferenceLogItem>>(
                        logItemsConverter(radioInterferenceMapper)
                ) {}
        );
        converters.registerConverter("gnssAntennas",
                new BidirectionalConverterWrapper<List<GnssAntennaPropertyType>, Set<GnssAntennaLogItem>>(
                        logItemsConverter(gnssAntennaMapper)
                ) {}
        );

        converters.registerConverter("moreInformation",
                new IsoConverter<MoreInformationType, MoreInformation>(moreInformationMapper) {});

        converters.registerConverter("formInformation",
                new IsoConverter<FormInformationType, FormInformation>(formInformationMapper) {});

        converters.registerConverter("collocationInformation",
                new BidirectionalConverterWrapper<List<LogItemPropertyType>, Set<CollocationInformationLogItem>>(
                        logItemsConverter(collocationInformationMapper)
                ) {}
        );

        converters.registerConverter("surveyedLocalTies",
                new BidirectionalConverterWrapper<List<LogItemPropertyType>, Set<SurveyedLocalTieLogItem>>(
                        logItemsConverter(surveyedLocalTieMapper)
                ) {}
        );

        converters.registerConverter("responsibleParty", new IsoConverter<CIResponsiblePartyType, ResponsibleParty>(new ResponsiblePartyMapper()) {});

        mapper = mapperFactory.getMapperFacade();
    }


    // TODO: refactor and document (work-around for https://github.com/orika-mapper/orika/issues/99)
    public class BidirectionalConverterWrapper<A, B> extends BidirectionalConverter<A, B> {

        private BidirectionalConverter<A, B> delegate;

        public BidirectionalConverterWrapper(BidirectionalConverter<A, B> delegate) {
            this.delegate = delegate;
        }

        @Override
        public B convertTo(A a, Type<B> type, MappingContext mappingContext) {
            return delegate.convertTo(a, type, mappingContext);
        }

        @Override
        public A convertFrom(B b, Type<A> type, MappingContext mappingContext) {
            return delegate.convertFrom(b, type, mappingContext);
        }
    }
     
    /**
     * Given a LogItemPropertyType isomorphism (from DTO to domain model), return a
     * bidirectional converter from a list of GML property types to a
     * set of domain model log items.
     */
    private <P extends LogItemPropertyType, T extends AbstractGMLType, L extends LogItem<?>>

    BidirectionalConverter<List<P>, Set<L>> logItemsConverter(Iso<T, L> logItemsIso) {

        return new IsoConverter<>(new ListToSet<>(new LogItemPropertyTypeMapper<>(logItemsIso)));
    }

    /**
     * Given an isomorphism from A to B, return an isomorphism from list of A
     * to set B.
     */
    private class ListToSet<A, B extends LogItem<?>> implements Iso<List<A>, Set<B>> {

        private Iso<A, B> elementIso;

        public ListToSet(Iso<A, B> elementIso) {
            this.elementIso = elementIso;
        }

        public Set<B> to(List<A> list) {
            return list.stream().map(elementIso::to).collect(Collectors.toSet());
        }

        public List<A> from(Set<B> set) {
            return set.stream().sorted().map(elementIso::from).collect(Collectors.toList());
        }
    }

    public <S, D> D map(S s, Class<D> clazz) {
        return mapper.map(s, clazz);
    }
}
