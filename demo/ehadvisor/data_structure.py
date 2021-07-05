from typing import Iterable, Optional, Tuple, List

import numpy as np

from . import nlp


class Method:
    """Method is an immutable data structure"""
    def __init__(self, package: str,
                 class_name: str,
                 method_name: str,
                 param_types: Iterable[str]):
        self.__method_name = method_name
        self.__class_name = class_name
        self.__package = package
        self.__param_types = tuple(param_types)
        params = ', '.join(param_types)
        self.__signature = f'{package}.{class_name}.{method_name}({params})'

    @property
    def package(self) -> str:
        return self.__package

    @property
    def class_name(self) -> str:
        return self.__class_name

    @property
    def method_name(self) -> str:
        return self.__method_name

    @property
    def package_depth(self) -> int:
        return self.__package.count('.') + 1

    @property
    def qualified_class_name(self):
        return f'{self.package}.{self.class_name}'

    @property
    def param_types(self) -> Tuple[str, ...]:
        return self.__param_types

    @property
    def param_num(self) -> int:
        return len(self.param_types)

    def __eq__(self, other):
        if isinstance(other, Method):
            return self.__signature == other.__signature
        return False

    def __repr__(self):
        return self.__signature

    def __hash__(self):
        return hash(self.__signature)


class ChainEntry:
    """Method & Probability"""
    def __init__(self, method: Optional[Method], probability: float):
        self.method = method
        self.probability = probability

    @property
    def is_no_handler(self) -> bool:
        return self.method is None

    def __lt__(self, other):
        return self.probability < other.probability

    def __eq__(self, other):
        if isinstance(other, ChainEntry):
            return self.method == other.method and self.probability == other.probability
        return False

    def __repr__(self):
        return f'({"no-handler" if self.is_no_handler else self.method}, {self.probability})'


class Dependency:
    def __init__(self, group_id: str, artifact_id: str):
        self.group_id = group_id
        self.artifact_id = artifact_id

    def __repr__(self):
        return f'({self.group_id}, {self.artifact_id})'


class ProjectFeature:
    def __init__(self, abstract_vec: Optional[np.ndarray] = None,
                 dependencies_vec: Optional[np.ndarray] = None):
        self.abstract_vec = abstract_vec
        self.dependencies_vec = dependencies_vec


class MethodFeature:
    def __init__(self, method: Method):
        self.method = method
        self.exception_id: Optional[int] = None
        self.throw: Optional[bool] = None
        self.comments: List[str] = []

    @property
    def docs(self) -> List[str]:
        docs = nlp.split_by_notation(self.method.class_name)
        docs.extend(nlp.split_by_notation(self.method.method_name))
        for param in self.method.param_types:
            docs.extend(nlp.split_by_notation(param))
        for comment in self.comments:
            docs.extend(nlp.text_to_tokens(comment))
        return docs

    @property
    def package_depth(self):
        return self.method.package_depth

    @property
    def param_num(self):
        return self.method.param_num


class PositionFeature:
    def __init__(self):
        self.method_top: Optional[int] = None
        self.method_bottom: Optional[int] = None
        self.class_top: Optional[int] = None
        self.class_bottom: Optional[int] = None
        self.package_top: Optional[int] = None
        self.package_bottom: Optional[int] = None

    def is_valid(self):
        to_check = (
            self.method_top,
            self.method_bottom,
            self.class_top,
            self.class_bottom,
            self.package_top,
            self.package_bottom
        )
        return all(map(lambda x: x is not None, to_check))
