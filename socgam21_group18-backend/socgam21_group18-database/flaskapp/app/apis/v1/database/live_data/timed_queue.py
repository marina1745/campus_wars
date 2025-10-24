#!/usr/bin/python
# -*- coding: utf-8 -*-

__date__ = "2021-07-07"
__version__ = "0.0.1"

__all__ = ("TimedQueue",)

from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.job import Job
from operator import attrgetter
from dataclasses import dataclass
from contextlib import suppress
from apis.v1.utils.time_functions import timestamp, from_timestamp, now
from typing import Any, Dict


class TimedQueue(dict, Dict[str, "Item"]):
    """Utility to store any element in a dictionary, that gets removed after a certain amount of time."""

    scheduler: BackgroundScheduler
    life_time: int
    max_refresh: int

    @dataclass
    class Item:
        """Helper, that stores the 'real' item with a the scheduler job."""
        name: str
        job: Job
        item: str

        @property
        def eta(self) -> int:
            """Time the object gets 'game ended'."""
            return self.job.next_run_time.timestamp()

        @property
        def eta_debug(self):
            return str(self.job.next_run_time)

        def __hash__(self) -> int:
            return hash(self.name)

    def __init__(self):
        super(TimedQueue, self).__init__()
        self.scheduler = BackgroundScheduler({"apscheduler.timezone": "Europe/Vienna"})
        self.scheduler.start()

    def __del__(self) -> None:
        self.scheduler.shutdown(wait=False)

    def __getitem__(self, item: str) -> Any:
        if item not in self:
            return None
        return super(TimedQueue, self).__getitem__(item).item

    def __setitem__(self, key, value) -> None:
        if key not in self and not isinstance(value, TimedQueue.Item):
            self.add(key, value)
        if not isinstance(value, TimedQueue.Item):
            self[key].item = value
        else:
            super(TimedQueue, self).__setitem__(key, value)

    def __delitem__(self, key: str) -> None:
        with suppress(AttributeError):
            self.get(key).job.remove()
        with suppress(KeyError):
            super(TimedQueue, self).__delitem__(key)

    def refresh(self, item: str) -> bool:
        """Attempt to refresh the lifetime of an object."""
        item = self.get(item)
        if item.eta + self.life_time > timestamp() + self.max_refresh: return False
        item.job = item.job.reschedule('interval', start_date=from_timestamp(item.eta), seconds=self.life_time)
        return True

    def eta(self, item: str) -> int:
        return self.get(item).eta

    def add(self, name: str, item: Any) -> bool:
        if name in self:
            return self.refresh(item)
        self[name] = TimedQueue.Item(
            name,
            self.scheduler.add_job(
                self.__delitem__,
                "interval",
                args=(name,),
                id=f"{self.__class__.__name__}:{name}",
                start_date=now(),
                seconds=self.life_time,
            ),
            item,
        )

    def dict(self) -> Dict[str, Any]:
        """Get a 'normal' representation of the dict."""
        return dict(zip(self.keys(), map(attrgetter("item"), self.values())))

    def values(self):
        """Return the normal dict values."""
        return map(attrgetter("item"), super().values())

    def debug_values(self):
        for item in self:
            info = self[item].json
            info["time"] = self.get(item).eta_debug
            yield info

    def items(self):
        return zip(self.keys(), self.values())

    def debug_items(self):
        return zip(self.keys(), self.debug_values())


if __name__ == "__main__":
    pass
