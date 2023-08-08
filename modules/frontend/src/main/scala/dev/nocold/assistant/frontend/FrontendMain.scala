package dev.nocold.assistant.frontend

import tyrian.*
import cats.effect.IO
import cats.effect.unsafe.implicits.global

object FrontendMain:
  def main(args: Array[String]): Unit =
    val program =
      for app <- StudyAssistantApp[IO](_.unsafeRunAndForget())
      yield TyrianApp.onLoad("StudyAssistantApp" -> app)

    program.unsafeRunAndForget()
