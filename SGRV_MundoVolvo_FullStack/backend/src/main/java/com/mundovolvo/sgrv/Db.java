package com.mundovolvo.sgrv;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;

@Component
public class Db {
 public final JdbcTemplate jdbc;
 public Db(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 public List<Map<String,Object>> rows(String sql, Object... args) {return jdbc.queryForList(sql,args);}
 public Map<String,Object> one(String sql, Object... args) {
  List<Map<String,Object>> r=rows(sql,args);
  if(r.isEmpty()) throw ApiException.notFound("Registro");
  return r.getFirst();
 }
 public Map<String,Object> optional(String sql,Object...args){List<Map<String,Object>> r=rows(sql,args);return r.isEmpty()?null:r.getFirst();}
 public long id(String sql,Object...args){return jdbc.queryForObject(sql,Long.class,args);}
 public int exec(String sql,Object...args){return jdbc.update(sql,args);}
 public BigDecimal num(String sql,Object...args){return jdbc.queryForObject(sql,BigDecimal.class,args);}
 public void ensure(boolean cond,String msg){if(!cond)throw ApiException.bad(msg);}
 public void audit(long user,String action,String type,long id,String details){
  jdbc.update("INSERT INTO audit_logs(user_id,action,entity_type,entity_id,details) VALUES(?,?,?,?,?)",user,action,type,id,details);
 }
 public static long longVal(Object v){if(v==null)throw ApiException.bad("Identificador obligatorio");try{return Long.parseLong(v.toString());}catch(Exception e){throw ApiException.bad("Identificador inválido");}}
 public static BigDecimal decimal(Object v,String field){try{if(v==null)throw new Exception();return new BigDecimal(v.toString());}catch(Exception e){throw ApiException.bad(field+" inválido");}}
 public static String text(Object v,String field,int max){String s=v==null?"":v.toString().trim();if(s.isBlank()||s.length()>max)throw ApiException.bad(field+" obligatorio (máximo "+max+" caracteres)");return s;}
 public static String nullable(Object v){String s=v==null?"":v.toString().trim();return s.isEmpty()?null:s;}
 public static Map<String,Object> requireMap(Map<String,Object> input){if(input==null)throw ApiException.bad("Cuerpo obligatorio");return input;}
}
