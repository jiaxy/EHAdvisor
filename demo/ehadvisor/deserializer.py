import re
from typing import List

from .data_structure import Method


def method_from_txt(txt: str) -> Method:
    match = re.search(r'([^$]+)\$([^@]+)(.*)', txt.strip())
    assert match is not None
    qualified_class_name = match[1]
    method_name = match[2]
    params = match[3]
    name_split = qualified_class_name.split('.')
    class_name = name_split[-1]
    package = '.'.join(name_split[0:-1])
    if len(params) == 0:
        param_types = []
    else:
        param_types = [p for p in params.split('@') if len(p) > 0]
    return Method(package, class_name, method_name, param_types)


def method_chain_from_txt(txt: str) -> List[Method]:
    chain = [method_from_txt(s) for s in txt.strip().split('->') if s != '']
    # FIXME 当前 link.txt 里的箭头方向是反着的，所以要反转一下
    chain.reverse()
    return chain
