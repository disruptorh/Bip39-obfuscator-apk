package com.reimen.bip39obfuscator.io

/**
 * Parseo de seeds desde contenido de archivo, 1:1 con parse_seeds_from_file del Python.
 *
 * HORIZONTAL — cada línea es una seed completa.
 * VERTICAL — una palabra por línea, seeds separadas por líneas vacías.
 * Si todas las líneas no vacías tienen una sola palabra y no hay líneas vacías,
 * todas las palabras se agrupan como una única seed.
 */
object SeedFileParser {

    fun parseSeedsFromFile(content: String): List<String> {
        val rawLines = content.lines()

        // ¿Todas las líneas no vacías tienen una sola palabra?
        val nonEmpty = rawLines.map { it.trim() }.filter { it.isNotEmpty() }
        if (nonEmpty.isEmpty()) return emptyList()

        val allSingleWord = nonEmpty.all { it.split(Regex("\\s+")).size == 1 }

        if (!allSingleWord) {
            // HORIZONTAL: cada línea no vacía es una seed
            return nonEmpty
        }

        // VERTICAL: agrupar por bloques separados por líneas vacías
        val groups = mutableListOf<List<String>>()
        val currentGroup = mutableListOf<String>()

        for (line in rawLines) {
            val word = line.trim()
            if (word.isNotEmpty()) {
                currentGroup.add(word)
            } else {
                if (currentGroup.isNotEmpty()) {
                    groups.add(currentGroup.toList())
                    currentGroup.clear()
                }
            }
        }
        if (currentGroup.isNotEmpty()) {
            groups.add(currentGroup.toList())
        }

        return groups.map { it.joinToString(" ") }
    }
}
