package com.mycompany.backend.config;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.WebApplicationContext;

@Configuration
@MapperScan(basePackages= {"com.mycompany.backend.dao"})
public class MyBatisConfig {
	
	@Resource  //의존 주입 -> 이거 안하려면 sqlSessionFactory()의 매개변수로 DataSource dataSource를 써주면 된다.
	private DataSource dataSource; 
	
	@Resource
	WebApplicationContext wac;  //IOC 컨테이너 역할
	
	@Bean
	public SqlSessionFactory sqlSessionFactory() throws Exception {
		SqlSessionFactoryBean ssfb = new SqlSessionFactoryBean();
		ssfb.setDataSource(dataSource);
		ssfb.setConfigLocation(wac.getResource("classpath:mybatis/mapper-config.xml"));
		ssfb.setMapperLocations(wac.getResources("classpath:mybatis/mapper/*.xml"));
		
		return ssfb.getObject();
	}
	
	@Bean
	public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory ssf) {
		SqlSessionTemplate sst = new SqlSessionTemplate(ssf);
		return sst;
	}
}
