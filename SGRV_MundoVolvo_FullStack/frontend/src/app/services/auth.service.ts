import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
export type Role='ADMIN'|'SELLER'|'WAREHOUSE';
export interface User{ id:number;username:string;full_name:string;role:Role;active:boolean;}
@Injectable({providedIn:'root'})
export class AuthService{
 private api=inject(ApiService);
 user=signal<User|null>(JSON.parse(localStorage.getItem('sgrv_user')||'null'));
 async login(username:string,password:string){const result=await this.api.post<{token:string,user:User}>('/auth/login',{username,password});localStorage.setItem('sgrv_token',result.token);localStorage.setItem('sgrv_user',JSON.stringify(result.user));this.user.set(result.user);}
 async verify(){if(!localStorage.getItem('sgrv_token'))return;try{const me=await this.api.get<User>('/auth/me');this.user.set(me);}catch{this.clear();}}
 async logout(){try{await this.api.post('/auth/logout',{});}catch{}this.clear();}
 clear(){this.user.set(null);localStorage.removeItem('sgrv_token');localStorage.removeItem('sgrv_user');}
 can(section:string){const role=this.user()?.role;if(!role)return false;if(role==='ADMIN')return true;
  if(role==='SELLER')return ['dashboard','inventory','customers','quotes','sales','reports'].includes(section);
  return ['dashboard','inventory','purchases','stock'].includes(section);
 }
 canEditInventory(){return this.user()?.role==='ADMIN'||this.user()?.role==='WAREHOUSE';}
}
