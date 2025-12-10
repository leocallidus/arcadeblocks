package com.arcadeblocks.story;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Static configuration for chapter story overlays, allowing the runtime to look up
 * whether a narrative overlay should be shown for a given level.
 */
public final class StoryConfig {

    private StoryConfig() {
    }

    private static final List<ChapterStoryData> STORIES = List.of(
        new ChapterStoryData(
            1,
            1,
            "textures/chapter1_background.png",
            "music/chapter1_theme.mp3",
            "chapter.story.chapter1"
        ),
        new ChapterStoryData(
            2,
            11,
            "textures/chapter2_background.png",
            "music/chapter2_theme.mp3",
            "chapter.story.chapter2"
        ),
        new ChapterStoryData(
            3,
            21,
            "textures/chapter3_background.png",
            "music/chapter3_theme.mp3",
            "chapter.story.chapter3"
        ),
        new ChapterStoryData(
            4,
            31,
            "textures/chapter4_background.png",
            "music/chapter4_theme.mp3",
            "chapter.story.chapter4"
        ),
        new ChapterStoryData(
            5,
            32,
            "textures/chapter5_background.png",
            "music/chapter5_theme.mp3",
            "chapter.story.chapter5"
        ),
        new ChapterStoryData(
            6,
            41,
            "textures/chapter6_background.png",
            "music/chapter6_theme.mp3",
            "chapter.story.chapter6"
        ),
        new ChapterStoryData(
            7,
            51,
            "textures/chapter7_background.png",
            "music/chapter7_theme.mp3",
            "chapter.story.chapter7"
        ),
        new ChapterStoryData(
            8,
            61,
            "textures/chapter8_background.png",
            "music/chapter8_theme.mp3",
            "chapter.story.chapter8"
        ),
        new ChapterStoryData(
            9,
            71,
            "textures/chapter9_background.png",
            "music/chapter9_theme.mp3",
            "chapter.story.chapter9"
        ),
        new ChapterStoryData(
            10,
            81,
            "textures/chapter10_background.png",
            "music/chapter10_theme.mp3",
            "chapter.story.chapter10"
        ),
        new ChapterStoryData(
            11,
            91,
            "textures/chapter11_background.png",
            "music/chapter11_theme.mp3",
            "chapter.story.chapter11"
        ),
        new ChapterStoryData(
            12,
            101,
            "textures/chapter12_background.png",
            "music/chapter12_theme.mp3",
            "chapter.story.chapter12"
        ),
        // Bonus Chapter I: Symphony of Chaos
        new ChapterStoryData(
            101,  // Bonus chapter number
            1001, // First bonus level
            "textures/chapter1_bonus_background.png",
            "music/chapter1_bonus_theme.mp3",
            "bonus.chapter.story.1"
        ),
        // LBreakout1 campaign intro
        new ChapterStoryData(
            201,
            5000,
            "textures/chapter_lbreakout1.png",
            "",
            "chapter.story.lbreakout1"
        )
    );

    /**
     * Attempt to locate a story overlay configuration for the provided level number.
     */
    public static Optional<ChapterStoryData> findForLevel(int levelNumber) {
        return STORIES.stream()
            .filter(story -> story.firstLevelNumber() == levelNumber)
            .findFirst();
    }

    /**
     * Decide whether the story overlay should be shown for the provided level.
     * Future expansions can check player progress or user settings.
     */
    public static boolean shouldShowForLevel(int levelNumber, GameProgress progress) {
        Optional<ChapterStoryData> story = findForLevel(levelNumber);
        if (story.isEmpty()) {
            return false;
        }

        if (progress == null) {
            return true;
        }

        if (!progress.storyOverlaysEnabled()) {
            return false;
        }

        return !progress.shownChapterNumbers().contains(story.get().chapterNumber());
    }

    /**
     * Lightweight projection of a player's progress used for deciding whether to show
     * a story overlay. Until persistence is added, callers may use {@link #emptyProgress()}.
     */
    public record GameProgress(Set<Integer> shownChapterNumbers, boolean storyOverlaysEnabled) {
        public GameProgress {
            shownChapterNumbers = shownChapterNumbers == null
                ? Collections.emptySet()
                : Collections.unmodifiableSet(Set.copyOf(shownChapterNumbers));
        }
    }

    public static GameProgress emptyProgress() {
        return new GameProgress(Collections.emptySet(), true);
    }
}
