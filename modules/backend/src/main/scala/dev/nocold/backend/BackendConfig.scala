package dev.nocold.assistant.backend

import cats.syntax.either.*

import pureconfig.*
import pureconfig.generic.derivation.default.*
import pureconfig.error.CannotConvert
import scodec.bits.ByteVector

import BackendConfig.*

final case class BackendConfig(
    login: Map[String, LoginConfig],
    server: ServerConfig,
    openAi: OpenAiConfig,
    systemMsg: String,
) derives ConfigReader

object BackendConfig:
  def load: BackendConfig = ConfigSource.default.loadOrThrow[BackendConfig]

  final case class LoginConfig(
      salt: String,
      passwordHash: ByteVector,
  ) derives ConfigReader

  final case class ServerConfig(
      port: Int,
  ) derives ConfigReader

  final case class OpenAiConfig(
      apiKey: String,
  ) derives ConfigReader

  given byteVectorConfigReader: ConfigReader[ByteVector] =
    ConfigReader[String].emap: str =>
      ByteVector.fromHexDescriptive(str).leftMap: msg =>
        CannotConvert(str, "ByteVector", msg)
