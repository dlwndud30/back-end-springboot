package com.mycompany.backend.config;

import javax.annotation.Resource;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.mycompany.backend.security.JwtAuthenticationFilter;

import lombok.extern.log4j.Log4j2;

@Log4j2
@EnableWebSecurity  //자동적으로 메소드들을 실행시켜 spring security를 설정해줌
public class SecurityConfig extends WebSecurityConfigurerAdapter{
  
  @Resource
  private RedisTemplate redisTemplate;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    log.info("실행");

    //서버 세션 비활성화 : 스프링은 기본적으로 세션을 사용하므로 세션을 생성하지 않도록 막겠다
    http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS); //JSESSIONID 자체가 생성되지 않는다

    //로그인폼 비활성화 -> MPA 방식에서는 이걸 하면 안돼
    http.formLogin().disable(); //폼 자체가 나오지 않는다

    //사이트간 요청 위조 방지 비활성화
    http.csrf().disable();

    //요청 경로 권한 설정
    http.authorizeRequests()
    //  /board/가 들어가면 인증을 거쳐야 함
    .antMatchers("/board/**").authenticated()   ///board/뒤에 뭐가 오든 매칭이 되는 모든 url이라면 인증된 사용자만 할 수 있다(로그인이 필요하다) -> 이걸 제외한 나머지 것들은 로그인 없이도 사용가능하게 하겠다
    .antMatchers("/**").permitAll();  // permitAll(): 누구든지 요청할 수 있다 -> spring security가 관여

    //CORS 설정: 다른 도메인의 자바스크립트로 접근을 할 수 있도록 허용
    http.cors(); //CORS 활성화 시키겠다 -> 자세한 설정은 밑에서
    
    //JWT 인증 필터 추가
    //UsernamePasswordAuthenticationFilter.class  대신 new JwtAuthenticationFilter()를 사용한다는 뜻
    http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);  // 우리가 만든 JwtAuthenticationFilter 필터를 추가
    // UsernamePasswordAuthenticationFilter

  }
  
  //@Bean : SecurityConfig.java 안에서만 쓸거니까 관리 객체로 만들 필요 X
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter();
    jwtAuthenticationFilter.setRedisTemplate(redisTemplate);
    return jwtAuthenticationFilter;
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {  //DB에서 무엇을 가져올지, passwordEncoder를 어떤걸 쓸지
    log.info("실행");
    /*
    //MPA 폼 인증 방식에서 사용하는 방법(MPA 방식으로 개발할 때 이런 방식으로 할 수 있다) -> 우린 JWT로 해야함(accesstoken만 맞으면 된다. DB에서 가져와서 맞는지 확인할 필요 X)
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(new CustomUserDetailsService());  //DB에서 member에 대한 정보를 가져옴
    provider.setPasswordEncoder(passwordEncoder());
    auth.authenticationProvider(provider);
     */
  }

  @Override
  public void configure(WebSecurity web) throws Exception {
    log.info("실행");

    DefaultWebSecurityExpressionHandler defaultWebSecurityExpressionHandler = new DefaultWebSecurityExpressionHandler();
    defaultWebSecurityExpressionHandler.setRoleHierarchy(roleHierarchyImpl());   //setRoleHierarchy: 권한을 계층화
    web.expressionHandler(defaultWebSecurityExpressionHandler);

    /*
    //MPA 방식에서 시큐리티를 적용하지 않는 경로 설정 -> REST에서는 사용하지 않는 것들
    web.ignoring()  //security를 적용하지 않을 경로 -> spring security가 관여하지 않음 
    .antMatchers("/images/**") 
    .antMatchers("/css/**")
    .antMatchers("/js/**")
    .antMatchers("/bootstrap/**")
    .antMatchers("/jquery/**")
    .antMatchers("/favicon.ico");
     */
  }

  //@EnableWebSecurity이 @Configuration로 만들어져 있기 때문에 @EnableWebSecurity도 @Configuration라고 할 수 있음
  @Bean  // 메소드 이름으로 메소드가 리턴하는 객체를 관리객체로 만들어줌 -> @Configuration이 붙어있어야함 
  public PasswordEncoder passwordEncoder() {  //회원가입할 때 사용
    //암호화 기술은 계속 발전함 -> 시간이 지남에 따라 암호화해서 저장하는 기법이 달라짐 -> DB에 서로 다른 알고리즘을 이용한 password가 공존함 -> 어떤 알고리즘으로 암호화했는지 알려줘야함
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();  

    //앞으로 바뀔 암호화를 적용하지 않고 bcrypt로만 하겠다
    //return new BCryptPasswordEncoder();
  }

  @Bean
  public RoleHierarchyImpl roleHierarchyImpl() {  //권한 계층화
    log.info("실행");
    RoleHierarchyImpl roleHierarchyImpl = new RoleHierarchyImpl();
    roleHierarchyImpl.setHierarchy("ROLE_ADMIN > ROLE_MANAGER > ROLE_USER"); 
    return roleHierarchyImpl;
  }

  @Bean  //Rest API에서만 사용 -> MPA에서는 사용 X
  public CorsConfigurationSource corsConfigurationSource() {  //CorsConfigurationSource 객체가 있으면 
    log.info("실행");
    CorsConfiguration configuration = new CorsConfiguration();
    //모든 요청 사이트 허용 : 모든 도메인의 자바스크립트가 접근하도록 허용
    configuration.addAllowedOrigin("*");
    //모든 요청 방식 허용 : get, post, put, delete의 모든 방식을 허용하겠다
    configuration.addAllowedMethod("*");
    //모든 요청 헤더 허용 : 어떤 헤더명이든 다 받겠다
    configuration.addAllowedHeader("*");
    //모든 URL 요청에 대해서 위 내용을 적용
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

}
