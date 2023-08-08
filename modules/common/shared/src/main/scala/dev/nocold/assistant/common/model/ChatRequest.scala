package dev.nocold.assistant.common.model

import cats.syntax.all.*
import io.circe.{Encoder, Decoder}

final case class ChatMessage(
    role: SpeakerRole,
    content: String,
)
object ChatMessage:
  given Encoder[ChatMessage] =
    Encoder.forProduct2("role", "content")(m => (m.role, m.content))
  given Decoder[ChatMessage] =
    Decoder.forProduct2("role", "content")(ChatMessage.apply)

  given Encoder[SpeakerRole] = Encoder.encodeString.contramap:
    case SpeakerRole.User    => "User"
    case SpeakerRole.Chatbot => "Chatbot"
  given Decoder[SpeakerRole] = Decoder.decodeString.emap:
    case "User"    => SpeakerRole.User.asRight[String]
    case "Chatbot" => SpeakerRole.Chatbot.asRight[String]
    case other     => s"Invalid SpeakerRole: $other".asLeft[SpeakerRole]

enum SpeakerRole:
  case User, Chatbot
