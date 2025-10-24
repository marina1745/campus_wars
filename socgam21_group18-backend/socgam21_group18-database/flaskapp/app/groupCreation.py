"""
This file contains the functions to separate our users into teams. They all are based on a graph with users as nodes
but use different python packages or algorithms for this. We included the old functions, that we no longer use.
The main functions take the data directly from the database
"""

import networkx as nx
import pulp

from apis.v1.utils.random_stuff import get_random_color, generate_team_name, used_names
from dataclasses import dataclass
from apis.v1.database import interface
import variables
from typing import Any
from random import choice
import metis

threshold = 100
finished = True

MIN_GROUP_SIZE = 4
MAX_GROUP_SIZE = 6
MIN_GROUP_SIZE_NO_MAX = 5
@dataclass
class Group:
    """
    A team or group has 3 distinct values when we create them: the name, the color to be displayed and a list of the
    members
    """

    name: str
    color: str
    members: Any


def wedding_seating():
    """
    uses the wedding seating problem and solution to match users to groups
    (see https://coin-or.github.io/pulp/CaseStudies/a_set_partitioning_problem.html)
    gets the lectures and users from the db,
    and saves the new groups with a random name and color to the db
    This function takes a very long time to run so we no longer use it in our productive code.

    :return: tuple (bool, Team): the bool says whether the insertion of the new teams into the db was successful and
    if not, the second entry of the tuple said which was the team where the insertion failed
    :rtype: tuple
    """
    social_network = get_graph()

    max_groups = get_max_groups(social_network, MIN_GROUP_SIZE)

    # create list of all possible tables
    possible_groups = []
    for i in range(MIN_GROUP_SIZE, MAX_GROUP_SIZE + 1):
        possible_groups.extend(
            [tuple(c) for c in pulp.combination(social_network.nodes, i)]
        )

    # create a binary variable to state that a table setting is used
    x = pulp.LpVariable.dicts(
        "table", possible_groups, lowBound=0, upBound=1, cat=pulp.LpInteger
    )
    seating_model = pulp.LpProblem("Wedding-Seating-Model", pulp.LpMinimize)
    seating_model += sum(
        [happiness(group, social_network) * x[group] for group in possible_groups]
    )

    # specify the maximum number of groups
    seating_model += (
        sum([x[group] for group in possible_groups]) <= max_groups,
        "Maximum_number_of_tables",
    )
    for user in social_network.nodes:
        seating_model += (
            sum([x[group] for group in possible_groups if user in group]) == 1,
            "Must_seat_%s" % user,
        )
    seating_model.solve()
    user_groups = []
    for group in possible_groups:
        if x[group].value() == 1.0:
            user_groups.append(Group(generate_team_name(), get_random_color(), group))
    # return user_groups
    variables.finished = True
    return interface.add_new_teams(user_groups)


def happiness(group, social_network):
    """
    find the happiness or sum of edge weights a group of people have in between themselves
    :param social_network: complete nx graph with edges, nodes and weight on edges
    :param group: list of node names to check
    :return: sum of all weights between the group
    """
    return_value = 0
    for i in range(0, len(group) - 1):
        for j in range(i + 1, len(group)):
            if social_network.has_edge(group[i], group[j]):
                return_value += social_network[group[i]][group[j]]["weight"]
    return return_value


def alternative_calculation():
    """
    initial alternative calculation, since wedding_seating is way too inefficient.
    the players are divided into all possible amount of groups based on the min / max group size
    Then, players will swap into groups as long as a swap is found that would increase the average happiness of
    groups.
    :return: tuple (bool, Team): the bool says whether the insertion of the new teams into the db was successful and
    if not, the second entry of the tuple said which was the team where the insertion failed
    :rtype: tuple
    """
    biggest_change = -1
    social_network = get_graph()

    current_partition = [
        list(social_network.nodes)[x: x + MIN_GROUP_SIZE]
        for x in range(0, social_network.number_of_nodes(), MIN_GROUP_SIZE)
    ]
    if len(current_partition[len(current_partition) - 1]) != MIN_GROUP_SIZE:
        last_entries = current_partition.pop()
        for i in range(0, len(last_entries)):
            current_partition[i].append(last_entries[i])
    should_swap_again = True
    copied_list = []
    for i in current_partition:
        copied_list.append(i[:])

    while should_swap_again:
        next_swap = find_next_swap(
            social_network, current_partition, MIN_GROUP_SIZE, MAX_GROUP_SIZE
        )

        if next_swap["sum"] == 0:
            break
        if biggest_change < next_swap["sum"]:
            biggest_change = next_swap["sum"]
        if next_swap["sum"] < biggest_change * 0.01:
            should_swap_again = False
        if not next_swap["player1"] is None:
            player = current_partition[next_swap["partition1"]].pop(
                next_swap["player1"]
            )
            current_partition[next_swap["partition2"]].append(player)
        if not next_swap["player2"] is None:
            player = current_partition[next_swap["partition2"]].pop(
                next_swap["player2"]
            )
            current_partition[next_swap["partition1"]].append(player)

    teams = []
    for group in current_partition:
        teams.append(Group(generate_team_name(), get_random_color(), group))
    variables.finished = True
    return interface.add_new_teams(teams)


def find_next_swap(graph, current_partition, min_size, max_size):
    """
    finds the next swap based on a current partition
    next swap: the swapping of a player into another team or two players of different teams swapping their places
               only if a swap would result in the increase of the average happiness of the teams, it is considered
               the highest increase will be chosen as the next swap
    :param graph: the graph that describes who has a relationship (and how string is it) to whom
    :param current_partition: how the players are currently partitioned into groups
    :param min_size: the min_size of members that a group can't go under
    :param max_size: the max_size of members that a group can't go over
    :return: the next swap that should be performed, described as a dict
    :rtype: dict
    """
    best_result = get_best_result_as_dict(0, None, None, None, None)
    for i, p in enumerate(current_partition):
        for j, p2 in enumerate(current_partition):
            if i == j:
                continue
            old_sum = happiness(p, graph) + happiness(p2, graph)
            if len(p) > min_size and len(p2) < max_size:
                for k in range(0, len(p)):
                    new_p = p[:]
                    pl1 = new_p.pop(k)
                    new_p2 = p2[:]
                    new_p2.append(pl1)
                    new_sum = happiness(new_p, graph) + happiness(new_p2, graph)
                    if new_sum - old_sum > best_result["sum"]:
                        best_result = get_best_result_as_dict(
                            new_sum - old_sum, k, None, i, j
                        )
            if i <= j:
                for k in range(0, len(p)):
                    for m in range(0, len(p2)):
                        new_p = p[:]
                        new_p2 = p2[:]
                        pl2 = new_p2.pop(m)
                        pl1 = new_p.pop(k)
                        new_p.append(pl2)
                        new_p2.append(pl1)
                        new_sum = happiness(new_p, graph) + happiness(new_p2, graph)
                        if new_sum - old_sum > best_result["sum"]:
                            best_result = get_best_result_as_dict(
                                new_sum - old_sum, k, m, i, j
                            )

    return best_result


def get_best_result_as_dict(sum, pl1, pl2, part1, part2):
    """returns a dict with the values given"""
    return {
        "sum": sum,
        "player1": pl1,
        "player2": pl2,
        "partition1": part1,
        "partition2": part2,
    }


def get_graph():
    """
    creates a nx graph with users as nodes and edges, when 2 students attended the same lecture. The edge weights are
    higher the more lectures the two users share and the less users in total attended the lecture
    :return: nx.Graph described above
    """
    used_names.clear()
    lectures = {}
    lecture_list = interface.get_all_lecture_ids()
    for lecture in lecture_list:
        lectures[lecture] = interface.get_users_of_lecture(lecture)
    social_network = nx.Graph()
    for title, attendants in lectures.items():
        social_network.add_nodes_from(attendants)

    for users in lectures.values():
        for i in range(0, len(users) - 1):
            for j in range(i + 1, len(users)):
                if social_network.has_edge(users[i], users[j]):
                    social_network[users[i]][users[j]]["weight"] += 1 / len(users)
                    social_network[users[i]][users[j]]["counter"] += 1
                else:
                    social_network.add_edge(
                        users[i], users[j], weight=(1 / len(users)), counter=1
                    )
    loners = nx.isolates(social_network)
    for user in list(loners):
        other_nodes = list(social_network.nodes())[:]
        other_nodes.remove(user)
        social_network.add_edge(
            user, choice(other_nodes), weight=0.0001, counter=1
        )
        social_network.add_edge(
            user, choice(other_nodes), weight=0.0001, counter=1
        )
    for u, v, d in social_network.edges(data=True):
        d["weight"] = d["weight"] / d["counter"]
    return social_network


def get_max_groups(social_network, min_group_size=4):
    """
    This function calculates how many teams we have to create
    :param social_network: nx.Graph of our users
    :param min_group_size: minimum amount of players in every group
    :return: the number of max groups
    :rtype: int
    """
    if len(social_network.nodes) % min_group_size > 0:
        max_groups = int((len(social_network.nodes) / min_group_size) + 1)
    else:
        max_groups = int(len(social_network.nodes) / min_group_size)
    return max_groups


def metis_calulation():
    """
    uses the c library metis for efficient partitioning based on weighted graph
    :return: tuple (bool, Team): the bool says whether the insertion of the new teams into the db was successful and
    if not, the second entry of the tuple said which was the team where the insertion failed
    :rtype: tuple
    """
    social_network = get_graph()
    social_network.graph["edge_weight_attr"] = "weight"
    # for metis to use the weights, they have to be int
    for u, v, d in social_network.edges(data=True):
        d["weight"] = int(d["weight"] * 10000)
    max_groups = get_max_groups(social_network, MIN_GROUP_SIZE_NO_MAX)
    (edgecuts, parts) = metis.part_graph(social_network, max_groups)
    teams = []
    for i in range(0, max_groups):
        teams.append([])
    j = 0
    for node in social_network.nodes():
        teams[parts[j]].append(node)
        j = j + 1
    user_groups = []
    for team in teams:
        user_groups.append(Group(generate_team_name(), get_random_color(), team))
    variables.finished = True
    return interface.add_new_teams(user_groups)


def greedy_random():
    """
    Selects amount of teams random users and adds the strongest connected not yet matched users to their group, if
    None are available, selects a random one. This is an alternative algorithm, that is not as good as the other,
    but with over 10000 users the runtime is a lot better, so we keep it as a backup.
    :return: tuple (bool, Team): the bool says whether the insertion of the new teams into the db was successful and
    if not, the second entry of the tuple said which was the team where the insertion failed
    :rtype: tuple
    """
    social_network = get_graph()
    max_groups = get_max_groups(social_network, MIN_GROUP_SIZE_NO_MAX)
    all_users = list(social_network.nodes)
    teams = []
    graphs = []
    for i in range(0, max_groups):
        teams.append([])
        graphs.append(social_network.copy())
        start_user = choice(all_users)
        teams[i].append(start_user)
        all_users.remove(start_user)
    for i in range(0, max_groups):
        for j in range(1, max_groups):
            graphs[i].remove_node(teams[j % max_groups][0])
    j = 0
    while len(all_users) > 0:
        edges = graphs[j].edges(teams[j][0])
        if len(edges) >= 1:
            friends = sorted(
                graphs[j].edges(teams[j][0], data=True),
                key=lambda x: x[2]["weight"],
                reverse=True,
            )
            best_friend = friends[0][1]
            teams[j].append(best_friend)
            for graph in graphs:
                graph.remove_node(best_friend)
            all_users.remove(best_friend)
        else:
            random_user = choice(all_users)
            teams[j].append(random_user)
            for graph in graphs:
                graph.remove_node(random_user)
            all_users.remove(random_user)
        j = (j + 1) % max_groups
    user_groups = []
    for team in teams:
        user_groups.append(Group(generate_team_name(), get_random_color(), team))
    variables.finished = True
    interface.add_new_teams(user_groups)
