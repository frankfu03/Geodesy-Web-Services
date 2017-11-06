package au.gov.ga.geodesy.domain.model.sitelog;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.Valid;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import au.gov.ga.geodesy.domain.model.ContactType;
import au.gov.ga.geodesy.domain.model.ContactTypeRepository;
import au.gov.ga.geodesy.domain.model.SiteResponsibleParty;

/**
 * http://sopac.ucsd.edu/ns/geodesy/doc/igsSiteLog/2004/igsSiteLog.xsd:igsSiteLogType
 */
@Configurable
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "SITELOG_SITE")
public class SiteLog {

    @Autowired
    @Transient
    private ContactTypeRepository contactTypes;

    @Id
    @GeneratedValue(generator = "surrogateKeyGenerator")
    @SequenceGenerator(name = "surrogateKeyGenerator", sequenceName = "SEQ_SITELOGSITE")
    private @MonotonicNonNull Integer id;

    // TODO: does every table need entryDate?
    private @MonotonicNonNull Instant entryDate;

    @LastModifiedDate
    @Column(name = "LAST_DATE_MODIFIED")
    private @MonotonicNonNull ZonedDateTime lastModifiedDate;

    @Column(name = "SITE_LOG_TEXT", length = 500000 /* ~500KB */, nullable = false)
    private @MonotonicNonNull String siteLogText;

    @Valid
    @Embedded
    protected FormInformation formInformation = new FormInformation();

    @Valid
    @Embedded
    protected SiteIdentification siteIdentification = new SiteIdentification();

    @Valid
    @Embedded
    protected SiteLocation siteLocation = new SiteLocation();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_GNSS_RECEIVER"))
    protected Set<GnssReceiverLogItem> gnssReceivers = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_GNSS_ANTENNA"))
    protected Set<GnssAntennaLogItem> gnssAntennas = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_SURVEYEDLOCALTIE"))
    protected Set<SurveyedLocalTieLogItem> surveyedLocalTies = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOGFREQUENCYSTANDARD"))
    protected Set<FrequencyStandardLogItem> frequencyStandards = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_COLLOCATIONINFORMATION"))
    protected Set<CollocationInformationLogItem> collocationInformation = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_HUMIDITYSENSOR"))
    protected Set<HumiditySensorLogItem> humiditySensors = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_PRESSURESENSOR"))
    protected Set<PressureSensorLogItem> pressureSensors = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_TEMPERATURESENSOR"))
    protected Set<TemperatureSensorLogItem> temperatureSensors = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_WATERVAPORSENSOR"))
    protected Set<WaterVaporSensorLogItem> waterVaporSensors = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_OTHERINSTRUMENTATION"))
    protected Set<OtherInstrumentationLogItem> otherInstrumentationLogItem = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_RADIOINTERFERENCE"))
    protected Set<RadioInterferenceLogItem> radioInterferences = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_MUTLIPATHSOURCE"))
    protected Set<MultipathSourceLogItem> multipathSourceLogItems = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_SIGNALOBSTRACTION"))
    protected Set<SignalObstructionLogItem> signalObstructionLogItems = new HashSet<>();

    @Valid
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID", foreignKey = @ForeignKey(name="FK_SITELOG_SITE_SITELOG_LOCALEPISODICEFFECT"))
    protected Set<LocalEpisodicEffectLogItem> localEpisodicEffectLogItems = new HashSet<>();

    @Valid
    @Embedded
    protected MoreInformation moreInformation = new MoreInformation();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "SITE_ID", referencedColumnName = "ID")
    @OrderColumn(name = "INDEX")
    protected @Nullable List<SiteResponsibleParty> responsibleParties = new ArrayList<>();

    public @Nullable Integer getId() {
        return id;
    }

    @SuppressWarnings("unused")
    private void setId(Integer id) {
        this.id = id;
    }

    public @Nullable Instant getEntryDate() {
        return entryDate;
    }

    public @Nullable ZonedDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Return site log text.
     */
    public @Nullable String getSiteLogText() {
        return siteLogText;
    }

    /**
     * Set site log text.
     */
    public void setSiteLogText(String siteLogText) {
        this.siteLogText = siteLogText;
    }

    /**
     * Return form information.
     */
    public FormInformation getFormInformation() {
        return formInformation;
    }

    /**
     * Set form information.
     */
    public void setFormInformation(FormInformation value) {
        this.formInformation = value;
    }

    /**
     * Return site identification.
     */
    public SiteIdentification getSiteIdentification() {
        return siteIdentification;
    }

    /**
     * Set site identification.
     */
    public void setSiteIdentification(SiteIdentification value) {
        this.siteIdentification = value;
    }

    /**
     * Return site location.
     */
    public SiteLocation getSiteLocation() {
        return siteLocation;
    }

    /**
     * Set site location.
     */
    public void setSiteLocation(SiteLocation value) {
        this.siteLocation = value;
    }

    /**
     * Return GNSS receivers.
     */
    public Set<GnssReceiverLogItem> getGnssReceivers() {
        return gnssReceivers;
    }

    /**
     * Set GNSS receivers.
     */
    public void setGnssReceivers(Set<GnssReceiverLogItem> rs) {
        gnssReceivers = rs;
    }

    /**
     * Return GNSS antennas.
     */
    public Set<GnssAntennaLogItem> getGnssAntennas() {
        return gnssAntennas;
    }

    /**
     * Set GNSS antennas.
     */
    public void setGnssAntennas(Set<GnssAntennaLogItem> as) {
        gnssAntennas = as;
    }

    /**
     * Return surveyed local ties.
     */
    public Set<SurveyedLocalTieLogItem> getSurveyedLocalTies() {
        return this.surveyedLocalTies;
    }

    /**
     * Set surveyed local ties.
     */
    public void setSurveyedLocalTies(Set<SurveyedLocalTieLogItem> ties) {
        surveyedLocalTies = ties;
    }

    /**
     * Return frequency standards.
     */
    public Set<FrequencyStandardLogItem> getFrequencyStandards() {
        return frequencyStandards;
    }

    /**
     * Set frequency standards.
     */
    public void setFrequencyStandards(Set<FrequencyStandardLogItem> fs) {
        frequencyStandards = fs;
    }

    /**
     * Return collocation information.
     */
    public Set<CollocationInformationLogItem> getCollocationInformation() {
        return collocationInformation;
    }

    /**
     * Set collocation information.
     */
    public void setCollocationInformation(Set<CollocationInformationLogItem> c) {
        collocationInformation = c;
    }

    /**
     * Return humidity sensor.
     */
    public Set<HumiditySensorLogItem> getHumiditySensors() {
        return this.humiditySensors;
    }

    /**
     * Set humidity sensor.
     */
    public void setHumiditySensors(Set<HumiditySensorLogItem> hs) {
        humiditySensors = hs;
    }

    /**
     * Return pressure sensors.
     */
    public Set<PressureSensorLogItem> getPressureSensors() {
        return this.pressureSensors;
    }

    /**
     * Set pressure sensors.
     */
    public void setPressureSensors(Set<PressureSensorLogItem> ps) {
        pressureSensors = ps;
    }

    /**
     * Return temperature sensors.
     */
    public Set<TemperatureSensorLogItem> getTemperatureSensors() {
        return this.temperatureSensors;
    }

    /**
     * Set temperature sensors.
     */
    public void setTemperatureSensors(Set<TemperatureSensorLogItem> ts) {
        temperatureSensors = ts;
    }

    /**
     * Return water vapor sensors.
     */
    public Set<WaterVaporSensorLogItem> getWaterVaporSensors() {
        return this.waterVaporSensors;
    }

    /**
     * Set water vapor sensors.
     */
    public void setWaterVaporSensors(Set<WaterVaporSensorLogItem> vs) {
        waterVaporSensors = vs;
    }

    /**
     * Return other instrumentation.
     */
    public Set<OtherInstrumentationLogItem> getOtherInstrumentationLogItem() {
        return this.otherInstrumentationLogItem;
    }

    /**
     * Set other instrumentation.
     */
    public void setOtherInstrumentationLogItem(Set<OtherInstrumentationLogItem> i) {
        otherInstrumentationLogItem = i;
    }

    /**
     * Return radio interferences.
     */
    public Set<RadioInterferenceLogItem> getRadioInterferences() {
        return this.radioInterferences;
    }

    /**
     * Set radio interferences.
     */
    public void setRadioInterferences(Set<RadioInterferenceLogItem> rs) {
        radioInterferences = rs;
        /* CollectionUtils.filter(radioInterferences, NotNullPredicate.INSTANCE); */
    }

    /**
     * Return multipath sources.
     */
    public Set<MultipathSourceLogItem> getMultipathSourceLogItems() {
        return this.multipathSourceLogItems;
    }

    /**
     * Set multipath sources.
     */
    public void setMultipathSourceLogItems(Set<MultipathSourceLogItem> ms) {
        multipathSourceLogItems = ms;
    }

    /**
     * Return signal obstructions.
     */
    public Set<SignalObstructionLogItem> getSignalObstructionLogItems() {
        return this.signalObstructionLogItems;
    }

    /**
     * Set signal obstructions.
     */
    public void setSignalObstructionLogItems(Set<SignalObstructionLogItem> s) {
        signalObstructionLogItems = s;
    }

    /**
     * Return local episodic events.
     */
    public Set<LocalEpisodicEffectLogItem> getLocalEpisodicEffectLogItems() {
        return this.localEpisodicEffectLogItems;
    }

    /**
     * Set local episodic events.
     */
    public void setLocalEpisodicEffectLogItems(Set<LocalEpisodicEffectLogItem> es) {
        localEpisodicEffectLogItems = es;
    }

    /**
     * Return more information.
     */
    public MoreInformation getMoreInformation() {
        return moreInformation;
    }

    /**
     * Set more information.
     */
    public void setMoreInformation(MoreInformation value) {
        this.moreInformation = value;
    }

    /**
     * Return responsible parties associated with this site.
     */
    public List<SiteResponsibleParty> getResponsibleParties() {
        return responsibleParties;
    }

    /**
     * Return site contacts.
     */
    public List<SiteResponsibleParty> getSiteContacts() {
        return getResponsiblePartiesByType(contactTypes.siteContact());
    }

    /**
     * Return site metadata custodians.
     */
    public List<SiteResponsibleParty> getSiteMetadataCustodians() {
        return getResponsiblePartiesByType(contactTypes.siteMetadataCustodian());
    }

    public List<SiteResponsibleParty> getResponsiblePartiesByType(ContactType contactType) {
        return responsibleParties.stream().filter(p ->
            p.getContactTypeId().equals(contactType.getId())
        ).collect(Collectors.toList());
    }

    public List<EquipmentLogItem<?>> getEquipmentLogItems() {
        List<EquipmentLogItem<?>> equipment = new ArrayList<EquipmentLogItem<?>>();
        equipment.addAll(getGnssReceivers());
        equipment.addAll(getGnssAntennas());
        equipment.addAll(getHumiditySensors());
        equipment.addAll(getFrequencyStandards());
        return equipment;
    }
}
