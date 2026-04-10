package com.sridhar.ragapi.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingCacheService {
    private final EmbeddingModel embeddingModel;

    public EmbeddingCacheService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Cacheable(value = "embeddings", key = "#input")
    public List<Double> getEmbedding(String input) {
        List<Double> vectors = new ArrayList<>();
        float[] vector = embeddingModel.embed(input);
        for(float d : vector){
            vectors.add((double) d);
        }
        return vectors;
    }
}
