package com.myenglishvocab.server.word.repository;

import com.myenglishvocab.server.admin.dto.PopularWordResponse;
import com.myenglishvocab.server.word.entity.Word;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<Word> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);

    @Query("""
            SELECT COUNT(DISTINCT word.user.id)
            FROM Word word
            WHERE word.createdAt >= :from
            """)
    long countDistinctUsersWhoSavedSince(Instant from);

    @Query("""
            SELECT new com.myenglishvocab.server.admin.dto.PopularWordResponse(
                LOWER(word.term),
                COUNT(word),
                COUNT(DISTINCT word.user.id)
            )
            FROM Word word
            GROUP BY LOWER(word.term)
            ORDER BY COUNT(DISTINCT word.user.id) DESC, COUNT(word) DESC, LOWER(word.term)
            """)
    List<PopularWordResponse> findPopularWords(Pageable pageable);
}
