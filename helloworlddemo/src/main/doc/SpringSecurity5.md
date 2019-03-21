#Spring Security5 与Spring5 结合
##1.使用依赖版本
|依赖|版本|
|----|----|
|Spring|5.1.5.RELEASE|
|Spring Security|5.1.4.RELEASE|
|Servlet|4.0.1|
|jsp|2.3.3|
|jstl|1.2|
注意:由于Servlet3.0之后,可以是注解的方式来替代xml的方式，本文全部使用注解的方式来实现。
##2.依赖的pom.xml
```
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-config</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-taglibs</artifactId>
    </dependency>
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
    </dependency>
    <dependency>
        <groupId>javax.servlet.jsp</groupId>
        <artifactId>javax.servlet.jsp-api</artifactId>
    </dependency>
    <!--jstl-->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>jstl</artifactId>
    </dependency>
</dependencies>
```
在pom.xml文件中,需要配置如何打包的方式,可以使用tomcat插件，来完成打包，或者使用build来完成打包。
##2.添加SpringSecurity配置
在编写Spring Security配置的时候，需要继承WebSecurityConfigurerAdapter类，主要做一下事情:
>* 配置全局安全:AuthenticationManagerBuilder创建AuthenticationManager(负责处理身份认证请求)，使用内存方式、JDBC、LDAP或者其他方式进行身份认证。
>* 基于web的安全性，HttpSecurity允许为特定的http请求配置，默认情况下，它将应用与所有的请求。根据请求的开头，设置不同的访问权限。
>* 设置自定义的登陆页面，并设置传过来的用户名和密码的参数。
>* 在用户访问请求，没有相应的权限，跳转到相应的页面。
>* 设置密码的编码格式。
>* 设置认证成功之后的跳转策略。
```
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.*;


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
        .antMatchers("/", "/home").permitAll() //任何用户都可以访问
        .antMatchers("/admin/**").access("hasRole('ADMIN')")  //访问以admin开头的请求，需要拥有admin的权限
        .antMatchers("/db/**").access("hasRole('ADMIN') and hasRole('DBA')") //访问以dba开头的请求，需要拥有admin/dba的权限
        .and().formLogin().loginPage("/login").successHandler(successHandler()) //设置自定义登陆页面,在登陆成功之后，根据用户
              .usernameParameter("username").passwordParameter("password") //设置自定义的登陆页面，并设置传过来的用户名和密码的参数
        .and().exceptionHandling().accessDeniedPage("/Access_Denied"); //在没有权限时，可以跳转到accessdenied.jsp
   }
}
```
如果使用xml的格式的时候，代码如下:
```
<beans:beans xmlns="http://www.springframework.org/schema/security"
    xmlns:beans="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-5.1.xsd
    http://www.springframework.org/schema/security http://www.springframework.org/schema/security/spring-security-5.1.xsd"> 
    <http auto-config="true" >
        <intercept-url pattern="/" access="permitAll" />
        <intercept-url pattern="/home" access="permitAll" />
        <intercept-url pattern="/admin**" access="hasRole('ADMIN')" />
        <intercept-url pattern="/dba**" access="hasRole('ADMIN') and hasRole('DBA')" />
        <form-login  login-page="/login" username-parameter="username" password-parameter="password" authentication-failure-url="/Access_Denied" authentication-success-handler-ref="customSuccessHandler" />
        <csrf/>
    </http>
    <authentication-manager >
        <authentication-provider>
            <user-service>
                <user name="xiaoyao"  password="abc123"  authorities="ROLE_USER" />
                <user name="admin" password="root123" authorities="ROLE_ADMIN" />
                <user name="dba"   password="root123" authorities="ROLE_ADMIN,ROLE_DBA" />
            </user-service>
        </authentication-provider>
    </authentication-manager>
    <bean id="customSuccessHandler" class="com.edu.helloworld.configuration.CustomSuccessHandler"/>
</beans:beans>
```
##3.注册Spring Security Filter
在注册Spring Security Filter时，需要继承AbstractSecurityWebApplicationInitializer。
```
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

public class SecurityWebApplicationInitializer 
      extends AbstractSecurityWebApplicationInitializer {
}
```
如果使用xml格式的话，代码如下:
```
<filter>
    <filter-name>springSecurityFilterChain</filter-name>
    <filter-class>org.springframework.web.filter.DelegatingFilterProxy</filter-class>
</filter>
 
<filter-mapping>
    <filter-name>springSecurityFilterChain</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```
##4.添加Spring Mvc 配置
在Spring Security4时，配置Spring Mvc时，需要继承WebMvcConfigurerAdapter类，但是在Spring Security5时，该类就已经过时啦，可以使用@Bean方式添加相应的参数，也可以直接实现WebMvcConfigurer接口来配置springmvc。
```
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

   public void configureViewResolvers(ViewResolverRegistry registry) {
      InternalResourceViewResolver viewResolver=new InternalResourceViewResolver();
      viewResolver.setViewClass(JstlView.class);
      viewResolver.setPrefix("/WEB-INF/views/");
      viewResolver.setSuffix(".jsp");
      registry.viewResolver(viewResolver);
   }

   public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/static/**").addResourceLocations("/static/");
   }
}
```
如果使用xml方式的话，需要使用下面的代码:
```
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="
        http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans-5.1.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context-5.1.xsd
        http://www.springframework.org/schema/mvc 
        http://www.springframework.org/schema/mvc/spring-mvc-5.1.xsd">

    <context:component-scan base-package="com.edu.helloworld" />

    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix">
            <value>/WEB-INF/pages/</value>
        </property>
        <property name="suffix">
            <value>.jsp</value>
        </property>
    </bean>
    <mvc:resources mapping="/static/**" location="/static/"></mvc:resources>
</beans>
```
##5.添加初始化类
这个类类似于web.xml的功能，需要继承AbstractAnnotationConfigDispatcherServletInitializer，它是所有WebApplicationInitializer实现的基类。
在Servlet3.0的时候,使用对WebApplicationInitializer实现的方式来配置ServletContext。
```
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class MvcWebApplicationInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

   @Override
   protected Class<?>[] getRootConfigClasses() {
      return new Class[] { WebSecurityConfig.class };
   }

   @Override
   protected Class<?>[] getServletConfigClasses() {
      return new Class[] { WebConfig.class };
   }

   @Override
   protected String[] getServletMappings() {
      return new String[] { "/" };
   }
}
```
## 6.编写相应的controller
需要在相应的类的添加@Controller、@RequestMapping注解，或者实现Controller的接口，如果使用这种方式，可以自行实现。
```
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Controller
public class UserController {

    @RequestMapping(value = { "/", "/home" }, method = RequestMethod.GET)
    public String homePage(ModelMap model) {
        model.addAttribute("greeting", "Hi, Welcome to mysite");
        return "welcome";
    }

    @RequestMapping(value = "/admin", method = RequestMethod.GET)
    public String adminPage(ModelMap model) {
        model.addAttribute("user", getPrincipal());
        return "admin";
    }

    @RequestMapping(value = "/db", method = RequestMethod.GET)
    public String dbaPage(ModelMap model) {
        model.addAttribute("user", getPrincipal());
        return "dba";
    }

    @RequestMapping(value = "/Access_Denied", method = RequestMethod.GET)
    public String accessDeniedPage(ModelMap model) {
        model.addAttribute("user", getPrincipal());
        return "accessDenied";
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String loginPage() {
        return "login";
    }

    @RequestMapping(value="/logout", method = RequestMethod.GET)
    public String logoutPage (HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null){
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/login?logout";
    }

    private String getPrincipal(){
        String userName = null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            userName = ((UserDetails)principal).getUsername();
        } else {
            userName = principal.toString();
        }
        return userName;
    }
}
```

## 7.添加自定义跳转
这个主要是按照用户的权限，跳转到相应的界面。
```
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
 
    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
 
    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        String targetUrl = determineTargetUrl(authentication);
 
        if (response.isCommitted()) {
            System.out.println("Can't redirect");
            return;
        }
 
        redirectStrategy.sendRedirect(request, response, targetUrl);
    }
 
    /*
     * This method extracts the roles of currently logged-in user and returns
     * appropriate URL according to his/her role.
     */
    protected String determineTargetUrl(Authentication authentication) {
        String url = "";
 
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
 
        List<String> roles = new ArrayList<String>();
 
        for (GrantedAuthority a : authorities) {
            roles.add(a.getAuthority());
        }
 
        if (isDba(roles)) {
            url = "/db";
        } else if (isAdmin(roles)) {
            url = "/admin";
        } else if (isUser(roles)) {
            url = "/home";
        } else {
            url = "/accessDenied";
        }
 
        return url;
    }
 
    private boolean isUser(List<String> roles) {
        if (roles.contains("ROLE_USER")) {
            return true;
        }
        return false;
    }
 
    private boolean isAdmin(List<String> roles) {
        if (roles.contains("ROLE_ADMIN")) {
            return true;
        }
        return false;
    }
 
    private boolean isDba(List<String> roles) {
        if (roles.contains("ROLE_DBA")) {
            return true;
        }
        return false;
    }
 
    public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
    }
 
    protected RedirectStrategy getRedirectStrategy() {
        return redirectStrategy;
    }
 
}
```
## 8.添加页面
由于页面代码比较多，可以参考本实例中的github代码。
需要注意的是，如果想要方式csrf攻击，需要在页面添加<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /></strong>标志。
在页面中，如果想要使用spring security 的tag表达式来限制内容的显示，需要读者自行实现，本文的pom文件已经添加了相应的依赖。


##代码地址
[helloworlddemo](https://github.com/yishengxiaoyao/springsecurity5demo/tree/master/helloworlddemo)

## 参考文献
[Spring Security4 Tutoril](http://websystique.com/spring-security-tutorial/)
[Spring Security5 Tutoril](https://www.boraji.com/category/spring-security)
