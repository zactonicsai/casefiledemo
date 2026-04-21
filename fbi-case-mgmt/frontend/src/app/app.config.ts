import { ApplicationConfig, APP_INITIALIZER, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { OAuthService, provideOAuthClient, AuthConfig } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { authInterceptor } from './core/auth.interceptor';

function initOAuth(oauth: OAuthService) {
  return async () => {
    const cfg: AuthConfig = {
      issuer:       environment.auth.issuer,
      clientId:     environment.auth.clientId,
      redirectUri:  window.location.origin,
      responseType: environment.auth.responseType,
      scope:        environment.auth.scope,
      requireHttps: environment.auth.requireHttps,
      showDebugInformation: false
    };
    oauth.configure(cfg);
    try {
      await oauth.loadDiscoveryDocumentAndTryLogin();
      if (oauth.hasValidAccessToken()) {
        oauth.setupAutomaticSilentRefresh();
      }
    } catch (e) {
      console.warn('OIDC init failed:', e);
    }
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideAnimations(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideOAuthClient(),
    {
      provide: APP_INITIALIZER,
      useFactory: initOAuth,
      deps: [OAuthService],
      multi: true
    }
  ]
};
