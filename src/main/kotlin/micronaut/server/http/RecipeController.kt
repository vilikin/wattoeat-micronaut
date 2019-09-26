package micronaut.server.http

import com.contentful.java.cma.model.CMAAsset
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.MediaType.MULTIPART_FORM_DATA
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.multipart.StreamingFileUpload
import io.reactivex.Flowable.fromPublisher
import io.reactivex.Single
import micronaut.server.core.Recipe
import micronaut.server.core.RecipeService
import java.io.File

data class UploadResult(val status: String, val asset: CMAAsset)

@Controller("\${custom.server.base-path:}/recipes")
class RecipeController(
  private val recipeService: RecipeService
) {
  @Get
  fun getRecipes(): Single<List<Recipe>> {
    return recipeService.findAllRecipes()
      .toList()
  }

  @Get("/{id}")
  fun getRecipe(id: String): Single<Recipe> {
    return recipeService.findRecipe(id)
  }

  @Post("/assets", consumes = [MULTIPART_FORM_DATA])
  fun createImageAsset(image: StreamingFileUpload): Single<CMAAsset> {
    if (image.contentType.get() != MediaType.IMAGE_JPEG_TYPE) {
      throw HttpError(HttpStatus.BAD_REQUEST, "Only JPEG images allowed")
    }

    val file = File.createTempFile("image_", ".jpg")

    return Single.just(image)
      .flatMapPublisher {
        fromPublisher(it.transferTo(file))
      }
      .map { success ->
        if (success) {
          val asset = recipeService.createImageAsset(image.filename, file)
          file.delete()
          asset
        } else {
          throw HttpError(HttpStatus.INTERNAL_SERVER_ERROR, "Couldn't upload")
        }
      }
      .firstOrError()
  }
}
