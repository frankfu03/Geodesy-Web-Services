# pylint: disable=line-too-long, missing-docstring

from collections import OrderedDict
import json
from pprint import pprint


import requests
import pyjq


GWS_URL = 'https://test.geodesy.ga.gov.au'
# GWS_URL = 'http://localhost:8081'


def get_antenna_setups_for_site(four_char_id, effective_from, effective_to):
    response = requests.get(GWS_URL + '/setups/search/findByFourCharacterId', params={
        'id': four_char_id,
        'type': 'GnssAntennaSetup',
        'effectiveFrom': effective_from,
        'effectiveTO': effective_to,
    })
    response.raise_for_status()

    query = """
       ._embedded.setups[] |
           .current as $current |
           .effectivePeriod as $period |
           .equipmentInUse[].content |
           {
               "current": $current,
               "period": $period,
               "type": .id.type,
               "serialNumber": .id.serialNumber,
               "markerArpUpEccentricity": .configuration.markerArpUpEccentricity,
           }
    """

    return pyjq.apply(query, json.loads(response.text))


def get_site_contact_type_id():
    response = requests.get(GWS_URL + '/contactTypes?code=SiteContact')
    response.raise_for_status()
    return pyjq.first('._embedded.contactTypes[0].id', json.loads(response.text))


def get_site_contact(four_char_id):
    response = requests.get(GWS_URL + '/siteLogs?siteIdentification.fourCharacterId=' + four_char_id)
    response.raise_for_status()

    query = """
        ._embedded.siteLogs[] |
            .responsibleParties[0] | select(.contactTypeId == {site_contact_type_id}) |
            {{
                "siteContact": {{
                    "name": .party.individualName,
                    "organisation": .party.organisationName,
                    "address": .party.contactInfo.address.deliveryPoints,
                    "city": .party.contactInfo.address.city,
                    "country": .party.contactInfo.address.country,
                    "postalCode": .party.contactInfo.address.postalCode,
                }}
            }}
    """.format(site_contact_type_id=get_site_contact_type_id())

    return pyjq.apply(query, json.loads(response.text))


def get_antenna_setups_for_sites(four_char_ids, effective_from, effective_to):
    antennas = []
    for four_char_id in four_char_ids:
        antennas.append(OrderedDict({
            "fourCharId": four_char_id,
            "contact": get_site_contact(four_char_id),
            "antennaSetups": get_antenna_setups_for_site(four_char_id, effective_from, effective_to),
        }))
    return antennas

pprint(get_antenna_setups_for_sites(['WEDD', 'PERT'], '1994-04-27', '2012-04-27'))
