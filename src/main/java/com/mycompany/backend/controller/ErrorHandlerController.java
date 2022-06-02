package com.mycompany.backend.controller;

import java.net.URI;

import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
//implements ErrorController를 구현하는 컨트롤러를 찾으면 무조건 /error를 실행시킴
public class ErrorHandlerController implements ErrorController{  
  @RequestMapping("/error")
  public ResponseEntity<String> error(HttpServletResponse response) {
    int status = response.getStatus(); //401, 404, 403, ... 여러 가지가 나옴
    if(status == 404) {  //사용자가 엉뚱한 url을 입력했을 때
      return ResponseEntity
          .status(HttpStatus.MOVED_PERMANENTLY) //HttpStatus.MOVED_PERMANENTLY: redirect의 응답코드 번호(301)
          .location(URI.create("/")) //redirect로 갈 url
          .body("");
    } else if(status == 403){
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body("invailde access token");
    } else {
      return ResponseEntity.status(status).body("");
    }
  }
}
