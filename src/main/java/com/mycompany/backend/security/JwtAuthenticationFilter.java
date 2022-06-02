/*package com.mycompany.backend.security;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class JwtAuthenticationFilter extends OncePerRequestFilter{  //OncePerRequestFilter: 요청 하나당 딱 1번만 실행하는

  private RedisTemplate redisTemplate;
  public void setRedisTemplate(RedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override  //doFilterInternal: 시큐리티에는 기본 필터가 여러개 있는데 Jwt 필터를 거기에 추가 해주겠다
  //인증이나 권한이 필요한 주소요청이 있을 떄 이 필터를 타게 된다.
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    log.info("실행");

    //요청 헤더로부터 Authorization 헤더 값 얻기(Authorization에 accessToken이 있으므로) 
    String authorization = request.getHeader("Authorization");

    //AccessToken 추출
    String accessToken = Jwt.getAccessToken(authorization); //getAccessToken: Authorization에서 앞의 Bearer 빼고 access token 얻기


    //검증 작업
    if(accessToken!=null && Jwt.validateToken(accessToken)) {  // validateToken: 유효하면 true


      //Redis에 존재 여부 확인: 로그인했으면 존재, 로그아웃했으면 존재 x
      ValueOperations<String, String> vo = redisTemplate.opsForValue();
      String redisRefreshToken = vo.get(accessToken);  //redisRefreshToken == null : accessToken이 없다 -> accessToken이 넘어왔긴하지만 redis에 키로 존재하지 않는다

      if(redisRefreshToken != null) {
        //인증 처리
        Map<String, String> userInfo = Jwt.getUserInfo(accessToken); //accessToken: mid와 authority 정보 얻기
        String mid = userInfo.get("mid");
        String authority = userInfo.get("authority");
        //accessToken이 유효하다면 이미 인증 완료이기 때문에 password 체크를 할 필요가 없음
        //UsernamePasswordAuthenticationToken(user를 식별할 수 있는 아이디(id), password, 권한) 
        //인증 객체를 직접 만들어서 Security에 설정해주면됨
        UsernamePasswordAuthenticationToken authentication= new UsernamePasswordAuthenticationToken(mid, null,AuthorityUtils.createAuthorityList(authority)); //Jwt 토큰 서명을 통해서 서명이 정상이면 Authentication 객체를 만들어준다
        SecurityContext sc = SecurityContextHolder.getContext();  //강제로 시큐리티 세션에 접근하여 Authentication 객체를 저장
        sc.setAuthentication(authentication);//인증 처리 완료
      }

    }
    //그 다음 필터를 실행
    filterChain.doFilter(request, response);  
  }


}
*/


package com.mycompany.backend.security;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private RedisTemplate redisTemplate;
  public void setRedisTemplate(RedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
      // TODO Auto-generated method stub
      log.info("실행");
      
      //요청 헤더로부터 Authorization 헤더 값 얻기
      String authorization = request.getHeader("Authorization");
      
      //AccessToken 추출
      String accessToken = Jwt.getAccessToken(authorization);
      log.info(accessToken);
      
   
    
      
      //검증 작업
      if(accessToken !=null && Jwt.validateToken(accessToken)) {
        //Redis 존재 여부 확인
        ValueOperations<String, String> vo=redisTemplate.opsForValue();
        String redisRefreshToken=vo.get(accessToken);
        
        if(redisRefreshToken!=null) {
        //인증 처리
          Map<String,String> userInfo = Jwt.getUserInfo(accessToken);
          String mid=userInfo.get("mid");
          String authority=userInfo.get("authority");
          UsernamePasswordAuthenticationToken authentication =new UsernamePasswordAuthenticationToken(mid,null,AuthorityUtils.createAuthorityList(authority));//기본 생성자 없음
          SecurityContext securityContext=SecurityContextHolder.getContext();
          securityContext.setAuthentication(authentication);
        }
      }
     //다음 필터 실행
      filterChain.doFilter(request, response);
    
  }
  
}