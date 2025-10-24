#!/usr/bin/python
# -*- coding: utf-8 -*-

__date__ = "2021-05-13"
__version__ = "0.0.1"

__all__ = ("create_app",)

from os import environ
from config import ProductionConfig
from flask import Flask, jsonify, request, abort
from apis.v1.routes import v1
from apis.v1 import mongo
import variables


def check_ua(ua, candidates) -> bool:
    """Check if the User-Agent is known."""
    for uac in candidates:
        if uac in ua:
            return True
    return False


# The first idea was to return a random response code when we don't expect the User-Agent
# TODO maybe return a random response later, for now 418 seems fine if the UA check fails
STATUS = [
    204, 205, 206,
    300, 301, 302, 303, 304, 305, 307, 308,
    400, 402, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 425, 426, 428, 429, 431, 451,
]


def create_app():
    """Creates Flask application."""
    app = Flask(__name__)
    app.config.from_object(ProductionConfig)
    app.register_blueprint(v1, url_prefix="/v1")
    variables.finished = True
    app.config["MONGO_URI"] = (
        f"mongodb://"
        f"{environ['MONGODB_USERNAME']}:{environ['MONGODB_PASSWORD']}"
        f"@{environ['MONGODB_HOSTNAME']}:27017/{environ['MONGODB_DATABASE']}"
    )
    mongo.init_app(app)

    @app.before_request
    def ua_check():
        """Actual check if the User-Agent is know to us."""
        if (not request.url_rule
                or "live-debug" not in request.url_rule.rule
                and not check_ua(
                    request.headers.get("User-Agent", "NONE"),
                    ["CampusWarsFrontend", "Postman", "Android"]
                )
        ):
            abort(418)

    @app.after_request
    def header(response):
        """Add some headers (That get probably overwritten by nginx.)"""
        response.headers["Server"] = "CampusWarsBackend/0.0.1"
        response.headers["X-Robots-Tag"] = "noindex, noarchive"
        response.headers["Accept"] = "application/json"
        response.headers["Access-Control-Allow-Origin"] = "*"
        return response

    return app


if __name__ == "__main__":
    application = create_app()
    ENVIRONMENT_DEBUG = environ.get("APP_DEBUG", True)
    ENVIRONMENT_PORT = environ.get("APP_PORT", 5000)
    application.run(host="0.0.0.0", port=ENVIRONMENT_PORT, debug=ENVIRONMENT_DEBUG)
