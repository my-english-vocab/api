package com.myenglishvocab.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    // 왜 bean으로 등록하는지?
    // BCryptPasswordEncoder를 여러 곳에서 쓸 때마다 new 하면 안 되고, Spring이 하나만 만들어서 주입해 줍니다. 나중에 로그인에서도 같은 인스턴스를 씁니다.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
