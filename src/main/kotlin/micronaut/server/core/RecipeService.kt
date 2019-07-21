package micronaut.server.core

import com.contentful.java.cda.CDAAsset
import com.contentful.java.cda.CDAClient
import com.contentful.java.cda.TransformQuery.*
import com.fasterxml.jackson.annotation.JsonIgnore
import io.micronaut.http.HttpStatus
import io.reactivex.Flowable
import io.reactivex.Single
import micronaut.server.Config
import micronaut.server.http.HttpError
import javax.inject.Singleton

@ContentfulEntryModel("recipe")
class Recipe(
  @field:ContentfulField
  val name: String,

  @field:ContentfulField
  val ingredients: List<String>,

  @field:ContentfulField
  val instructions: String?,

  @field:ContentfulField
  val link: String?,

  @field:ContentfulField
  @JsonIgnore
  val image: CDAAsset?,

  @field:ContentfulSystemField("id")
  val contentfulId: String
) {
  val imageUrl: String?
    get() = if (image != null) "https://${image.url()}" else null

  // Empty constructor for Contentful to be able to instantiate the class
  constructor(): this("", listOf(), null, null, null, "")
}

@Singleton
class RecipeService(
  config: Config
) {
  private val client: CDAClient = CDAClient.builder()
    .setSpace(config.contentful.space)
    .setToken(config.contentful.token)
    .build()

  fun findAllRecipes(): Flowable<Recipe> {
    return client
      .observeAndTransform(Recipe::class.java)
      .all()
      .flatMap { Flowable.fromIterable(it) }
  }

  fun findRecipe(id: String): Single<Recipe> {
    return client
      .observeAndTransform(Recipe::class.java)
      .one(id)
      .singleOrError()
      .onErrorResumeNext { e: Throwable ->
        Single.error(HttpError(HttpStatus.NOT_FOUND, "No recipe found with given id", e))
      }
  }
}
