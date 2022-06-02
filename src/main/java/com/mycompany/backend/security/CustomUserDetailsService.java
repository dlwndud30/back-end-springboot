package com.mycompany.backend.security;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mycompany.backend.dao.MemberDao;
import com.mycompany.backend.dto.Member;

@Service
public class CustomUserDetailsService implements UserDetailsService {
	private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
	
	@Resource
	private MemberDao memberDao;	
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {  //String username: mid
		
		Member member = memberDao.selectByMid(username);   //DB에서 mid에 해당하는 member를 가져오기
		if(member == null) {  //mid에 해당하는 member가 없다면
			throw new UsernameNotFoundException(username);
		}
		
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(member.getMrole()));
		
		CustomUserDatails userDetails = new CustomUserDatails(
				member.getMid(), 
				member.getMpassword(),
				member.isMenabled(),  //1: 활성화, 0: 비활성화
				authorities,  //ROLE_USER, ROLE_ADMIN 
				//여기까지 4개는 기본적으로 필수적으로 들어가야함
				
				//추가적으로 필요한 정보
				member.getMname(),
				member.getMemail());
		
		return userDetails;
	}
}

