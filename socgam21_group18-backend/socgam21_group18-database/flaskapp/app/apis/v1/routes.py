#!/usr/bin/python
# -*- coding: utf-8 -*-

__date__ = "2021-05-13"
__version__ = "0.0.1"

"""Url routes for the server."""

__all__ = ("v1",)

from flask import jsonify, request, make_response
from flask_restful import Resource
from apis.v1 import v1, api
import groupCreation
import variables
from apis.v1.decorators import request_requires, check_timed_out_users
import json
import threading
from apis.v1.database import interface
from apis.v1.database.interface import (
    get_all_rooms,
    find_closest_room,
    add_lectures_to_user,
    add_question_to_quiz,
    add_user,
    get_full_name_of_current_lecture_in_room,
    get_player_name,
    get_time_table_of_room,
    get_escaped_by_db,
    get_current_team_with_member_names,
    get_colour_of_team,
)
from contextlib import suppress
from apis.v1.database.live_data import LiveData
import ftfy

live_data = LiveData()

ROOMFINDER_DISTANCE: int = 50


@v1.app_errorhandler(404)
def error_404(_):
    """404 error handling, write response"""
    return make_response(jsonify({"exception": "Not found!", "code": 404}), 404)


@api.resource("/roomfinder")
class RoomFinder(Resource):
    @check_timed_out_users(live_data.timedout_users)
    @request_requires(headers=["uid", "team", "latitude", "longitude"])
    def post(self):
        """

        This function has to be called regularly by a player that is currently actively playing; When calling this
        function, the player is queued in the queue of the room closest to them.

        If they don't call the function in a certain interval, they will be removed from the queue
        The request requires:
            :key uid: the Firebase ID of the user
            :key team: the team name of the team that the user is in
            :key latitude: the current latitude of the player's current position
            :key longitude: the current longitude of the player's current position

        If the player is close enough to a room / lecture hall, the player will get information about that room returned
        :return: room information
        :rtype: dict

        The returned dict contains following information:
            :key occupancy: the occupancy status of the room
            :key occupier: the current occupier of the room
            :key room_name: the name of the room
            :key lid: the lecture hall/ room ID of the room
            :key multiplier: the current multiplier factor applied to the room
            :currentLecture: the name of the current lecture held in the room

        """
        (
            lat,
            lon,
        ) = map(float, [request.headers["longitude"], request.headers["latitude"]])
        room = find_closest_room(lat, lon, ROOMFINDER_DISTANCE)
        if room is None:
            return jsonify({"message": "nothing near you"})
        room_name = room["roomName"]
        live_data.room_queue(
            uid=request.headers["uid"], team=request.headers["team"], room=room_name
        )
        occupier = live_data.room_queue.get_each_rooms_occupiers()[room_name]
        occupier_team = occupier.team
        occupier_multiplier = occupier.multiplier
        team_occupancy = live_data.room_queue.get_each_rooms_occupancies()[room_name]
        with suppress(KeyError):
            team_occupancy[occupier_team] *= occupier_multiplier

        return_room = {
            "message": "closest room to you:",
            "occupancy": team_occupancy,
            "occupier": occupier_team,
            "room_name": room_name,
            "lid": str(room["_id"]),
            "multiplier": occupier_multiplier,
            "currentLecture": get_full_name_of_current_lecture_in_room(
                str(room["_id"])
            ),
        }

        if return_room["currentLecture"] is None:
            return_room["currentLecture"] = "currently no lecture"
        return jsonify(return_room)

    def get(self):
        """
        Get a list of all lecture halls/rooms with information about current states
        information received per room:
            :key _id: the generated id of the room from the database
            :key currentLecture: the lecture currently held in the room
            :key occupier: the team & colour of the team that currently occupies the room
            :key roomName: the name of the room
            :key timetable: the schedule of the room, sorted by time
        """
        result = []
        for room in get_all_rooms():
            occupier = live_data.room_queue.get_each_rooms_occupiers()[
                room["roomName"]
            ].team
            if occupier:
                color = get_colour_of_team(occupier)
            else:
                occupier = "Nobody"
                color = "#212121"
            timetable = get_time_table_of_room(room["_id"])
            item = {
                "location": {
                    "longitude": room["location"]["coordinates"][0],
                    "latitude": room["location"]["coordinates"][1],
                },
                "roomName": room["roomName"],
                "_id": str(room["_id"]),
                "occupier": {"color": color, "name": occupier},
                "currentLecture": get_full_name_of_current_lecture_in_room(room["_id"]),
                "timetable": timetable,
            }
            result.append(item)
        return jsonify(result)


@api.resource("/quiz-request")
class QuizRequest(Resource):
    @check_timed_out_users(live_data.timedout_users)
    @request_requires(headers=["uid", "team", "room"])
    def post(self):
        """When this function is called, it indicates that the player with Firebase ID uid wants to participate in
        a quiz
        header param description:
            :key uid: the Firebase ID of the user
            :key team: the team name of the user
            :key room: the current room that the user is in
        """
        if request.headers["uid"] not in live_data.room_queue:
            return jsonify({"quiz-request": False, "reason": "not in a room"})
        live_data.quiz_queue(
            request.headers["uid"], request.headers["team"], request.headers["room"]
        )
        return jsonify({"quiz-request": True})


@api.resource("/quiz-refresh")
class QuizRefresh(Resource):
    @check_timed_out_users(live_data.timedout_users)
    @request_requires(headers=["uid", "team", "room", "lid"])
    def post(self):
        """
        Refresh quiz state and maybe or join a game.
        First, do the same thing as with quiz-request, but then also check if it was possible to match you with
        an opponent
        if yes, the game information will be returned
        """
        # I know it is the same as above, but we might
        # have to something different here later
        if request.headers["uid"] not in live_data.room_queue:
            return jsonify({"quiz-request": False, "reason": "not in a room"})
        result = live_data.quiz_queue(
            request.headers["uid"],
            request.headers["team"],
            request.headers["room"],
            request.headers["lid"],
        )
        if result:
            (
                descriptor,
                game,
            ) = result
            if game:
                return jsonify(
                    {

                        "gid": game.game_id,  # game_id: a 24 byte string to identify each game
                        "pid": game.get_player_id(request.headers["uid"]),  # 0 or 1 identifies player in game
                        "opp-name": get_player_name(
                            game.players[not game.get_player_id(request.headers["uid"])].uid
                        ),
                        # The opposite of the players pid, since they are only 0 and 1, we treat them like bools
                        "opp-team": game.players[not game.get_player_id(request.headers["uid"])].team,
                        "name": game.quiz_name,
                        "quiz": game.question,  # quiz in the already specified format
                        "game-ready": descriptor == "game",  # unimportant

                    }
                )

        return jsonify({"nothing": True})


@api.resource("/quiz-answer")
class QuizAnswer(Resource):
    @check_timed_out_users(live_data.timedout_users)
    @request_requires(headers=["uid", "gid", "pid", "result", "outcome"])
    def post(self):
        """
        Answer the quiz
        """
        if request.headers["gid"] not in live_data.game_queue:
            return jsonify({"quiz-request": False, "reason": "invalid gid"})
        if not live_data.game_queue[request.headers["gid"]].is_player_in_game:
            return jsonify({"quiz-request": False, "reason": "player not in this game"})
        # live_data.game_queue.refresh(request.headers['gid'])
        live_data.game_queue.submit_answer(
            request.headers["gid"],
            int(request.headers["pid"]),
            int(request.headers["outcome"]),
        )
        return jsonify({"quiz-answer": True})


@api.resource("/quiz-state")
class QuizState(Resource):
    @check_timed_out_users(live_data.timedout_users)
    @request_requires(headers=["uid", "gid", "pid"])
    def get(self):
        """
        Ask the server if the other player has answered yet, if yes show result
        """
        if request.headers["gid"] not in live_data.game_queue:
            return jsonify({"quiz-state": False, "reason": "invalid gid"})
        if not live_data.game_queue[request.headers["gid"]].is_player_in_game:
            return jsonify({"quiz-state": False, "reason": "player not in this game"})
        if live_data.game_queue[request.headers["gid"]].finished[
            int(request.headers["pid"])
        ]:
            return jsonify({"quiz-state": False, "reason": "already answered"})
        # live_data.game_queue.refresh(request.headers['gid'])
        if live_data.game_queue[request.headers["gid"]].all_answered:
            result = live_data.game_queue[request.headers["gid"]].get_result_for_player(
                int(request.headers["pid"])
            )
            live_data.game_queue[request.headers["gid"]].set_finished(
                int(request.headers["pid"])
            )
            if live_data.game_queue[request.headers["gid"]].is_finished:
                with suppress(KeyError):
                    del live_data.game_queue[request.headers["gid"]]
            if result == "LOST":
                live_data.timedout_users(request.headers["uid"])
                with suppress(KeyError):
                    del live_data.room_queue[request.headers["uid"]]
            return jsonify({"quiz-state": True, "result": result})

        return jsonify({"quiz-state": False, "reason": "not all players answered yet"})


@api.resource("/rally")
class Rally(Resource):
    @check_timed_out_users(live_data.timedout_users)
    @request_requires(headers=["uid", "team", "room"])
    def post(self):
        """
        Manage Rally request
        """
        if live_data.rally_timeout(
                request.headers["team"],
                request.headers["room"],
                get_player_name(request.headers["uid"]),
        ):
            return {"rally": True}
        return {"rally": False, "reason": "already rallying"}

    @check_timed_out_users(live_data.timedout_users)
    @request_requires(headers=["team"])
    def get(self):
        """
        Manage Rally request
        """
        return {"rally": live_data.rally_timeout.info(request.headers["team"])}


@api.resource("/lectures")
class Lectures(Resource):
    @request_requires(headers=["lectures", "encodingformat", "uid"])
    def post(self):
        """
        Tell the server what lectures you visited in your time at TUM
        """
        lectures = json.loads(request.headers["lectures"])
        lectures_list = []
        for lec in lectures:
            lectures_list.append(
                ftfy.fix_text(
                    get_escaped_by_db(
                        lec.encode(request.headers["encodingformat"]).decode("utf-8")
                    )
                )
            )
        if groupCreation.threshold < interface.get_number_of_players():
            group_creation = threading.Thread(target=groupCreation.metis_calulation)
            group_creation.start()
            groupCreation.threshold = groupCreation.threshold * 1.6
        return add_lectures_to_user(request.headers["uid"], lectures_list)


@api.resource("/mygroup")
class MyGroup(Resource):
    @request_requires(headers=["uid"])
    def get(self):
        """
        Tells you your team name/color and other members
        """
        return jsonify(get_current_team_with_member_names(request.headers["uid"]))


@api.resource("/start")
class Start(Resource):
    @request_requires(headers=["passphrase", "variant"])
    def post(self):
        """
        Triggers the matching algorithm
        """
        if request.headers["passphrase"] == "YOU ONLY CALL THIS TWICE A YEAR PLS":
            variables.finished = False
            if request.headers["variant"] == "pulp":
                group_creation = threading.Thread(target=groupCreation.wedding_seating)
            elif request.headers["variant"] == "greedy":
                group_creation = threading.Thread(target=groupCreation.greedy_random)
            elif request.headers["variant"] == "alternative":
                group_creation = threading.Thread(target=groupCreation.alternative_calculation)
            else:
                group_creation = threading.Thread(target=groupCreation.metis_calulation)

            group_creation.start()
            return jsonify({"message": "Started group creation"})
        return jsonify({"message": "You are not allowed to restart"})


@api.resource("/question")
class Question(Resource):
    @request_requires(headers=["question", "right_answer", "wrong_answers", "quiz_id"])
    def post(self):
        """
        Add a question to the database
        """
        status = add_question_to_quiz(
            request.headers["question"],
            request.headers["right_answer"],
            request.headers["wrong_answers"],
            request.headers["quiz_id"],
        )
        return jsonify({"success": status})


@api.resource("/register")
class Register(Resource):
    @request_requires(headers=["uid", "name"])
    def post(self):
        """
        Register with name and user id
        """
        status = add_user(request.headers["uid"], request.headers["name"])

        return jsonify({"success": status})


@api.resource("/timetable")
class TimeTable(Resource):
    @request_requires(headers=["room_id"])
    def post(self):
        """
        Tells the current schedule of lectures for the room
        """
        return get_time_table_of_room(request.headers["room_id"])


@api.resource("/marina")
class Test(Resource):
    """Test route for marina, ignore for grading"""

    def get(self):
        return {"finished": variables.finished, "rest": groupCreation.alternative_calculation()}


@api.resource("/robin")
class ClearLiveData(Resource):
    """test route for robin, ignore for grading"""
    base_entries = {"room", "quiz", "game", "timeout", "rally"}

    # entries in the headers exclude items from getting reset
    def post(self):
        live_data(ClearLiveData.base_entries.intersection(request.headers))
        return jsonify({"clear": True})


@api.resource("/live-debug")
class LiveDebug(Resource):
    """debug route for live data; shows current live data"""

    def get(self):
        return jsonify(live_data.json)


@api.resource("/echo")
class Echo(Resource):
    def get(self):
        """echo for frontend so they can check whether our server is alive"""
        return "Hallo Echo!", 200


if __name__ == "__main__":
    pass
