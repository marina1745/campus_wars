#!/usr/bin/python
# -*- coding: utf-8 -*-

__date__ = "2021-07-07"
__version__ = "0.0.1"

__all__ = (
    "GameQueue",
    "create_game_id",
)

from apis.v1.database.live_data.timed_queue import TimedQueue
from apis.v1.database.live_data.items import Game
from os import urandom
from base64 import b64encode
from typing import Optional


def create_game_id(length: int = 24) -> str:
    """Create id to identify games."""
    return b64encode(urandom(length)).decode("utf-8")


class GameQueue(TimedQueue):
    """Stores active games with data such as player info or questions"""

    life_time = 300
    max_refresh = 350

    def __call__(self, gid: str) -> None:
        if gid in self:
            self.refresh(gid)

    def is_player_in_game(self, uid: str) -> Optional[Game]:  # get game if true
        for game in self.values():
            if game.is_player_in_game(uid):
                return game
        return None

    def submit_answer(self, gid: str, pid: int, answer: int) -> None:
        self[gid].results[pid] = answer

    def close(self, gid: str) -> None:
        del self[gid]


if __name__ == "__main__":
    pass
