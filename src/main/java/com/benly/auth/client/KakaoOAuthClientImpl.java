package com.benly.auth.client;

import com.benly.auth.client.dto.KakaoTokenResponse;
import com.benly.auth.client.dto.KakaoUserResponse;
import com.benly.auth.exception.AuthErrorCode;
import com.benly.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
            if (user == null || user.id() == null) {
                throw new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED);
            }
            return String.valueOf(user.id());
        } catch (RestClientException e) {
            throw new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED);
        }
    }

    // TODO: Client Secret 적용 (현재 카카오 콘솔에서 비활성화 상태)
    private String requestAccessToken(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);

        KakaoTokenResponse response = restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED);
        }

        return response.accessToken();
    }

    private KakaoUserResponse requestKakaoUser(String accessToken) {
        return restClient.get()
                .uri(userInfoUri)
                // TODO: JWT 필터 도입 후 SecurityContext에서 userId 추출로 변경
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(KakaoUserResponse.class);
    }


}
