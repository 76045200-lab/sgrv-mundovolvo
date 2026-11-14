import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req,next)=>{
 const token=localStorage.getItem('sgrv_token');
 return next(token?req.clone({setHeaders:{Authorization:`Bearer ${token}`}}):req);
};
@Injectable({providedIn:'root'})
export class ApiService{
 private http=inject(HttpClient);
 private path(url:string){return '/api'+url;}
 private async run<T>(request:any):Promise<T>{
  try{return await firstValueFrom(request) as T;}catch(e:any){
   if(e instanceof HttpErrorResponse){if(e.status===401&& !e.url?.endsWith('/auth/login')){localStorage.removeItem('sgrv_token');localStorage.removeItem('sgrv_user');window.dispatchEvent(new Event('sgrv-logout'));}
    throw new Error(e.error?.error||`Error del servidor (${e.status||'sin conexión'})`);}
   throw e;
  }
 }
 get<T=any>(url:string):Promise<T>{return this.run(this.http.get<T>(this.path(url)));}
 post<T=any>(url:string,p:any):Promise<T>{return this.run(this.http.post<T>(this.path(url),p));}
 put<T=any>(url:string,p:any):Promise<T>{return this.run(this.http.put<T>(this.path(url),p));}
 patch<T=any>(url:string,p:any):Promise<T>{return this.run(this.http.patch<T>(this.path(url),p));}
 delete<T=any>(url:string):Promise<T>{return this.run(this.http.delete<T>(this.path(url)));}
}
