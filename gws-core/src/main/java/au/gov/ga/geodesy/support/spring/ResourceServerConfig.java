package au.gov.ga.geodesy.support.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkTokenStore;

import com.mashape.unirest.http.Unirest;

@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ResourceServerConfig.class);

    @Autowired
    private Config config;

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers(HttpMethod.POST, "/corsNetworks").hasAuthority("superuser")
            .antMatchers(HttpMethod.PUT, "/corsNetworks/**").hasAuthority("superuser")
            .antMatchers(HttpMethod.PATCH, "/corsNetworks/**").hasAuthority("superuser")
            .antMatchers(HttpMethod.DELETE, "/corsNetworks/**").hasAuthority("superuser")

            .antMatchers(HttpMethod.PATCH, "/corsSites/**").hasAuthority("superuser")
            .antMatchers(HttpMethod.PUT, "/corsSites/*/addToNetwork").hasAuthority("superuser")

            .antMatchers(HttpMethod.PUT, "/setups/request/updateSetups").hasAuthority("superuser")

            .antMatchers(HttpMethod.POST, "/newCorsSiteRequests").authenticated()
            .antMatchers(HttpMethod.POST, "/siteLogs/upload").authenticated()
            .antMatchers(HttpMethod.GET, "/siteLogs/isAuthorisedToUpload").authenticated()

            .antMatchers(HttpMethod.POST, "/siteLogs/validate").permitAll()
            .antMatchers(HttpMethod.POST, "/userRegistrations").permitAll()
            .antMatchers(HttpMethod.GET, "/**").permitAll()
            .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()

            .antMatchers(HttpMethod.POST, "/**").denyAll()
            .antMatchers(HttpMethod.PUT, "/**").denyAll()
            .antMatchers(HttpMethod.PATCH, "/**").denyAll()
            .antMatchers(HttpMethod.DELETE, "/**").denyAll()

            .anyRequest().denyAll()
        ;
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.resourceId(null);

        String jwksUri = Unirest.get(config.getOAuthProviderUrl() + "/.well-known/openid-configuration")
            .asJson()
            .getBody()
            .getObject()
            .getString("jwks_uri");

        resources.tokenStore(new JwkTokenStore(jwksUri));
        resources.eventPublisher(new AuthenticationEventPublisher() {

            @Override
            public void publishAuthenticationFailure(AuthenticationException e, Authentication a) {
                log.info("Authentication failure", e);
            }

            @Override
            public void publishAuthenticationSuccess(Authentication a) {
            }
        });
    }
}
