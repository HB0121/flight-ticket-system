from dataclasses import dataclass
from urllib.parse import urlencode


PROVIDERS = {
    "ctrip": {
        "source": "ctrip_live",
        "base_url": "https://flights.ctrip.com/online/channel/domestic",
        "query": {"from": "from_city", "to": "to_city", "date": "date"},
    },
    "fliggy": {
        "source": "fliggy_live",
        "base_url": "https://www.fliggy.com/flight",
        "query": {"from": "from_city", "to": "to_city", "date": "date"},
    },
    "qunar": {
        "source": "qunar_live",
        "base_url": "https://flight.qunar.com/site/oneway_list.htm",
        "query": {
            "searchDepartureAirport": "from_city",
            "searchArrivalAirport": "to_city",
            "searchDepartureTime": "date",
        },
    },
}

SOURCE_TO_PROVIDER = {
    config["source"]: provider for provider, config in PROVIDERS.items()
}


def provider_from_source(source):
    try:
        return SOURCE_TO_PROVIDER[source]
    except KeyError as exc:
        raise ValueError(f"Unsupported live source: {source}") from exc


@dataclass(frozen=True)
class SiteAdapter:
    provider: str

    def __post_init__(self):
        if self.provider not in PROVIDERS:
            raise ValueError(f"Unsupported provider: {self.provider}")

    @property
    def data_source(self):
        return PROVIDERS[self.provider]["source"]

    def build_url(self, from_city, to_city, date):
        config = PROVIDERS[self.provider]
        values = {
            "from_city": from_city,
            "to_city": to_city,
            "date": date,
        }
        query = {
            target_name: values[value_name]
            for target_name, value_name in config["query"].items()
        }
        return f"{config['base_url']}?{urlencode(query)}"
