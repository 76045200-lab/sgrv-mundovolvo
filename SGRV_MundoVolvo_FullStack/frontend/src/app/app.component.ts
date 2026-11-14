import { Component,HostListener,inject,signal,OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';import { FormsModule } from '@angular/forms';
import { AuthService } from './services/auth.service';
import { DashboardPage } from './pages/dashboard.page';
import { InventoryPage } from './pages/inventory.page';
import { QuotesPage } from './pages/quotes.page';
import { SalesPage } from './pages/sales.page';
import { PurchasesPage } from './pages/purchases.page';
import { ContactsPage } from './pages/contacts.page';
import { UsersPage } from './pages/users.page';
import { ReportsPage } from './pages/reports.page';
import { SettingsPage } from './pages/settings.page';

@Component({selector:'app-root',standalone:true,imports:[CommonModule,FormsModule,DashboardPage,InventoryPage,QuotesPage,SalesPage,PurchasesPage,ContactsPage,UsersPage,ReportsPage,SettingsPage],
template:`
@if(!auth.user()){
 <div class="min-h-screen bg-[#08192e] flex items-center justify-center px-4 relative overflow-hidden">
  <div class="absolute -top-40 -right-44 w-[600px] h-[600px] rounded-full bg-sky-800/20 blur-3xl"></div>
  <div class="w-full max-w-md bg-white p-8 sm:p-10 rounded-3xl shadow-2xl relative">
   <div class="flex items-center gap-3 mb-8"><span class="flex items-center justify-center w-12 h-12 bg-[#0c1d34] rounded-xl text-white text-2xl font-black">M</span><div><p class="font-extrabold text-xl text-slate-900">Mundo Volvo</p><p class="text-xs text-slate-500 tracking-[.14em]">SGRV · SISTEMA DE GESTIÓN</p></div></div>
   <h1 class="text-3xl font-bold text-slate-800">Bienvenido</h1><p class="text-slate-500 mt-2 mb-7 text-sm">Ingresa con tu cuenta autorizada para acceder al sistema.</p>
   @if(loginError){<div class="notice-error">{{loginError}}</div>}
   <form (ngSubmit)="login()" class="space-y-4"><label class="block"><span class="field-label">Usuario</span><input class="field" name="username" [(ngModel)]="username" required autocomplete="username" placeholder="Nombre de usuario" /></label>
   <label class="block"><span class="field-label">Contraseña</span><input class="field" name="password" type="password" [(ngModel)]="password" required autocomplete="current-password" placeholder="••••••••••••" /></label>
   <button class="btn-primary w-full mt-3" [disabled]="logging||!username||!password">{{logging?'Verificando...':'Ingresar al SGRV'}}</button></form>
   <p class="mt-8 text-xs text-center text-slate-400">Sistema conectado a PostgreSQL · Acceso protegido</p>
  </div>
 </div>
}@else{
 <div class="min-h-screen flex">
  @if(mobileOpen){<div class="fixed inset-0 bg-slate-950/60 z-30 lg:hidden" (click)="mobileOpen=false"></div>}
  <aside class="no-print fixed inset-y-0 left-0 z-40 w-64 bg-[#0c1d34] text-slate-200 flex flex-col transform transition-transform lg:translate-x-0" [ngClass]="!mobileOpen ? '-translate-x-full' : 'translate-x-0'">
   <div class="px-6 py-7 border-b border-white/10 flex items-center gap-3"><div class="bg-white text-[#0c1d34] font-black rounded-xl w-10 h-10 flex items-center justify-center text-xl">M</div><div><div class="font-extrabold text-white text-lg">Mundo Volvo</div><div class="text-[10px] tracking-[.16em] text-slate-400">SISTEMA SGRV</div></div></div>
   <nav class="px-3 py-5 flex-1 space-y-1 overflow-auto">
    @for(item of nav;track item.key){@if(auth.can(item.key)){
     <button (click)="go(item.key)" class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-left text-sm transition" [ngClass]="page===item.key?'bg-[#19699a] text-white shadow-sm':'text-slate-300 hover:bg-white/10'">
      <span class="text-lg">{{item.icon}}</span><span>{{item.label}}</span>
     </button>
    }}
   </nav>
   <div class="px-5 py-5 border-t border-white/10"><p class="text-sm font-semibold text-white truncate">{{auth.user()?.full_name}}</p><p class="text-xs text-slate-400 mt-0.5">{{roleLabel}}</p><button (click)="auth.logout()" class="text-sm text-red-200 mt-4 hover:text-white">⏻ Cerrar sesión</button></div>
  </aside>
  <div class="flex-1 min-w-0 lg:ml-64">
   <header class="no-print h-20 bg-white border-b border-slate-200 flex items-center justify-between px-4 sm:px-8 sticky top-0 z-20">
    <div class="flex items-center gap-3"><button class="lg:hidden text-2xl text-slate-700" (click)="mobileOpen=true" aria-label="Abrir menú">☰</button><div><div class="text-[11px] uppercase tracking-widest text-slate-400">Panel de administración</div><div class="text-base font-semibold text-slate-800">{{currentLabel}}</div></div></div>
    <div class="flex items-center gap-3 text-sm text-slate-500"><span class="hidden sm:inline">{{today | date:'dd/MM/yyyy'}}</span><span class="bg-slate-100 rounded-full w-10 h-10 flex items-center justify-center font-bold text-slate-700">{{auth.user()?.full_name?.charAt(0)}}</span></div>
   </header>
   <main class="p-4 sm:p-8 max-w-[1500px] mx-auto">
    @switch(page){
     @case('dashboard'){<app-dashboard/>}
     @case('inventory'){<app-inventory/>}
     @case('quotes'){<app-quotes/>}
     @case('sales'){<app-sales/>}
     @case('purchases'){<app-purchases/>}
     @case('customers'){<app-contacts kind="customers"/>}
     @case('suppliers'){<app-contacts kind="suppliers"/>}
     @case('users'){<app-users/>}
     @case('reports'){<app-reports/>}
     @case('settings'){<app-settings/>}
    }
   </main>
  </div>
 </div>
}
`})
export class AppComponent implements OnInit {
 auth=inject(AuthService);page='dashboard';mobileOpen=false;today=new Date();username='';password='';logging=false;loginError='';
 nav=[{key:'dashboard',label:'Resumen general',icon:'▦'},{key:'inventory',label:'Inventario',icon:'▤'},{key:'quotes',label:'Cotizaciones',icon:'▧'},{key:'sales',label:'Ventas',icon:'◈'},{key:'purchases',label:'Compras',icon:'▣'},{key:'customers',label:'Clientes',icon:'◉'},{key:'suppliers',label:'Proveedores',icon:'◍'},{key:'reports',label:'Reportes',icon:'◫'},{key:'users',label:'Usuarios y permisos',icon:'♙'},{key:'settings',label:'Configuración',icon:'⚙'}];
 get currentLabel(){return this.nav.find(n=>n.key===this.page)?.label||'SGRV'}
 get roleLabel(){return ({ADMIN:'Administrador',SELLER:'Vendedor',WAREHOUSE:'Almacén'} as any)[this.auth.user()?.role||'']}
 ngOnInit(){this.auth.verify();window.addEventListener('sgrv-logout',()=>this.auth.clear());}
 async login(){this.logging=true;this.loginError='';try{await this.auth.login(this.username,this.password);this.password='';this.go('dashboard')}catch(e:any){this.loginError=e.message}finally{this.logging=false}}
 go(page:string){if(this.auth.can(page)){this.page=page;this.mobileOpen=false;}}
}
