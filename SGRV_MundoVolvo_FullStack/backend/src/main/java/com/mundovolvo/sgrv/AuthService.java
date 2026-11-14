package com.mundovolvo.sgrv;

import java.security.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements CommandLineRunner {
 private final Db db;private final PasswordEncoder passwords;private final String bootstrapUser,bootstrapPassword;
 private final SecureRandom random=new SecureRandom();
 AuthService(Db db,PasswordEncoder passwords,@Value("${sgrv.bootstrap.username}") String user,@Value("${sgrv.bootstrap.password}") String pass){this.db=db;this.passwords=passwords;bootstrapUser=user;bootstrapPassword=pass;}
 @Override public void run(String...args){
  Long count=db.jdbc.queryForObject("SELECT count(*) FROM app_users",Long.class);
  if(count!=null&&count==0){
   if(bootstrapUser.isBlank()||bootstrapPassword.length()<12)throw new IllegalStateException("Se requiere BOOTSTRAP_ADMIN_USER y BOOTSTRAP_ADMIN_PASSWORD (mínimo 12 caracteres) para el primer inicio");
   db.exec("INSERT INTO app_users(username,full_name,password_hash,role) VALUES(?,?,?,'ADMIN')",bootstrapUser,"Administrador inicial",passwords.encode(bootstrapPassword));
  }
 }
 public static String sha(String value){try{byte[] sum=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));return HexFormat.of().formatHex(sum);}catch(Exception e){throw new IllegalStateException(e);}}
 @Transactional public Map<String,Object> login(String username,String password){
  Map<String,Object> user=db.optional("SELECT id,username,full_name,role,active,password_hash FROM app_users WHERE lower(username)=lower(?)",username);
  if(user==null||!Boolean.TRUE.equals(user.get("active"))||!passwords.matches(password,(String)user.get("password_hash")))throw new ApiException(HttpStatus.UNAUTHORIZED,"Usuario o contraseña incorrectos");
  byte[] bytes=new byte[32];random.nextBytes(bytes);String token=HexFormat.of().formatHex(bytes);
  db.exec("INSERT INTO auth_sessions(token_hash,user_id,expires_at) VALUES(?,?,now()+interval '12 hours')",sha(token),user.get("id"));
  user.remove("password_hash");return Map.of("token",token,"user",user);
 }
 public Map<String,Object> findSession(String token){
  if(token==null||token.length()!=64)return null;
  return db.optional("SELECT u.id,u.username,u.full_name,u.role,u.active FROM auth_sessions s JOIN app_users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>now() AND u.active=true",sha(token));
 }
 public static Map<String,Object> actor(){Authentication a=SecurityContextHolder.getContext().getAuthentication();if(a==null||!(a.getPrincipal() instanceof Map))throw new ApiException(HttpStatus.UNAUTHORIZED,"Sesión requerida");return (Map<String,Object>)a.getPrincipal();}
 public static long uid(){return Db.longVal(actor().get("id"));}
 @Transactional public void logout(String token){if(token!=null&&token.length()==64)db.exec("DELETE FROM auth_sessions WHERE token_hash=?",sha(token));}
}
