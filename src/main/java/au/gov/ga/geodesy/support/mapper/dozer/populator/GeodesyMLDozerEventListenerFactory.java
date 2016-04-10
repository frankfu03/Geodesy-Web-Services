package au.gov.ga.geodesy.support.mapper.dozer.populator;

import org.dozer.DozerEventListener;
import org.dozer.event.DozerEvent;

/**
 * Factory to create DozerEventListener for specific elements. Only one instance of this class will be created.
 * 
 * @author brookes
 *
 */
public class GeodesyMLDozerEventListenerFactory implements DozerEventListener {
    private SiteLocationTypePopulator siteLocationTypeEventListener = null;
    private GnssReceiverTypePopulator gnssReceiverTypeEventListener = null;
    private GnssAntennaTypePopulator gnssAntennaTypeEventListener = null;
    private SurveyedLocalTiesTypePopulator surveyedLocalTiesTypeEventListener = null;
    private SensorTypesPopulator sensorTypesEventListener = null;
    private EventListenerFactory eventListenerFactory = new EventListenerFactory();

    @Override
    public void mappingStarted(DozerEvent event) {
        // I don't call getHandlerIfOfInterest() here as sometimes nulls are sent and I'm not using this ATM.
    }

    @Override
    public void preWritingDestinationValue(DozerEvent event) {
        DozerEventListener handler = eventListenerFactory.getHandlerIfOfInterest(event);
        handler.postWritingDestinationValue(event);
    }

    @Override
    public void postWritingDestinationValue(DozerEvent event) {
        DozerEventListener handler = eventListenerFactory.getHandlerIfOfInterest(event);
        handler.postWritingDestinationValue(event);
    }

    @Override
    public void mappingFinished(DozerEvent event) {
        DozerEventListener handler = eventListenerFactory.getHandlerIfOfInterest(event);
        handler.mappingFinished(event);
    }

    class EventListenerFactory {

        /**
         * We may want to return a DozerEventListener to handle a specific element, such as for SiteLocationType to turn the countryCode
         * that is coming out as the country name from the translate into a code.
         * 
         * @param event
         * @return DozerEventListener that can handle if interesting or null if not.
         */
        public DozerEventListener getHandlerIfOfInterest(DozerEvent event) {
            switch (event.getDestinationObject().getClass().getSimpleName()) {
            case "SiteLocationType":
                if (siteLocationTypeEventListener == null) {
                    siteLocationTypeEventListener = new SiteLocationTypePopulator();
                }
                return siteLocationTypeEventListener;
            case "GnssReceiverType":
                if (gnssReceiverTypeEventListener == null) {
                    gnssReceiverTypeEventListener = new GnssReceiverTypePopulator();
                }
                return gnssReceiverTypeEventListener;
            case "GnssAntennaType":
                if (gnssAntennaTypeEventListener == null) {
                    gnssAntennaTypeEventListener = new GnssAntennaTypePopulator();
                }
                return gnssAntennaTypeEventListener;
            case "SurveyedLocalTiesType":
                if (surveyedLocalTiesTypeEventListener == null) {
                    surveyedLocalTiesTypeEventListener = new SurveyedLocalTiesTypePopulator();
                }
                return surveyedLocalTiesTypeEventListener;
            case "HumiditySensorType":
            case "PressureSensorType":
            case "TemperatureSensorType":
            case "WaterVaporSensorType":
                if (sensorTypesEventListener == null) {
                    sensorTypesEventListener = new SensorTypesPopulator();
                }
                return sensorTypesEventListener;
            }
            return new NoopPopulator();
        }
    }
}