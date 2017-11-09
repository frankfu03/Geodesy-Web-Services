package au.gov.ga.geodesy.port.adapter.rest;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.Rollback;
import org.testng.annotations.Test;

import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;
import au.gov.ga.geodesy.domain.service.CorsSiteLogService;
import au.gov.ga.geodesy.port.adapter.sopac.SopacSiteLogReader;
import au.gov.ga.geodesy.support.TestResources;
import au.gov.ga.geodesy.support.spring.IntegrationTest;

public class SetupEndpointITest extends IntegrationTest {

    @Autowired
    private CorsSiteLogService siteLogService;

    @Test
    @Rollback(false)
    public void upload() throws Exception {
        SiteLog alice = new SopacSiteLogReader(TestResources.originalSopacSiteLogReader("ALIC")).getSiteLog();
        SiteLog zimmerwald = new SopacSiteLogReader(TestResources.originalSopacSiteLogReader("ZIMM")).getSiteLog();
        siteLogService.upload(alice);
        siteLogService.upload(zimmerwald);
    }

    @Test(dependsOnMethods = {"upload"})
    @Rollback(false)
    public void testFindCurrentByFourCharacterId() throws Exception {
        // find current CORS setup
        mvc.perform(get("/setups/search/findCurrentByFourCharacterId?id=ALIC&type=CorsSetup"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effectivePeriod.from").value("2013-07-08T00:45:00Z"))
            .andExpect(jsonPath("$.effectivePeriod.to").value(nullValue()))
            .andExpect(jsonPath("$._links.self").isNotEmpty())
            .andDo(document("findCurrentByFourCharacterId"));

        // find current GNSS antenna setup
        mvc.perform(get("/setups/search/findCurrentByFourCharacterId?id=ALIC&type=GnssAntennaSetup"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effectivePeriod.from").value("2011-07-20T00:00:00Z"))
            .andExpect(jsonPath("$.effectivePeriod.to").value(nullValue()))
            .andExpect(jsonPath("$._links.self").isNotEmpty())
            .andDo(document("findCurrentByFourCharacterId"));
    }

    @Test(dependsOnMethods = {"upload"})
    @Rollback(false)
    public void testFindByFourCharacterIdAndDate() throws Exception {
        // find CORS setups
        mvc.perform(get("/setups/search/findByFourCharacterId?id=ALIC&type=CorsSetup&effectiveFrom=2011-12-12&effectiveTo=2014-12-12&timeFormat=uuuu-MM-dd"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(3))
            .andExpect(jsonPath("$._embedded.setups[0].effectivePeriod.from").value("2011-07-20T00:00:00Z"))
            .andExpect(jsonPath("$._embedded.setups[0].effectivePeriod.to").value("2013-03-08T00:00:00Z"))
            .andExpect(jsonPath("$._embedded.setups[0]._links.self").isNotEmpty());

        // find GNSS antenna setups
        mvc.perform(get("/setups/search/findByFourCharacterId?id=ALIC&type=GnssAntennaSetup&effectiveFrom=2011-12-12&effectiveTo=2014-12-12&timeFormat=uuuu-MM-dd"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(1))
            .andExpect(jsonPath("$._embedded.setups[0].effectivePeriod.from").value("2011-07-20T00:00:00Z"))
            .andExpect(jsonPath("$._embedded.setups[0].effectivePeriod.to").value(nullValue()))
            .andExpect(jsonPath("$._embedded.setups[0]._links.self").isNotEmpty());

    }

    @Test(dependsOnMethods = {"upload"})
    @Rollback(false)
    public void testFindByFourCharacterId() throws Exception {
        int expectedCorsSetups = 20;
        mvc.perform(get("/setups/search/findByFourCharacterId?id=ALIC&type=CorsSetup&size=" + expectedCorsSetups))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(expectedCorsSetups))
            .andExpect(jsonPath("$._embedded.setups[0].effectivePeriod.from").value("1994-05-15T00:00:00Z"))
            .andExpect(jsonPath("$._embedded.setups[0]._links.self").isNotEmpty())
            .andExpect(jsonPath("$._embedded.setups[" + (expectedCorsSetups - 1) + "].effectivePeriod.to").value(nullValue()));

        int expectedGnssAntennaSetups = 3;
        mvc.perform(get("/setups/search/findByFourCharacterId?id=ALIC&type=GnssAntennaSetup&size=" + expectedGnssAntennaSetups))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements").value(expectedGnssAntennaSetups))
            .andExpect(jsonPath("$._embedded.setups[0].effectivePeriod.from").value("1994-05-15T00:00:00Z"))
            .andExpect(jsonPath("$._embedded.setups[0]._links.self").isNotEmpty())
            .andExpect(jsonPath("$._embedded.setups[" + (expectedGnssAntennaSetups- 1) + "].effectivePeriod.to").value(nullValue()));
    }

    @Test(dependsOnMethods = {"upload"})
    @Rollback(false)
    public void testUpdateSetupsUnauthenticated() throws Exception {
        given()
        .when()
            .put("/setups/request/updateSetups")
        .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test(dependsOnMethods = {"upload"})
    @Rollback(false)
    public void testUpdateSetupsUnauthorised() throws Exception {
        given().auth().with(super.bareUserToken())
        .when()
            .put("/setups/request/updateSetups")
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test(dependsOnMethods = {"upload"})
    @Rollback(false)
    public void testUpdateSetups() throws Exception {
        given().auth().with(super.superuserToken())
        .when()
            .put("/setups/request/updateSetups")
        .then()
            .statusCode(HttpStatus.OK.value());
    }
}
