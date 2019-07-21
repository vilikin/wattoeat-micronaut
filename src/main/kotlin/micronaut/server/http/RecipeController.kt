package micronaut.server.http

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.reactivex.Single
import micronaut.server.core.Recipe
import micronaut.server.core.RecipeService

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
}