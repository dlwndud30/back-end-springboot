package com.mycompany.backend.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.json.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mycompany.backend.dto.Member;
import com.mycompany.backend.security.Jwt;
import com.mycompany.backend.service.MemberService;
import com.mycompany.backend.service.MemberService.JoinResult;
import com.mycompany.backend.service.MemberService.LoginResult;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/member")
public class MemberController {
  @Resource
  private MemberService memberService;
  
  @Resource 
  private PasswordEncoder passwordEncoder;
  
  @Resource
  private RedisTemplate<String, String> redisTemplate;  //accessToken과 refreshToken을 redis에 저장
  
  //회원가입
  @PostMapping("/join")
  public Map<String, Object> join(@RequestBody Member member) {  //@RequestBody : 요청 바디에 json이 들오가야함
    //계정 활성화
    member.setMenabled(true);
    
    //비밀번호 암호화
    member.setMpassword(passwordEncoder.encode(member.getMpassword()));
    
    //회원가입 처리
    JoinResult joinResult = memberService.join(member);
    
    //응답 내용 설정
    Map<String, Object> map = new HashMap<>();
    if(joinResult == JoinResult.SUCCESS) {
      map.put("result", "success");
      map.put("member", member);
    }else if(joinResult == JoinResult.DUPLICATED) {
      map.put("result", "duplicated");
    }else {
      map.put("result", "fail");
    }
    
    return map;
  }
  
  //로그인 -> accessToken, refreshToken 생성 ->
  @PostMapping("/login")
  public ResponseEntity<String> login(@RequestBody Member member) {
    log.info("실행");
    log.info(member);
    
    //id나 password가 없을 경우
    if(member.getMid()==null || member.getMpassword()==null) {
      return ResponseEntity
          .status(401) //401: 에러 코드 -> 인증상의 문제가 있다는 뜻/로그인 했을 때 문제가 있다
          .body("mid or mpassword cannot be null");
    }
    
    //로그인 결과 얻기
    LoginResult loginResult = memberService.login(member);
    if(loginResult != LoginResult.SUCCESS) {
      return ResponseEntity
          .status(401) //401: 에러 코드 -> 인증상의 문제가 있다는 뜻/로그인 했을 때 문제가 있다
          .body("mid or mpassword is wrong");
    }
    
    //로그인 결과가 SUCCESS인 경우--------------------------------------------------------
    //DB에 있는 ROLE을 가져오기 위해
    Member dbMember = memberService.getMember(member.getMid());
    
    //accessToken : 응답의 바디로 전달
    String accessToken = Jwt.createAccessToken(member.getMid(), dbMember.getMrole());  //"ROLE_USER": 권한 
    //refreshToken : httpOnly 속성의 쿠키로 전달
    String refreshToken = Jwt.createRefreshToken(member.getMid(), dbMember.getMrole());
    
    //Redis에 저장
    ValueOperations<String, String> vo = redisTemplate.opsForValue();
    vo.set(accessToken, refreshToken, Jwt.REFRESH_TOKEN_DURATION, TimeUnit.MILLISECONDS);  //Jwt.REFRESH_TOKEN_DURATION: 만료시간을 refreshToken의 만료시간과 같게/ TimeUnit.MILLISECONDS : 만료시간의 단위
    
    //쿠키 생성
    String refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
       .httpOnly(true)
       .secure(false)
       .path("/")
       .maxAge(Jwt.REFRESH_TOKEN_DURATION/1000)
       .domain("localhost").build().toString();
    
    //본문 생성
    String json = new JSONObject()
        .put("accessToken",accessToken)
        .put("mid", member.getMid())
        .toString();
    
    return ResponseEntity
        .ok() //응답 상태 코드: 200 -> 성공적으로 응답을 보낼 때
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie)  //응답 헤더 추가
        .header(HttpHeaders.CONTENT_TYPE, "application/json")  //응답 헤더 추가
        .body(json);  //응답 바디 추가
  }
  
  @GetMapping("/refreshToken")
  public ResponseEntity<String> refreshToken(@RequestHeader("Authorization") String authorization,
                                             @CookieValue("refreshToken")String refreshToken) {
    //accessToken 얻기
    String accessToken = Jwt.getAccessToken(authorization);

    //accessToken이 오지 않으면
    if(accessToken == null) {
      return ResponseEntity.status(401).body("no access token");
    }
    
    //refreshToken 존재 여부 확인
    if(refreshToken == null) {
      return ResponseEntity.status(401).body("no refresh token");
    }
    
    //동일한 토큰인지 확인
    ValueOperations<String, String> vo = redisTemplate.opsForValue();
    //redisRefreshTokken가 null이 아니면 내가 로그인할 때 발급한 토큰이 맞다
    String redisRefreshToken = vo.get(accessToken);  //클라이언트한테 받은 accessToken으로 검색해서 가져오겠다
    if(redisRefreshToken == null) {  //accessToken이 잘못되었다
      return ResponseEntity.status(401).body("invalidate access token");
    }
    if(!refreshToken.equals(redisRefreshToken)) { //클라이언트가 보낸 refreshToken과 redis에 저장된 refreshToken이 같은지 확인
      return ResponseEntity.status(401).body("invalidate refresh token");
    }
    
//    if(Jwt.validateToken(refreshToken)) { //redis에 저장하는 기간을 refreshToken과 동일하게 했으므로 redis에 있다면 유효기간이 지나지 않았음을 의미 -> 굳이 이렇게 따로 검사할 필요 X
//      return ResponseEntity.status(401).body("invalidate refresh token");
//    }
    
    //accessToken과 refreshToken이 모두 같다면 새로운 accessToken 발급
    Map<String, String> userInfo = Jwt.getUserInfo(refreshToken);//mid, authority: refreshToken에서 얻어내야함 -> accessToken은 이미 만료되었으므로
    String mid = userInfo.get("mid");
    String authority = userInfo.get("authority");
    String newAccessToken = Jwt.createAccessToken(mid, authority);  
    
    //기존의 토큰은 지우고 새로 바뀐 토큰으로 redis에 저장해줘야함 : 새로 만들었으니까 새로운 정보로 갱신시켜줘야함
    //Redis에 저장된 기존 정보를 삭제
    redisTemplate.delete(accessToken);
    
    //Redis에 새로운 정보를 저장
    Date expiration = Jwt.getExpiration(refreshToken);
    vo.set(newAccessToken, refreshToken, expiration.getTime()-new Date().getTime(), TimeUnit.MILLISECONDS); //AccessToken만 새로 생성하고 refreshToken은 새로 생성하지 않으므로 지금 시점에서 남아있는 refreshToken의 만료기간을 넣어줘야함
    
    
    //응답 설정 : 다시 클라이언트한테 보내기
    String json = new JSONObject().put("accessToken", newAccessToken).put("mid", mid).toString();
   
    //ResponseEntity<String> -> body에는 String이 들어가야함 
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "application/json").body(json);
  }
  
  @GetMapping("/logout")
  public ResponseEntity<String> logout(@RequestHeader("Authorization") String authorization){
    String accessToken = Jwt.getAccessToken(authorization);
//    if(accessToken == null || !Jwt.validateToken(accessToken)) {  //accessToken이 null이거나 accessToken이 유효하지 않다면 로그아웃
//      return ResponseEntity.status(401).body("invalidate access token");
//    }  //-> 유효하지 않다고 로그아웃을 못하게 하는건 이상해서 주석 처리함
    if(accessToken == null) {
      return ResponseEntity.status(401).body("invalidate access token");
    }
    
    //Redis에 저장된 인증 정보 삭제
    redisTemplate.delete(accessToken);
    
    //RefreshToken 쿠키 삭제-> 클라이언트에 보낸 쿠키 삭제 : 쿠키의 maxAge를 refreshToken의 만료기간으로 설정했었음 -> 이걸 0으로 바꿔주면 쿠키 삭제 효과
    String refreshTokenCookie = ResponseCookie.from("refreshToken", "")
        .httpOnly(true)
        .secure(false)//true=> https만 가능, false=>http와 https 모두 가능
        .path("/")//어떤 api더라도 가능하도록 공통 경로
        .maxAge(0)//쿠키가 살아있는 시간(=토큰의 만료 시간), 초단위로 변환
        .domain("localhost")
        .build()
        .toString();

    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshTokenCookie).body("success");
  }
}
