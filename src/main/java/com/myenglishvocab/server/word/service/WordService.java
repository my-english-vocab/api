package com.myenglishvocab.server.word.service;

import com.myenglishvocab.server.common.exception.BusinessException;
import com.myenglishvocab.server.common.exception.ErrorCode;
import com.myenglishvocab.server.user.entity.User;
import com.myenglishvocab.server.user.repository.UserRepository;
import com.myenglishvocab.server.word.dto.CreateWordRequest;
import com.myenglishvocab.server.word.dto.UpdateFavoriteRequest;
import com.myenglishvocab.server.word.dto.UpdateWordRequest;
import com.myenglishvocab.server.word.dto.WordResponse;
import com.myenglishvocab.server.word.entity.Word;
import com.myenglishvocab.server.word.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordService {

    private final WordRepository wordRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<WordResponse> getMyWords(Long userId) {
        return wordRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(WordResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WordResponse getMyWord(Long userId, Long wordId) {
        return WordResponse.from(getOwnedWord(wordId, userId));
    }

    @Transactional
    public WordResponse create(Long userId, CreateWordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Word word = Word.builder()
                .user(user)
                .term(request.term())
                .definition(request.definition())
                .exampleSentence(request.exampleSentence())
                .meaningOfExampleSentence(request.meaningOfExampleSentence())
                .build();

        Word saved = wordRepository.save(word);
        log.info("단어 생성 userId={} wordId={} term={}", userId, saved.getId(), saved.getTerm());
        return WordResponse.from(saved);
    }

    @Transactional
    public WordResponse update(Long userId, Long wordId, UpdateWordRequest request) {
        Word word = getOwnedWord(wordId, userId);

        word.updateContent(
                request.term(),
                request.definition(),
                request.exampleSentence(),
                request.meaningOfExampleSentence()
        );

        log.info("단어 수정 userId={} wordId={}", userId, wordId);
        return WordResponse.from(word);
    }

    @Transactional
    public void delete(Long userId, Long wordId) {
        Word word = getOwnedWord(wordId, userId);
        wordRepository.delete(word);
        log.info("단어 삭제 userId={} wordId={}", userId, wordId);
    }

    @Transactional
    public WordResponse markLearned(Long userId, Long wordId) {
        Word word = getOwnedWord(wordId, userId);
        word.markLearned();
        log.info("단어 레벨업 userId={} wordId={} term={} level={}", userId, wordId, word.getTerm(), word.getLevel());
        return WordResponse.from(word);
    }

    @Transactional
    public WordResponse updateFavorite(Long userId, Long wordId, UpdateFavoriteRequest request) {
        Word word = getOwnedWord(wordId, userId);
        word.changeFavorite(request.favorite());
        log.info("단어 즐겨찾기 변경 userId={} wordId={} favorite={}", userId, wordId, word.isFavorite());
        return WordResponse.from(word);
    }

    private Word getOwnedWord(Long wordId, Long userId) {
        return wordRepository.findByIdAndUserId(wordId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));
    }
}
