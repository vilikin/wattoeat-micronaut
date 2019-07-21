package micronaut.server

import io.micronaut.runtime.Micronaut
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info

@OpenAPIDefinition(
  info = Info(
    title = "Wattoeat API",
    version = "1.0",
    description = "Wattoeat API"
  )
)
object Application {

  @JvmStatic
  fun main(args: Array<String>) {
    Micronaut.build()
      .packages("micronaut.server")
      .mainClass(Application.javaClass)
      .start()
  }
}
