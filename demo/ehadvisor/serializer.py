from typing import Iterable

from .data_structure import Method


def method_to_txt(method: Method) -> str:
    qualified_name = f'{method.qualified_class_name}${method.method_name}'
    params = ''.join([f'@{param}' for param in method.param_types])
    return qualified_name + params


def method_chain_to_txt(chain: Iterable[Method], with_tail=True) -> str:
    return '->'.join(map(str, chain)) + ('->' if with_tail else '')
