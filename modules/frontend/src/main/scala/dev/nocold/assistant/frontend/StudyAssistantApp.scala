package dev.nocold.assistant
package frontend

import java.util.UUID

import common.model.*

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.all.*

import tyrian.*
import tyrian.cmds.{LocalStorage, Logger}
import tyrian.Html.*

import org.scalajs.dom

trait StudyAssistantApp[F[_]: Async: ApiClient.Ref]
    extends TyrianAppF[F, ChatAppMsg, ChatAppModel]:

  val AccessTokenKey = "AccessToken"

  def init(flags: Map[String, String]): (ChatAppModel, Cmd[F, ChatAppMsg]) =
    val client = flags.get("BackendUrl").map(ApiClient.build[F])
    val cmd0 = Cmd.SideEffect:
      ApiClient.Ref[F].set(client)
    val cmd = LocalStorage.getItem(AccessTokenKey):
      case Right(LocalStorage.Result.Found(accessToken)) =>
        StartChat(UUID.fromString(accessToken))
      case Left(LocalStorage.Result.NotFound(e)) => NoOp
    (LoginModel("", ""), Cmd.Batch(cmd0, cmd))

  def update(
      model: ChatAppModel,
  ): ChatAppMsg => (ChatAppModel, Cmd[F, ChatAppMsg]) =
    case NoOp          => (model, Cmd.None)
    case ErrorMsg(msg) => (model, Logger.info(s"Error: $msg"))
    case UpdateChatInput(chat) =>
      model match
        case ChatModel(accessKey, _, messages, isLoading) =>
          val newModel = ChatModel(accessKey, chat, messages, isLoading)
          (newModel, Cmd.None)
        case _ => (model, Cmd.None)
    case SendChat =>
      model match
        case ChatModel(accessKey, input, messages, _) =>
          val newMessages = messages :+ ChatLog(input, Speaker.User)
          val newModel    = ChatModel(accessKey, "", newMessages, true)
          val program = for
            _ <- EitherT.pure:
              println(s"Send chat: $input")
            clientOption <- EitherT.liftF:
              ApiClient.Ref[F].get
            client <- EitherT.fromOption(
              clientOption,
              s"Client not initialized",
            )
            response <- client.chat(
              accessKey,
              newMessages.map(_.toChatMessage()),
            )
          yield response.content

          val cmd = Cmd.Run(program.value):
            case Right(chatMsg) => ReceiveChat(chatMsg)
            case Left(msg)      => ErrorMsg(msg)
          (newModel, cmd)
        case _ => (model, Cmd.None)

    case StartChat(accessKey) =>
      model match
        case LoginModel(username, password) =>
          val newModel = ChatModel(accessKey, "", Seq.empty, true)
          scribe.info(s"Start chat with accessKey: $accessKey")

          val program = for
            _ <- EitherT.pure:
              println(s"Start login: $username, $password")
            clientOption <- EitherT.liftF:
              ApiClient.Ref[F].get
            client <- EitherT.fromOption(
              clientOption,
              s"Client not initialized",
            )
            response <- client.chat(accessKey, Seq.empty)
          yield response.content

          val cmd = Cmd.Run(program.value):
            case Right(chatMsg) => ReceiveChat(chatMsg)
            case Left(msg)      => ErrorMsg(msg)

          (newModel, cmd)
        case _ => (model, Cmd.None)
    case ReceiveChat(chatMsg) =>
      model match
        case ChatModel(accessKey, input, messages, _) =>
          val newMessages = messages :+ ChatLog(chatMsg, Speaker.Chatbot)
          val newModel    = ChatModel(accessKey, "", newMessages, false)
          (newModel, Cmd.None)
        case _ => (model, Cmd.None)
    case LoginMsg.UpdateUsername(username) =>
      model match
        case LoginModel(_, password) =>
          (LoginModel(username, password), Cmd.None)
        case _ => (model, Cmd.None)
    case LoginMsg.UpdatePassword(password) =>
      model match
        case LoginModel(username, _) =>
          (LoginModel(username, password), Cmd.None)
        case _ => (model, Cmd.None)
    case LoginMsg.Submit =>
      model match
        case LoginModel(username, password) =>
          println(model)
          val program = for
            _ <- EitherT.pure:
              println(s"Start login: $username, $password")
            clientOption <- EitherT.liftF:
              ApiClient.Ref[F].get
            client <- EitherT.fromOption(
              clientOption,
              s"Client not initialized",
            )
            response <- client.login(LoginRequest(username, password))
            _ <- EitherT.pure:
              dom.window.localStorage
                .setItem(AccessTokenKey, response.accessToken.toString)
          yield
            println(s"Access token: ${response.accessToken}")
            response.accessToken
          val cmd = Cmd.Run(program.value):
            case Right(accessToken) => StartChat(accessToken)
            case Left(msg)          => ErrorMsg(msg)
          (model, cmd)

        case _ => (model, Cmd.None)

  def view(model: ChatAppModel): Html[ChatAppMsg] = model match
    case LoginModel(username, password) =>
      div(id := "loginPage")(
        h1("Study Assistant"),
        h2("Log In / Sign Up"),
        form(
          label(_for := "username")("Username"),
          input(
            `type` := "text",
            id     := "username",
            name   := "username",
            value  := username,
            onInput(LoginMsg.UpdateUsername(_)),
          ),
          label(_for := "password")("Password"),
          input(
            `type` := "password",
            id     := "password",
            name   := "password",
            value  := password,
            onInput(LoginMsg.UpdatePassword(_)),
          ),
          input(
            `type` := "submit",
            value  := "Log In / Sign Up",
            onClick(LoginMsg.Submit),
          ),
        ),
      )
    case model: ChatModel =>
      val hiddenClass = if model.isLoading then "" else "hidden"
      val chatLogBlocks = model.chatLogs
        .map:
          case ChatLog(msg, from) =>
            div(`class` := s"message ${from.name}")(msg)
        :+ div(`class` := s"message chatbot loader ${hiddenClass}")()
      div(id := "chatPage")(
        div(id := "chatLog")(chatLogBlocks.toList),
        form(
          label(_for := "chatInput")("Message:"),
          textarea(
            id   := "chatInput",
            name := "chatInput",
            rows := "4",
            cols := "50",
            onInput(UpdateChatInput(_)),
          )(text(model.chatInput)),
          input(`type` := "submit", value := "Send", onClick(SendChat)),
        ),
      )

  def router: Location => ChatAppMsg = _ => NoOp

  def subscriptions(model: ChatAppModel): Sub[F, ChatAppMsg] =
    Sub.None

object StudyAssistantApp:
  def apply[F[_]: Async](_run: F[Nothing] => Unit): F[StudyAssistantApp[F]] =
    for given ApiClient.Ref[F] <- ApiClient.Ref.empty[F]
    yield new StudyAssistantApp[F]:
      override val run: F[Nothing] => Unit = _run

sealed trait ChatAppModel
final case class LoginModel(username: String, password: String)
    extends ChatAppModel
final case class ChatModel(
    accessKey: UUID,
    chatInput: String,
    chatLogs: Seq[ChatLog],
    isLoading: Boolean,
) extends ChatAppModel
final case class ChatLog(
    msg: String,
    from: Speaker,
):
  def toChatMessage(): ChatMessage =
    val speakerRole = from match
      case Speaker.User    => SpeakerRole.User
      case Speaker.Chatbot => SpeakerRole.Chatbot
    ChatMessage(speakerRole, msg)
enum Speaker(val name: String):
  case User    extends Speaker("user")
  case Chatbot extends Speaker("chatbot")

sealed trait ChatAppMsg
case object NoOp                               extends ChatAppMsg
final case class ErrorMsg(msg: String)         extends ChatAppMsg
final case class StartChat(accessKey: UUID)    extends ChatAppMsg
final case class UpdateChatInput(chat: String) extends ChatAppMsg
object SendChat                                extends ChatAppMsg
final case class ReceiveChat(chat: String)     extends ChatAppMsg
sealed trait LoginMsg                          extends ChatAppMsg
object LoginMsg:
  final case class UpdateUsername(username: String) extends LoginMsg
  final case class UpdatePassword(password: String) extends LoginMsg
  object Submit                                     extends LoginMsg
