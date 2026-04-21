import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '',       pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'login',      loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
  { path: 'forbidden',  loadComponent: () => import('./features/auth/forbidden.component').then(m => m.ForbiddenComponent) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then(m => m.ShellComponent),
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'cases',     loadComponent: () => import('./features/cases/case-list.component').then(m => m.CaseListComponent) },
      { path: 'cases/new',
        canActivate: [roleGuard('AGENT', 'SUPERVISOR', 'SYSTEM_ADMIN')],
        loadComponent: () => import('./features/cases/case-create.component').then(m => m.CaseCreateComponent) },
      { path: 'cases/:id', loadComponent: () => import('./features/cases/case-detail.component').then(m => m.CaseDetailComponent) },
      { path: 'search',    loadComponent: () => import('./features/search/search.component').then(m => m.SearchComponent) },
      { path: 'audit',
        canActivate: [roleGuard('AUDITOR', 'SYSTEM_ADMIN')],
        loadComponent: () => import('./features/audit/audit.component').then(m => m.AuditComponent) }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
