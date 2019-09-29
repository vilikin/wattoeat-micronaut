package micronaut.server.core

import com.contentful.java.cda.CDAAsset
import com.contentful.java.cda.CDAClient
import com.contentful.java.cda.TransformQuery.ContentfulEntryModel
import com.contentful.java.cda.TransformQuery.ContentfulField
import com.contentful.java.cda.TransformQuery.ContentfulSystemField
import com.contentful.java.cma.CMAClient
import com.contentful.java.cma.model.CMAAsset
import com.contentful.java.cma.model.CMAAssetFile
import com.contentful.java.cma.model.CMAEntry
import com.contentful.java.cma.model.CMALink
import com.contentful.java.cma.model.CMASystem
import com.contentful.java.cma.model.CMAType
import com.fasterxml.jackson.annotation.JsonIgnore
import io.micronaut.http.HttpStatus
import io.reactivex.Flowable
import io.reactivex.Single
import micronaut.server.Config
import micronaut.server.http.CreateRecipeBody
import micronaut.server.http.HttpError
import java.io.File
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
    get() = if (image != null) "https:${image.url()}" else null

  // Empty constructor for Contentful to be able to instantiate the class
  constructor(): this("", listOf(), null, null, null, "")
}

@Singleton
class RecipeService(
  config: Config
) {
  private val cdaClient: CDAClient = CDAClient.builder()
    .setSpace(config.contentful.space)
    .setToken(config.contentful.cda_token)
    .build()

  private val cmaClient: CMAClient = CMAClient.Builder()
    .setSpaceId(config.contentful.space)
    .setAccessToken(config.contentful.cma_token)
    .build()

  fun findAllRecipes(): Flowable<Recipe> {
    return cdaClient
      .observeAndTransform(Recipe::class.java)
      .all()
      .flatMap { Flowable.fromIterable(it) }
  }

  fun findRecipe(id: String): Single<Recipe> {
    return cdaClient
      .observeAndTransform(Recipe::class.java)
      .one(id)
      .singleOrError()
      .onErrorResumeNext { e: Throwable ->
        Single.error(HttpError(HttpStatus.NOT_FOUND, "No recipe found with given id", e))
      }
  }

  fun createRecipe(
    body: CreateRecipeBody
  ): Single<Recipe> {
    val entry = CMAEntry()

    entry.setField("name", "fi", body.name)

    body.imageAssetId?.let {
      val imageAssetLink = CMALink()
        .setSystem(
          CMASystem()
            .setType(CMAType.Link)
            .setLinkType(CMAType.Asset)
            .setId(it)
        )
      entry.setField("image", "fi", imageAssetLink)
    }

    body.ingredients?.let {
      entry.setField("ingredients", "fi", it)
    }

    body.instructions?.let {
      entry.setField("instructions", "fi", it)
    }

    body.link?.let {
      entry.setField("link", "fi", it)
    }

    val createdEntry = cmaClient.entries().create("recipe", entry)
    val publishedEntry = cmaClient.entries().publish(createdEntry)
    return findRecipe(publishedEntry.id)
  }

  fun createImageAsset(name: String, image: File): CMAAsset {
    val upload = cmaClient.uploads().create(image.inputStream())
    val asset = CMAAsset().setFields(
      CMAAsset.Fields()
        .setFile(
          "fi",
          CMAAssetFile()
            .setFileName(name)
            .setContentType("image/jpeg")
            .setUploadFrom(
              CMALink()
                .setSystem(
                  CMASystem()
                    .setType(CMAType.Link)
                    .setLinkType(CMAType.Upload)
                    .setId(upload.id)
                )
            )
        )
    )

    val createdAsset = cmaClient.assets().create(asset)
    cmaClient.assets().process(createdAsset, "fi")

    // We need to manually up the version
    val processedAsset = setAssetVersion(createdAsset, 2)
    val publishedAsset = cmaClient.assets().publish(processedAsset)

    return publishedAsset
  }

  private fun setAssetVersion(asset: CMAAsset, version: Int): CMAAsset {
    asset.version = version
    asset.fields.getFile("fi").uploadFrom.version = version
    return asset
  }
}
