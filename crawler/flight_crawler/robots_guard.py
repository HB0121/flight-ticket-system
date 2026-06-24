from urllib.parse import urljoin
from urllib.robotparser import RobotFileParser

import requests


class RobotsGuard:
    def __init__(self, user_agent="flight-learning-crawler", timeout=10, fetch=None):
        self.user_agent = user_agent
        self.timeout = timeout
        self.fetch = fetch or requests.get

    def can_fetch(self, target_url):
        robots_url = urljoin(target_url, "/robots.txt")
        parser = RobotFileParser()
        parser.set_url(robots_url)
        try:
            response = self.fetch(
                robots_url,
                timeout=self.timeout,
                headers={"User-Agent": self.user_agent},
            )
            response.raise_for_status()
        except Exception:
            return False, "ROBOTS_UNAVAILABLE"

        parser.parse(response.text.splitlines())
        if parser.can_fetch(self.user_agent, target_url):
            return True, None
        return False, "ROBOTS_DISALLOWED"
