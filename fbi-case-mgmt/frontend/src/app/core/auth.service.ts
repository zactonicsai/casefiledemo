import { Injectable, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';

export interface UserClaims {
  username: string;
  roles: string[];
  email?: string;
  fullName?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly user = signal<UserClaims | null>(null);

  constructor(private oauth: OAuthService) {
    this.refreshFromToken();
    this.oauth.events.subscribe(() => this.refreshFromToken());
  }

  login(): void          { this.oauth.initCodeFlow(); }
  logout(): void         { this.oauth.logOut(); }
  isLoggedIn(): boolean  { return this.oauth.hasValidAccessToken(); }

  hasRole(role: string): boolean {
    const r = role.startsWith('ROLE_') ? role : 'ROLE_' + role;
    return this.user()?.roles?.includes(r) ?? false;
  }
  hasAnyRole(...roles: string[]): boolean { return roles.some(r => this.hasRole(r)); }

  private refreshFromToken(): void {
    if (!this.oauth.hasValidAccessToken()) { this.user.set(null); return; }
    const claims = this.oauth.getIdentityClaims() as any;
    if (!claims) return;
    const realmAccess = claims.realm_access || {};
    this.user.set({
      username: claims.preferred_username,
      email:    claims.email,
      fullName: claims.name,
      roles:    (realmAccess.roles || []).map((r: string) =>
                   r.startsWith('ROLE_') ? r : 'ROLE_' + r)
    });
  }
}
