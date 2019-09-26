package micronaut.server

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("custom")
class Config(
  val server: ServerConfig,
  val contentful: ContentfulConfig
)

@ConfigurationProperties("custom.server")
class ServerConfig {
  lateinit var secret: String
}

@ConfigurationProperties("custom.contentful")
class ContentfulConfig {
  lateinit var space: String
  lateinit var cda_token: String
  lateinit var cma_token: String
}
