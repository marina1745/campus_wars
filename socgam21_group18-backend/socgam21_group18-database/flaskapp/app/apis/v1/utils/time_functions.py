"""
This file contains various functions related to time that are needed for our project
"""

from datetime import datetime
import pytz
import calendar
from time import gmtime
from time import strftime

__all__ = (
    "now",
    "timestamp",
    "from_timestamp",
)


START_SUMMER_TERM = 4
END_SUMMER_TERM = 9

tz = pytz.timezone("Europe/Vienna")


def now() -> datetime:
    """returns the current time & date"""
    return datetime.now(tz)


def timestamp() -> int:
    """returns the current time as a timestamp"""
    return int(now().timestamp())


def from_timestamp(timestamp_var: int) -> datetime:
    """returns the local datetime"""
    return datetime.fromtimestamp(timestamp_var, tz=tz)


def get_current_time_and_day():
    """returns a dictionary containing the day of the week and the time in seconds

    :return: the current time in seconds since midnight and day of the week as int (Monday = 0, Tuesday = 1...)
    :rtype: dict
    """
    now_var = datetime.now(tz)
    midnight = now_var.replace(hour=0, minute=0, second=0, microsecond=0)
    seconds = (now_var - midnight).seconds
    result = {"seconds": seconds, "day": now_var.weekday()}
    return result


def get_time_as_seconds(hour, minutes):
    """calculates how many seconds have passed since midnight based on the input

     :param hour: the hours passed since midnight
     :type hour: int
     :param minutes: the minutes passed since last full hour
     :type minutes: int

     :return: the time passed since midnight in seconds
     :rtype: int
     """
    now_var = datetime.now()
    time_needed = now_var.replace(hour=hour, minute=minutes, second=0)
    midnight = now_var.replace(hour=0, minute=0, second=0, microsecond=0)
    seconds = (time_needed - midnight).seconds
    return seconds


def get_day_as_string(day):
    """gets the day given as an integer (0 = Monday, 1 = Tuesday, ...) and returns the name of the day of week as
    a string

    :param day: the index of the day
    :type day: int

    :return: the name of the day of the week
    :rtype: str
    """
    return calendar.day_name[int(day)]


def get_seconds_as_string(seconds):
    """
    formats the the seconds that have passed since midnight into the format HH:mm

    :param seconds: the seconds passed since midnight
    :type seconds: int

    :return: the time in the format "HH:mm"
    :rtype: str
    """
    return strftime("%H:%M", gmtime(seconds))


def get_current_term():
    """returns the current term of the academic year we are currently in

    :return: the current term in format "YYS" YY being year, S being season
    :rtype: str
    """
    today = datetime.today()
    year = today.year % 100
    if START_SUMMER_TERM <= today.month <= END_SUMMER_TERM:
        letter = "S"
    else:
        letter = "W"
    return str(year) + letter
