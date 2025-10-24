#!/usr/bin/python
# -*- coding: utf-8 -*-

__date__ = "2021-07-07"
__version__ = "0.0.1"

__all__ = ("LiveData",)


from apis.v1.database.live_data.room_queue import RoomQueue
from apis.v1.database.live_data.quiz_queue import QuizQueue
from apis.v1.database.live_data.game_queue import GameQueue
from apis.v1.database.live_data.timedout_users import TimedOutUsers
from apis.v1.database.live_data.rally_timeout import RallyTimeout
from contextlib import suppress
from typing import Any, Dict, Iterable


class LiveData:
    """Provides functionality to store data in memory rather than in a database."""
    room_queue: RoomQueue
    quiz_queue: QuizQueue
    game_queue: GameQueue
    timedout_users: TimedOutUsers
    rally_timeout: RallyTimeout

    def __init__(self):
        self()

    def __call__(self, ex: Iterable[str] = None) -> None:
        """Either create or reset data storage."""
        ex = ex or []
        if "room" not in ex:
            with suppress(AttributeError):
                del self.room_queue
            self.room_queue = RoomQueue(self)
        if "quiz" not in ex:
            with suppress(AttributeError):
                del self.quiz_queue
            self.quiz_queue = QuizQueue(self)
        if "game" not in ex:
            with suppress(AttributeError):
                del self.game_queue
            self.game_queue = GameQueue()
        if "timedout" not in ex:
            with suppress(AttributeError):
                del self.timedout_users
            self.timedout_users = TimedOutUsers()
        if "rally" not in ex:
            with suppress(AttributeError):
                del self.rally_timeout
            self.rally_timeout = RallyTimeout()

    @property
    def json(self) -> Dict[str, Any]:
        """Data for debugging."""
        return dict(
            room_queue=dict(self.room_queue.debug_items()),
            quiz_queue=dict(self.quiz_queue.debug_items()),
            game_queue=dict(self.game_queue.debug_items()),
            timedout_users=dict(self.timedout_users.debug_items()),
            rally_timeout=dict(self.rally_timeout.debug_items()),
            # misc=dict(
            #     multiplier=self.room_queue.multiplier,
            #     room_queue_occupancy=self.room_queue.get_each_rooms_occupancies()
            # )
        )


if __name__ == "__main__":
    pass
