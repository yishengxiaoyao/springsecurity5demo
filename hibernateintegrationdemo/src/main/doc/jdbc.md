#Spring Security 5 相关操作
本文主要是介绍使用JDBC的方式来存储用户的用户名、密码、权限信息，根据用户的权限跳转到不同的页面，拥有remember-me功能，以及不同用户的相关操作。
##1.相关依赖版本
|软件|版本|
|----|----|
|Spring|5.1.5.RELEASE|
|Spring Security|5.1.4.RELEASE|
|Servlet|4.0.1|
|jsp|2.3.3|
|jstl|1.2|
|Hibernate|5.4.1.Final|
|validation|2.0.1.Final|
|mysql|5.1.47|
##2.编写pom.xml
```
 <dependencies>
    <!--Spring-->
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
        <groupId>org.springframework</groupId>
        <artifactId>spring-tx</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-orm</artifactId>
    </dependency>
    <!--Spring Security-->
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
    <!--Hibernate-->
    <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-core</artifactId>
    </dependency>
    <!--validation-->
    <dependency>
        <groupId>javax.validation</groupId>
        <artifactId>validation-api</artifactId>
    </dependency>
    <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-validator</artifactId>
    </dependency>
    <!--MySQL-->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>
    <!--Servlet-->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
    </dependency>
    <!--Jsp-->
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
## 3.配置安全
在进行安全配置的时候，做了以下事情:
>* 设置自定义处理登陆逻辑
>* 在登陆成功之后,自定义的跳转逻辑
>* 设置remember-me功能
>* 针对全局进行安全设置
>* 设置一些url的权限控制
>* 对密码进行某种方式加密(本文未实现，请读者自行实现)
>* LDAP方式，需要读者自行实现
```
import com.edu.hibernateintergration.service.CustomSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    DataSource dataSource;

    @Autowired
    PersistentTokenRepository tokenRepository;
    //用户登陆完成之后，根据用户的权限，跳转到不同的页面
    @Bean
    public AuthenticationSuccessHandler successHandler(){
        return new CustomSuccessHandler();
    }
    //这里没有对用户的密码进行加密，读者可以自行添加
    @Bean
    public PasswordEncoder passwordEncoder(){
        DelegatingPasswordEncoder delegatingPasswordEncoder=(DelegatingPasswordEncoder) PasswordEncoderFactories.createDelegatingPasswordEncoder();
        delegatingPasswordEncoder.setDefaultPasswordEncoderForMatches(NoOpPasswordEncoder.getInstance());
        return delegatingPasswordEncoder;
    }
    //可以使用自带的JDBC的方式来实现remember-me功能
    /*@Bean
    public PersistentTokenRepository tokenRepository(){
        JdbcTokenRepositoryImpl jdbcTokenRepository=new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);
        return jdbcTokenRepository;
    }*/
    //配置全局的安全限制
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/", "/home").permitAll()
                .antMatchers("/admin/**").access("hasRole('ADMIN')")
                .antMatchers("/db/**").access("hasRole('ADMIN') and hasRole('DBA')")
                .antMatchers("/", "/list")
                .access("hasRole('USER') or hasRole('ADMIN') or hasRole('DBA')")
                .antMatchers("/newuser/**", "/delete-user-*").access("hasRole('ADMIN')").antMatchers("/edit-user-*")
                .access("hasRole('ADMIN') or hasRole('DBA')")
                .and().formLogin().loginPage("/login").successHandler(successHandler()) //设置自定义的登陆页面，在登陆成功之后，根据用户角色进行自定义处理
                .usernameParameter("username").passwordParameter("password")
                .and().rememberMe().rememberMeParameter("remember-me").tokenValiditySeconds(86400).tokenRepository(tokenRepository).userDetailsService(userDetailsService) //remember-me功能的设置和处理类设置
                .and().csrf()
                .and().exceptionHandling().accessDeniedPage("/Access_Denied");
    }
    //设置用户处理remember-me功能的token
    @Bean
    public PersistentTokenBasedRememberMeServices getPersistentTokenBasedRememberMeServices(){
        PersistentTokenBasedRememberMeServices tokenBasedRememberMeServices=new PersistentTokenBasedRememberMeServices("remember-me",userDetailsService,tokenRepository);
        return tokenBasedRememberMeServices;
    }

    //辅助完成remember-me功能
    @Bean
    public AuthenticationTrustResolver getAuthenticationTrustResolver(){
        return new AuthenticationTrustResolverImpl();
    }
}
```
可以在xml文件中这样配置，代码如下:
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
        <form-login  login-page="/login" 
                     username-parameter="username" 
                     password-parameter="password" 
                     authentication-success-handler-ref="customSuccessHandler"
                     authentication-failure-url="/Access_Denied" />
        <csrf/>
    </http>
 
    <authentication-manager >
        <authentication-provider user-service-ref="customUserDetailsService"/>
    </authentication-manager>
     <!--设置自定义的登陆逻辑-->
    <beans:bean id="customUserDetailsService" class="com.edu.hibernateintergration.service.CustomUserDetailsService" />
    <!--根据用户的角色做不同的处理-->
    <beans:bean id="customSuccessHandler"     class="com.edu.hibernateintergration.service.CustomSuccessHandler" />
</beans:beans>
```
## 4.注册springSecurityFilter
```
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
public class SecurityWebApplicationInitializer 
      extends AbstractSecurityWebApplicationInitializer {
}
```
等同于下面的xml:
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
## 5.自定义登陆处理逻辑
```
import java.util.ArrayList;
import java.util.List;
import com.edu.hibernateintergration.model.User;
import com.edu.hibernateintergration.model.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service("customUserDetailsService")
public class CustomUserDetailsService implements UserDetailsService{
    @Autowired
    private UserService userService;    
    @Transactional(readOnly=true)
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = userService.findByUsername(username);
        System.out.println("User : "+user);
        if(user==null){
            System.out.println("User not found");
            throw new UsernameNotFoundException("Username not found");
        }
            return new org.springframework.security.core.userdetails.User(user.getUsername(),user.getPassword(),
                 user.getState().equals("Active"), true, true, true, getGrantedAuthorities(user));
    }
    private List<GrantedAuthority> getGrantedAuthorities(User user){
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
         
        for(UserProfile userProfile : user.getUserProfiles()){
            System.out.println("UserProfile : "+userProfile);
            authorities.add(new SimpleGrantedAuthority("ROLE_"+userProfile.getType()));
        }
        System.out.print("authorities :"+authorities);
        return authorities;
    }
}
```
## 6.编写根据角色不同的处理逻辑
```
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
@Component
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler{
    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    @Override
    protected void handle(HttpServletRequest request, 
      HttpServletResponse response, Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(authentication);
        if (response.isCommitted()) {
            System.out.println("Can't redirect");
            return;
        }
        redirectStrategy.sendRedirect(request, response, targetUrl);
    }
    protected String determineTargetUrl(Authentication authentication) {
        String url="";
        Collection<? extends GrantedAuthority> authorities =  authentication.getAuthorities();
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
            url="/accessDenied";
        }
        return url;
    }
    public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
        this.redirectStrategy = redirectStrategy;
    }
    protected RedirectStrategy getRedirectStrategy() {
        return redirectStrategy;
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
}
```
## 7.编写Spring MVC的配置
在进行Spring MVC进行配置的时候:
>* 页面的位置和后缀，进行映射
>* 静态资源的处理器
>* 视图与处理器的对应关系
>* 国际化资源的配置
```
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"com.edu.hibernateintergration"})
public class WebConfig implements WebMvcConfigurer {
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.jsp().prefix("/WEB-INF/views/").suffix(".jsp");
    }
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("/static/");
    }
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
    }
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseRegisteredSuffixPatternMatch(true);
    }
    //配置国际化资源
    @Bean
    public MessageSource messageSource(){
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        return messageSource;
    }
}
```
相应的xml配置为:
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

    <context:component-scan base-package="com.edu.hibernateintergration" />

    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix">
            <value>/WEB-INF/pages/</value>
        </property>
        <property name="suffix">
            <value>.jsp</value>
        </property>
    </bean>
    <mvc:resources mapping="/static/**" location="/static/"></mvc:resources>
     <!--配置处理器映射器-->
        <bean class="org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping"></bean>
        <!--配置处理器适配器-->
    <bean class="org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter"></bean>
</beans>
```
## 8.配置Spring Initializer
```
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;
public class MvcWebApplicationInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
   @Override
   protected Class<?>[] getRootConfigClasses() {
      return new Class[] { WebSecurityConfig.class,HibernateConfiguration.class };
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
## 9.配置Hibernate
```
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {"com.edu.hibernateintergration"})
@PropertySource(value = {"classpath:application.properties"})
public class HibernateConfiguration {
    //获取环境变量对象
    @Autowired
    private Environment environment;
    //创建session管理对象
    @Bean
    public LocalSessionFactoryBean sessionFactoryBean(){
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource());
        sessionFactory.setPackagesToScan(new String[] { "com.edu.hibernateintergration.model" });
        sessionFactory.setHibernateProperties(hibernateProperties());
        return sessionFactory;
    }
    //获取数据库的对象
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(environment.getRequiredProperty("jdbc.driverClassName"));
        dataSource.setUrl(environment.getRequiredProperty("jdbc.url"));
        dataSource.setUsername(environment.getRequiredProperty("jdbc.username"));
        dataSource.setPassword(environment.getRequiredProperty("jdbc.password"));
        return dataSource;
    }
    //从配置文件中获取值
    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", environment.getRequiredProperty("hibernate.dialect"));
        properties.put("hibernate.show_sql", environment.getRequiredProperty("hibernate.show_sql"));
        properties.put("hibernate.format_sql", environment.getRequiredProperty("hibernate.format_sql"));
        properties.put("hibernate.hbm2ddl.auto", environment.getRequiredProperty("hibernate.hbm2ddl.auto"));
        return properties;
    }
    //创建事务管理对象
    @Bean
    public HibernateTransactionManager transactionManager() {
        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
        transactionManager.setSessionFactory(sessionFactoryBean().getObject());
        return transactionManager;
    }
}
```
## 10.配置文件
application.properties
```
jdbc.driverClassName = com.mysql.jdbc.Driver
jdbc.url = jdbc:mysql://localhost:3306/test
jdbc.username = root
jdbc.password = 123456
hibernate.dialect = org.hibernate.dialect.MySQL5InnoDBDialect
hibernate.show_sql = true
hibernate.format_sql = true
hibernate.hbm2ddl.auto = update
```
message.properties
```
NotEmpty.user.firstName=First name can not be blank.
NotEmpty.user.lastName=Last name can not be blank.
NotEmpty.user.email=Email can not be blank.
NotEmpty.user.password=Password can not be blank.
NotEmpty.user.username=username can not be blank.
NotEmpty.user.userProfiles=At least one profile must be selected.
non.unique.username=username {0} already exist. Please fill in different value.
```
## 11.自定义对Token操作
这个类主要是对token进行更新、创建、删除操作。
```
import com.edu.hibernateintergration.dao.AbstractDao;
import com.edu.hibernateintergration.model.PersistentLogins;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Repository
@Transactional
public class PersistentTokenDaoImp extends AbstractDao<String,PersistentLogins> implements PersistentTokenRepository {
    public void createNewToken(PersistentRememberMeToken token) {
        PersistentLogins logins=new PersistentLogins();
        logins.setUsername(token.getUsername());
        logins.setSeries(token.getSeries());
        logins.setToken(token.getTokenValue());
        logins.setLastUsed(token.getDate());
        persist(logins);
    }
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        PersistentLogins persistentLogin = getByKey(series);
        persistentLogin.setToken(tokenValue);
        persistentLogin.setLastUsed(lastUsed);
        update(persistentLogin);
    }
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        try {
            Criteria crit = createEntityCriteria();
            crit.add(Restrictions.eq("series", seriesId));
            PersistentLogins persistentLogin = (PersistentLogins) crit.uniqueResult();
            return new PersistentRememberMeToken(persistentLogin.getUsername(), persistentLogin.getSeries(),
                    persistentLogin.getToken(), persistentLogin.getLastUsed());
        } catch (Exception e) {
            return null;
        }
    }
    public void removeUserTokens(String username) {
        Criteria crit = createEntityCriteria();
        crit.add(Restrictions.eq("username", username));
        PersistentLogins persistentLogin = (PersistentLogins) crit.uniqueResult();
        if (persistentLogin != null) {
            delete(persistentLogin);
        }
    }
}
```
## 12.相关创建表信息
```
/*All User's are stored in t_user table*/
create table t_user (
   id BIGINT NOT NULL AUTO_INCREMENT,
   user_name VARCHAR(30) NOT NULL,
   password VARCHAR(100) NOT NULL,
   first_name VARCHAR(30) NOT NULL,
   last_name  VARCHAR(30) NOT NULL,
   email VARCHAR(30) NOT NULL,
   state VARCHAR(30) NOT NULL,  
   PRIMARY KEY (id),
   UNIQUE (user_name)
);
  
/* t_user_profile table contains all possible roles */
create table t_user_profile(
   id BIGINT NOT NULL AUTO_INCREMENT,
   type VARCHAR(30) NOT NULL,
   PRIMARY KEY (id),
   UNIQUE (type)
);
  
/* JOIN TABLE for MANY-TO-MANY relationship*/ 
CREATE TABLE user_to_user_profile (
    user_id BIGINT NOT NULL,
    user_profile_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, user_profile_id),
    CONSTRAINT FK_APP_USER FOREIGN KEY (user_id) REFERENCES t_user (id),
    CONSTRAINT FK_USER_PROFILE FOREIGN KEY (user_profile_id) REFERENCES t_user_profile (id)
);

CREATE TABLE persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) NOT NULL,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL,
    PRIMARY KEY (series)
);
 
/* Populate USER_PROFILE Table */
INSERT INTO t_user_profile(type)
VALUES ('USER');
 
INSERT INTO t_user_profile(type)
VALUES ('ADMIN');
 
INSERT INTO t_user_profile(type)
VALUES ('DBA');
 
/* Populate t_user Table */
INSERT INTO t_user(username, password, first_name, last_name, email, state)
VALUES ('xiaoyao','abc123', 'xiaoyao','Watcher','xiaoyao@xyz.com', 'Active');
 
INSERT INTO t_user(username, password, first_name, last_name, email, state)
VALUES ('zhangsan','abc124', 'zhangsan','Theys','zhangsan@xyz.com', 'Active');
 
INSERT INTO t_user(username, password, first_name, last_name, email, state)
VALUES ('lisi','abc125', 'lisi','Smith','lisiy@xyz.com', 'Active');
 
INSERT INTO t_user(username, password, first_name, last_name, email, state)
VALUES ('wangwu','abc126', 'wangwu','warner','wangwu@xyz.com', 'Active');
 
INSERT INTO t_user(username, password, first_name, last_name, email, state)
VALUES ('xiaoliu','abc127', 'xiaoliu','Roger','xiaoliu@xyz.com', 'Active');
 
/* Populate JOIN Table */
INSERT INTO user_to_user_profile (user_id, user_profile_id)
  SELECT user.id, profile.id FROM t_user user, t_user_profile profile  
  where user.username='xiaoyao' and profile.type='USER';
 
INSERT INTO user_to_user_profile (user_id, user_profile_id)
  SELECT user.id, profile.id FROM t_user user, t_user_profile profile
  where user.username='zhangsan' and profile.type='USER';
 
INSERT INTO user_to_user_profile (user_id, user_profile_id)
  SELECT user.id, profile.id FROM t_user user, t_user_profile profile
  where user.username='lisi' and profile.type='ADMIN';
 
INSERT INTO user_to_user_profile (user_id, user_profile_id)
  SELECT user.id, profile.id FROM t_user user, t_user_profile profile
  where user.username='wangwu' and profile.type='DBA';
 
INSERT INTO user_to_user_profile (user_id, user_profile_id)
  SELECT user.id, profile.id FROM t_user user, t_user_profile profile  
  where user.username='xiaoliu' and profile.type='ADMIN';
 
INSERT INTO user_to_user_profile (user_id, user_profile_id)
  SELECT user.id, profile.id FROM t_user user, t_user_profile profile  
  where user.username='xiaoliu' and profile.type='DBA';
```
## 13.实体类
由于自定义存储用户的信息、权限、角色、记录用户登陆的session信息，需要创建相应的实体类，用来传递谁给数据库或者从数据库中接收数据。

程序会根据用户提供的实体类和数据库的连接，会自动在数据库中创建相应的数据表。

本文以User、PersistentLogins实体类为例，其他相关类可以参考本文代码:
```
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
@Entity
@Table(name="t_user")
public class User implements Serializable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int id;
    @Column(name="username", unique=true, nullable=false)
    private String username;
    @Column(name="password", nullable=false)
    private String password;  
    @Column(name="first_name", nullable=false)
    private String firstName;
    @Column(name="last_name", nullable=false)
    private String lastName;
    @Column(name="email", nullable=false)
    private String email;
    @Column(name="state", nullable=false)
    private String state=State.ACTIVE.getState();
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_to_user_profile",
             joinColumns = { @JoinColumn(name = "user_id") },
             inverseJoinColumns = { @JoinColumn(name = "user_profile_id") })
    private Set<UserProfile> userProfiles = new HashSet<UserProfile>();
    //省略了get和set方法     
}
```
```
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;
@Entity
@Table(name = "persistent_logins")
public class PersistentLogins implements Serializable {
    @Id
    @Column(name = "series",nullable = false)
    private String series;
    @Column(name = "username",nullable = false)
    private String username;
    @Column(name = "token",nullable = false)
    private String token;
    @Column(name = "last_used",nullable = false)
    private Date lastUsed;
    //省略get和set方法
}
```
## 14.其他
关于页面、controller和数据库的操作，请参考本文[代码](https://github.com/yishengxiaoyao/springsecurity5demo/tree/master/hibernateintegrationdemo)

## 参考文献
[Spring Security4 Tutoril](http://websystique.com/spring-security-tutorial/)
[Spring Security5 Tutoril](https://www.boraji.com/category/spring-security)
