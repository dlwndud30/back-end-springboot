package com.mycompany.backend.security;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class CustomUserDatails extends User {
  //인증 정보로 추가로 저장하고 싶은 내용 정의
	private String mname;
	private String memail;
	
	public CustomUserDatails(
			String mid, //User 생성자로 넘겨줌
			String mpassword, //User 생성자로 넘겨줌
			boolean menabled, //User 생성자로 넘겨줌 : 활성화여부
			List<GrantedAuthority> mauthorities,  //User 생성자로 넘겨줌 : 권한 
			//여기까지 4개가 spring security에 꼭 있어야 하는것
			
			//더 필요한게 있으면 추가해주기(밑에 2개)
			String mname,  //CustomUserDatails가 가지고 있음
			String memail) {  //CustomUserDatails가 가지고 있음
		super(mid, mpassword, menabled, true, true, true, mauthorities);
		this.mname = mname;
		this.memail = memail;
	}
	
	public String getMname() {
		return mname;
	}

	public String getMemail() {
		return memail;
	}
}

