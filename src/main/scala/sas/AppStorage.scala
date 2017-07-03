package sas

import java.nio.file.{Files, Path}

class AppStorage(path: Path) {
  def load(location: String): Array[Byte] = Files.readAllBytes(path.resolve(location))
}
