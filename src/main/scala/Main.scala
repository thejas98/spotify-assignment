import requests._
import ujson._

object SpotifyPlaylistAnalyzer {

  val clientId = ""
  val clientSecret = ""
  val playlistId = "5Rrf7mqN8uus2AaQQQNdc1"
  val accessTokenUrl = "https://accounts.spotify.com/api/token"
  val playlistUrl = s"https://api.spotify.com/v1/playlists/$playlistId"
  val artistUrl = "https://api.spotify.com/v1/artists/"

  // Function to get Spotify access token
  def getAccessToken(clientId: String, clientSecret: String): String = {
    val response = post(
      accessTokenUrl,
      data = Map(
        "grant_type" -> "client_credentials"
      ),
      headers = Map(
        "Authorization" -> s"Basic ${java.util.Base64.getEncoder.encodeToString(s"$clientId:$clientSecret".getBytes("UTF-8"))}"
      )
    )
    ujson.read(response.text)("access_token").str
  }

  // Function to get playlist data
  def getPlaylist(accessToken: String): Value = {
    ujson.read(requests.get(
      playlistUrl,
      headers = Map(
        "Authorization" -> s"Bearer $accessToken"
      )
    ).text)
  }

  // Function to get artist details
  def getArtistDetails(artistId: String, accessToken: String): Value = {
    ujson.read(requests.get(
      s"$artistUrl$artistId",
      headers = Map(
        "Authorization" -> s"Bearer $accessToken"
      )
    ).text)
  }

  def main(args: Array[String]): Unit = {
    val accessToken = getAccessToken(clientId, clientSecret)

    // Part 1: Get the top 10 longest songs
    val playlistResponse = getPlaylist(accessToken)

    val topSongs = for {
      items <- playlistResponse("tracks")("items").arr
      track = items("track").obj
      durationMs = track("duration_ms").num.toLong
    } yield (track("name").str, durationMs)

    val top10Songs = topSongs.sortBy(-_._2).take(10)

    println("Part 1)")
    top10Songs.foreach { case (songName, durationMs) =>
      println(s"$songName , $durationMs")
    }

    // Part 2: Get artist details and order by followers

    val topArtists = for {
      items <- playlistResponse("tracks")("items").arr
      track = items("track").obj
      durationMs = track("duration_ms").num.toLong
      artists <- track("artists").arr
      artistId = artists("id").str
      if top10Songs.exists { case (songName, _) => track("name").str == songName }
    } yield (artistId, durationMs)


    val artistDetails = topArtists.map { case (artistId, _) =>
      val details = getArtistDetails(artistId, accessToken)
      (details("name").str, details("followers")("total").num.toLong)
    }


    val orderedArtistDetails = artistDetails.distinct.sortBy(_._2).reverse

    println("\nPart 2)")
    orderedArtistDetails.foreach { case (artistName, followers) =>
      println(s"$artistName : $followers")
    }
  }
}
