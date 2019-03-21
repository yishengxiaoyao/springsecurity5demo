package com.edu.helloworld.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;


@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

   @Bean
   public AuthenticationSuccessHandler successHandler(){
      return new CustomSuccessHandler();
   }

   //添加密码加密方法，由于Spring Security5必须要对密码进行编码，而Spring Security4不需要
   @Bean
   public PasswordEncoder passwordEncoder(){
      DelegatingPasswordEncoder delegatingPasswordEncoder=(DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
      delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(NoOpPasswordEncoder.getInstance());
      return delegatingPasswordEncoder;
   }
   //第二种方式
   /*@Bean
   public static NoOpPasswordEncoder passwordEncoder() {
      return (NoOpPasswordEncoder) NoOpPasswordEncoder.getInstance();
   }*/
   //将用户的用户名、密码和角色写入内存。
   @Override
   protected void configure(AuthenticationManagerBuilder auth) throws Exception {
      auth.inMemoryAuthentication().passwordEncoder(passwordEncoder()).withUser("xiaoyao").password("abc123").roles("USER");
      auth.inMemoryAuthentication().passwordEncoder(passwordEncoder()).withUser("admin").password("root123").roles("ADMIN");
      auth.inMemoryAuthentication().passwordEncoder(passwordEncoder()).withUser("dba").password("root123").roles("ADMIN","DBA");
   }
   //
   @Override
   protected void configure(HttpSecurity http) throws Exception {
      //设置认证请求
      http.authorizeRequests()
        .antMatchers("/", "/home").access("hasRole('USER')") //任何用户都可以访问
        .antMatchers("/admin/**").access("hasRole('ADMIN')")  //访问以admin开头的请求，需要拥有admin的权限
        .antMatchers("/db/**").access("hasRole('ADMIN') and hasRole('DBA')") //访问以dba开头的请求，需要拥有admin/dba的权限
        .and().formLogin().loginPage("/login").successHandler(successHandler()) //设置自定义登陆页面,在登陆成功之后，根据用户
              .usernameParameter("username").passwordParameter("password") //设置自定义的登陆页面，并设置传过来的用户名和密码的参数
        .and().exceptionHandling().accessDeniedPage("/Access_Denied"); //在没有权限时，可以跳转到accessdenied.jsp
   }
}