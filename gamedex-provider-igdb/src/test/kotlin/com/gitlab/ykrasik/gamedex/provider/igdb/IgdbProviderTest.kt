package com.gitlab.ykrasik.gamedex.provider.igdb

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.test.*
import io.kotlintest.matchers.*
import io.kotlintest.mock.`when`
import io.kotlintest.mock.mock

/**
 * User: ykrasik
 * Date: 16/04/2017
 * Time: 15:41
 */
class IgdbProviderTest : ScopedWordSpec() {
    init {
        "IgdbProvider.search" should {
            "be able to return a single search result".inScope(Scope()) {
                val searchResult = searchResult(name = name, releaseDate = releaseDate)

                givenClientSearchReturns(listOf(searchResult), name = name)

                search(name) shouldBe listOf(ProviderSearchResult(
                    apiUrl = "$baseUrl/${searchResult.id}",
                    name = name,
                    releaseDate = releaseDate,
                    criticScore = Score(searchResult.aggregatedRating!!, searchResult.aggregatedRatingCount!!),
                    userScore = Score(searchResult.rating!!, searchResult.ratingCount!!),
                    thumbnailUrl = thumbnailUrl(searchResult.cover!!.cloudinaryId!!)
                ))
            }

            "be able to return empty search results".inScope(Scope()) {
                givenClientSearchReturns(emptyList())

                search() shouldBe emptyList<ProviderSearchResult>()
            }

            "be able to return multiple search results".inScope(Scope()) {
                val searchResult1 = searchResult(name)
                val searchResult2 = searchResult("$name ${randomString()}")
                givenClientSearchReturns(listOf(searchResult1, searchResult2), name)

                search(name) should have2SearchResultsThat { first, second ->
                    first.name shouldBe searchResult1.name
                    second.name shouldBe searchResult2.name
                }
            }

            "filter search results that do not contain all words of the original search term".inScope(Scope()) {
                val name = "must contain all words"

                val searchResult1 = searchResult("must contain all")
                val searchResult2 = searchResult("contain all words")
                val searchResult3 = searchResult("must all words")
                val searchResult4 = searchResult("must contain")
                val searchResult5 = searchResult("case Insensitive must additions ContAin possibly ALL WorDs stuff")
                val searchResult6 = searchResult("words all contain must")

                givenClientSearchReturns(listOf(
                    searchResult1, searchResult2, searchResult3,
                    searchResult4, searchResult5, searchResult6
                ), name)

                search(name) should have2SearchResultsThat { first, second ->
                    first.name shouldBe searchResult5.name
                    second.name shouldBe searchResult6.name
                }
            }

            "handle null aggregatedRating and ignore aggregatedRatingCount".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult().copy(aggregatedRating = null, aggregatedRatingCount = 1)))

                search() should haveASingleSearchResultThat {
                    it.criticScore shouldBe null
                }
            }

            "handle null rating and ignore ratingCount".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult().copy(rating = null, ratingCount = 1)))

                search() should haveASingleSearchResultThat {
                    it.userScore shouldBe null
                }
            }

            "handle null releaseDates".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult().copy(releaseDates = null)))

                search() should haveASingleSearchResultThat {
                    it.releaseDate shouldBe null
                }
            }

            "use null releaseDate when no releaseDate exists for given platform".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult(releaseDatePlatformId = platformId + 1)))

                search() should haveASingleSearchResultThat {
                    it.releaseDate shouldBe null
                }
            }

            "parse a release date of format YYYY-MMM-dd and return YYYY-MM-dd instead".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult(releaseDate = "2000-Apr-07")))

                search() should haveASingleSearchResultThat {
                    it.releaseDate shouldBe "2000-04-07"
                }
            }

            "fallback to returning the original release date when parsing as YYYY-MMM-dd fails".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult(releaseDate = "2017-Q4")))

                search() should haveASingleSearchResultThat {
                    it.releaseDate shouldBe "2017-Q4"
                }
            }

            "handle null cover cloudinaryId".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult().copy(cover = image(cloudinaryId = null))))

                search() should haveASingleSearchResultThat {
                    it.thumbnailUrl shouldBe null
                }
            }

            "handle null cover".inScope(Scope()) {
                givenClientSearchReturns(listOf(searchResult().copy(cover = null)))

                search() should haveASingleSearchResultThat {
                    it.thumbnailUrl shouldBe null
                }
            }
        }

        "IgdbProvider.download" should {
            "download details".inScope(Scope()) {
                val detailsResult = detailsResult(releaseDate = releaseDate)

                givenClientFetchReturns(detailsResult, apiUrl = baseUrl)

                download(baseUrl) shouldBe ProviderData(
                    header = ProviderHeader(
                        id = "Igdb",
                        apiUrl = baseUrl,
                        updateDate = nowMock
                    ),
                    gameData = GameData(
                        siteUrl = detailsResult.url,
                        name = detailsResult.name,
                        description = detailsResult.summary,
                        releaseDate = releaseDate,
                        criticScore = Score(detailsResult.aggregatedRating!!, detailsResult.aggregatedRatingCount!!),
                        userScore = Score(detailsResult.rating!!, detailsResult.ratingCount!!),
                        genres = listOf(genre)
                    ),
                    imageUrls = ImageUrls(
                        thumbnailUrl = thumbnailUrl(detailsResult.cover!!.cloudinaryId!!),
                        posterUrl = posterUrl(detailsResult.cover!!.cloudinaryId!!),
                        screenshotUrls = detailsResult.screenshots!!.map { screenshotUrl(it.cloudinaryId!!) }
                    )
                )
            }

            "handle null summary".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(summary = null))

                download().gameData.description shouldBe null
            }

            "handle null releaseDates".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(releaseDates = null))

                download().gameData.releaseDate shouldBe null
            }

            "use null releaseDate when no releaseDate exists for given platform".inScope(Scope()) {
                givenClientFetchReturns(detailsResult(releaseDatePlatformId = platformId + 1))

                download().gameData.releaseDate shouldBe null
            }

            "parse a release date of format YYYY-MMM-dd and return YYYY-MM-dd instead".inScope(Scope()) {
                givenClientFetchReturns(detailsResult(releaseDate = "2000-Apr-07"))

                download().gameData.releaseDate shouldBe "2000-04-07"
            }

            "fallback to returning the original release date when parsing as YYYY-MMM-dd fails".inScope(Scope()) {
                givenClientFetchReturns(detailsResult(releaseDate = "2017-Q4"))

                download().gameData.releaseDate shouldBe "2017-Q4"
            }

            "handle null aggregatedRating and ignore aggregatedRatingCount".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(aggregatedRating = null, aggregatedRatingCount = 1))

                download().gameData.criticScore shouldBe null
            }

            "handle null rating and ignore ratingCount".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(rating = null, ratingCount = 1))

                download().gameData.userScore shouldBe null
            }

            "handle null genres".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(genres = null))

                download().gameData.genres shouldBe emptyList<String>()
            }

            "handle null cover cloudinaryId".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(cover = image(cloudinaryId = null)))

                download().imageUrls.thumbnailUrl shouldBe null
                download().imageUrls.posterUrl shouldBe null
            }

            "handle null cover".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(cover = null))

                download().imageUrls.thumbnailUrl shouldBe null
                download().imageUrls.posterUrl shouldBe null
            }

            "handle null screenshot cloudinaryId".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(screenshots = listOf(image(cloudinaryId = null))))

                download().imageUrls.screenshotUrls shouldBe emptyList<String>()
            }

            "handle null & non-null screenshot cloudinaryId".inScope(Scope()) {
                val image = image()
                givenClientFetchReturns(detailsResult().copy(screenshots = listOf(image(cloudinaryId = null), image)))

                download().imageUrls.screenshotUrls shouldBe listOf(screenshotUrl(image.cloudinaryId!!))
            }

            "handle null screenshots".inScope(Scope()) {
                givenClientFetchReturns(detailsResult().copy(screenshots = null))

                download().imageUrls.screenshotUrls shouldBe emptyList<String>()
            }
        }
    }

    class Scope {
        val platform = randomEnum<Platform>()
        val platformId = rnd.nextInt(100)
        val genreId = rnd.nextInt(100)
        val genre = randomString()
        val name = randomName()
        val releaseDate = randomLocalDateString()
        val account = IgdbUserAccount(apiKey = randomString())

        val baseUrl = randomUrl()
        val baseImageUrl = randomUrl()

        fun thumbnailUrl(imageId: String) = "$baseImageUrl/t_thumb_2x/$imageId.jpg"
        fun posterUrl(imageId: String) = "$baseImageUrl/t_screenshot_huge/$imageId.jpg"
        fun screenshotUrl(imageId: String) = posterUrl(imageId)

        fun searchResult(name: String = this.name,
                         releaseDate: String = randomLocalDateString(),
                         releaseDatePlatformId: Int = this.platformId) = IgdbClient.SearchResult(
            id = rnd.nextInt(),
            name = name,
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            releaseDates = listOf(releaseDate(releaseDate, releaseDatePlatformId)),
            cover = image()
        )

        fun detailsResult(releaseDate: String = randomLocalDateString(),
                          releaseDatePlatformId: Int = this.platformId) = IgdbClient.DetailsResult(
            url = randomString(),
            name = name,
            summary = randomSentence(),
            releaseDates = listOf(releaseDate(releaseDate, releaseDatePlatformId)),
            aggregatedRating = randomScore().score,
            aggregatedRatingCount = randomScore().numReviews,
            rating = randomScore().score,
            ratingCount = randomScore().numReviews,
            cover = image(),
            screenshots = listOf(image(), image()),
            genres = listOf(genreId)
        )

        private fun releaseDate(releaseDate: String, platformId: Int) = IgdbClient.ReleaseDate(
            platform = platformId,
            category = 0,
            human = releaseDate
        )

        fun image(cloudinaryId: String? = randomString()) = IgdbClient.Image(cloudinaryId = cloudinaryId)

        fun givenClientSearchReturns(results: List<IgdbClient.SearchResult>, name: String = this.name) {
            `when`(client.search(name, platform, account)).thenReturn(results)
        }

        fun givenClientFetchReturns(result: IgdbClient.DetailsResult, apiUrl: String = baseUrl) {
            `when`(client.fetch(apiUrl, account)).thenReturn(result)
        }

        fun search(name: String = this.name) = provider.search(name, platform, account)

        fun download(apiUrl: String = baseUrl) = provider.download(apiUrl, platform, account)

        private val client = mock<IgdbClient>()
        val provider = IgdbProvider(IgdbConfig(
            endpoint = baseUrl,
            baseImageUrl = baseImageUrl,
            accountUrl = "",
            maxSearchResults = rnd.nextInt(),
            thumbnailImageType = IgdbProvider.IgdbImageType.thumb_2x,
            posterImageType = IgdbProvider.IgdbImageType.screenshot_huge,
            screenshotImageType = IgdbProvider.IgdbImageType.screenshot_huge,
            defaultOrder = ProviderOrderPriorities.default,
            platforms = mapOf(platform.toString() to platformId),
            genres = mapOf(genreId.toString() to genre)
        ), client)

        fun haveASingleSearchResultThat(f: (ProviderSearchResult) -> Unit) = object : Matcher<List<ProviderSearchResult>> {
            override fun test(value: List<ProviderSearchResult>): Result {
                value should haveSize(1)
                f(value.first())
                return Result(true, "")
            }
        }

        fun have2SearchResultsThat(f: (first: ProviderSearchResult, second: ProviderSearchResult) -> Unit) = object : Matcher<List<ProviderSearchResult>> {
            override fun test(value: List<ProviderSearchResult>): Result {
                value should haveSize(2)
                f(value[0], value[1])
                return Result(true, "")
            }
        }
    }
}