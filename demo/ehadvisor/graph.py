from typing import Iterable, Dict, List, Set

from .data_structure import Method


def build_call_graph(chains: Iterable[List[Method]]) -> Dict[Method, List[Method]]:
    graph: Dict[Method, List[Method]] = {}

    def ensure_in_graph(m: Method):
        if m not in graph:
            graph[m] = []

    for chain in chains:
        if len(chain) == 0:
            continue
        for i, method in enumerate(chain[:-1]):
            ensure_in_graph(method)
            graph[method].append(chain[i + 1])
        ensure_in_graph(chain[-1])
    return graph


def chains_from_source(graph: Dict[Method, List[Method]], source: Method) -> List[List[Method]]:
    """
    获取从异常源出发的所有 call chain

    根据定义，异常源自身不在 call chain 内

    :param graph: call graph
    :param source: 异常源
    :return: list of call chains
    """
    chains: List[List[Method]] = []
    chain: List[Method] = []
    chain_set: Set[Method] = set()

    def dfs(u: Method):
        chain.append(u)
        chain_set.add(u)
        if len(graph[u]) == 0:  # u is a leaf
            # 异常源自身不在 call chain 中
            # slicing a list will make a copy
            chains.append(chain[1:])
        else:
            for v in graph[u]:
                if v not in chain_set:
                    dfs(v)
        assert chain[-1] == u
        chain.pop()
        chain_set.remove(u)

    dfs(source)
    return chains


def no_circle(graph: Dict[Method, List[Method]]) -> bool:
    """
    Identify if there is a circle in the call graph.
    """
    vis: Dict[Method, int] = {}

    def dfs(u: Method) -> bool:
        vis[u] = -1
        for v in graph[u]:
            if v in vis and vis[v] == -1:
                return False
            elif v not in vis:
                if not dfs(v):
                    return False
        vis[u] = 1
        return True

    for method in graph:
        if method not in vis:
            if not dfs(method):
                return False
    return True


def has_circle(graph: Dict[Method, List[Method]]) -> bool:
    return not no_circle(graph)
