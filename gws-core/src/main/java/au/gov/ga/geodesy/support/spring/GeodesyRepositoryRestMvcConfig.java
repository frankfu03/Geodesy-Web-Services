package au.gov.ga.geodesy.support.spring;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.geotools.metadata.iso.citation.TelephoneImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurerAdapter;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.impl.BeanAsArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import au.gov.ga.geodesy.domain.model.ContactType;
import au.gov.ga.geodesy.domain.model.CorsNetwork;
import au.gov.ga.geodesy.domain.model.CorsSite;
import au.gov.ga.geodesy.domain.model.EquipmentInUse;
import au.gov.ga.geodesy.domain.model.Setup;
import au.gov.ga.geodesy.domain.model.equipment.Equipment;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentConfiguration;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentConfigurationRepository;
import au.gov.ga.geodesy.domain.model.equipment.EquipmentRepository;
import au.gov.ga.geodesy.domain.model.sitelog.SiteLog;

@Component
public class GeodesyRepositoryRestMvcConfig extends RepositoryRestConfigurerAdapter {

    @Autowired
    private RepositoryRestMvcConfiguration configuration;

    @Bean
    public RootResourceProcessor getRootResourceProcessor() {
        return new RootResourceProcessor();
    }

    @Bean
    public ResourceProcessor<Resource<CorsSite>> corsSiteResourceProcessor() {

        return new ResourceProcessor<Resource<CorsSite>>() {
            @Override
            public Resource<CorsSite> process(Resource<CorsSite> resource) {
                LinkBuilder link =
                    configuration.entityLinks().linkForSingleResource(
                        CorsSite.class,
                        resource.getContent().getId()
                    )
                    .slash("addToNetwork");

                resource.add(link.withRel("addToNetwork"));
                return resource;
            }
        };
    }

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
        config.exposeIdsFor(SiteLog.class, CorsSite.class, CorsNetwork.class, Setup.class, ContactType.class);
    }

    // See: https://github.com/spring-projects/spring-hateoas/issues/134
    @Autowired
    @Qualifier("_halObjectMapper")
    private void configureHalObjectMapper(ObjectMapper mapper) {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        configureJacksonObjectMapper(mapper);
    }

    @JsonIgnoreProperties({"standard", "modifiable", "interface"})
    abstract class TelephoneImplMixin {
    }

    @SuppressWarnings("serial")
    @Override
    public void configureJacksonObjectMapper(ObjectMapper mapper) {
        SimpleDateFormat format = new SimpleDateFormat("uuuu-MM-dd HH:mm:ss"); // ISO 8601
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.addMixIn(TelephoneImpl.class, TelephoneImplMixin.class);
        mapper.setDateFormat(format);
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new SimpleModule() {
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addBeanSerializerModifier(new SerializerModifier());
            }
        });
    }

    public static class SerializerModifier extends BeanSerializerModifier {
        public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
                JsonSerializer<?> ser) {

            Class<?> beanClass = beanDesc.getBeanClass();

            if (EquipmentInUse.class.isAssignableFrom(beanClass)) {
                return new CustomSerializer((BeanSerializerBase) ser);
            } else {
                return ser;
            }
        }
    }

    @SuppressWarnings("serial")
    @Configurable
    public static class CustomSerializer extends BeanSerializerBase {

        @Autowired
        private transient EquipmentRepository equipment;

        @Autowired
        private transient EquipmentConfigurationRepository configurations;

        public CustomSerializer(BeanSerializerBase source) {
            super(source);
        }

        public CustomSerializer(BeanSerializerBase source, Class<?> clazz) {
            super(source);
        }

        public CustomSerializer(CustomSerializer source, ObjectIdWriter objectIdWriter) {
            super(source, objectIdWriter);
        }

        public CustomSerializer(CustomSerializer source, ObjectIdWriter objectIdWriter, Object filterId) {
            super(source, objectIdWriter, filterId);
        }

        public CustomSerializer(CustomSerializer source, String[] toIgnore) {
            super(source, toIgnore);
        }

        public BeanSerializerBase withObjectIdWriter(ObjectIdWriter objectIdWriter) {
            return new CustomSerializer(this, objectIdWriter);
        }

        public BeanSerializerBase withIgnorals(String[] toIgnore) {
            return new CustomSerializer(this, toIgnore);
        }

        public BeanSerializerBase withFilterId(Object id) {
            return new CustomSerializer(this, _objectIdWriter, id);
        }

        public BeanSerializerBase asArraySerializer() {
            if ((_objectIdWriter == null)
                    && (_anyGetterWriter == null)
                    && (_propertyFilterId == null)) {
                return new BeanAsArraySerializer(this);
            }
            return this;
        }

        public void serialize(Object bean, JsonGenerator jgen, SerializerProvider provider) throws IOException,
                JsonGenerationException {
            jgen.writeStartObject();

            if (bean instanceof EquipmentInUse) {
                EquipmentInUse atSetup = (EquipmentInUse) bean;
                Integer equipmentId = atSetup.getEquipmentId();
                Integer configId = atSetup.getConfigurationId();
                Equipment e = equipment.findOne(equipmentId);
                EquipmentConfiguration c = configurations.findOne(configId);
                jgen.writeObjectField("id", e);
                jgen.writeObjectField("configuration", c);
            }
            serializeFields(bean, jgen, provider);
            jgen.writeEndObject();
        }
    }
}
