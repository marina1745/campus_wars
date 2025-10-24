#!/usr/bin/python
# -*- coding: utf-8 -*-

__date__ = "2021-05-13"
__version__ = "0.0.1"

__all__ = (
    "v1",
    "api",
    "mongo",
    "db",
)

from flask import Blueprint, current_app
from flask_restful import Api
from flask_mongoengine import MongoEngine
from flask_pymongo import PyMongo
from apis.v1.errorhandler import error_handler

v1 = Blueprint("v1", __name__)

api = Api(v1)

api.decorators.append(error_handler)

mongo = PyMongo(uri="mongodb://marina:vmNJcntLwoi@mongodb:27017/campuswarsdb")
db = mongo.db

if __name__ == "__main__":
    pass
