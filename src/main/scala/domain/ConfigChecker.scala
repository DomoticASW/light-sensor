package domain

import utils.Eithers.leftIf

case class BadConfiguration(message: String)

case class Config(id: String, name: String, updateRate: Long)

object ConfigChecker:
  def apply(id: String, name: String, updateRate: Long): Either[BadConfiguration, Config] =
    for
      _ <- Either.leftIf(
          updateRate > 0,
          BadConfiguration("Given update rate should be greater than 0")
        )
    yield Config(id = id, name = name, updateRate = updateRate)