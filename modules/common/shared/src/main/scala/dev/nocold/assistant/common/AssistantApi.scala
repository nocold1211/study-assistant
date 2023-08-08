package dev.nocold.assistant.common

import model.*

import java.util.UUID

import io.circe.generic.auto.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*

object AssistantApi:

  val loginEndpoint = endpoint.post
    .in("login")
    .in(jsonBody[LoginRequest])
    .errorOut(stringBody)
    .out(jsonBody[LoginResponse])

  val chatEndpoint = endpoint.post
    .securityIn(auth.bearer[UUID]())
    .in("chat")
    .in(jsonBody[Seq[ChatMessage]])
    .errorOut(stringBody)
    .out(jsonBody[ChatResponse])

  val rootEndpoint = endpoint.get.in("").out(stringBody)
  