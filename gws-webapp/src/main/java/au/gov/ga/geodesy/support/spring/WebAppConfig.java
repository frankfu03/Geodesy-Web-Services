package au.gov.ga.geodesy.support.spring;

import java.io.IOException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.config.EnableEntityLinks;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.util.ResourceUtils;

import au.gov.ga.geodesy.port.adapter.geodesyml.GeodesyMLValidator;

/**
 * Spring Configuration class for the web application.
 */
@Configuration
@EnableEntityLinks
@EnableAsync
@ComponentScan(basePackages = {
        "au.gov.ga.geodesy.java.util.service",
})
@Import({
    Config.class,
    GeodesySupportConfig.class,
    GeodesyServiceConfig.class,
    GeodesyRestMvcConfig.class,
    GeodesyRepositoryRestMvcConfig.class,
    PersistenceJpaConfig.class,
    SecurityConfig.class,
    ResourceServerConfig.class,
})
public class WebAppConfig {

    private static Logger logger = LoggerFactory.getLogger(WebAppConfig.class);

    /**
     * Gets a GeodesyMLValidator bean that has a reference to the catalog.xml file that is
     * on the classpath (exists inside the war file in the web context)
     * if the catalog file cannot be found there then return a GeodesyMLValidator with a null catalog
     * @return a configured {@link GeodesyMLValidator}
     */
    @Bean
    public GeodesyMLValidator getGeodesyMLValidator() {
        String catalogPath = null;
        try {
            // TODO: find the best way to load resources, javadoc says that ResourceUtils is
            // mosty a spring-internal utility class.
            URL catalogURL = ResourceUtils.getURL("classpath:xsd/geodesyml-0.1.0/catalog.xml");
            catalogPath = catalogURL.getPath();
        } catch (IOException e) {
            logger.error("Failed to find the catalog.xml file on the classpath. Check the package structure.", e);
        }
        // TODO: use a default constructor to handle the no-catalog case
        return new GeodesyMLValidator(catalogPath);
    }
}
