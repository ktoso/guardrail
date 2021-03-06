package com.twilio.swagger.generators.http4s

import _root_.io.swagger.parser.SwaggerParser
import cats.instances.all._
import com.twilio.swagger._
import com.twilio.swagger.codegen.generators.Http4s
import com.twilio.swagger.codegen.{ClassDefinition, Client, Clients, Context, ClientGenerator, ProtocolGenerator, ProtocolDefinitions, RandomType, CodegenApplication, Target}
import org.scalatest.{FunSuite, Matchers}
import scala.meta._

class BasicTest extends FunSuite with Matchers {
  val swagger = s"""
    |swagger: "2.0"
    |info:
    |  title: Whatever
    |  version: 1.0.0
    |host: localhost:1234
    |schemes:
    |  - http
    |paths:
    |  /foo:
    |    get:
    |      operationId: getFoo
    |      responses:
    |        200:
    |          description: Success
    |    put:
    |      operationId: putFoo
    |      responses:
    |        200:
    |          description: Success
    |    post:
    |      operationId: postFoo
    |      responses:
    |        200:
    |          description: Success
    |    delete:
    |      operationId: deleteFoo
    |      responses:
    |        200:
    |          description: Success
    |    patch:
    |      operationId: patchFoo
    |      responses:
    |        200:
    |          description: Success
    |  /bar:
    |    get:
    |      operationId: getBar
    |      responses:
    |        200:
    |          type: object
    |  /baz:
    |    get:
    |      operationId: getBaz
    |      responses:
    |        200:
    |          schema:
    |            $$ref: "#/definitions/Baz"
    |definitions:
    |  Baz:
    |    type: object
    |  Blix:
    |    type: object
    |    required:
    |      - map
    |    properties:
    |      map:
    |        type: object
    |""".stripMargin


  test("Generate JSON alias definitions") {
    val (
      ProtocolDefinitions(RandomType(_, tpe) :: _, _, _, _),
      _,
      _
    ) = runSwaggerSpec(swagger)(Context.empty, Http4s)

    tpe.structure should equal(t"io.circe.Json".structure)
  }

  test("Handle json subvalues") {
    val (
      ProtocolDefinitions(_ :: ClassDefinition(_, _, cls, cmp) :: _, _, _, _),
      _,
      _
    ) = runSwaggerSpec(swagger)(Context.empty, Http4s)

    val definition = q"""
      case class Blix(map: io.circe.Json)
    """

    val companion = q"""
      object Blix {
        implicit val encodeBlix = {
          val readOnlyKeys = Set[String]()
          Encoder.forProduct1("map")((o: Blix) => o.map).mapJsonObject(_.filterKeys(key => !(readOnlyKeys contains key)))
        }
        implicit val decodeBlix = Decoder.forProduct1("map")(Blix.apply _)
      }
    """

    cls.structure should equal(definition.structure)
    cmp.structure should equal(companion.structure)
  }

  test("Properly handle all methods") {
    val (
      _,
      Clients(Client(tags, className, statements) :: _),
      _
    ) = runSwaggerSpec(swagger)(Context.empty, Http4s)
    val List(cmp, cls) = statements.dropWhile(_.isInstanceOf[Import])

    val client = q"""
    class Client(host: String = "http://localhost:1234")(implicit httpClient: Client) {
      val basePath: String = ""
      def getFoo(headers: List[Header] = List.empty): Task[IgnoredEntity] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        httpClient.expect[IgnoredEntity](Request(method = Method.GET, uri = Uri.unsafeFromString(host + basePath + "/foo"), headers = Headers(allHeaders)).withBody(EmptyBody))
      }
      def putFoo(headers: List[Header] = List.empty): Task[IgnoredEntity] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        httpClient.expect[IgnoredEntity](Request(method = Method.PUT, uri = Uri.unsafeFromString(host + basePath + "/foo"), headers = Headers(allHeaders)).withBody(EmptyBody))
      }
      def postFoo(headers: List[Header] = List.empty): Task[IgnoredEntity] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        httpClient.expect[IgnoredEntity](Request(method = Method.POST, uri = Uri.unsafeFromString(host + basePath + "/foo"), headers = Headers(allHeaders)).withBody(EmptyBody))
      }
      def deleteFoo(headers: List[Header] = List.empty): Task[IgnoredEntity] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        httpClient.expect[IgnoredEntity](Request(method = Method.DELETE, uri = Uri.unsafeFromString(host + basePath + "/foo"), headers = Headers(allHeaders)).withBody(EmptyBody))
      }
      def patchFoo(headers: List[Header] = List.empty): Task[IgnoredEntity] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        httpClient.expect[IgnoredEntity](Request(method = Method.PATCH, uri = Uri.unsafeFromString(host + basePath + "/foo"), headers = Headers(allHeaders)).withBody(EmptyBody))
      }
      def getBar(headers: List[Header] = List.empty): Task[IgnoredEntity] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        httpClient.expect[IgnoredEntity](Request(method = Method.GET, uri = Uri.unsafeFromString(host + basePath + "/bar"), headers = Headers(allHeaders)).withBody(EmptyBody))
      }
      def getBaz(headers: List[Header] = List.empty): Task[io.circe.Json] = {
        val allHeaders = headers ++ List[Option[Header]]().flatten
        httpClient.expect[io.circe.Json](Request(method = Method.GET, uri = Uri.unsafeFromString(host + basePath + "/baz"), headers = Headers(allHeaders)).withBody(EmptyBody))
      }
    }
    """

    cls.structure should equal(client.structure)
  }
}
