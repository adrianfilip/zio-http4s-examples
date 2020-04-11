package uzsttp.servers
import uzhttp._
import Request._
import uzhttp.header._
import java.net.URI

import zio.stream.{Stream, StreamChunk, Take, ZStream}
import zio._
import zio.test._
import Assertion._
import uzhttp.Version.Http11
import uzhttp.Request.Method.GET
import HTTPError._
import uzhttp.server.Server
import java.net.InetSocketAddress

import uzsttp.auth.Authorizer
import uzsttp.auth.Authorizer.{AuthInfo, Authorizer}
import zio.blocking.Blocking
import zio.clock.Clock

object TestUtil {

  def serverLayer(handler: PartialFunction[Request, IO[HTTPError, Response]]) = ZLayer.fromManaged(
    Server.builder(new InetSocketAddress("127.0.0.1", 8080))
  .handleSome(handler)
  .serve
  )

  def serverLayerM[R](handlerM: RIO[R, PartialFunction[Request, IO[HTTPError, Response]]]) =
    ZLayer.fromManaged {
      val zm = handlerM.map { handler =>
        Server.builder(new InetSocketAddress("127.0.0.1", 8080))
          .handleSome(handler)
          .serve
      }
      ZManaged.unwrap(zm)
    }

  def authLayer(handler: PartialFunction[(Request, AuthInfo), IO[HTTPError, Response]]):
    ZLayer[Blocking with Clock with Authorizer, Throwable, Has[Server]] =
    serverLayerM[Authorizer](Authorizer.authorized(handler))

  type UZServer = Has[Server]

  def serverUp = ZIO.access[UZServer](_.get).map{_.awaitUp}

 
}
