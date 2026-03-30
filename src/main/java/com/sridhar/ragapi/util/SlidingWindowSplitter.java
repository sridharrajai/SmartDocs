package com.sridhar.ragapi.util;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class SlidingWindowSplitter {

    private static final int CHUNK_SIZE = 512;
    private static final int OVERLAP = 50;
    private static final int MIN_CHUNK_CHARS = 50;

    private final Encoding encoding = Encodings
            .newDefaultEncodingRegistry()
            .getEncoding(EncodingType.CL100K_BASE);

    public List<Document> split(List<Document> pages) {
        List<Document> chunks = new ArrayList<>();

        for (Document page : pages) {
            String text = page.getText();

            // skip blank or null pages
            if (text == null || text.isBlank()) {
                continue;
            }

            // convert page text to token IDs once
            IntArrayList tokens = encoding.encode(text);
            int[] tokenArray = tokens.toArray(); // convert once outside loop
            int start = 0;

            while (start < tokenArray.length) {

                // Step 1 — slice the next 512 tokens (or remainder)
                int end = Math.min(start + CHUNK_SIZE, tokenArray.length);
                int[] slice = Arrays.copyOfRange(tokenArray, start, end);

                // Step 2 — convert slice back to IntArrayList for decode
                IntArrayList sliceList = new IntArrayList();
                for (int token : slice) {
                    sliceList.add(token);
                }
                String chunkText = encoding.decode(sliceList);

                // Step 3 — snap to nearest sentence boundary
                chunkText = snapToSentenceBoundary(chunkText);

                // Step 4 — re-encode snapped text to get actual token count
                // needed so overlap is always exactly 50 tokens from real end
                int actualTokenCount = encoding.encode(chunkText).size();

                // Step 5 — store chunk only if meaningful length
                if (chunkText.trim().length() >= MIN_CHUNK_CHARS) {
                    chunks.add(new Document(chunkText, page.getMetadata()));
                }

                // Step 6 — slide window forward by (actual - overlap)
                // this ensures next chunk starts 50 tokens before current end
                if (actualTokenCount <= OVERLAP) {
                    // safety guard — chunk too small, jump past it
                    start = end;
                } else {
                    start += (actualTokenCount - OVERLAP);
                }
            }
        }

        return chunks;
    }

    /**
     * Snaps chunk text to the nearest sentence boundary.
     * Finds the last . ! or ? and truncates there.
     * Guard: only snaps if boundary is in the second half of the text
     * to avoid cutting too aggressively on early punctuation.
     */
    private String snapToSentenceBoundary(String text) {
        int lastPeriod = Math.max(
                text.lastIndexOf('.'),
                Math.max(text.lastIndexOf('!'), text.lastIndexOf('?'))
        );

        if (lastPeriod > text.length() / 2) {
            return text.substring(0, lastPeriod + 1).trim();
        }

        return text.trim();
    }
}