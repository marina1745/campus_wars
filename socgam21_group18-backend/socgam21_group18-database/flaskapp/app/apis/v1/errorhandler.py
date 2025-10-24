#!/usr/bin/python
# -*- coding: utf-8 -*-

__date__ = "2021-05-15"
__version__ = "0.0.0"

__all__ = (
    "error_handler",
    "exception_factory",
)

from datetime import datetime
from flask import jsonify, request
from functools import wraps
from traceback import format_exception
import pytz
from typing import Any, Dict, Callable


def exception_factory(
    ex_type: str, description: str, url: str, status: int, tb: Any
) -> Dict[str, Any]:
    """Create a exception response."""
    return dict(
        exception=ex_type,
        description=description,
        time=datetime.now(pytz.timezone("Europe/Vienna")).timestamp(),
        url=url,
        status_code=status,
        tb=tb,
    )


def error_handler(method: Callable) -> Callable:
    """Decorator for flask routes to catch errors."""
    @wraps(method)
    def wrapper(*args, **kwargs):
        try:
            return method(*args, **kwargs)
        except Exception as ex:
            return (
                jsonify(
                    exception_factory(
                        ex.__class__.__name__,
                        str(ex),
                        request.url,
                        500,
                        "".join(format_exception(None, ex, ex.__traceback__)),
                    )
                ),
                500,
            )
    return wrapper


if __name__ == "__main__":
    pass
