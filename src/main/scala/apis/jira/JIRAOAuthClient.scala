package apis.jira

import akka.event.LoggingAdapter
import com.google.inject.Inject
import com.typesafe.config.Config

/**
  * Created by markmo on 15/10/2016.
  */
class JIRAOAuthClient @Inject()(config: Config, logger: LoggingAdapter) {

  val callbackURI = config.getString("api.host")

  val consumerKey = config.getString("services.atlassian.jira.consumer.key")

  val consumerPrivateKey = System.getenv("JIRA_CONSUMER_PRIVATE_KEY")

  def requestToken(baseURL: String, callback: String = "oob"): (TokenSecretVerifierHolder, String) = {
    val client = new AtlassianOAuthClient(consumerKey, consumerPrivateKey, baseURL, callback)

    // STEP 1: get request token
    val requestToken: TokenSecretVerifierHolder = client.getRequestToken
    val authorizeURL: String = client.getAuthorizeURLForToken(requestToken.token)
    logger.debug("Token is [{}]", requestToken.token)
    logger.debug("Token secret is [{}]", requestToken.secret)
    logger.debug("Retrieved request token. Goto {}", authorizeURL)
    (requestToken, authorizeURL)
  }

  def accessToken(baseURL: String, requestToken: String, tokenSecret: String, verifier: String): String = {
    val client = new AtlassianOAuthClient(consumerKey, consumerPrivateKey, baseURL, callbackURI)
    val accessToken = client.swapRequestTokenForAccessToken(requestToken, tokenSecret, verifier)
    logger.debug("Access token is [{}]", accessToken)
    accessToken
  }

  def request(accessToken: String, url: String): String = {
    val client = new AtlassianOAuthClient(consumerKey, consumerPrivateKey, null, callbackURI)
    client.makeAuthenticatedRequest(url, accessToken)
  }

}
