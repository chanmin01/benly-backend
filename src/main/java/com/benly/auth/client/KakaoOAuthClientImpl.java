package com.benly.auth.client;

import com.benly.auth.client.dto.KakaoTokenResponse;
import com.benly.auth.client.dto.KakaoUserResponse;
import com.benly.auth.exception.AuthErrorCode;
import com.benly.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoOAuthClientImpl implements KakaoOAuthClient {

    private final RestClient restClient;
    private final String clientId;
    private final String redirectUri;
    private final String tokenUri;
    private final String userInfoUri;

    public KakaoOAuthClientImpl(
            RestClient restClient,
            @Value("${kakao.client-id}") String clientId,
            @Value("${kakao.redirect-uri}") String redirectUri,
            @Value("${kakao.token-uri}") String tokenUri,
            @Value("${kakao.user-info-uri}") String userInfoUri
    ) {
        this.restClient = restClient;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
    }

    @Override
    public String getKakaoId(String authorizationCode) {
        try {
            String accessToken = requestAccessToken(authorizationCode);
            KakaoUserResponse user = requestKakaoUser(accessToken);
            return String.valueOf(user.id());
        } catch (RestClientException e) {
            throw new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED);
        }
    }

    private String requestAccessToken(String authorizationCode) {
        KakaoTokenResponse response = restClient.post()
                .uri(tokenUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("grant_type=authorization_code"
                        + "&client_id=" + clientId
                        + "&redirect_uri=" + redirectUri
                        + "&code=" + authorizationCode)
                .retrieve()
                .body(KakaoTokenResponse.class);

        return response.accessToken();
    }

    private KakaoUserResponse requestKakaoUser(String accessToken) {
        return restClient.get()
                .uri(userInfoUri)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserResponse.class);
    }


}
