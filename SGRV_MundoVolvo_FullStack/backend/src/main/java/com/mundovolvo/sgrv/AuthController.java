package com.mundovolvo.sgrv;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api")
public class AuthController {
 private final AuthService auth;private final Db db;private final PasswordEncoder password;
 AuthController(AuthService auth,Db db,PasswordEncoder password){this.auth=auth;this.db=db;this.password=password;}
 @PostMapping("/auth/login") public Map<String,Object> login(@RequestBody Map<String,Object> p){return auth.login(Db.text(p.get("username"),"Usuario",80),Db.text(p.get("password"),"Contraseña",200));}
 @GetMapping("/auth/me") public Map<String,Object> me(){return AuthService.actor();}
 @PostMapping("/auth/logout") public Map<String,String> logout(@RequestHeader(value="Authorization",required=false) String header){auth.logout(header==null?null:header.replace("Bearer ",""));return Map.of("message","Sesión finalizada");}
 @GetMapping("/users") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> users(){return db.rows("SELECT id,username,full_name,role,active,created_at FROM app_users ORDER BY id");}
 @PostMapping("/users") @PreAuthorize("hasRole('ADMIN')") @Transactional public Map<String,Object> add(@RequestBody Map<String,Object> p){
  String role=role(p.get("role"));String pass=Db.text(p.get("password"),"Contraseña",120);if(pass.length()<12)throw ApiException.bad("Contraseña mínima: 12 caracteres");
  long id=db.id("INSERT INTO app_users(username,full_name,password_hash,role,active) VALUES(?,?,?,?,true) RETURNING id",Db.text(p.get("username"),"Usuario",80),Db.text(p.get("fullName"),"Nombre",160),password.encode(pass),role);
  db.audit(AuthService.uid(),"CREATE","USER",id,role);return db.one("SELECT id,username,full_name,role,active FROM app_users WHERE id=?",id);
 }
 @PutMapping("/users/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional public Map<String,Object> update(@PathVariable long id,@RequestBody Map<String,Object> p){
  String role=role(p.get("role"));boolean active=Boolean.TRUE.equals(p.get("active"));
  if(id==AuthService.uid()&&(!active||!"ADMIN".equals(role)))throw ApiException.bad("No puedes desactivar tu propia cuenta administrativa");
  int result=db.exec("UPDATE app_users SET full_name=?,role=?,active=? WHERE id=?",Db.text(p.get("fullName"),"Nombre",160),role,active,id);if(result==0)throw ApiException.notFound("Usuario");
  if(p.get("password")!=null&&!p.get("password").toString().isBlank()){String pass=Db.text(p.get("password"),"Contraseña",120);if(pass.length()<12)throw ApiException.bad("Contraseña mínima: 12 caracteres");db.exec("UPDATE app_users SET password_hash=? WHERE id=?",password.encode(pass),id);}
  if(!active)db.exec("DELETE FROM auth_sessions WHERE user_id=?",id);
  db.audit(AuthService.uid(),"UPDATE","USER",id,role);return db.one("SELECT id,username,full_name,role,active FROM app_users WHERE id=?",id);
 }
 private static String role(Object v){String s=Db.text(v,"Rol",20);if(!Set.of("ADMIN","SELLER","WAREHOUSE").contains(s))throw ApiException.bad("Rol no permitido");return s;}
}
