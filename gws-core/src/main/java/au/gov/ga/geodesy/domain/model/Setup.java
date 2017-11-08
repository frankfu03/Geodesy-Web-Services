package au.gov.ga.geodesy.domain.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import au.gov.ga.geodesy.domain.model.sitelog.EffectiveDates;

@Entity
@Table(name = "SETUP")
public class Setup {

    /**
     * RDBMS surrogate key
     */
    @Id
    @Column(name = "ID")
    @GeneratedValue(generator = "surrogateKeyGenerator")
    @SequenceGenerator(name = "surrogateKeyGenerator", sequenceName = "seq_surrogate_keys")
    protected @MonotonicNonNull Integer id;

    @Column(name = "SITE_ID", nullable = false)
    private Integer siteId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SetupType type;

    @NotNull
    @Embedded
    public EffectiveDates effectivePeriod;

    @Column(nullable = false)
    private Boolean invalidated = false;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "SETUP_ID")
    private List<EquipmentInUse> equipmentInUse = new ArrayList<>();

    @SuppressWarnings({"unused", "initialization.fields.uninitialized"}) // used by hibernate
    private Setup() {
    }

    public Setup(Integer siteId, SetupType type, EffectiveDates effectivePeriod) {
        this.siteId = siteId;
        this.type = type;
        this.effectivePeriod = effectivePeriod;
    }

    public @Nullable Integer getId() {
        return id;
    }

    public Integer getSiteId() {
        return siteId;
    }

    public void setSiteId(Integer siteId) {
        this.siteId = siteId;
    }

    public SetupType getType() {
        return type;
    }

    public EffectiveDates getEffectivePeriod() {
        return effectivePeriod;
    }

    public Boolean getInvalidated() {
        return invalidated;
    }

    public void invalidate() {
        invalidated = true;
    }

    public List<EquipmentInUse> getEquipmentInUse() {
        return equipmentInUse;
    }

    public void setEquipmentInUse(List<EquipmentInUse> equipment) {
        this.equipmentInUse = equipment;
    }

    public boolean isCurrent() {
        return getEffectivePeriod().getTo() == null;
    }

    @Override
    public boolean equals(@Nullable Object x) {
        if (x == null) {
            return false;
        }
        if (x == this) {
            return true;
        }
        if (x.getClass() != getClass()) {
            return false;
        }
        Setup other = (Setup) x;
        return new EqualsBuilder()
            .append(siteId, other.getSiteId())
            .append(type, other.getType())
            .append(getEffectivePeriod(), other.getEffectivePeriod())
            .isEquals()
            &&
            areEqual(equipmentInUse, other.getEquipmentInUse());
    }

    /**
     * Hibernate uses AbstractPersistentCollections whose equals methods do the
     * wrong thing when called with ordinary Java collections. We disregard the
     * types of colletions and define equality element-wise.
     */
    private boolean areEqual(Collection<EquipmentInUse> as, Collection<EquipmentInUse> bs) {
        if (as == bs) {
            return true;
        }
        if (as.size() != bs.size()) {
            return false;
        }
        Iterator<EquipmentInUse> a = as.iterator();
        Iterator<EquipmentInUse> b = bs.iterator();
        while (a.hasNext()) {
            if (!new EqualsBuilder().append(a.next(), b.next()).isEquals()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(19, 41).
            append(siteId).
            append(type).
            append(effectivePeriod).
            append(equipmentInUse).
            toHashCode();
   }
}
