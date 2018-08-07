package au.gov.ga.geodesy.support.mapper.orika.geodesyml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import au.gov.ga.geodesy.domain.model.sitelog.GnssAntennaLogItem;
import au.gov.ga.geodesy.support.spring.UnitTest;
import au.gov.xml.icsm.geodesyml.v_0_4.GnssAntennaType;
import au.gov.xml.icsm.geodesyml.v_0_4.IgsAntennaModelCodeType;
import au.gov.xml.icsm.geodesyml.v_0_4.IgsRadomeModelCodeType;

import net.opengis.gml.v_3_2_1.CodeType;
import net.opengis.gml.v_3_2_1.TimePositionType;

public class GnssAntennaMapperTest extends UnitTest {

    private final DateTimeFormatter outputFormat = dateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    
    @Autowired
    private GnssAntennaMapper mapper;

    @Test
    public void testMappingToLogItem() {
        final String dateInstalled = "2012-03-24T02:03:23.000Z";
        final String dateRemoved = "2014-03-24T02:03:23.000Z";
        final String serialNumber = "CR20020709";

        IgsRadomeModelCodeType igsRadomeModelCodeType = new IgsRadomeModelCodeType();
        igsRadomeModelCodeType.withValue("SomeAntennaRadomeType").withCodeSpace("eGeodesy/antennaRadomeType");

        GnssAntennaType antenna = new GnssAntennaType()
                .withIgsModelCode((IgsAntennaModelCodeType) new IgsAntennaModelCodeType()
                        .withCodeSpace("https://igscb.jpl.nasa.gov/igscb/station/general/rcvr_ant.tab")
                        .withCodeList("http://xml.gov.au/icsm/geodesyml/codelists/antenna-Antenna-codelists.xml#GeodesyML_GNSSAntennaTypeCode")
                        .withCodeListValue("LEICA GRX1200GGPRO")
                        .withValue("LEICA GRX1200GGPRO"))
                .withManufacturerSerialNumber(serialNumber)
                .withAntennaReferencePoint(new CodeType().withValue("NONE").withCodeSpace("eGeodesy/antennaReferencePoint"))
                .withMarkerArpUpEcc(0.00015)
                .withMarkerArpNorthEcc(0.1)
                .withMarkerArpEastEcc(0.2)
                .withAlignmentFromTrueNorth(0.0)
                .withAntennaRadomeType(igsRadomeModelCodeType)
                .withAntennaCableLength(12.0)
                .withAntennaCableType("Twine")
                .withDateInstalled(timePosition(dateInstalled))
                .withDateRemoved(timePosition(dateRemoved));

        GnssAntennaLogItem logItem = mapper.to(antenna);
        assertThat(logItem.getType(), is(antenna.getIgsModelCode().getCodeListValue()));
        assertThat(logItem.getAntennaReferencePoint(), is(antenna.getAntennaReferencePoint().getValue()));
        assertThat(logItem.getSerialNumber(), is(antenna.getManufacturerSerialNumber()));
        assertThat(logItem.getMarkerArpEastEcc(), is(antenna.getMarkerArpEastEcc()));
        assertThat(logItem.getMarkerArpNorthEcc(), is(antenna.getMarkerArpNorthEcc()));
        assertThat(logItem.getMarkerArpUpEcc(), is(0.0002));
        assertThat(logItem.getAlignmentFromTrueNorth(), is(String.valueOf(antenna.getAlignmentFromTrueNorth())));
        assertThat(logItem.getAntennaRadomeType(), is(antenna.getAntennaRadomeType().getValue()));
        assertThat(logItem.getAntennaCableLength(), is(String.valueOf(antenna.getAntennaCableLength())));
        assertThat(logItem.getAntennaCableType(), is(String.valueOf(antenna.getAntennaCableType())));
        Instant installed = logItem.getDateInstalled();
        assertThat(outputFormat.format(installed), is(dateInstalled));
        assertThat(outputFormat.format(logItem.getDateRemoved()), is(dateRemoved));

        GnssAntennaType antennaB = mapper.from(logItem);
        assertThat(antennaB.getIgsModelCode().getCodeListValue(), is(antenna.getIgsModelCode().getCodeListValue()));
        assertThat(antennaB.getManufacturerSerialNumber(), is(antenna.getManufacturerSerialNumber()));
        assertThat(antennaB.getAntennaReferencePoint(), is(antenna.getAntennaReferencePoint()));
        assertThat(antennaB.getMarkerArpEastEcc(), is(antenna.getMarkerArpEastEcc()));
        assertThat(antennaB.getMarkerArpNorthEcc(), is(antenna.getMarkerArpNorthEcc()));
        assertThat(antennaB.getMarkerArpUpEcc(), is(0.0002));
        assertThat(antennaB.getAlignmentFromTrueNorth(), is(antenna.getAlignmentFromTrueNorth()));
        assertThat(antennaB.getAntennaRadomeType(), is(antenna.getAntennaRadomeType()));
        assertThat(antennaB.getAntennaCableLength(), is(antenna.getAntennaCableLength()));
        assertThat(antennaB.getAntennaCableType(), is(antenna.getAntennaCableType()));
        assertThat(antennaB.getDateInstalled().getValue().get(0), is(dateInstalled));
        assertThat(antennaB.getDateRemoved().getValue().get(0), is(dateRemoved));
    }

    private TimePositionType timePosition(String date) {
        TimePositionType timePosition = new TimePositionType();
        timePosition.getValue().add(date);
        return timePosition;
    }

    private static DateTimeFormatter dateFormat(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.of("UTC"));
    }
}

