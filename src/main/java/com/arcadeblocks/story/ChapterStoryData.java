package com.arcadeblocks.story;

/**
 * Immutable descriptor for a chapter intro story overlay.
 */
public record ChapterStoryData(
    int chapterNumber,
    int firstLevelNumber,
    String imagePath,
    String musicPath,
    String localizationPrefix
) {
}


