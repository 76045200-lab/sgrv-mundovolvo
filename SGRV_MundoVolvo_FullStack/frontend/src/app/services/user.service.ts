import { Injectable,inject } from '@angular/core';import {ApiService} from './api.service';
@Injectable({providedIn:'root'})export class UserService{
 private api=inject(ApiService);
 list(){return this.api.get<any[]>('/users')}
 create(p:any){return this.api.post('/users',p)}
 update(id:number,p:any){return this.api.put(`/users/${id}`,p)}
}
