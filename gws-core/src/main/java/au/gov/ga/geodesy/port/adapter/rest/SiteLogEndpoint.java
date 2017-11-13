package au.gov.ga.geodesy.port.adapter.rest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.config.EnableEntityLinks;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLogRepository;
import au.gov.ga.geodesy.domain.service.CorsSiteLogService;
import au.gov.ga.geodesy.exception.GeodesyRuntimeException;
import au.gov.ga.geodesy.port.InvalidSiteLogException;
import au.gov.ga.geodesy.port.SiteLogReader;
import au.gov.ga.geodesy.port.adapter.geodesyml.GeodesyMLMarshaller;
import au.gov.ga.geodesy.port.adapter.geodesyml.GeodesyMLSiteLogReader;
import au.gov.ga.geodesy.port.adapter.geodesyml.GeodesyMLValidator;
import au.gov.ga.geodesy.port.adapter.geodesyml.MarshallingException;
import au.gov.ga.geodesy.port.adapter.sopac.SopacSiteLogReader;
import au.gov.ga.geodesy.support.mapper.orika.geodesyml.SiteLogMapper;
import au.gov.ga.xmlschemer.Violation;
import au.gov.xml.icsm.geodesyml.v_0_4.GeodesyMLType;
import au.gov.xml.icsm.geodesyml.v_0_4.ObjectFactory;
import au.gov.xml.icsm.geodesyml.v_0_4.SiteLogType;

@RepositoryRestController
@EnableEntityLinks
@RequestMapping("/siteLogs")
public class SiteLogEndpoint {

    private static final Logger log = LoggerFactory.getLogger(SiteLogEndpoint.class);

    private static ObjectFactory geodesyMLFactory = new ObjectFactory();

    @Autowired
    private SiteLogMapper mapper;

    @Autowired
    private CorsSiteLogService service;

    @Autowired
    private SiteLogRepository siteLogs;

    @Autowired
    private EntityLinks entityLinks;

    @Autowired
    private GeodesyMLValidator geodesyMLValidator;

    @Autowired
    private GeodesyMLMarshaller marshaller;

    @Autowired
    private EndpointSecurity endpointSecurity;

    @Transactional
    @RequestMapping(
        value = "/search/findByFourCharacterId",
        method = RequestMethod.GET,
        produces = {"application/xml"})
    public void findByFourCharacterId(
        HttpServletResponse rsp,
        @RequestParam(required = true) String id) throws IOException, MarshallingException {

        SiteLog siteLog = siteLogs.findByFourCharacterId(id);

        if (siteLog == null) {
            rsp.setStatus(HttpStatus.NOT_FOUND.value());
        } else {
            SiteLogType siteLogType = mapper.from(siteLog);

            GeodesyMLType document = geodesyMLFactory.createGeodesyMLType()
                .withElements(geodesyMLFactory.createSiteLog(siteLogType));

            rsp.setContentType("application/xml");
            marshaller.marshal(document, new PrintWriter(new OutputStreamWriter(rsp.getOutputStream(), StandardCharsets.UTF_8), true));
        }
    }

    @Transactional
    @RequestMapping(
        value = "/search/findByFourCharacterId",
        method = RequestMethod.GET,
        produces = "application/json")
    @ResponseBody
    public ResponseEntity<PersistentEntityResource> findByFourCharacterId(
            @RequestParam("id") String id,
            PersistentEntityResourceAssembler assembler) {

        SiteLog siteLog = siteLogs.findByFourCharacterId(id);
        if (siteLog == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            return ResponseEntity.ok(assembler.toResource(siteLog));
        }
    }

    @RequestMapping(value = "/validate", method = RequestMethod.POST, consumes = "application/xml")
    public ResponseEntity<List<Violation>> validateGeodesyMLSiteLog(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        StreamSource source = new StreamSource(req.getInputStream(), "data:");
        List<Violation> violations = geodesyMLValidator.validate(source);
        if (violations.isEmpty()) {
            return ResponseEntity.ok().body(null);
        } else {
            return ResponseEntity.badRequest().body(violations);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/isAuthorisedToUpload", method = RequestMethod.GET)
    public ResponseEntity<String> isPermittedToUpload(@RequestParam("fourCharacterId") String fourCharId) {
        return endpointSecurity.hasAuthorityToEditSiteLog(fourCharId)
            ? ResponseEntity.ok().build()
            : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public ResponseEntity<String> secureUploadGeodesyMLSiteLog(HttpServletRequest req, HttpServletResponse rsp) throws IOException, InvalidSiteLogException {
        SiteLogReader reader = new GeodesyMLSiteLogReader(new InputStreamReader(req.getInputStream(), StandardCharsets.UTF_8));
        SiteLog siteLog = reader.getSiteLog();

        endpointSecurity.assertAuthorityToEditSiteLog(siteLog.getSiteIdentification().getFourCharacterId());

        service.upload(siteLog);
        try {
            String location = entityLinks.linkToSingleResource(SiteLog.class, siteLog.getId()).getHref();
            int indexOfBrace = location.indexOf("{");
            if (indexOfBrace != -1) {
                location = location.substring(0, indexOfBrace);
            }
            return ResponseEntity.created(new URI(location)).body("");
        }
        catch (URISyntaxException e) {
            throw new GeodesyRuntimeException(e);
        }
    }

    @RequestMapping(value = "/sopac/upload", method = RequestMethod.POST)
    public ResponseEntity<String> uploadSopacSiteLog(
            HttpServletRequest req, HttpServletResponse rsp) throws IOException, InvalidSiteLogException {

        String siteLogText = IOUtils.toString(req.getInputStream());
        log.debug("Received SOPAC site log: " + siteLogText);
        SiteLogReader reader = new SopacSiteLogReader(new StringReader(siteLogText));
        SiteLog siteLog = reader.getSiteLog();
        service.upload(siteLog);
        try {
            String location = entityLinks.linkToSingleResource(SiteLog.class, siteLog.getId()).getHref();
            return ResponseEntity.created(new URI(location)).body(null);
        }
        catch (URISyntaxException e) {
            throw new GeodesyRuntimeException(e);
        }
    }

    @ExceptionHandler(InvalidSiteLogException.class)
    public ResponseEntity<String> invalidSiteLogHandler(InvalidSiteLogException e) throws IOException {
        StringWriter response = new StringWriter();

        log.error("Received invalid site log", e);
        e.printStackTrace(new PrintWriter(response));

        if (e.getCause() instanceof au.gov.ga.geodesy.igssitelog.interfaces.xml.MarshallingException) {
            List<String> validationMessages =
                ((au.gov.ga.geodesy.igssitelog.interfaces.xml.MarshallingException) e.getCause()).getValidationMessages();

            for (String validationMsg : validationMessages) {
                log.error(validationMsg);
                response.append(validationMsg + System.lineSeparator());
            }
        }
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(response.toString());
    }
}
