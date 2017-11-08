package au.gov.ga.geodesy.domain.service;

import au.gov.ga.geodesy.domain.model.CorsSiteRepository;
import au.gov.ga.geodesy.domain.model.NodeRepository;
import au.gov.ga.geodesy.domain.model.SetupRepository;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentRepository;
import au.gov.ga.geodesy.port.SiteLogReader;
import au.gov.ga.geodesy.port.adapter.sopac.SopacSiteLogReader;
import au.gov.ga.geodesy.support.TestResources;
import au.gov.ga.geodesy.support.spring.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@Transactional("geodesyTransactionManager")
public class KeepUnmodifiedNodesAndSetupsITest extends IntegrationTest {

    private static final String fourCharId = "ABRK";

    @Autowired
    private CorsSiteRepository sites;

    @Autowired
    private CorsSiteLogService siteLogService;

    @Autowired
    private SetupRepository setups;

    @Autowired
    private NodeRepository nodes;

    @Autowired
    private EquipmentRepository equipment;

    @Autowired
    private PlatformTransactionManager txnManager;

    private abstract class InTransaction {
        public void f() throws Exception {
            DefaultTransactionDefinition txnDef = new DefaultTransactionDefinition();
            TransactionStatus txn = txnManager.getTransaction(txnDef);
            try {
                f();
            }
            finally {
                txnManager.commit(txn);
            }
        }
    }

    private InTransaction uploadABRK = new InTransaction() {
        public void f() throws Exception {
            SiteLogReader input = new SopacSiteLogReader(TestResources.originalSopacSiteLogReader(fourCharId));
            siteLogService.upload(input.getSiteLog());
        }
    };
    private InTransaction[] scenario = {
        uploadABRK,
        uploadABRK,
    };

    private void execute(InTransaction... scenario) throws Exception {
        for (InTransaction s : scenario) {
            s.f();
        }
    }

    @Test
    @Rollback(false)
    public void execute() throws Exception {
        execute(scenario);
    }

    @Test(dependsOnMethods = "execute")
    @Rollback(false)
    public void checkSetupId() throws Exception {
        assertThat(sites.count(), equalTo(1L));
        assertThat(setups.count(), equalTo(2L));
        assertThat(nodes.count(), equalTo(1L));
        assertThat(equipment.count(), equalTo(3L));
    }
}
