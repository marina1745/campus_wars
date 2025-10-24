# CampusWarsBackend

RestAPI and Database Backend for CampusWars.

---

## Description

Provides a REST API for the CampusWars App. It manages dynamic data like current user status and games as well as static databases for users, teams, lecturehalls or schedules

## Installation

Clone the *flaskapp* folder wherever you want (and have docker and docker-compose installed)

## Usage

Run `docker-compose up --build`  from within the *flaskapp* folder if you directly want to see the log output otherwise add the `-d` flag to omit that.

Also it is recommended to run `docker image prune -f` to delete any dangling images. This can sometimes safe up to ~500 MB of data.

On the server there is also a `./update.sh` script in the home directory of the *campuswars* user. This will do all the pulling, building and pruning for you.

## License

[MIT](https://choosealicense.com/licenses/mit/)

