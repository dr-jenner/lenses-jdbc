package com.landoop.jdbc4

import com.landoop.rest.RestClient
import com.landoop.rest.domain.Credentials
import fi.iki.elonen.NanoHTTPD
import io.kotlintest.Spec
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.specs.WordSpec
import javax.net.ssl.SSLHandshakeException

class RestClientTest : WordSpec() {

  class LoginServer : NanoHTTPD(61864) {
    override fun serve(session: IHTTPSession): Response {
      return newFixedLengthResponse("""{"success":true, "token": "wibble"}""".trimIndent())
    }
  }

  val server = LoginServer()

  override fun interceptSpec(context: Spec, spec: () -> Unit) {
    server.makeSecure(NanoHTTPD.makeSSLSocketFactory("/keystore.jks", "password".toCharArray()), null)
    server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    spec()
    server.stop()
  }

  init {
    "RestClient" should {
      "support self signed certificates if weak ssl is set to true" {
        val client = RestClient(listOf("https://localhost:61864"), Credentials("any", "any"), true)
        client.token shouldBe "wibble"
      }
      "reject self signed certificates if weak ssl is set to false" {
        shouldThrow<SSLHandshakeException> {
          RestClient(listOf("https://localhost:61864"), Credentials("any", "any"), false)
        }
      }
    }
  }
}