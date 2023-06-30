package no.nav.tiltakspenger.tiltak.auth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OAuth2AccessTokenResponse(
    @JsonProperty("token_type")
    val tokenType: String,
    @JsonProperty("access_token")
    var accessToken: String,
    @JsonProperty("ext_expires_in")
    val extExpiresIn: Int,
    @JsonProperty("expires_in")
    val expiresIn: Int,
)
