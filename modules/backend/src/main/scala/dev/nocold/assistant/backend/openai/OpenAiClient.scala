package dev.nocold.assistant
package backend.openai

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.syntax.all.*

import sttp.client4.*
import sttp.client4.armeria.cats.ArmeriaCatsBackend
import sttp.openai.*
import sttp.openai.OpenAIExceptions.OpenAIException
import sttp.openai.OpenAIExceptions.OpenAIException.DeserializationOpenAIException
import sttp.openai.json.SnakePickle
import sttp.openai.json.SttpUpickleApiExtension.{
  asJsonSnake,
  upickleBodySerializerSnake,
}
import sttp.openai.requests.completions.Stop
import sttp.openai.requests.completions.chat.*
import sttp.openai.requests.completions.chat.ChatRequestResponseData.ChatResponse
import ujson.Str

import common.model.*

trait OpenAiClient[F[_]]:
  def chat(systemMsg: String)(
      previousChat: Seq[ChatMessage],
  ): EitherT[F, OpenAIException, ChatRequestResponseData.ChatResponse]

object OpenAiClient:
  def apply[F[_]: OpenAiClient]: OpenAiClient[F] = summon

  def make[F[_]: Async](openaiKey: String): Resource[F, OpenAiClient[F]] =
    ArmeriaCatsBackend
      .resource[F]()
      .map: backend =>
//        val openAI = OpenAI(openaiKey)
        new OpenAiClient[F]:
          def chat(systemMsg: String)(
              previousChat: Seq[ChatMessage],
          ): EitherT[F, OpenAIException, ChatRequestResponseData.ChatResponse] =
            val systemMessage = Message(
              role = Role.System,
              content = systemMsg,
            )

            val bodyMessages: Seq[Message] = previousChat.map: chatMessage =>
              val role = chatMessage.role match
                case SpeakerRole.User    => Role.User
                case SpeakerRole.Chatbot => Role.Assistant
              Message(
                role = role,
                content = chatMessage.content,
              )

            val chatRequestBody: ChatBody = ChatBody(
              model = ChatCompletionModel.GPT35Turbo_0613,
              messages = systemMessage +: bodyMessages,
            )

            val response: F[Either[OpenAIException, ChatResponse]] =
              basicRequest.auth
                .bearer(openaiKey)
                .post(uri"https://api.openai.com/v1/chat/completions")
                .body(chatRequestBody)
                .response(asJsonSnake[ChatResponse])
                .send(backend)
                .map(_.body)

            EitherT(response).flatTap: chatResponse =>
              EitherT.pure(scribe.info(s"chatResponse: ${chatResponse}"))

  final case class ChatBody(
      model: ChatCompletionModel,
      messages: Seq[Message],
      temperature: Option[Double] = None,
      topP: Option[Double] = None,
      n: Option[Int] = None,
      stop: Option[Stop] = None,
      maxTokens: Option[Int] = None,
      presencePenalty: Option[Double] = None,
      frequencyPenalty: Option[Double] = None,
      logitBias: Option[Map[String, Float]] = None,
      user: Option[String] = None,
  )

  object ChatBody:
    implicit val chatRequestW: SnakePickle.Writer[ChatBody] =
      SnakePickle.macroW[ChatBody]

  sealed abstract class ChatCompletionModel(val value: String)

  object ChatCompletionModel:
    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    implicit val chatCompletionModelRW
        : SnakePickle.ReadWriter[ChatCompletionModel] = SnakePickle
      .readwriter[ujson.Value]
      .bimap[ChatCompletionModel](
        model => SnakePickle.writeJs(model.value),
        jsonValue =>
          SnakePickle.read[ujson.Value](jsonValue) match
            case Str(value) =>
              byChatModelValue.getOrElse(
                value,
                throw DeserializationOpenAIException(
                  new Exception(s"Could not deserialize: $value"),
                ),
              )
            case e =>
              throw DeserializationOpenAIException(
                new Exception(s"Could not deserialize: $e"),
              ),
      )

    case object GPT4_0613 extends ChatCompletionModel("gpt-4-0613")

    case object GPT4_32k_0613 extends ChatCompletionModel("gpt-4-32k-0613")

    case object GPT35Turbo_0613
        extends ChatCompletionModel("gpt-3.5-turbo-0613")

    case object GPT35Turbo_16k_0613
        extends ChatCompletionModel("gpt-3.5-turbo-16k-0613")

    val values: Set[ChatCompletionModel] =
      Set(GPT4_0613, GPT4_32k_0613, GPT35Turbo_0613, GPT35Turbo_16k_0613)

    private val byChatModelValue =
      values.map(model => model.value -> model).toMap

  case class Message(role: Role, content: String, name: Option[String] = None)

  object Message:
    implicit val messageRW: SnakePickle.ReadWriter[Message] =
      SnakePickle.macroRW[Message]

  sealed abstract class Role(val value: String)

  object Role:
    case object System extends Role("system")

    case object User extends Role("user")

    case object Assistant extends Role("assistant")

    case object Function extends Role("function")

    case class Custom(customRole: String) extends Role(customRole)

    val values: Set[Role] = Set(System, User, Assistant, Function)

    private val byRoleValue = values.map(role => role.value -> role).toMap

    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    given roleRW: SnakePickle.ReadWriter[Role] = SnakePickle
      .readwriter[ujson.Value]
      .bimap[Role](
        role => SnakePickle.writeJs(role.value),
        jsonValue =>
          SnakePickle.read[ujson.Value](jsonValue) match
            case Str(value) => byRoleValue.getOrElse(value, Custom(value))
            case e =>
              throw DeserializationOpenAIException(
                new Exception(s"Could not deserialize: $e"),
              ),
      )
