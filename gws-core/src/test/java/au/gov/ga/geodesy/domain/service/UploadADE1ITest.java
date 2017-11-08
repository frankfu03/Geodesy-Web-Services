package au.gov.ga.geodesy.domain.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

import au.gov.ga.geodesy.domain.model.CorsSite;
import au.gov.ga.geodesy.domain.model.CorsSiteRepository;
import au.gov.ga.geodesy.domain.model.Node;
import au.gov.ga.geodesy.domain.model.NodeRepository;
import au.gov.ga.geodesy.domain.model.Setup;
import au.gov.ga.geodesy.domain.model.SetupRepository;
import au.gov.ga.geodesy.domain.model.SetupType;
import au.gov.ga.geodesy.domain.model.sitelog.SiteIdentification;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLogRepository;
import au.gov.ga.geodesy.port.SiteLogReader;
import au.gov.ga.geodesy.port.adapter.sopac.SopacSiteLogReader;
import au.gov.ga.geodesy.support.TestResources;
import au.gov.ga.geodesy.support.spring.IntegrationTest;

@Transactional("geodesyTransactionManager")
public class UploadADE1ITest extends IntegrationTest {

    private static final String fourCharId = "ADE1";

    @Autowired
    private CorsSiteLogService siteLogService;

    @Autowired
    private CorsSiteRepository sites;

    @Autowired
    private SetupRepository setupRepo;

    @Autowired
    private NodeRepository nodeRepo;

    @Autowired
    private SiteLogRepository siteLogs;

    @Test
    @Rollback(false)
    public void saveSiteLog() throws Exception {
        SiteLogReader input = new SopacSiteLogReader(TestResources.originalSopacSiteLogReader(fourCharId));
        siteLogService.upload(input.getSiteLog());
    }

    @Test(dependsOnMethods = {"saveSiteLog"})
    @Rollback(false)
    public void checkSite() throws Exception {
        SiteLog siteLog = siteLogs.findByFourCharacterId(fourCharId);
        CorsSite site = sites.findByFourCharacterId(fourCharId);
        assertThat(site, notNullValue());

        SiteIdentification identification = siteLog.getSiteIdentification();
        assertThat(site.getName(), equalTo(identification.getSiteName()));
        assertThat(site.getDateInstalled(), equalTo(identification.getDateInstalled()));

        List<Setup> setups = setupRepo.findBySiteId(site.getId(), SetupType.CorsSetup);
        assertThat(setups.size(), equalTo(6));

        List<Node> nodes = nodeRepo.findBySiteId(site.getId());
        assertThat(nodes.size(), equalTo(4));
    }
}
