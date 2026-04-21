import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [RouterLink, MatButtonModule],
  template: `
    <div class="page" style="text-align:center; padding-top:80px">
      <h1>403 — Access Denied</h1>
      <p class="subtitle">You do not have permission to view this resource.</p>
      <a mat-raised-button color="primary" routerLink="/dashboard">Back to dashboard</a>
    </div>
  `
})
export class ForbiddenComponent {}
