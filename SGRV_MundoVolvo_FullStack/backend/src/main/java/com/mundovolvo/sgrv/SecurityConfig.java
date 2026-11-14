package com.mundovolvo.sgrv;

import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean SecurityFilterChain chain(HttpSecurity http,TokenFilter filter,@Value("${sgrv.allowed-origin}") String origin) throws Exception {
  return http.csrf(c->c.disable())
   .cors(c->c.configurationSource(req->{CorsConfiguration cfg=new CorsConfiguration();cfg.setAllowedOrigins(List.of(origin));cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));cfg.setAllowedHeaders(List.of("Authorization","Content-Type"));return cfg;}))
   .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(c->c.requestMatchers("/api/auth/login","/actuator/health").permitAll().anyRequest().authenticated())
   .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
   .exceptionHandling(c->c.authenticationEntryPoint((req,res,e)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"error\":\"No autenticado\"}");})
    .accessDeniedHandler((req,res,e)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Permiso insuficiente\"}");}))
   .build();
 }
}
@Component
class TokenFilter extends OncePerRequestFilter {
 private final AuthService auth;
 TokenFilter(AuthService auth){this.auth=auth;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws java.io.IOException, ServletException {
  String h=req.getHeader("Authorization");
  if(h!=null&&h.startsWith("Bearer ")) {
   Map<String,Object> u=auth.findSession(h.substring(7));
   if(u!=null){
    String role=u.get("role").toString();
    Authentication a=new UsernamePasswordAuthenticationToken(u, null, List.of(new SimpleGrantedAuthority("ROLE_"+role)));
    SecurityContextHolder.getContext().setAuthentication(a);
   }
  }
  try{chain.doFilter(req,res);}finally{SecurityContextHolder.clearContext();}
 }
}
