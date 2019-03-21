package com.edu.helloworld.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = { "com.edu.helloworld" })
public class WebConfig extends WebMvcConfigurerAdapter {

   //使用@Bean方式的时候，不用继承WebMvcConfigurerAdapter
   /*@Bean
   public ViewResolver viewResolver(){
      InternalResourceViewResolver viewResolver=new InternalResourceViewResolver();
      viewResolver.setViewClass(JstlView.class);
      viewResolver.setPrefix("/WEB-INF/views/");
      viewResolver.setSuffix(".jsp");
      return viewResolver;
   }*/

   @Override
   public void configureViewResolvers(ViewResolverRegistry registry) {
     registry.jsp().prefix("/WEB-INF/views/").suffix(".jsp");
   }
   @Override
   public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/static/**").addResourceLocations("/static/");
   }

}
