#!/usr/bin/python
# -*- coding: utf-8 -*-

__date__ = "2021-07-07"
__version__ = "0.0.1"

__all__ = ("TimedOutUsers",)

from apis.v1.database.live_data.timed_queue import TimedQueue
from apis.v1.database.live_data.items import UID


class TimedOutUsers(TimedQueue):
    """Stores currently timed out users, like for example due to lost quizzes."""

    life_time = 600
    max_refresh = 0  # no refreshes here

    def __call__(self, uid: str) -> None:
        if uid not in self:
            self[uid] = UID(uid)


if __name__ == "__main__":
    pass
