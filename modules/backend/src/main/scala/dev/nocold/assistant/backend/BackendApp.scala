package dev.nocold.assistant
package backend

import java.util.UUID

import cats.effect.{Async, Clock, Ref, Resource}
import cats.effect.std.{Dispatcher, SecureRandom}
import cats.syntax.all.*

import com.linecorp.armeria.server.Server
import sttp.tapir.server.armeria.cats.{
  ArmeriaCatsServerInterpreter,
  ArmeriaCatsServerOptions,
}
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.interceptor.log.DefaultServerLog

import common.AssistantApi
import common.model.*
import openai.*
import student.StudentService

object BackendApp:

  def rootServerEndpoint[F[_]: Async] =
    AssistantApi.rootEndpoint.serverLogicSuccess: _ =>
      Async[F].delay("Study Assistant API Server is running!")

  def loginServerEndpoint[F[_]: Async: Clock: SecureRandom](
    config: BackendConfig,
    session: Ref[F, Set[UUID]],
  ) =
    AssistantApi.loginEndpoint.serverLogic:
      (loginRequest: LoginRequest) =>
        StudentService.login[F](loginRequest, config, session).value

  @SuppressWarnings(Array("org.wartremover.warts.Nothing"))
  def chatServerEndpoint[F[_]: Async: OpenAiClient: Clock: SecureRandom](
    systemMsg: String,
    session: Ref[F, Set[UUID]],
  ) =
    AssistantApi.chatEndpoint
      .serverSecurityLogic(StudentService.checkToken[F](_, session).value)
      .serverLogic: 
        _ => (chatMessages: Seq[ChatMessage]) =>
          OpenAiService.chat[F](systemMsg, chatMessages).value

  def allEndpoints[F[_]: Async: OpenAiClient: Clock: SecureRandom](
    config: BackendConfig,
    session: Ref[F, Set[UUID]],
  ) = List(
    rootServerEndpoint[F],
    loginServerEndpoint[F](config, session),
    chatServerEndpoint[F](config.systemMsg, session),
  )

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def getServer[F[_]: Async: OpenAiClient: Clock: SecureRandom](
      config: BackendConfig,
      dispatcher: Dispatcher[F],
      session: Ref[F, Set[UUID]],
  ): F[Server] = Async[F].async_[Server]: cb =>
    def log[F[_]: Async](
        level: scribe.Level,
    )(msg: String, exOpt: Option[Throwable])(using
        mdc: scribe.data.MDC,
    ): F[Unit] = Async[F].delay:
      exOpt match
        case None     => scribe.log(level, mdc, msg)
        case Some(ex) => scribe.log(level, mdc, msg, ex)
    val serverLog = DefaultServerLog(
      doLogWhenReceived = log(scribe.Level.Info)(_, None),
      doLogWhenHandled = log(scribe.Level.Info),
      doLogAllDecodeFailures = log(scribe.Level.Info),
      doLogExceptions =
        (msg: String, ex: Throwable) => Async[F].delay(scribe.warn(msg, ex)),
      noLog = Async[F].pure(()),
    )
    val serverOptions = ArmeriaCatsServerOptions
      .customiseInterceptors[F](dispatcher)
      .corsInterceptor(CORSInterceptor.default)
      .serverLog(serverLog)
      .options
    val tapirService = ArmeriaCatsServerInterpreter[F](serverOptions)
      .toService(allEndpoints[F](config, session))
    val server = Server.builder
      .maxRequestLength(128 * 1024 * 1024)
      .requestTimeout(java.time.Duration.ofMinutes(10))
      .http(config.server.port)
      .service(tapirService)
      .build
    server.start.handle[Unit]:
      case (_, null)  => cb(server.asRight[Throwable])
      case (_, cause) => cb(cause.asLeft[Server])

    ()


  def resource[F[_]: Async: OpenAiClient: Clock: SecureRandom](
      config: BackendConfig,
      session: Ref[F, Set[UUID]],
  ): Resource[F, Server] =
    for
      dispatcher <- Dispatcher.parallel[F]
      server <- Resource.make(getServer(config, dispatcher, session)): server =>
        Async[F]
          .fromCompletableFuture(Async[F].delay(server.closeAsync()))
          .map(_ => ())
    yield server
