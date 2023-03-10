package cn.huanzi.qch.baseadmin.config.security;

import cn.huanzi.qch.baseadmin.sys.sysauthority.service.SysAuthorityService;
import cn.huanzi.qch.baseadmin.sys.sysauthority.vo.SysAuthorityVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private CaptchaFilterConfig captchaFilterConfig;

    @Autowired
    private UserConfig userConfig;

    @Autowired
    private PasswordConfig passwordConfig;

    @Autowired
    private LoginFailureHandlerConfig loginFailureHandlerConfig;

    @Autowired
    private LoginSuccessHandlerConfig loginSuccessHandlerConfig;

    @Autowired
    private LogoutHandlerConfig logoutHandlerConfig;

    @Autowired
    private SysAuthorityService sysAuthorityService;

    @Autowired
    private MyFilterInvocationSecurityMetadataSource myFilterInvocationSecurityMetadataSource;

    @Autowired
    private MyInvalidSessionStrategy myInvalidSessionStrategy;

    @Autowired
    private DataSource dataSource;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
                //??????????????????
                .userDetailsService(userConfig)
                //????????????
                .passwordEncoder(passwordConfig);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // ??????csrf??????
                .csrf().disable()
                .headers().frameOptions().disable()
                .and();

        http
                //????????????
                .addFilterBefore(captchaFilterConfig, UsernamePasswordAuthenticationFilter.class)
                .formLogin()
                .loginProcessingUrl("/login")
                //??????????????????????????????
                .loginPage("/loginPage")
                .failureHandler(loginFailureHandlerConfig)
                .successHandler(loginSuccessHandlerConfig)
                .permitAll()
                .and();
        http
                //????????????
                .logout()
                .addLogoutHandler(logoutHandlerConfig)
                .logoutUrl("/logout")
                .logoutSuccessUrl("/loginPage")
                .permitAll()
                .and();
        http
                //??????url?????????????????????????????????????????????https://www.jianshu.com/p/0a06496e75ea
                .addFilterAfter(dynamicallyUrlInterceptor(), FilterSecurityInterceptor.class)
                .authorizeRequests()

                //??????????????????
                .antMatchers("/favicon.ico","/common/**", "/webjars/**", "/getVerifyCodeImage").permitAll()

                //???????????????????????????????????????
                .anyRequest().authenticated()
                .and();

        http.sessionManagement()
                //session??????????????????
                .invalidSessionStrategy(myInvalidSessionStrategy)
                .and();

        http
                //???????????????
                .rememberMe()
                .tokenValiditySeconds(604800)//???????????????
                .tokenRepository(persistentTokenRepository())
                .userDetailsService(userConfig)
                .and();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl persistentTokenRepository = new JdbcTokenRepositoryImpl();
        persistentTokenRepository.setDataSource(dataSource);
        return persistentTokenRepository;
    }

    //??????filter
    @Bean
    public DynamicallyUrlInterceptor dynamicallyUrlInterceptor(){
        //????????????
        List<SysAuthorityVo> authorityVoList = sysAuthorityService.list(new SysAuthorityVo()).getData();
        myFilterInvocationSecurityMetadataSource.setRequestMap(authorityVoList);
        //????????????????????????????????????????????????????????????new?????????????????????spring?????????spring???????????????
        DynamicallyUrlInterceptor interceptor = new DynamicallyUrlInterceptor();
        interceptor.setSecurityMetadataSource(myFilterInvocationSecurityMetadataSource);

        //??????RoleVoter??????
        List<AccessDecisionVoter<? extends Object>> decisionVoters = new ArrayList<>();
        decisionVoters.add(new RoleVoter());

        //???????????????????????????
        interceptor.setAccessDecisionManager(new MyAccessDecisionManager(decisionVoters));
        return interceptor;
    }
}
