package sas

import java.nio.file.Path

class AppStorage(path: Path) {
  def resolve(location: String): Path = path.resolve(location)
}
