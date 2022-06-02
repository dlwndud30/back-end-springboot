package com.mycompany.backend.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Jwt {
  //jwt 설정할 때(서명할 때) 쓸 비밀키 -> 노출이 되면 안됨 -> 암호화 해야함
  private static final String JWT_SECRET_KEY = "kosa12345";
  
  //ACCESS_TOKEN의 유효기간 : 30분
  private static final long ACCESS_TOKEN_DURATION = 1000*5;
  
  //REFRESH_TOKEN의 유효기간 : 24시간
  public static final long REFRESH_TOKEN_DURATION = 1000*60*60*24;
  
  
  //
  //AccessToken 생성
  public static String createAccessToken(String mid, String authority) {  // mid: 아이디, authority: (Spring Security)권한
    log.info("AccessToken 생성");
    String accessToken = null;
    
    try {
    accessToken = Jwts.builder()
        //헤더 설정
        .setHeaderParam("alg", "HS256").setHeaderParam("typ", "JWT")
        //페이로드 설정
        .setExpiration(new Date(new Date().getTime()+ACCESS_TOKEN_DURATION))  //현재 시간으로부터 30분 뒤
        .claim("mid", mid)
        .claim("authority", authority)
        //서명 설정
        .signWith(SignatureAlgorithm.HS256, JWT_SECRET_KEY.getBytes("UTF-8"))
        .compact();  //.compact(): 문자열을 리턴해줌
    }catch(Exception e) {
      log.error(e.getMessage());
    }
    return accessToken;
  }
  
  //
  //RefreshToken 생성
  public static String createRefreshToken(String mid, String authority) {  // mid: 아이디, authority: (Spring Security)권한
    log.info("RefreshToken 생성");
    String refreshToken = null;
    
    try {
    refreshToken = Jwts.builder()
        //헤더 설정
        .setHeaderParam("alg", "HS256").setHeaderParam("typ", "JWT")
        //페이로드 설정
        .setExpiration(new Date(new Date().getTime()+REFRESH_TOKEN_DURATION))  //현재 시간으로부터 24시간 뒤
        .claim("mid", mid)  //claim: payload 부분에 들어갈 정보 조각들
        .claim("authority", authority)
        //서명 설정
        .signWith(SignatureAlgorithm.HS256, JWT_SECRET_KEY.getBytes("UTF-8"))
        .compact();  //.compact(): 문자열을 리턴해줌
    }catch(Exception e) {
      log.error(e.getMessage());
    }
    return refreshToken;
  }
  
  //토큰 유효성 검사
  public static boolean validateToken(String token) {
    log.info("실행");
    boolean result = false;
    try {
      result = Jwts.parser()
          .setSigningKey(JWT_SECRET_KEY.getBytes("UTF-8"))  //서명키(비밀키)가 맞는지 확인
          .parseClaimsJws(token) //// 파싱 및 검증, 실패 시 에러 / Claim 객체 리턴
          .getBody()  //Claim 안의 Body 얻기
          .getExpiration()  //만료기간
          .after(new Date());   //만료기간이 현재 시간보다 나중인지 -> 기간이 남았다면 true, 기간이 지났다면 false 
      
    }catch(Exception e) {
      log.error(e.getMessage());
    }
    
    return result;
  }
  
  //토큰 만료 날짜(시간) 얻기
  public static Date getExpiration(String token) {
    log.info("실행");
    Date result = null;
    try {
      result = Jwts.parser()
          .setSigningKey(JWT_SECRET_KEY.getBytes("UTF-8"))
          .parseClaimsJws(token)
          .getBody()
          .getExpiration();
      
    }catch(Exception e) {
      log.error(e.getMessage());
    }
    
    return result;
  
  }
  
  //인증 사용자 정보 얻기
  public static Map<String, String> getUserInfo(String token){
    log.info("실행");
    Map<String, String> result = new HashMap<>();
    try {
      Claims claims = Jwts.parser()
          .setSigningKey(JWT_SECRET_KEY.getBytes("UTF-8"))
          .parseClaimsJws(token)
          .getBody();
      result.put("mid", claims.get("mid", String.class));
      result.put("authority", claims.get("authority", String.class));
    }catch(Exception e) {
      log.error(e.getMessage());
    }
    
    return result;
  }
  
  //요청 Authorization 헤더 값에서 AccessToken 얻기
  //Bearer xxxxxxxxxxxxx.xxxxxxxxxxxx.xxxxxxxxxxxx(accessToken)
  public static String getAccessToken(String authorization) {
    String accessToken = null;
    
    //Bearer xxxxxxxxxxxxx.xxxxxxxxxxxx.xxxxxxxxxxxx(accessToken)에서 Bearer 뒤에 있는 accessToken 얻어오기
    if(authorization != null && authorization.startsWith("Bearer ")) {
      accessToken = authorization.substring(7);
    }
    
    return accessToken;
  }
  
//  테스트용
  public static void main(String[] args) throws Exception {
    String accessToken = createAccessToken("user", "ROLE_USER");
    Thread.sleep(3000);
    System.out.println(validateToken(accessToken));
    
    Date expiration = getExpiration(accessToken);
    System.out.println(expiration);
    
    Map<String, String> userInfo = getUserInfo(accessToken);
    System.out.println(userInfo.toString());

  }
  
}
