package dev.nocold.assistant
package backend
package student

import java.nio.ByteBuffer
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import cats.Monad
import cats.data.EitherT
import cats.effect.{Async, Clock, Ref}
import cats.effect.std.SecureRandom
import cats.syntax.all.*

import scodec.bits.ByteVector

import common.model.*

object StudentService:
  def login[F[_]: Async: Clock: SecureRandom](
      loginRequest: LoginRequest,
      config: BackendConfig,
      session: Ref[F, Set[UUID]],
  ): EitherT[F, String, LoginResponse] =
    val loginConfigOption: Option[BackendConfig.LoginConfig] = config.login.get(loginRequest.username)
    for
      loginConfig <- EitherT.fromOption[F](loginConfigOption, s"No user of name ${loginRequest.username}")
      _ <- EitherT.fromEither[F](checkPassword(loginConfig, loginRequest.password))
      accessToken <- EitherT.right(createAccessToken[F])
      _ <- EitherT.right(session.update(_ + accessToken))
    yield LoginResponse(accessToken)

  def checkPassword[F[_]: Async](
      loginConfig: BackendConfig.LoginConfig,
      password: String,
  ): Either[String, Unit] =
    val hash = pbkdf2Hash(password, loginConfig.salt)
    Either.cond(
      hash === loginConfig.passwordHash,
      (),
      "Invalid password",
    )

  def pbkdf2Hash(content: String, salt: UUID): ByteVector =
    val saltArray = uuidToByteArray(salt)
    val spec = new PBEKeySpec(content.toCharArray(), saltArray, 65536, 128)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val hash = factory.generateSecret(spec).getEncoded()
    ByteVector.view(hash)

  def createAccessToken[F[_]: Async: SecureRandom]: F[UUID] = for
    token0 <- SecureRandom[F].nextLong
    token1 <- SecureRandom[F].nextLong
  yield new UUID(token0, token1)

  def uuidToByteArray(uuid: UUID): Array[Byte] =
    ByteBuffer
      .allocate(16)
      .putLong(uuid.getMostSignificantBits)
      .putLong(uuid.getLeastSignificantBits)
      .array()

  def checkToken[F[_]: Async](
      token: UUID,
      session: Ref[F, Set[UUID]],
  ): EitherT[F, String, Unit] = EitherT:
    session.get.map: tokens =>
      Either.cond(
        tokens.contains(token),
        (),
        "Invalid token",
      )

  def createHashAndSalt[F[_]: Monad: SecureRandom](password: String): F[(ByteVector, UUID)] =
    for
      salt0 <- SecureRandom[F].nextLong
      salt1 <- SecureRandom[F].nextLong
    yield
      val salt = new UUID(salt0, salt1)
      val hash = pbkdf2Hash(password, salt)
      (hash, salt)
