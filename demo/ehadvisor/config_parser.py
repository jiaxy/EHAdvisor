import os
import re
import xml.dom.minidom
from typing import List, Optional

from .data_structure import Dependency


def dependencies_in_pom_xml(filepath: str) -> List[Dependency]:
    """Extract dependencies in pom.xml"""
    dependencies: List[Dependency] = []
    dom = xml.dom.minidom.parse(filepath)
    root = dom.documentElement
    dependency_tags = root.getElementsByTagName('dependency')
    if len(dependency_tags) > 0:
        for tag in dependency_tags:
            group_id_tags = tag.getElementsByTagName('groupId')
            artifact_id_tags = tag.getElementsByTagName('artifactId')
            if len(group_id_tags) > 0 and len(artifact_id_tags) > 0:
                group_id = tag.getElementsByTagName('groupId')[0].firstChild.data
                artifact_id = tag.getElementsByTagName('artifactId')[0].firstChild.data
                assert type(group_id) == str and type(artifact_id) == str
                dependencies.append(Dependency(group_id, artifact_id))
    return dependencies


def dependencies_in_build_gradle(filepath: str) -> List[Dependency]:
    """a naive way to extract dependencies in build.gradle"""

    def parse_line(dep_line: str) -> Optional[Dependency]:
        """
        :param dep_line: something like "implementation 'androidx.appcompat:appcompat:1.2.0'"
        """
        dep_line = dep_line.replace('\'', '"')
        match = re.search('"(.+):(.+):.+"', dep_line)
        return Dependency(match[1], match[2]) if match is not None else None

    dependencies: List[Dependency] = []
    with open(filepath) as file:
        in_dependencies = False
        for line in file:
            line = line.replace(' ', '')
            line = line.replace('\n', '')
            line = line.replace('\r', '')
            if line == 'dependencies{':
                in_dependencies = True
            elif line == '}':
                in_dependencies = False
            elif in_dependencies and len(line) > 0:
                dependency = parse_line(line)
                if dependency is not None:
                    dependencies.append(dependency)
    return dependencies


def dependencies_in_path(path: str) -> List[Dependency]:
    """Recursively extract all dependencies in the folder"""
    dependencies: List[Dependency] = []
    if os.path.isfile(path):
        filename = os.path.basename(path)
        if filename == 'pom.xml':
            dependencies.extend(dependencies_in_pom_xml(path))
        elif filename == 'build.gradle':
            dependencies.extend(dependencies_in_build_gradle(path))
    else:
        assert os.path.isdir(path)
        for child in os.listdir(path):
            child_path = os.path.join(path, child)
            dependencies.extend(dependencies_in_path(child_path))
    return dependencies
