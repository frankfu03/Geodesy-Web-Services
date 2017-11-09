package au.gov.ga.geodesy.domain.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.Test;

import au.gov.ga.geodesy.domain.model.SetupRepository;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentConfigurationRepository;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentRepository;
import au.gov.ga.geodesy.port.SiteLogReader;
import au.gov.ga.geodesy.port.adapter.sopac.SopacSiteLogReader;
import au.gov.ga.geodesy.support.TestResources;
import au.gov.ga.geodesy.support.spring.IntegrationTest;

@Transactional("geodesyTransactionManager")
public class SetupServiceITest extends IntegrationTest {

    private static final String fourCharId = "ADE1";

    @Autowired
    private SetupService setupService;

    @Autowired
    private CorsSiteLogService siteLogService;

    @Autowired
    private SetupRepository setups;

    @Autowired
    private EquipmentConfigurationRepository equipmentConfigurations;

    @Autowired
    private EquipmentRepository equipment;

    @Test
    @Rollback(false)
    public void saveSiteLog() throws Exception {
        SiteLogReader input = new SopacSiteLogReader(TestResources.originalSopacSiteLogReader(fourCharId));
        siteLogService.upload(input.getSiteLog());
    }

    @Test(dependsOnMethods = {"saveSiteLog"})
    @Rollback(false)
    public void deleteSetups() throws Exception {
        setupService.deleteSetups();
    }

    @Test(dependsOnMethods = {"deleteSetups"})
    @Rollback(false)
    public void createSetups() throws Exception {
        assertThat(setups.count(), is(0L));
        assertThat(equipment.count(), is(0L));
        assertThat(equipmentConfigurations.count(), is(0L));

        setupService.createSetups();
    }

    @Test(dependsOnMethods = {"createSetups"})
    @Rollback(false)
    public void checkSetups() throws Exception {
        assertThat(setups.count(), is(7L));
        assertThat(equipment.count(), is(3L));
        assertThat(equipmentConfigurations.count(), is(6L));
    }
}
