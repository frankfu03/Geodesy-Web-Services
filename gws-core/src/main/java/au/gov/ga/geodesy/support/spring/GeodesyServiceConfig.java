package au.gov.ga.geodesy.support.spring;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import au.gov.ga.geodesy.domain.model.event.AsynchronousEventPublisher;
import au.gov.ga.geodesy.domain.model.event.EventPublisher;
import au.gov.ga.geodesy.port.NotificationPort;
import au.gov.ga.geodesy.port.adapter.sns.SnsNotificationAdapter;

@Configuration
@EnableSpringConfigured
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackages = {
    "au.gov.ga.geodesy.domain.model",
    "au.gov.ga.geodesy.domain.service",
    "au.gov.ga.geodesy.domain.model.equipment",
    "au.gov.ga.geodesy.port.adapter.sopac",
    "au.gov.ga.geodesy.port.adapter.geodesyml",
    "au.gov.ga.geodesy.port.adapter.smpt",
    "au.gov.ga.geodesy.support.email",
    "au.gov.ga.geodesy.support.properties"
})
public class GeodesyServiceConfig {

    @Bean
    public EventPublisher eventPublisher() {
        return new AsynchronousEventPublisher();
    }

    @Bean
    public NotificationPort notificationPort() {
        return new SnsNotificationAdapter();
    }
}
