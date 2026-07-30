package com.myenglishvocab.server.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "USER_DUPLICATE_USERNAME", "이미 사용 중인 username입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_INVALID_INPUT", "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_ERROR", "서버 오류가 발생했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_REFRESH_TOKEN", "유효하지 않은 refresh token입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    // 실무에선 소유자가 아니면 다 404로 숨김. 해커가 1, 2, 3, ... 을 대입하며 id를 찾지 못하게
    WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "WORD_NOT_FOUND", "단어를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
