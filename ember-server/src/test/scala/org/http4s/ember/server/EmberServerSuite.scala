/*
 * Copyright 2019 http4s.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.http4s.ember.server

import cats.syntax.all._
import cats.effect._
import org.http4s._
import org.http4s.server.Server
import org.http4s.implicits._
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.client.EmberClientBuilder

import java.net.BindException

class EmberServerSuite extends Http4sSuite {

  def service[F[_]](implicit F: Async[F]): HttpApp[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    HttpRoutes
      .of[F] { case GET -> Root =>
        Ok("Hello!")
      }
      .orNotFound
  }

  val serverResource: Resource[IO, Server] =
    EmberServerBuilder
      .default[IO]
      .withHttpApp(service[IO])
      .build

  val client = ResourceFixture(EmberClientBuilder.default[IO].build)

  val server = ResourceFixture(
    EmberServerBuilder
      .default[IO]
      .withHttpApp(service[IO])
      .build)

  val fixture = (server, client).mapN(FunFixture.map2(_, _))

  fixture.test("server responds to requests") { case (server, client) =>
    client
      .get(s"http://${server.address.getHostName}:${server.address.getPort}")(_.status.pure[IO])
      .assertEquals(Status.Ok)
  }

  server.test("server startup fails if address is already in use") { case _ =>
    serverResource.use(_ => IO.unit).intercept[BindException]
  }
}
