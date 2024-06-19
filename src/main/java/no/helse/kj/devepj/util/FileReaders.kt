package no.helse.kj.devepj.util

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import java.nio.file.Files
import java.nio.file.Path

val YAML_MAPPER = YAMLMapper()

fun <T> readFiles(
  path: Path,
  converter: (fileContent: String) -> T,
  filter: (path: Path) -> Boolean = { true },
): List<T> {
  return Files.walk(path)
    .filter { Files.isRegularFile(it) }
    .filter { filter(it) }
    .map { readFile(it) }
    .map { converter(it) }
    .toList()
}

fun readFile(path: Path): String {
  val stream = Files.newInputStream(path)
  return String(stream.readAllBytes())
}