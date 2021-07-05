import string
from typing import List, Iterable

import nltk
import numpy as np
from gensim.models import doc2vec
from nltk.corpus import stopwords


def snake_case_split(identifier: str) -> List[str]:
    return list(filter(lambda s: s != '', identifier.strip().split('_')))


def camel_case_split(identifier: str) -> List[str]:
    result: List[str] = []
    word: List[str] = []
    for i, ch in enumerate(identifier):
        if ch.isupper():
            is_word_begin = (i < len(identifier) - 1 and identifier[i + 1].islower()) \
                            or (i > 0 and identifier[i - 1].islower())
            if is_word_begin:
                if len(word) > 0:
                    result.append(''.join(word))
                    word.clear()
        word.append(ch)
    if len(word) > 0:
        result.append(''.join(word))
    return result


def split_by_notation(identifier: str) -> List[str]:
    by_snake_case = snake_case_split(identifier)
    result: List[str] = []
    for word in by_snake_case:
        result.extend(camel_case_split(word))
    return result


def split_list_by_notation(identifiers: Iterable[str]) -> List[str]:
    result: List[str] = []
    for identifier in identifiers:
        result.extend(split_by_notation(identifier))
    return result


def text_to_tokens(text: str) -> List[str]:
    text = text.replace('\r', '').replace('\n', '')
    table = str.maketrans('', '', string.punctuation)
    translated_text = text.lower().translate(table)
    return list(filter(lambda token: token not in stopwords.words('english'),
                       nltk.word_tokenize(translated_text)))


def lines_to_tokens(lines: Iterable[str]) -> List[str]:
    tokens: List[str] = []
    for line in lines:
        tokens.extend(text_to_tokens(line))
    return tokens


def train_model_by_tokens(documents_in_tokens: Iterable[Iterable[str]],
                          vector_size: int, min_count: int):
    documents = [doc2vec.TaggedDocument(tokens, [i])
                 for i, tokens in enumerate(documents_in_tokens)]
    return doc2vec.Doc2Vec(documents, vector_size=vector_size, min_count=min_count)


def document_to_vec(content: Iterable[str], vec_size: int):
    tokens_by_line: List[List[str]] = [text_to_tokens(line) for line in content]
    documents = [doc2vec.TaggedDocument(tokens, [i]) for i, tokens in enumerate(tokens_by_line)]
    model = doc2vec.Doc2Vec(documents, vector_size=vec_size, min_count=1)
    doc_words: List[str] = []
    for tokens in tokens_by_line:
        doc_words.extend(tokens)
    return np.array(model.infer_vector(doc_words))
