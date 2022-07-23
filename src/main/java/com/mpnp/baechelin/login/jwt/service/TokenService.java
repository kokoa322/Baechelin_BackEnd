package com.mpnp.baechelin.login.jwt.service;

import com.mpnp.baechelin.common.properties.AppProperties;
import com.mpnp.baechelin.exception.CustomException;
import com.mpnp.baechelin.exception.ErrorCode;
import com.mpnp.baechelin.login.oauth.common.AuthResponse;
import com.mpnp.baechelin.login.oauth.entity.RoleType;
import com.mpnp.baechelin.login.jwt.AuthToken;
import com.mpnp.baechelin.login.jwt.AuthTokenProvider;
import com.mpnp.baechelin.login.jwt.entity.UserRefreshToken;
import com.mpnp.baechelin.login.jwt.repository.UserRefreshTokenRepository;
import com.mpnp.baechelin.util.CookieUtil;
import com.mpnp.baechelin.util.HeaderUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {

    private final AppProperties appProperties;
    private final AuthTokenProvider tokenProvider;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final static String REFRESH_TOKEN = "refresh_token";

    /**
     * 새로운 Access Token을 재발급 받는 메소드
     * Access Token이 만료되었는 지와 상관없이 재발급
     * Access Token을 재발급 받을 때, Refresh Token도 재발급해주어서 보안을 높인다.
     * @param request Access Token이 들어있는
     * @param response 쿠키를 삭제하기 위한 response
     * @return 재발급된 Access Token
     */
    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = HeaderUtil.getAccessToken(request);
        AuthToken authToken = tokenProvider.convertAuthToken(accessToken);

        Claims claims = authToken.getExpiredTokenClaims();

        // 유효한 access token 인지, 만료된 token 인지 확인
        if (authToken.getExpiredTokenClaims() == null) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        String userId = claims.getSubject();
        RoleType roleType = RoleType.of(claims.get("role", String.class));

        // refresh token을 cookie에서 가져온다.
        String refreshToken = CookieUtil.getCookie(request, REFRESH_TOKEN)
                .map(Cookie::getValue)
                .orElse((null));
        AuthToken authRefreshToken = tokenProvider.convertAuthToken(refreshToken);

        if (authRefreshToken.getToken() == null || !authRefreshToken.tokenValidate()) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // userId와 refresh token 으로 DB 확인
        UserRefreshToken userRefreshToken = userRefreshTokenRepository.findBySocialIdAndRefreshToken(userId, refreshToken);
        if (userRefreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Access token 재발급
        Date now = new Date();

        AuthToken newAccessToken = tokenProvider.createAuthToken(
                userId,
                roleType.getCode(),
                new Date(now.getTime() + appProperties.getAuth().getTokenExpiry())
        );


        // refresh 토큰 유효기간 가져오기
        long refreshTokenExpiry = appProperties.getAuth().getRefreshTokenExpiry();

        // refresh 토큰 생성
        authRefreshToken = tokenProvider.createAuthToken(
                appProperties.getAuth().getTokenSecret(),
                new Date(now.getTime() + refreshTokenExpiry)
        );

        // DB에 refresh 토큰 업데이트
        userRefreshToken.setRefreshToken(authRefreshToken.getToken());


        int cookieMaxAge = (int) refreshTokenExpiry / 60;
        CookieUtil.deleteCookie(request, response, REFRESH_TOKEN);
        CookieUtil.addCookie(response, REFRESH_TOKEN, authRefreshToken.getToken(), cookieMaxAge);


        return new AuthResponse(newAccessToken.getToken());
    }
}
