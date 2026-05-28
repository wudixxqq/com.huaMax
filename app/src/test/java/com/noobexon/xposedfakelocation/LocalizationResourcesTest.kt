package com.noobexon.xposedfakelocation

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText

class LocalizationResourcesTest {
    @Test
    fun `Chinese resources match default resources`() {
        val root = projectRoot()
        val defaultStrings = root.resolve("app/src/main/res/values/strings.xml")
        val chineseStrings = root.resolve("app/src/main/res/values-zh/strings.xml")

        assertTrue("Missing values-zh/strings.xml", chineseStrings.exists())

        val defaultKeys = stringKeys(defaultStrings)
        val chineseKeys = stringKeys(chineseStrings)

        assertTrue(
            "Missing Chinese string keys: ${(defaultKeys - chineseKeys).sorted()}",
            chineseKeys.containsAll(defaultKeys)
        )
        assertTrue(
            "Extra Chinese string keys: ${(chineseKeys - defaultKeys).sorted()}",
            defaultKeys.containsAll(chineseKeys)
        )
    }

    @Test
    fun `manager UI does not use hard-coded user visible strings`() {
        val root = projectRoot()
        val sourceRoots = listOf(
            root.resolve("app/src/main/java/com/noobexon/xposedfakelocation/manager/ui"),
            root.resolve("app/src/main/java/com/noobexon/xposedfakelocation/xposed")
        )
        val patterns = listOf(
            Regex("Text\\(\\s*\"([^\"]*[A-Za-z][^\"]*)\""),
            Regex("Toast\\.makeText\\([^\\n]*\"([^\"]*[A-Za-z][^\"]*)\""),
            Regex("contentDescription\\s*=\\s*\"([^\"]*[A-Za-z][^\"]*)\""),
            Regex("label\\s*=\\s*\"([^\"]*[A-Za-z][^\"]*)\"")
        )
        val allowed = setOf("Telegram", "Discord", "GitHub", "noobexon")

        val kotlinFiles = sourceRoots
            .filter { it.exists() }
            .flatMap { sourceRoot ->
                Files.walk(sourceRoot).use { paths ->
                    val files = mutableListOf<Path>()
                    paths.iterator().forEachRemaining { path ->
                        if (path.name.endsWith(".kt")) files.add(path)
                    }
                    files
                }
            }

        val violations = kotlinFiles.flatMap { path ->
            path.readText().lineSequence().withIndex().flatMap { (index, line) ->
                patterns.mapNotNull { pattern ->
                    val match = pattern.find(line) ?: return@mapNotNull null
                    val value = match.groupValues[1]
                    if (value in allowed) null else "${root.relativize(path)}:${index + 1}: $value"
                }
            }.toList()
        }

        assertTrue(
            "Hard-coded user-visible strings must be moved to string resources:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    private fun projectRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (!current.resolve("settings.gradle.kts").exists()) {
            current = current.parent ?: error("Could not locate project root")
        }
        return current
    }

    private fun stringKeys(file: Path): Set<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile())
        return document.getElementsByTagName("string").let { nodes ->
            (0 until nodes.length).map { index ->
                (nodes.item(index) as Element).getAttribute("name")
            }.toSet()
        }
    }
}
