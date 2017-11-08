package au.gov.ga.geodesy.domain.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.time.Instant;
import java.util.List;

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import au.gov.ga.geodesy.domain.model.sitelog.EffectiveDates;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;
import au.gov.ga.geodesy.domain.service.CorsSiteLogService;
import au.gov.ga.geodesy.port.adapter.geodesyml.GeodesyMLSiteLogReader;
import au.gov.ga.geodesy.support.TestResources;
import au.gov.ga.geodesy.support.spring.IntegrationTest;
import au.gov.ga.geodesy.support.utils.GMLDateUtils;

public class SetupRepositoryITest extends IntegrationTest {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(SetupRepositoryITest.class);

    private static final String DATE_FORMAT = "uuuu-MM-dd";
    private static final PageRequest maximumPageRequest = new PageRequest(0, Integer.MAX_VALUE);

    @Autowired
    private SetupRepository setups;

    @Autowired
    private CorsSiteRepository sites;

    @Autowired
    private CorsSiteLogService siteLogService;

    private @MonotonicNonNull Integer siteId;

    @Test
    @Rollback(false)
    @EnsuresNonNull("siteId")
    public void upload() throws Exception {
        SiteLog alice = new GeodesyMLSiteLogReader(TestResources.customGeodesyMLSiteLogReader("ALIC")).getSiteLog();
        SiteLog aliceWithNewReceiver = new GeodesyMLSiteLogReader(TestResources.customGeodesyMLSiteLogReader("ALIC-with-new-receiver")).getSiteLog();
        siteLogService.upload(alice);
        siteLogService.upload(aliceWithNewReceiver);
        siteId = sites.findByFourCharacterId("ALIC").getId();
        assertThat(siteId, not(nullValue()));
    }

    @Test(dependsOnMethods = {"upload"})
    public void findCurrentBySiteId() {
        setups.findCurrentBySiteId(siteId, SetupType.CorsSetup); // this will throw if the current setup is not unique
    }

    @Test(dependsOnMethods = {"upload"})
    public void findSetupsEffectiveWithinOpenPeriod() {
        List<Setup> result = toList(setups.findBySiteIdAndPeriod(
            siteId,
            SetupType.CorsSetup,
            null, null,
            maximumPageRequest));

        assertThat(getEffectiveFrom(result), is("1994-05-15"));
        assertThat(getEffectiveTo(result), is(nullValue()));
    }

    @Test(dependsOnMethods = {"upload"})
    public void findSetupsEffectiveWithinPeriod() {
        List<Setup> result = toList(setups.findBySiteIdAndPeriod(
            siteId,
            SetupType.CorsSetup,
            parse("2001-12-12"), parse("2010-05-05"),
            maximumPageRequest));

        assertThat(getEffectiveFrom(result), is("2000-01-24"));
        assertThat(getEffectiveTo(result), is("2010-05-06"));
    }

    @Test(dependsOnMethods = {"upload"})
    public void findSetupsEffectiveBeforeSpecificTime() {
        List<Setup> result = toList(setups.findBySiteIdAndPeriod(
            siteId,
            SetupType.CorsSetup,
            null, parse("2010-05-05"),
            maximumPageRequest));

        assertThat(getEffectiveFrom(result), is("1994-05-15"));
        assertThat(getEffectiveTo(result), is("2010-05-06"));
    }

    @Test(dependsOnMethods = {"upload"})
    public void findSetupsEffectiveAfterSpecificTime() {
        List<Setup> result = toList(setups.findBySiteIdAndPeriod(
            sites.findByFourCharacterId("ALIC").getId(),
            SetupType.CorsSetup,
            parse("2010-05-05"), null,
            maximumPageRequest));

        assertThat(getEffectiveFrom(result), is("2009-07-24"));
        assertThat(getEffectiveTo(result), is(nullValue()));
    }

    private List<Setup> toList(Page<Setup> page) {
        return Lists.newArrayList(page.iterator());
    }

    private String getEffectiveFrom(List<Setup> setups) {
        EffectiveDates firstPeriod = setups.get(0).getEffectivePeriod();
        return format(firstPeriod.getFrom());
    }

    private String getEffectiveTo(List<Setup> setups) {
        EffectiveDates lastPeriod = setups.get(setups.size() - 1).getEffectivePeriod();
        return format(lastPeriod.getTo());
    }

    private String format(Instant instant) {
        return GMLDateUtils.dateToString(instant, DATE_FORMAT);
    }

    private Instant parse(String instant) {
        return GMLDateUtils.stringToDate(instant, DATE_FORMAT);
    }
}
