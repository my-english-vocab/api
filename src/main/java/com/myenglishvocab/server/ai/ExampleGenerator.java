package com.myenglishvocab.server.ai;

import com.myenglishvocab.server.ai.dto.ExamplePair;

public interface ExampleGenerator {

    ExamplePair generate(String term, String definition);
}
