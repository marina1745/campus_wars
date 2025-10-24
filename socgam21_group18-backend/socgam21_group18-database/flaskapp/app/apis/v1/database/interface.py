#!/usr/bin/python
# -*- coding: utf-8 -*-
"""
In this file, we define functions necessary so we can communicate effectively with our database.
As recommended by the mongodb documentation (https://docs.mongodb.com/), we use lowercase with camelCase for the db,
while we use all lowercases with _ spaces for python

"""

__all__ = (
    "add_room",
    "add_lecture",
    "get_all_rooms",
    "find_closest_room",
    "add_lectures_to_user",
    "add_question_to_quiz",
    "add_user",
    "get_users_of_lecture",
    "get_full_name_of_current_lecture_in_room",
    "get_current_team",
    "get_player_name",
    "get_current_quizzes",
    "get_questions_of_quiz",
    "get_colour_of_team",
)

import groupCreation
from apis.v1.database import mongo, db
from datetime import datetime
from apis.v1.utils.time_functions import (
    get_current_time_and_day,
    get_current_term,
    get_day_as_string,
    get_seconds_as_string,
)
from bson.objectid import ObjectId


def find_closest_room(lon, lat, max_distance):
    """ Finds the room closest to the given longitude & latitude. If there is no room close than max_distance, None is
    returned.

    :param lon: longitude of the position
    :type lon: float
    :param lat: latitude of the position
    :type lat: float
    :param max_distance: the maximum distance that may be between a room and the position for it to be considered
    :type max_distance: float

    :return: A dictionary with the room information if a room was found, None otherwise
    :rtype: dict


    Description of the keys and their values of the returned dict:
        _id: The generated _id of the room
        location: the location of the room, format: {"type": "Point", "coordinates": [longitude,latitude]}
        roomName: the name of the room
        campusID: The id of the campus that room belongs to

    """
    return mongo.db.room.find_one(
        {
            "location": {
                "$near": {
                    "$geometry": {"type": "Point", "coordinates": [lon, lat]},
                    "$maxDistance": max_distance,
                }
            }
        }
    )


def add_room(room_name, longitude, latitude, campus_id=None):
    """Adds a room to the database

    :param room_name: the name of the room that should be added
    :type room_name: str
    :param longitude: the longitude of the position of the room
    :type longitude: float
    :param latitude: the latitude of the position of the room
    :type latitude: float
    :param campus_id: the campus the room belongs to, default None
    :type campus_id: ObjectId

    :return: True, if successfully added, False otherwise
    :rtype: bool
    """
    item = {
        "location": {"type": "Point", "coordinates": [longitude, latitude]},
        "roomName": room_name,
        "campusID": campus_id
    }
    return mongo.db.room.insert_one(item).acknowledged


def get_all_rooms():
    """Returns all rooms that are stored in the database.
    :return: the list with all rooms stored in the database. The rooms are stored as dicts
    :rtype: list

    Description of the keys and their values of the returned dicts:
        :key _id: The generated _id of the room
        :key location: the location of the room, format: {"type": "Point", "coordinates": [longitude,latitude]}
        :key roomName: the name of the room
        :key campusID: The id of the campus that room belongs to
    """
    return list(mongo.db.room.find())


def add_lecture(name, term, timetable=[]):
    """adds a new lecture to the database. If the lecture already exists in the database, only the new entries of the
        timetable are added instead.

    :param name: Name of the lecture
    :type name: str
    :param term: The term in which the lecture is/was held
    :type term: str
    :param timetable: The time schedule for the lecture, defaults to []
    :type timetable: list

    :return: True, if the lecture was successfully added to the database or all the new entries of the timetable
            were successfully added, False otherwise
    :rtype: bool


    """
    if timetable is None:
        timetable = []
    entry = mongo.db.lecture.find_one({"name": name, "term": term})
    if entry is None:  # Entry does not already exist in db, create new one
        item = {"name": name, "term": term, "timetable": timetable}
        return mongo.db.lecture.insert_one(item).acknowledged
    for t in timetable:  # add entries of timetable that do not already exist in the db
        if any(
            e["start"] == t["start"]
            and e["end"] == t["end"]
            and e["roomID"] == t["roomID"]
            and e["day"] == t["day"]
            for e in entry["timetable"]
        ):
            continue
        elif not any(
            e["start"] < t["end"]
            and e[  # test if there is no overlapping with previously added entries
                "end"
            ]
            > t["start"]
            and e["day"] == t["day"]
            for e in entry["timetable"]
        ):
            if (
                not mongo.db.lecture.update_one(
                    {"_id": entry["_id"]}, {"$push": {"timetable": t}}
                ).matched_count
                > 0
            ):
                return False
        else:
            return False

    return True


def add_user(firebase_id, name, lectures=[]):
    """adds a new user to the database. If the user already exists, only the new lectures will be added to the users'
        lectures list

    :param firebase_id: the Firebase ID of the user
    :type firebase_id: str
    :param name: the user name
    :type name: str
    :param lectures: the list of lectures the user attended/attends (type of lectures: ObjectId), defaults to []
    :type lectures: list

    :return: True, if successfully added the new user / the lectures, False otherwise
    :rtype: bool

    """
    if lectures is None:
        lectures = []
    entry = mongo.db.firebase_users.find_one({"firebaseID": firebase_id})
    if entry is None:
        item = {
            "firebaseID": firebase_id,
            "name": name,
            "lectures": lectures
        }
        return mongo.db.firebase_users.insert_one(item).acknowledged
    else:
        for lec in lectures:
            if lec not in entry["lectures"]:
                if (
                    not mongo.db.firebase_users.update_one(
                        {"_id": entry["_id"]}, {"$push": {"lectures": lec}}
                    ).matched_count
                    > 0
                ):
                    return False
        return True


def add_lectures_to_user(firebase_id, lectures):
    """adds the lectures given to the list of lectures attended by the user. The list, however, is a list of strings and
    is not in the right format for our database
    Given format: <lecture name>:<term>
    Wanted format: two strings: 1. <lecture name>, 2. <term>

    :param firebase_id: the firebase ID of the user that the lectures belong to
    :type firebase_id: str
    :param lectures: the list of the lectures the user attended / attends
    :type lectures: list

    :return: True, if all the insertions were successful, False otherwise
    :rtype:bool

    """
    for i in range(len(lectures)):
        split_string = lectures[i].split(":")
        name = split_string[0]
        j = 1
        while j < len(split_string) - 1:
            name += split_string[j]
            j = j + 1
        term = split_string[j][1:]
        if not add_lecture(name, term):
            return False
        lecture_id = mongo.db.lecture.find_one(
            {"name": name, "term": term}, {"_id": 1}
        )["_id"]
        if (
            not mongo.db.firebase_users.update_one(
                {"firebaseID": firebase_id}, {"$push": {"lectures": lecture_id}}
            ).matched_count
            > 0
        ):
            return False
    return True


def add_question_to_quiz(question, right_answer, wrong_answers, quiz_id):
    """Adds a question to a quiz into the database.

    :param question: The question text of the question
    :type question: str
    :param right_answer: The correct answer to the question
    :type right_answer: str
    :param wrong_answers: the wrong answers the player could also choose
    :type wrong_answers: str
    :param quiz_id: The ID of the quiz the question belongs to
    :type quiz_id: ObjectId

    :return: True, if insertion was successful, False otherwise
    :rtype: bool
    """
    if isinstance(quiz_id, str):
        quiz_id = ObjectId(quiz_id)
    item = {
        "question": question,
        "rightAnswer": right_answer,
        "wrongAnswers": wrong_answers,
        "quizID": quiz_id,
    }
    return mongo.db.question.insert_one(item)["acknowledged"]


def get_current_quizzes(room_id):
    """Gets all the quizzes of the lecture that is currently held in the room of with room ID = room_id. If no lecture
    or quizzes were found, it returns general campus quizzes instead

    :param room_id: The room ID of the room that you need the current quizzes of
    :type room_id: ObjectId or str

    :return: A list of all the quizzes (stored as dicts) that belong to the current lecture / campus
    :rtype: list

    Description of the keys and their values of the returned dicts:
        :key _id: The generated id of the quiz
        :key createdBy: The person that created the quiz
        :key creationDate: The creation date of the quiz
        :key name: The name of the quiz
        :key campusID: The ID of the campus the quiz belongs to, optional
        :key lectureID: the ID of the lecture the quiz belongs to, optional
    """
    if isinstance(room_id, str):
        room_id = ObjectId(room_id)
    lecture = get_current_lecture(room_id)
    room_info = mongo.db.room.find_one({"_id": room_id}, {"_id": 0})
    if lecture is None:
        return list(mongo.db.quiz.find({"campusID": room_info["campusID"]}))
    result = list(mongo.db.quiz.find({"lectureID": lecture["_id"]}))
    if len(result) == 0:
        return list(mongo.db.quiz.find({"campusID": room_info["campusID"]}))
    return result


def get_current_lecture(room_id):
    """Gets the current lecture of the room that has ID room_Id

    :param room_id: The ID of the room that you want the current lecture of

    :return: The current lecture that is held in the room
    :rtype: dict

    Description of the keys and their values of the returned dict:
        :key _id: the automatically generated id of the lecture
        :key name: The name of the lecture
        :key term: the term the lecture is/was held
        :key timetable: The schedule of the lecture

    """
    current_time = get_current_time_and_day()
    return mongo.db.lecture.find_one(
        {
            "term": get_current_term(),
            "timetable": {
                "$elemMatch": {
                    "start": {"$lt": current_time["seconds"]},
                    "end": {"$gte": current_time["seconds"]},
                    "roomID": room_id,
                    "day": current_time["day"],
                }
            },
        }
    )


def add_quiz(name, created_by, lecture_id):
    """adds a new quiz to the database

    :param name: The name of the quiz
    :type name: str
    :param created_by: the author of the quiz
    :type created_by: str
    :param lecture_id: the ID of the lecture that the quiz belongs to
    :type lecture_id: ObjectId or str

    :return: True, if insertion was successful, False otherwise
    :rtype: bool

    """
    if isinstance(lecture_id, str):
        lecture_id = ObjectId(lecture_id)
    item = {
        "name": name,
        "createdBy": created_by,
        "lectureID": lecture_id,
        "creationDate": datetime.now().isoformat(),
    }
    return mongo.db.quiz.insert_one(item).acknowledged


def add_campus_quiz(name, created_by, campus_id):
    """adds a 'campus' quiz to the database; a campus quiz is a quiz that shows up in a room / lecture hall
    (of a campus) were there is no current lecture / no quizzes to the current lecture

    :param name: The name of the quiz
    :type name: str
    :param created_by: the author of the quiz
    :type created_by: str
    :param campus_id: the ID of the campus that the quiz belongs to
    :type campus_id: ObjectId or str

    :return: True, if insertion was successful, False otherwise
    :rtype: bool

    """
    if isinstance(campus_id, str):
        campus_id = ObjectId(campus_id)
    item = {
        "name": name,
        "createdBy": created_by,
        "campusID": campus_id,
        "creationDate": datetime.now().isoformat(),
    }
    return mongo.db.quiz.insert_one(item).acknowledged


def get_lectures_of_user(firebase_id):
    """gets all the lectures that a user ever attended

    :param firebase_id: The Firebase ID of the user
    :type firebase_id: str

    :return: A list of all the lectures that the user ever attended, returns None if user could not be found
    :rtype: list

    """
    user = mongo.db.firebase_users.find_one({"firebaseID": firebase_id})
    if user is not None:
        return user["lectures"]
    return None


def get_users_of_lecture(lecture_id):
    """returns all the user that attended a given lecture

    :param lecture_id: the ID of the lecture that we want to know th users of
    :type lecture_id: ObjectId or str

    :return: the list of the Firebase IDs of all the users that have attended the lecture
    :rtype: list

    """
    if isinstance(lecture_id, str):
        lecture_id = ObjectId(lecture_id)
    return list(map(lambda x: str(x["firebaseID"]), list(
        mongo.db.firebase_users.find({"lectures": {"$elemMatch": {"$eq": lecture_id}}},
                                     {"firebaseID": 1, "_id": 0}))))


def get_full_name_of_current_lecture_in_room(room_id):
    """gets the full name (name + term) of the current lecture held in a room

    :param room_id: the ID of the room that we want to know the name of the current lecture of
    :type room_id: ObjectId or str

    :return: The full name of the current lecture held, if no lecture found, it will return None
    :rtype: str
    """
    if isinstance(room_id, str):
        room_id = ObjectId(room_id)
    lecture = get_current_lecture(room_id)

    if lecture is None:
        return None
    return lecture["name"] + ": " + lecture["term"]


def get_time_table_of_room(room_id):
    """get the timetable of the room

    :param room_id: the ID of the room you want to know the current lecture of
    :type room_id: ObjectId or str

    :return: the list of all the lectures, with weekday & time held, as dicts, sorted by the time of being held
             ascending order)
    :rtype: list

    Description of the keys and their values of the returned dict:
        :key name: Full name of the lecture
        :key day_as_int: the weekday as an integer: 0 = Monday, 1 = Tuesday, ...
        :key day: the weekday as a string, e.g. 'Monday'
        :start: the start time of the lecture as a string, e.g. '16:00'
        :end: the end time of the lecture as a string, e.g. '18:15'

    """
    if isinstance(room_id, str):
        room_id = ObjectId(room_id)
    lectures = list(
        mongo.db.lecture.find(
            {
                "term": get_current_term(),
                "timetable": {"$elemMatch": {"roomID": room_id}},
            }
        )
    )
    items = []
    for lec in lectures:
        for e in lec["timetable"]:
            if e["roomID"] == room_id:
                items.append(
                    {
                        "name": lec["name"] + ": " + lec["term"],
                        "day_as_int": e["day"],
                        "day": get_day_as_string(e["day"]),
                        "start": get_seconds_as_string(e["start"]),
                        "end": get_seconds_as_string(e["end"]),
                    }
                )
    items = sorted(items, key=lambda x: (x["day_as_int"], x["start"]))
    return items


def add_new_teams(team_list):
    """adds the new current teams for this term to the database; since some teams could have already been created, they
    will be deleted in the database first

    :param team_list: list of teams that should be added
    :type team_list: list

    :returns: A tuple: (True, None) if there have been no errors with the insertion, (False, Team) if there has been an
    issue with inserting Team
    :rtype: tuple
    """
    mongo.db.teams.delete_many({"term": get_current_term()})
    for team in team_list:
        if not add_team(team):
            return False, team
    return True, None


def add_team(team):
    """adds a new team to the database
    :param team: the team to be inserted
    :type team: Group

    :return: True, if insertion was successful, False otherwise
    :rtype: bool

    """
    item = {
        "name": team.name,
        "colour": team.color,
        "term": get_current_term(),
        "members": team.members,
    }
    return mongo.db.teams.insert_one(item).acknowledged


def get_all_lecture_ids():
    """gets the lecture IDs of all lectures

    :return: a list of the lecture IDs as strings
    :rtype: list
    """
    return list(map(lambda x: str(x["_id"]), list(mongo.db.lecture.find())))


def get_player_name(firebase_id):
    """gets the player name of the user with the given Firebase ID

    :param firebase_id: the Firebase ID of the user
    :type firebase_id: str

    :return: the name of the player, returns None if user could not be found database
    :rtype: str
    """
    user = mongo.db.firebase_users.find_one({"firebaseID": firebase_id}, {"name": 1})
    if user is None:
        return None
    return user["name"]


def get_current_team_with_member_names(firebase_id):
    """gets all the info about a team & the names of the members of the team that user with the given
    Firebase ID is in

    :param firebase_id: the Firebase ID of the user
    :type firebase_id: str

    :return: A dictionary with all the information about the team
    :rtype: dict

     Description of the keys and their values of the returned dict:
        :key name: the name of the team
        :key term: the term the team was active in
        :key colour: the colour of the team
        :key members: the list of member names of the team
    """
    my_team = get_current_team(firebase_id)
    if my_team is None:
        return None
    my_team["members"] = list(map(lambda x: get_player_name(x), my_team["members"]))
    return my_team


def get_questions_of_quiz(quiz_id):
    """gets the questions to a given quiz
    :param quiz_id: The ID of the quiz that we want the questions of
    :type quiz_id: ObjectId

    :return: the list of questions (as dicts) of the quiz
    :rtype: list

     Description of the keys and their values of the returned dicts:
        :key question: The question text of a question
        :key rightAnswer: the correct answer
        :key wrongAnswers: a list of wrong answers

    """
    return list(mongo.db.question.find({"quizID": quiz_id}, {"_id": 0, "quizID": 0}))


def get_current_team(firebase_id):
    """gets all the info about a team that user with the given Firebase ID is in

        :param firebase_id: the Firebase ID of the user
        :type firebase_id: str

        :return: A dictionary with all the information about the team
        :rtype: dict

         Description of the keys and their values of the returned dict:
            :key name: the name of the team
            :key term: the term the team was active in
            :key colour: the colour of the team
            :key members: the list of  Firebase IDs of the team members
        """
    return mongo.db.teams.find_one(
        {"members": {"$elemMatch": {"$eq": firebase_id}}, "term": get_current_term()},
        {"_id": 0},
    )


def get_colour_of_team(team_name):
    """gets the colour of a given team, the team has to be active this term

    :param team_name: the name of the team (is unique per term)
    :type team_name: str

    :return: the team's colour
    :rtype: str
    """
    result = mongo.db.teams.find_one({"name": team_name, "term": get_current_term()})
    if result is None:
        return "#212121"
    return result["colour"]


def get_escaped_by_db(text):
    """This is a function that purely exists because we had a lot of problems with the encoding format of the TUM API.
    Essentially, what happened is that no matter how we encoded/decoded the infos received by the TUM API prior to
    saving the values into the database, Umlaute would be shown incorrectly in the database, since there seems to be
    some encoding / decoding going on between python & mongodb. Therefore, the only way to correctly display our
    Umlaute, we need to use this function: We put a given text with Umlaute into the database (where an error with the
    encoding/decoding happens) and immediately read the entry from the database to see what went wrong with the
    decoding/encoding. Then we simply need to fix that (later, not in this function) in order for it to work.
    (For some odd reason, without writing the string into the database before fixing first, it will not work)

    :param text: The text received from TUM API
    :type text: str

    :return: The text that has been corrupted by mongodb
    :rtype: str

    """
    mongo.db.text.insert_one({"text": text})
    returned_text = mongo.db.text.find_one()["text"]
    mongo.db.text.delete_many({})
    return returned_text


def get_quiz_info(quiz_id):
    """get the quiz info of a quiz with Quiz ID quiz_id

    :param quiz_id: the ID of the quiz we want to know the info of
    :type quiz_id: ObjectId or str

    :return: the info of the quiz stored in a dict
    :rtype: dict

    Description of the keys and their values of the returned dict:
        :key _id: The generated id of the quiz
        :key createdBy: The person that created the quiz
        :key creationDate: The creation date of the quiz
        :key name: The name of the quiz
        :key campusID: The ID of the campus the quiz belongs to, optional
        :key lectureID: the ID of the lecture the quiz belongs to, optional
    """
    if isinstance(quiz_id, str):
        quiz_id = ObjectId(quiz_id)
    return mongo.db.quiz.find_one({"_id": quiz_id})


def get_number_of_players():
    """gets the number of players that play our game

    :return: the number of players our game has
    :rtype: int
    """
    return len(list(mongo.db.firebase_users.find()))


def get_all_lecture_names():
    """gets all the lectures (but only the names & id)

    :return: the lecture name & id as a dict
    :rtype: dict
    """
    return mongo.db.lecture.find({}, {"name": 1})


if __name__ == '__main__':
    pass
