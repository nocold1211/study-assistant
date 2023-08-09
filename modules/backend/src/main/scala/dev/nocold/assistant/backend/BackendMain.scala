package dev.nocold.assistant
package backend

import java.util.UUID

import cats.effect.{ExitCode, IO, IOApp, Ref, Resource}
import cats.effect.std.SecureRandom

import openai.OpenAiClient

object BackendMain extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    val conf = BackendConfig.load
    val resource = for
      given OpenAiClient[IO] <- OpenAiClient.make[IO](conf.openAi.apiKey)
      given SecureRandom[IO] <- Resource.eval:
        SecureRandom.javaSecuritySecureRandom[IO]
      session <- Resource.eval:
        Ref[IO].of(Set.empty[UUID])
      _ <- BackendApp.resource[IO](conf, session)
    yield ()

    resource.useForever.as(ExitCode.Success)

//    for
//      given SecureRandom[IO] <- SecureRandom.javaSecuritySecureRandom[IO]
//      hashAndSalt <- student.StudentService.createHashAndSalt[IO](
//        "???"
//      )
//      (hash, salt) = hashAndSalt
//    yield
//      println(s"Hash: ${hash.toHex}")
//      println(s"Salt: ${salt}")
//      ExitCode.Success
