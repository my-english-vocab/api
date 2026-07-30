package com.myenglishvocab.server.word.repository;

import com.myenglishvocab.server.word.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<Word> findByIdAndUserId(Long id, Long userId);
}
