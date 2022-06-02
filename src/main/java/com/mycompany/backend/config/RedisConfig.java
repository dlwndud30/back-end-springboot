package com.mycompany.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
public class RedisConfig {
  
  @Value("${spring.redis.hostName}")
  private String hostName;
  
  @Value("${spring.redis.port}")
  private String port;
  
  @Value("${spring.redis.password}")
  private String password;
  
  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    log.info("실행");
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
    config.setHostName(hostName);  // hostName: localhost
    config.setPort(Integer.parseInt(port));  //port : 6379
    config.setPassword(password); //password: "redis"
    LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(config);
    return connectionFactory;
  }
  
  @Bean //실제로 의존 주입해서 써야할 객체를 관리 객체로 만들어줌
  public RedisTemplate<String, String> redisTemplate() {  //RedisTemplate<키, 값>
    log.info("실행");
    RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory());
    redisTemplate.setKeySerializer(new StringRedisSerializer());  //StringRedisSerializer() : 전송가능한 바이트 배열로 만드는 객체
    redisTemplate.setValueSerializer(new StringRedisSerializer());
    return redisTemplate;
  }
}
