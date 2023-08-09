package dev.nocold.assistant
package backend.openai

import cats.data.EitherT
import cats.effect.Async

import common.model.*

object OpenAiService:
  def chat[F[_]: Async: OpenAiClient](
      systemMsg: String,
      chatMessages: Seq[ChatMessage],
  ): EitherT[F, String, ChatResponse] =
    for
      chatResponse <- OpenAiClient[F]
        .chat(systemMsg)(chatMessages)
        .leftMap: e =>
          s"OpenAI error: ${e}"
          
      choice <- EitherT.fromOption[F](
        chatResponse.choices.headOption,
        "No response from OpenAI",
      )
    yield ChatResponse(choice.message.content)
