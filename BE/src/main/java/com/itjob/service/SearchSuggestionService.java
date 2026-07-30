package com.itjob.service;

import java.util.List;

public interface SearchSuggestionService {

    void recordKeyword(String keyword);

    void removeKeyword(String keyword);

    List<String> getSuggestions(String prefix, int limit);
}
