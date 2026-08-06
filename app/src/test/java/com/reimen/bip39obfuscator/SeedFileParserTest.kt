package com.reimen.bip39obfuscator

import com.reimen.bip39obfuscator.io.SeedFileParser
import org.junit.Assert.assertEquals
import org.junit.Test

class SeedFileParserTest {

    private val twelveAbandonAbout =
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    private val twelveZooWrong =
        "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong"

    @Test
    fun parsesHorizontalFormat() {
        val content = "$twelveAbandonAbout\n$twelveZooWrong\n"
        val seeds = SeedFileParser.parseSeedsFromFile(content)
        assertEquals(2, seeds.size)
        assertEquals(twelveAbandonAbout, seeds[0])
        assertEquals(twelveZooWrong, seeds[1])
    }

    @Test
    fun parsesVerticalFormat() {
        val content = buildString {
            append("abandon\n".repeat(11))
            append("about\n\n")
            append("zoo\n".repeat(11))
            append("wrong\n")
        }
        val seeds = SeedFileParser.parseSeedsFromFile(content)
        assertEquals(2, seeds.size)
        assertEquals(twelveAbandonAbout, seeds[0])
        assertEquals(twelveZooWrong, seeds[1])
    }

    @Test
    fun singleWordPerLineWithoutBlanksGroupsAsOneSeed() {
        val content = buildString {
            append("abandon\n".repeat(11))
            append("about\n")
        }
        val seeds = SeedFileParser.parseSeedsFromFile(content)
        assertEquals(1, seeds.size)
        assertEquals(twelveAbandonAbout, seeds[0])
    }

    @Test
    fun emptyFileReturnsEmptyList() {
        assertEquals(emptyList<String>(), SeedFileParser.parseSeedsFromFile(""))
        assertEquals(emptyList<String>(), SeedFileParser.parseSeedsFromFile("\n\n   \n"))
    }

    @Test
    fun trailingNewlineIsHarmless() {
        val seeds = SeedFileParser.parseSeedsFromFile("$twelveAbandonAbout\n\n")
        assertEquals(1, seeds.size)
        assertEquals(twelveAbandonAbout, seeds[0])
    }
}
