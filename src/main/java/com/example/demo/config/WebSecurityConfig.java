package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.demo.service.UserDetailsServiceImpl;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        // Set the custom user details service and password encoder
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());

    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Disable CSRF (enable it in production if necessary)
        http.csrf().disable();

        // Define access rules for different roles
        http.authorizeRequests().antMatchers("/admin/orderList", "/admin/order", "/admin/accountInfo")//
                .access("hasAnyRole('ROLE_EMPLOYEE', 'ROLE_MANAGER')");

        // Pages only for MANAGER
        http.authorizeRequests().antMatchers("/admin/product").access("hasRole('ROLE_MANAGER')");

        // Handle access denied exceptions
        http.exceptionHandling().accessDeniedPage("/403");

        // When user login, role XX.
        // But access to the page requires the YY role,
        // An AccessDeniedException will be thrown.
        http.authorizeRequests().and().exceptionHandling().accessDeniedPage("/403");

        // Configure login form
        http.formLogin()
                .loginProcessingUrl("/j_spring_security_check") // URL to process login requests
                .loginPage("/admin/login") // Custom login page
                .defaultSuccessUrl("/admin/accountInfo") // Redirect after successful login
                .failureUrl("/admin/login?error=true") // Redirect after failed login
                .usernameParameter("userName") // Username input field name
                .passwordParameter("password"); // Password input field name

        // Configure logout functionality
        http.logout().logoutUrl("/admin/logout") // URL to trigger logout
                .logoutSuccessUrl("/"); // Redirect after successful logout

    }
}
