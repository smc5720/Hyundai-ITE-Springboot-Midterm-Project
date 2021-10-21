package com.mycompany.webapp.security;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;

import lombok.extern.slf4j.Slf4j;

@EnableWebSecurity
@Slf4j
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		log.info("HttpSecurity 실행");

		// 로그인 방식 설정
		http.formLogin().loginPage("/loginForm") // 권한없이 요청했을 때 가는 페이지 URL
				.usernameParameter("mid") // 기본 username -> mid로 변경
				.passwordParameter("mpassword") // 기본 password -> mpassword로 변경
				.defaultSuccessUrl("/") // 로그인 성공시 이동할 URL
				.failureUrl("/loginForm") // 로그인 실패시 이동할 URL
				.loginProcessingUrl("/login") // 반드시 post방식 요청이여야 한다.
		;

		// 로그아웃 설정
		http.logout().logoutUrl("/logout") // 기본 -> logout
				.logoutSuccessUrl("/") // 로그아웃 성공시 이동할 URL
		;

		// URL 권한 설정
		http.authorizeRequests()
			.antMatchers("/security/admin/**").hasAuthority("ROLE_ADMIN")
			.antMatchers("/event/resetEvent/**").hasAuthority("ROLE_ADMIN")
			.antMatchers("/security/manager/**").hasAuthority("ROLE_MANAGER")
			.antMatchers("/member/**").authenticated()
			.antMatchers("/order/**").authenticated()
			.antMatchers("/product/insert**").authenticated()
			.antMatchers("/event/eventdetail/**").authenticated()
			.antMatchers("/**").permitAll();
		
		// 권한 없음(403)일 경우 이동할 경로 설정
		http.exceptionHandling().accessDeniedPage("/security/accessDenied");

		// CSRF 설정
		http.csrf().disable();
	}

	@Resource
	private DataSource dataSource;

	@Resource // 밑에 bean객체를 주입해 놓았다.
	private PasswordEncoder passwordEncoder;

	@Resource
	private CustomUserDetailsService customUserDetailsService;

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		log.info("AuthenticationManagerBuilder 실행");

//		    auth.jdbcAuthentication()
//			// 데이터베이스에서 가져올 사용자 정보 설정
//			.dataSource(dataSource) // 데이터베이스 연결 정보
//			.usersByUsernameQuery("SELECT * FROM member WHERE mid=?") //
//			.authoritiesByUsernameQuery("SELECT * FROM member WHERE mid=?") //
//			// 패스워드 인코딩 방법 설정
//			.passwordEncoder(passwordEncoder) // 기본 -> DelegatingPasswordEncoder
//			;

		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(customUserDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		auth.authenticationProvider(provider);
	}

	@Resource // 밑에 bean객체를 주입해 놓았다.
	private RoleHierarchyImpl roleHierarchyImpl;

	@Override
	public void configure(WebSecurity web) throws Exception {
		log.info("WebSecurity 실행");

		// 권한 계층 관계 설정
		DefaultWebSecurityExpressionHandler handler = new DefaultWebSecurityExpressionHandler();
		handler.setRoleHierarchy(roleHierarchyImpl);
		web.expressionHandler(handler);

		// 인증 절차가 필요없는 경로 설정
		web.ignoring().antMatchers("/bootstrap-4.6.0-dist/**").antMatchers("/css/**").antMatchers("/images/**")
				.antMatchers("/jquery/**").antMatchers("/favicon.ico");
	}

	@Bean // 함수가 실행하고 리턴되는 값을 관리객체로 만들어준다.
	public PasswordEncoder passwordEncoder() {

		// defult 값
		PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

		// 다른 encoder를 사용할 때
		// PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		return passwordEncoder;
	}

	@Bean // 권한계층을 확인할 때 HttpSecurity가 사용하므로 관리객체로 주입해야 한다.
	public RoleHierarchyImpl roleHierarchyImpl() {
		RoleHierarchyImpl roleHierarchyImpl = new RoleHierarchyImpl();
		roleHierarchyImpl.setHierarchy("ROLE_ADMIN > ROLE_MANAGER > ROLE_USER");
		return roleHierarchyImpl;
	}
}
