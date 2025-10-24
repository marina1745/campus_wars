#!/usr/bin/python3
# -*- coding: utf-8 -*-

__date__ = "2021-06-28"
__version__ = "0.0.1"

__all__ = (
    "get_random_color",
    "generate_team_name",
    "used_names",
)

from random import random, seed
from datetime import datetime
from math import sqrt, pow
from petname import generate

from typing import Union, Tuple, List

seed(datetime.now().timestamp())

WEIGHT: float = 0.9
BASE_COLOR: Union[Tuple[int, int, int]] = (0, 0, 0)
used_names: List[str] = list()


# base color can be a string but it NEEDS to be in the long form! So NO `#aaa` but `#aaaaaa`!
def get_random_color(
    base_color: Union[Tuple[int, int, int], str] = BASE_COLOR, weight: float = WEIGHT
) -> str:
    """Generate a random color in rgb string format."""
    weight = max(0.0, min(weight, 1.0))
    if isinstance(base_color, str):
        base_color = tuple(
            int(base_color.replace("#", "")[i : i + 2], 16) for i in (0, 2, 4)
        )
    return "#" + "".join(
        map(
            lambda x: str(format(round(x * 255), "x")).rjust(2, "0"),
            (
                sqrt((1 - weight) * pow(base_color[0], 2) + weight * pow(random(), 2)),
                sqrt((1 - weight) * pow(base_color[0], 2) + weight * pow(random(), 2)),
                sqrt((1 - weight) * pow(base_color[0], 2) + weight * pow(random(), 2)),
            ),
        )
    )


def generate_team_name() -> str:
    """Generate a random team name."""
    name = generate(2, ' ')
    while name in used_names:
        name = generate(2, ' ')
    used_names.append(name)
    return name


if __name__ == "__main__":
    pass
