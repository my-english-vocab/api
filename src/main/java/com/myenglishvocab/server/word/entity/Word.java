package com.myenglishvocab.server.word.entity;

import com.myenglishvocab.server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "words")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String term;

    @Column(nullable = false, length = 150)
    private String definition;

    @Column(nullable = false)
    private int level;

    @Column(length = 1000)
    private String exampleSentence;

    @Column(length = 1000)
    private String meaningOfExampleSentence;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Builder
    public Word(
            User user,
            String term,
            String definition,
            String exampleSentence,
            String meaningOfExampleSentence
    ) {
        this.user = user;
        this.term = term;
        this.definition = definition;
        this.level = 0;
        this.exampleSentence = exampleSentence;
        this.meaningOfExampleSentence = meaningOfExampleSentence;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateContent(
            String term,
            String definition,
            String exampleSentence,
            String meaningOfExampleSentence
    ) {
        this.term = term;
        this.definition = definition;
        this.exampleSentence = exampleSentence;
        this.meaningOfExampleSentence = meaningOfExampleSentence;
    }

    public void markLearned() {
        this.level += 1;
    }
}
