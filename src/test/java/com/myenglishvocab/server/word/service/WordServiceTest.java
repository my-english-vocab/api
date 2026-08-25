package com.myenglishvocab.server.word.service;

import com.myenglishvocab.server.analytics.service.ActivityService;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WordServiceTest {

    @Mock WordRepository wordRepository;
    @Mock UserRepository userRepository;
    @Mock ActivityService activityService;
    @InjectMocks WordService wordService;

    @Test
    void 단어_생성_성공시_level은_0이고_즐겨찾기는_false다() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(wordRepository.save(any(Word.class))).willAnswer(invocation -> {
            Word word = invocation.getArgument(0);
            ReflectionTestUtils.setField(word, "id", 10L);
            ReflectionTestUtils.setField(word, "createdAt", Instant.parse("2026-07-29T00:00:00Z"));
            return word;
        });

        WordResponse response = wordService.create(
                1L,
                new CreateWordRequest("apple", "사과", "I like apples.", "나는 사과를 좋아한다.")
        );

        assertThat(response.term()).isEqualTo("apple");
        assertThat(response.definition()).isEqualTo("사과");
        assertThat(response.level()).isZero();
        assertThat(response.favorite()).isFalse();
        verify(wordRepository).save(any(Word.class));
    }

    @Test
    void 사용자가_없으면_단어_생성_실패() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wordService.create(
                99L,
                new CreateWordRequest("apple", "사과", null, null)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 내_단어_목록_조회() {
        User user = user(1L);
        Word word = word(user, "apple", "사과");
        ReflectionTestUtils.setField(word, "id", 1L);
        ReflectionTestUtils.setField(word, "createdAt", Instant.parse("2026-07-29T00:00:00Z"));

        given(wordRepository.findByUserIdOrderByCreatedAtAsc(1L)).willReturn(List.of(word));

        List<WordResponse> responses = wordService.getMyWords(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().term()).isEqualTo("apple");
    }

    @Test
    void 소유하지_않은_단어는_조회_실패() {
        given(wordRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wordService.getMyWord(2L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORD_NOT_FOUND);
    }

    @Test
    void 단어_수정_성공() {
        User user = user(1L);
        Word word = word(user, "apple", "사과");
        ReflectionTestUtils.setField(word, "id", 1L);
        ReflectionTestUtils.setField(word, "createdAt", Instant.parse("2026-07-29T00:00:00Z"));

        given(wordRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(word));

        WordResponse response = wordService.update(
                1L,
                1L,
                new UpdateWordRequest("apple", "사과(수정)", null, null)
        );

        assertThat(response.definition()).isEqualTo("사과(수정)");
    }

    @Test
    void 단어_삭제_성공() {
        User user = user(1L);
        Word word = word(user, "apple", "사과");
        given(wordRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(word));

        wordService.delete(1L, 1L);

        verify(wordRepository).delete(word);
    }

    @Test
    void 외웠음_처리시_level이_1_증가한다() {
        User user = user(1L);
        Word word = word(user, "apple", "사과");
        ReflectionTestUtils.setField(word, "id", 1L);
        ReflectionTestUtils.setField(word, "createdAt", Instant.parse("2026-07-29T00:00:00Z"));

        given(wordRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(word));

        WordResponse response = wordService.markLearned(1L, 1L);

        assertThat(response.level()).isEqualTo(1);
    }

    @Test
    void 즐겨찾기_상태_변경_성공() {
        User user = user(1L);
        Word word = word(user, "apple", "사과");
        ReflectionTestUtils.setField(word, "id", 1L);
        ReflectionTestUtils.setField(word, "createdAt", Instant.parse("2026-07-29T00:00:00Z"));

        given(wordRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(word));

        WordResponse response = wordService.updateFavorite(
                1L,
                1L,
                new UpdateFavoriteRequest(true)
        );

        assertThat(response.favorite()).isTrue();
        assertThat(word.isFavorite()).isTrue();
    }

    @Test
    void 소유하지_않은_단어는_즐겨찾기_변경_실패() {
        given(wordRepository.findByIdAndUserId(1L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wordService.updateFavorite(
                2L,
                1L,
                new UpdateFavoriteRequest(true)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORD_NOT_FOUND);
    }

    private User user(Long id) {
        User user = User.builder()
                .username("user" + id)
                .password("encoded")
                .displayName("테스터")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Word word(User user, String term, String definition) {
        return Word.builder()
                .user(user)
                .term(term)
                .definition(definition)
                .build();
    }
}
