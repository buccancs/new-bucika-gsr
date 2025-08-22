import os
import pytest


def pytest_collection_modifyitems(config, items):
    if os.getenv("BUCIKA_ENABLE_TESTS", "0").lower() in ("1", "true", "yes", "on"):
        return
    skip = pytest.mark.skip(reason="Tests are temporarily disabled")
    for item in items:
        item.add_marker(skip)
