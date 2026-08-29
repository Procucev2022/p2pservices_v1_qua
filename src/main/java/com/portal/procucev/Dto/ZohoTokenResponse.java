package com.portal.procucev.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ZohoTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonProperty("token_type")
    private String tokenType;

    // Sometimes not returned, but safe to keep
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * Zoho reports refresh failures as HTTP 200 with an error body such as
     * {"error":"invalid_client_secret"}, so this must be inspected rather than
     * relying on the response status.
     */
    @JsonProperty("error")
    private String error;
}
